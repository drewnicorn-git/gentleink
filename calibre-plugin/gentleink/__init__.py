#!/usr/bin/env python
# -*- coding: utf-8 -*-

from calibre.customize import InterfaceActionBase


class GentleInkPlugin(InterfaceActionBase):
    name = "GentleInk Language Filter"
    description = "Filter profanity from selected ebooks using GentleInk context-aware rules"
    supported_platforms = ["windows", "osx", "linux"]
    author = "GentleInk"
    version = (0, 1, 3)
    minimum_calibre_version = (6, 0, 0)
    actual_plugin = "calibre_plugins.gentleink.ui:GentleInkInterfaceAction"

    def is_customizable(self):
        return True

    def config_widget(self):
        from .config_widget import ConfigWidget
        return ConfigWidget()

    def save_settings(self, config_widget):
        config_widget.save_settings()

    def restore_settings(self, config_widget):
        config_widget.restore_settings()
