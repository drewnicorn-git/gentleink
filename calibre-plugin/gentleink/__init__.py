#!/usr/bin/env python
# -*- coding: utf-8 -*-

from calibre.customize import InterfaceActionBase


class GentleInkAction(InterfaceActionBase):
    name = "GentleInk Language Filter"
    action_type = "current"
    description = "Filter profanity from selected ebooks using GentleInk context-aware rules"
    supported_platforms = ["windows", "osx", "linux"]
    author = "GentleInk"
    version = (0, 1, 1)
    minimum_calibre_version = (6, 0, 0)

    def is_customizable(self):
        return True

    def config_widget(self):
        from .config_widget import ConfigWidget
        return ConfigWidget()

    def save_settings(self, config_widget):
        config_widget.save_settings()

    def restore_settings(self, config_widget):
        config_widget.restore_settings()

    def genesis(self):
        from qt.core import QAction
        self.qaction = QAction("GentleInk Clean", self.gui)
        self.qaction.setToolTip("Clean profanity from selected books")
        self.qaction.triggered.connect(self.run_clean)

    def run_clean(self):
        from qt.core import QMessageBox
        from .action import clean_selected_books

        db = self.gui.current_db
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
        QMessageBox.information(
            self.gui,
            "GentleInk complete",
            f"Cleaned: {ok}\nFailed: {fail}",
        )
