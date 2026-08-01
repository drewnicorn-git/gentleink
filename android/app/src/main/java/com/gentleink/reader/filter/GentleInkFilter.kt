package com.gentleink.reader.filter

import android.content.Context
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale

class GentleInkFilter private constructor(private val config: FilterConfig) {

    fun analyze(text: String, profile: FilterProfile = FilterProfile.FAMILY, windowSize: Int = 120): List<FilterMatch> {
        val prepared = prepareText(text)
        val matches = mutableListOf<FilterMatch>()

        for (word in config.wordList) {
            val pattern = Regex("\\b${Regex.escape(word)}\\b", RegexOption.IGNORE_CASE)
            pattern.findAll(prepared).forEach { match ->
                val start = match.range.first
                val end = match.range.last + 1
                if (!isWordBoundary(prepared, start, end)) return@forEach
                if (isInsideCompound(prepared, start, end)) return@forEach
                if (isContractionSpan(prepared, start, end)) return@forEach

                val token = text.substring(start.coerceAtMost(text.length), end.coerceAtMost(text.length))
                val lemma = match.value.lowercase(Locale.US)
                val contextStart = (start - windowSize).coerceAtLeast(0)
                val contextEnd = (end + windowSize).coerceAtMost(prepared.length)
                val contextText = prepared.substring(contextStart, contextEnd)

                if (lemma in config.tier1Words) {
                    if (isTier1Safe(lemma, contextText)) return@forEach
                    matches += FilterMatch(token, lemma, start, end, 1, "unambiguous profanity list")
                    return@forEach
                }

                val rules = config.ambiguous[lemma] ?: return@forEach
                val (safe, profane) = scoreContext(contextText, rules)
                if (shouldFilterContext(rules, safe, profane, profile)) {
                    val reason = when {
                        profane > safe -> "profane context ($profane > $safe)"
                        profane == safe && profane > 0 -> "ambiguous context tie-breaker"
                        profile == FilterProfile.RELIGIOUS_STRICT -> "strict profile default"
                        else -> "expletive default (no safe context)"
                    }
                    matches += FilterMatch(token, lemma, start, end, 2, reason)
                }
            }
        }

        return dedupeOverlapping(matches)
    }

    fun filterText(
        text: String,
        mode: FilterMode = FilterMode.SUBSTITUTE,
        profile: FilterProfile = FilterProfile.FAMILY,
        maskChar: Char = '*'
    ): FilterResult {
        var out = text
        val allMatches = mutableListOf<FilterMatch>()
        for (pass in 0 until MAX_PASSES) {
            val before = out
            val once = filterTextOnce(out, mode, profile, maskChar)
            out = once.text
            allMatches += once.matches
            if (out == before && !once.changed) break
        }
        return FilterResult(out, allMatches, out != text)
    }

    private fun filterTextOnce(
        text: String,
        mode: FilterMode,
        profile: FilterProfile,
        maskChar: Char
    ): FilterResult {
        val prepared = applyPhrases(text, mode, profile, maskChar)
        val matches = analyze(prepared, profile)
        if (matches.isEmpty()) {
            return FilterResult(prepared, emptyList(), prepared != text)
        }

        val subs = config.substitutions[profile.key].orEmpty()
        var out = prepared
        var offset = 0

        for (match in matches) {
            val start = match.start + offset
            val end = match.end + offset
            val original = out.substring(start, end)
            val replacement = when (mode) {
                FilterMode.REMOVE -> ""
                FilterMode.MASK -> maskChar.toString().repeat(maxOf(3, original.length))
                FilterMode.SUBSTITUTE -> {
                    val sub = subs[match.lemma] ?: subs[original.lowercase(Locale.US)] ?: maskChar.toString().repeat(3)
                    preserveCase(original, sub)
                }
            }
            out = out.substring(0, start) + replacement + out.substring(end)
            offset += replacement.length - original.length
        }

        return FilterResult(out, matches, true)
    }

    private fun applyPhrases(
        text: String,
        mode: FilterMode,
        profile: FilterProfile,
        maskChar: Char
    ): String {
        if (mode == FilterMode.REMOVE || config.phrases.isEmpty()) return text
        var out = prepareText(text)
        for (phrase in config.phrases) {
            val replacement = phrase.replacement(profile)
            val pattern = Regex(buildPhrasePattern(phrase.words), RegexOption.IGNORE_CASE)
            out = pattern.replace(out) { match ->
                val original = match.value
                if (mode == FilterMode.MASK) {
                    maskChar.toString().repeat(maxOf(3, original.length))
                } else {
                    preserveCase(original, replacement)
                }
            }
        }
        return out
    }

    private fun prepareText(text: String): String {
        var out = Normalizer.normalize(text, Normalizer.Form.NFKC)
            .replace('\u2018', '\'')
            .replace('\u2019', '\'')
            .replace('\u2032', '\'')
            .replace('\u201C', '"')
            .replace('\u201D', '"')
            .replace(Regex("&nbsp;|&#160;|&#xA0;", RegexOption.IGNORE_CASE), " ")
        for ((from, to) in config.leetMap) {
            out = out.replace(from, to)
        }
        return out
    }

    private fun isTier1Safe(lemma: String, contextText: String): Boolean {
        val patterns = config.tier1Safe[lemma].orEmpty()
        val lower = contextText.lowercase(Locale.US)
        return patterns.any { Regex(it, RegexOption.IGNORE_CASE).containsMatchIn(lower) }
    }

