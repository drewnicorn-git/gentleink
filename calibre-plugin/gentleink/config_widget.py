from PyQt5.QtWidgets import QComboBox, QFormLayout, QLabel, QVBoxLayout, QWidget

from .config import plugin_prefs
from .filter_engine import GentleInkFilter


class ConfigWidget(QWidget):
    def __init__(self):
        super().__init__()
        layout = QVBoxLayout(self)
        form = QFormLayout()

        self.mode = QComboBox()
        self.mode.addItems(["substitute", "mask", "remove"])
        form.addRow("Filter mode", self.mode)

        self.profile = QComboBox()
        self.profile.addItems(["family", "religious_strict"])
        form.addRow("Profile", self.profile)

        layout.addLayout(form)
        layout.addWidget(QLabel(
            "Select books in your library and click 'GentleInk Clean' on the toolbar.\n"
            "Originals are backed up as ORIGINAL_EPUB."
        ))

        self.filter = GentleInkFilter()

    def save_settings(self):
        plugin_prefs["mode"] = self.mode.currentText()
        plugin_prefs["profile"] = self.profile.currentText()

    def restore_settings(self):
        self.mode.setCurrentText(plugin_prefs.get("mode", "substitute"))
        self.profile.setCurrentText(plugin_prefs.get("profile", "family"))
