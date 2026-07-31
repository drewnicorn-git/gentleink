package com.gentleink.reader.filter

import android.content.Context
import org.json.JSONObject
import java.util.Locale

class GentleInkFilter private constructor(private val config: FilterConfig) {

    fun analyze(text: String, profile: FilterProfile = FilterProfile.FAMILY, windowSize: Int = 80): List<FilterMatch> {
        val matches = mutableListOf<FilterMatch>()
        val allWords = (config.tier1Words + config.ambiguous.keys).distinct()

        for (word in allWords) {
            val pattern = Regex("\\b${Regex.escape(word)}\\b", RegexOption.IGNORE_CASE)
            pattern.findAll(text).forEach { match ->
                val start = match.range.first
                val end = match.range.last + 1
                val token = match.value
                val lemma = token.lowercase(Locale.US)

                if (isInsideCompound(text, start, end)) return@forEach
                if (isContractionSpan(text, start, end)) return@forEach

                if (lemma in config.tier1Words) {
                    matches += FilterMatch(token, lemma, start, end, 1, "unambiguous profanity list")
                    return@forEach
                }

                val rules = config.ambiguous[lemma] ?: return@forEach
                val contextStart = (start - windowSize).coerceAtLeast(0)
                val contextEnd = (end + windowSize).coerceAtMost(text.length)
                val contextText = text.substring(contextStart, contextEnd)
                val (safe, profane) = scoreContext(contextText, rules)

                val shouldFilter = when {
                    profane > safe -> true
                    profane == safe && profane > 0 -> rules.defaultAction != "skip"
                    else -> false
                }

                if (shouldFilter) {
                    val reason = if (profane > safe) "profane context ($profane > $safe)" else "ambiguous context tie-breaker"
                    matches += FilterMatch(token, lemma, start, end, 2, reason)
                }
            }
        }

        return matches.sortedBy { it.start }
    }

    fun filterText(
        text: String,
        mode: FilterMode = FilterMode.SUBSTITUTE,
        profile: FilterProfile = FilterProfile.FAMILY,
        maskChar: Char = '*'
    ): FilterResult {
        val matches = analyze(text, profile)
        if (matches.isEmpty()) return FilterResult(text, emptyList(), false)

        val subs = config.substitutions[profile.key].orEmpty()
        val builder = StringBuilder()
        var cursor = 0
        var changed = false

        for (match in matches) {
            builder.append(text.substring(cursor, match.start))
            val original = text.substring(match.start, match.end)
            val replacement = when (mode) {
                FilterMode.REMOVE -> ""
                FilterMode.MASK -> maskChar.toString().repeat(maxOf(3, original.length))
                FilterMode.SUBSTITUTE -> {
                    val sub = subs[match.lemma] ?: subs[original.lowercase(Locale.US)] ?: maskChar.toString().repeat(3)
                    preserveCase(original, sub)
                }
            }
            builder.append(replacement)
            cursor = match.end
            changed = true
        }
        builder.append(text.substring(cursor))

        return FilterResult(builder.toString(), matches, changed)
    }

    private fun isInsideCompound(text: String, start: Int, end: Int): Boolean {
        val lower = text.lowercase(Locale.US)
        for (compound in config.compounds) {
            var idx = 0
            while (true) {
                idx = lower.indexOf(compound, idx)
                if (idx < 0) break
                val cEnd = idx + compound.length
                if (start >= idx && end <= cEnd) return true
                idx++
            }
        }
        return false
    }

    private fun isContractionSpan(text: String, start: Int, end: Int): Boolean {
        val span = text.substring(start, end).lowercase(Locale.US)
        val expandedStart = (start - 2).coerceAtLeast(0)
        val expandedEnd = (end + 2).coerceAtMost(text.length)
        val expanded = text.substring(expandedStart, expandedEnd).lowercase(Locale.US)
        return config.contractions.any { span == it || expanded.contains(it) }
    }

    private fun scoreContext(text: String, rules: AmbiguousRules): Pair<Int, Int> {
        var safe = 0
        var profane = 0
        for (pattern in rules.safePatterns) {
            if (Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(text)) safe++
        }
        for (pattern in rules.profanePatterns) {
            if (Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(text)) profane++
        }
        return safe to profane
    }

    private fun preserveCase(original: String, replacement: String): String {
        if (original == original.uppercase(Locale.US)) return replacement.uppercase(Locale.US)
        if (original.first().isUpperCase()) {
            return replacement.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
        }
        return replacement
    }

    companion object {
        fun fromContext(context: Context): GentleInkFilter {
            val assets = context.assets
            val allowlist = JSONObject(assets.open("allowlist.json").bufferedReader().readText())
            val tier1 = JSONObject(assets.open("tier1-unambiguous.json").bufferedReader().readText())
            val contextRules = JSONObject(assets.open("context-rules.json").bufferedReader().readText())
            val substitutions = JSONObject(assets.open("substitutions.json").bufferedReader().readText())

            val compounds = jsonArrayToSet(allowlist.getJSONArray("compounds"))
            val contractions = jsonArrayToSet(allowlist.getJSONArray("contractions"))
            val tier1Words = jsonArrayToSet(tier1.getJSONArray("words"))

            val ambiguousJson = contextRules.getJSONObject("ambiguous")
            val ambiguous = ambiguousJson.keys().asSequence().associateWith { key ->
                val obj = ambiguousJson.getJSONObject(key)
                AmbiguousRules(
                    safePatterns = jsonArrayToList(obj.optJSONArray("safePatterns")),
                    profanePatterns = jsonArrayToList(obj.optJSONArray("profanePatterns")),
                    defaultAction = obj.optString("defaultAction", "skip")
                )
            }

            val profilesJson = substitutions.getJSONObject("profiles")
            val subs = profilesJson.keys().asSequence().associateWith { key ->
                val obj = profilesJson.getJSONObject(key)
                obj.keys().asSequence().associateWith { word -> obj.getString(word) }
            }

            return GentleInkFilter(
                FilterConfig(
                    compounds = compounds,
                    contractions = contractions,
                    tier1Words = tier1Words,
                    ambiguous = ambiguous,
                    substitutions = subs
                )
            )
        }

        private fun jsonArrayToSet(array: org.json.JSONArray): Set<String> =
            (0 until array.length()).map { array.getString(it).lowercase(Locale.US) }.toSet()

        private fun jsonArrayToList(array: org.json.JSONArray?): List<String> {
            if (array == null) return emptyList()
            return (0 until array.length()).map { array.getString(it) }
        }
    }
}
