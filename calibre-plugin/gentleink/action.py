"""Batch-clean EPUB/AZW3 HTML content using GentleInk filter."""

from __future__ import annotations

import os
import re
import shutil
import tempfile
import zipfile
from pathlib import Path

from calibre_plugins.gentleink.filter_engine import GentleInkFilter

HTML_EXT = {".xhtml", ".html", ".htm"}


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
    backup_fmt = f"ORIGINAL_{fmt.upper()}"
    if fmt in db.formats(book_id):
        if backup_fmt not in db.formats(book_id):
            path = db.format(book_id, fmt)
            with open(path, "rb") as f:
                db.add_format(book_id, backup_fmt, f, replace=True)


def clean_selected_books(db, book_ids, gui=None) -> list[dict]:
    from calibre_plugins.gentleink.config import plugin_prefs

    mode = plugin_prefs.get("mode", "substitute")
    profile = plugin_prefs.get("profile", "family")
    engine = GentleInkFilter(_resolve_data_dir())

    results = []
    for book_id in book_ids:
        title = db.title(book_id, index_is_id=True)
        try:
            formats = [f for f in db.formats(book_id, verify_exists=True) if f.lower() in ("epub", "azw3")]
            if not formats:
                results.append({"id": book_id, "title": title, "ok": False, "error": "No EPUB/AZW3"})
                continue

            for fmt in formats:
                backup_original(db, book_id, fmt)
                path = db.format(book_id, fmt)
                changed = clean_epub_file(path, engine, mode, profile)
                results.append({"id": book_id, "title": title, "ok": True, "changed": changed, "format": fmt})
        except Exception as exc:
            results.append({"id": book_id, "title": title, "ok": False, "error": str(exc)})

    if gui is not None:
        gui.library_view.model().refresh_ids(book_ids)
    return results


def _resolve_data_dir() -> Path:
    bundled = Path(__file__).resolve().parent / "core_data"
    if bundled.exists():
        return bundled
    repo = Path(__file__).resolve().parents[2] / "core" / "data"
    return repo
