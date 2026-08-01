from calibre.gui2.actions import InterfaceAction

try:
    from qt.core import QMessageBox
except ImportError:
    from PyQt5.QtWidgets import QMessageBox


class GentleInkInterfaceAction(InterfaceAction):
    name = "GentleInk Language Filter"
    action_spec = (
        "GentleInk Clean",
        None,
        "Clean profanity from selected books",
        None,
    )
    action_type = "current"

    def genesis(self):
        self.qaction.triggered.connect(self.run_clean)

    def run_clean(self):
        from .action import clean_selected_books

        db = getattr(self.gui.current_db, "new_api", self.gui.current_db)
        ids = self.gui.library_view.get_selected_ids()
        if not ids:
            QMessageBox.warning(self.gui, "GentleInk", "Select one or more books first.")
            return

        reply = QMessageBox.question(
            self.gui,
            "GentleInk",
            f"Clean profanity from {len(ids)} book(s)?\n\n"
            "Original files are backed up as ORIGINAL_EPUB (or ORIGINAL_AZW3).",
            QMessageBox.StandardButton.Yes | QMessageBox.StandardButton.No,
        )
        if reply != QMessageBox.StandardButton.Yes:
            return

        results = clean_selected_books(db, ids, self.gui)
        ok = sum(1 for r in results if r.get("ok"))
        fail = len(results) - ok
        lines = [f"Cleaned: {ok}", f"Failed: {fail}"]
        for r in results:
            if not r.get("ok"):
                lines.append(f"{r.get('title', 'Book')}: {r.get('error', 'Unknown error')}")
            elif r.get("changed", 0) == 0:
                lines.append(f"{r.get('title', 'Book')}: no profanity found ({r.get('format', 'epub').upper()})")
        QMessageBox.information(
            self.gui,
            "GentleInk complete",
            "\n".join(lines),
        )
