"""Batch-clean EPUB/AZW3 HTML content using GentleInk filter."""

from __future__ import annotations

import os
import re
import shutil
import tempfile
import zipfile
from pathlib import Path

from .filter_engine import GentleInkFilter

HTML_EXT = {".xhtml", ".html", ".htm"}
FORMAT_PREFERENCE = ("epub", "azw3")


def _library_api(db):
    """Prefer new_api (book ids); fall back to legacy LibraryDatabase."""
    return getattr(db, "new_api", db)


def _book_title(db, book_id) -> str:
    api = _library_api(db)
    if hasattr(api, "field_for"):
        return api.field_for("title", book_id) or f"ID {book_id}"
    return db.title(book_id, index_is_id=True)


def _book_formats(db, book_id) -> list[str]:
    api = _library_api(db)
    if hasattr(api, "field_for"):
        return list(api.formats(book_id) or [])
    try:
        return list(db.formats(book_id, verify_exists=True, index_is_id=True))
    except TypeError:
        return list(db.formats(book_id, index_is_id=True))


def _pick_format(formats: list[str]) -> str | None:
    lower = {f.lower(): f for f in formats}
    for preferred in FORMAT_PREFERENCE:
        if preferred in lower:
            return lower[preferred]
    return None


def _filter_html(html: str, engine: GentleInkFilter, mode: str, profile: str) -> str:
    def transform(text: str) -> str:
        filtered, _ = engine.filter_text(text, mode=mode, profile=profile)
        return filtered

    pattern = re.compile(r">([^<]+)<")

    def replacer(match: re.Match) -> str:
        content = match.group(1)
        if not content.strip() or not any(c.isalpha() for c in content):
            return match.group(0)
        filtered = transform(content)
        if filtered == content:
            return match.group(0)
        return ">" + filtered + "<"

    return pattern.sub(replacer, html)


def clean_epub_file(path: str, engine: GentleInkFilter, mode: str = "substitute", profile: str = "family") -> int:
    if not zipfile.is_zipfile(path):
        raise ValueError(f"Not a zip-based ebook file: {path}")

    changed = 0
    tmp_fd, tmp_path = tempfile.mkstemp(suffix=".epub")
    os.close(tmp_fd)

    try:
        with zipfile.ZipFile(path, "r") as zin, zipfile.ZipFile(tmp_path, "w", compression=zipfile.ZIP_DEFLATED) as zout:
            for item in zin.infolist():
                data = zin.read(item.filename)
                ext = Path(item.filename).suffix.lower()
                if ext in HTML_EXT:
                    try:
                        html = data.decode("utf-8")
                    except UnicodeDecodeError:
                        html = data.decode("latin-1", errors="replace")
                    filtered = _filter_html(html, engine, mode, profile)
                    if filtered != html:
                        changed += 1
                        data = filtered.encode("utf-8")
                zout.writestr(item, data)
        shutil.move(tmp_path, path)
    except Exception:
        if os.path.exists(tmp_path):
            os.remove(tmp_path)
        raise

    return changed


def backup_original(db, book_id, fmt: str) -> None:
    api = _library_api(db)
    fmt_upper = fmt.upper()
    formats = _book_formats(db, book_id)
    if fmt_upper not in {f.upper() for f in formats}:
        return
    backup_fmt = f"ORIGINAL_{fmt_upper}"
    if backup_fmt in formats:
        return

    if hasattr(api, "save_original_format"):
        api.save_original_format(book_id, fmt_upper)
        return

    if hasattr(api, "field_for"):
        path = api.format(book_id, fmt)
    else:
        path = db.format(book_id, fmt, index_is_id=True)
    with open(path, "rb") as f:
        if hasattr(api, "field_for"):
            api.add_format(book_id, backup_fmt, f, replace=False)
        else:
            db.add_format(book_id, backup_fmt, f, replace=False, index_is_id=True)


def _export_format(db, book_id, fmt: str) -> str:
    api = _library_api(db)
    fd, tmp_path = tempfile.mkstemp(suffix=f".{fmt.lower()}")
    os.close(fd)
    fmt_upper = fmt.upper()
    if hasattr(api, "field_for"):
        api.copy_format_to(book_id, fmt_upper, tmp_path)
    else:
        db.copy_format_to(book_id, fmt_upper, tmp_path, index_is_id=True)
    return tmp_path


def _import_format(db, book_id, fmt: str, path: str) -> None:
    api = _library_api(db)
    fmt_upper = fmt.upper()
    if hasattr(api, "field_for"):
        api.add_format(book_id, fmt_upper, path, replace=True)
    else:
        with open(path, "rb") as f:
            db.add_format(book_id, fmt_upper, f, replace=True, index_is_id=True)


def clean_selected_books(db, book_ids, gui=None) -> list[dict]:
    from .config import plugin_prefs

    mode = plugin_prefs.get("mode", "substitute")
    profile = plugin_prefs.get("profile", "family")
    engine = GentleInkFilter()

    results = []
    for book_id in book_ids:
        title = _book_title(db, book_id)
        tmp_path = None
        try:
            fmt = _pick_format(_book_formats(db, book_id))
            if fmt is None:
                results.append({"id": book_id, "title": title, "ok": False, "error": "No EPUB or AZW3 format found"})
                continue

            backup_original(db, book_id, fmt)
            tmp_path = _export_format(db, book_id, fmt)
            changed = clean_epub_file(tmp_path, engine, mode, profile)
            _import_format(db, book_id, fmt, tmp_path)
            results.append({"id": book_id, "title": title, "ok": True, "changed": changed, "format": fmt})
        except Exception as exc:
            results.append({"id": book_id, "title": title, "ok": False, "error": str(exc)})
        finally:
            if tmp_path and os.path.exists(tmp_path):
                try:
                    os.remove(tmp_path)
                except OSError:
                    pass

    if gui is not None:
        gui.library_view.model().refresh_ids(book_ids)
    return results