    private fun shouldFilterContext(rules: AmbiguousRules, safe: Int, profane: Int, profile: FilterProfile): Boolean {
        if (profile == FilterProfile.RELIGIOUS_STRICT) {
            if (safe > profane) return false
            if (rules.defaultAction == "skip" && safe == 0 && profane == 0) return false
            return true
        }
        if (profane > safe) return true
        if (profane == safe && profane > 0) return rules.defaultAction != "skip"
        if (profane == 0 && safe == 0 && rules.defaultAction == "context") return true
        return false
    }

    private fun buildPhrasePattern(words: List<String>): String =
        words.joinToString("\\s+") { Regex.escape(it) }

    private fun isWordBoundary(text: String, start: Int, end: Int): Boolean {
        val before = if (start > 0) text[start - 1] else ' '
        val after = if (end < text.length) text[end] else ' '
        return !before.isLetterOrDigit() && before != '\'' && !after.isLetterOrDigit() && after != '\''
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

    private fun dedupeOverlapping(matches: List<FilterMatch>): List<FilterMatch> {
        val sorted = matches.sortedWith(compareBy<FilterMatch> { it.start }.thenByDescending { it.end - it.start })
        val kept = mutableListOf<FilterMatch>()
        for (match in sorted) {
            val last = kept.lastOrNull()
            if (last != null && match.start < last.end) {
                if (match.end - match.start > last.end - last.start) {
                    kept[kept.lastIndex] = match
                }
                continue
            }
            kept += match
        }
        return kept
    }

    private fun preserveCase(original: String, replacement: String): String {
        if (original.isEmpty()) return replacement
        if (replacement.isEmpty()) return original
        if (original == original.uppercase(Locale.US)) return replacement.uppercase(Locale.US)
        if (original.first().isUpperCase()) {
            return replacement.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
        }
        return replacement
    }

    companion object {
        private const val MAX_PASSES = 16

        fun fromContext(context: Context): GentleInkFilter {
            val assets = context.assets
            val allowlist = JSONObject(assets.open("allowlist.json").bufferedReader().readText())
            val tier1 = JSONObject(assets.open("tier1-unambiguous.json").bufferedReader().readText())
            val contextRules = JSONObject(assets.open("context-rules.json").bufferedReader().readText())
            val substitutions = JSONObject(assets.open("substitutions.json").bufferedReader().readText())

            val compounds = jsonArrayToSet(allowlist.getJSONArray("compounds"))
            val contractions = jsonArrayToSet(allowlist.getJSONArray("contractions"))
            val tier1Words = jsonArrayToSet(tier1.getJSONArray("words"))

            val leetMap = tier1.optJSONObject("leetMap")?.let { obj ->
                obj.keys().asSequence().associateWith { key -> obj.getString(key) }
            }.orEmpty()

            val ambiguousJson = contextRules.getJSONObject("ambiguous")
            val ambiguous = ambiguousJson.keys().asSequence().associateWith { key ->
                val obj = ambiguousJson.getJSONObject(key)
                AmbiguousRules(
                    safePatterns = jsonArrayToList(obj.optJSONArray("safePatterns")),
                    profanePatterns = jsonArrayToList(obj.optJSONArray("profanePatterns")),
                    defaultAction = obj.optString("defaultAction", "skip")
                )
            }

            val tier1SafeJson = contextRules.optJSONObject("tier1Safe")
            val tier1Safe = tier1SafeJson?.keys()?.asSequence()?.associateWith { key ->
                jsonArrayToList(tier1SafeJson.getJSONArray(key))
            }.orEmpty()

            val wordList = sortedWordList(tier1Words, ambiguous)

            val profilesJson = substitutions.getJSONObject("profiles")
            val subs = profilesJson.keys().asSequence().associateWith { key ->
                val obj = profilesJson.getJSONObject(key)
                obj.keys().asSequence().associateWith { word -> obj.getString(word) }
            }

            val phrases = contextRules.optJSONArray("phrases")?.let { array ->
                (0 until array.length()).map { index ->
                    val obj = array.getJSONObject(index)
                    val wordsJson = obj.getJSONArray("words")
                    val words = (0 until wordsJson.length()).map { wordsJson.getString(it) }
                    PhraseRule(
                        words = words,
                        family = obj.getString("family"),
                        religiousStrict = obj.getString("religious_strict"),
                    )
                }
            }.orEmpty()

            return GentleInkFilter(
                FilterConfig(
                    compounds = compounds,
                    contractions = contractions,
                    tier1Words = tier1Words,
                    wordList = wordList,
                    leetMap = leetMap,
                    tier1Safe = tier1Safe,
                    ambiguous = ambiguous,
                    phrases = phrases,
                    substitutions = subs
                )
            )
        }

        private fun sortedWordList(tier1Words: Set<String>, ambiguous: Map<String, AmbiguousRules>): List<String> {
            val tier1Sorted = tier1Words.sortedByDescending { it.length }
            val ambiguousSorted = ambiguous.keys
                .filter { it !in tier1Sorted }
                .sortedByDescending { it.length }
            return tier1Sorted + ambiguousSorted
        }

        private fun jsonArrayToSet(array: org.json.JSONArray): Set<String> =
            (0 until array.length()).map { array.getString(it).lowercase(Locale.US) }.toSet()

        private fun jsonArrayToList(array: org.json.JSONArray?): List<String> {
            if (array == null) return emptyList()
            return (0 until array.length()).map { array.getString(it) }
        }
    }
}
