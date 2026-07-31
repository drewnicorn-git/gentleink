from calibre.utils.config import JSONConfig

plugin_prefs = JSONConfig("plugins/gentleink")
plugin_prefs.defaults["mode"] = "substitute"
plugin_prefs.defaults["profile"] = "family"
