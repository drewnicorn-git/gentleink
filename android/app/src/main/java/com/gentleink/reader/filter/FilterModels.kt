package com.gentleink.reader.filter

enum class FilterMode {
    REMOVE, MASK, SUBSTITUTE
}

enum class FilterProfile(val key: String) {
    FAMILY("family"),
    RELIGIOUS_STRICT("religious_strict");

    companion object {
        fun fromKey(key: String): FilterProfile =
            entries.firstOrNull { it.key == key } ?: FAMILY
    }
}

data class FilterMatch(
    val word: String,
    val lemma: String,
    val start: Int,
    val end: Int,
    val tier: Int,
    val reason: String
)

data class FilterResult(
    val text: String,
    val matches: List<FilterMatch>,
    val changed: Boolean
)

data class AmbiguousRules(
    val safePatterns: List<String> = emptyList(),
    val profanePatterns: List<String> = emptyList(),
    val defaultAction: String = "skip"
)

data class PhraseRule(
    val words: List<String>,
    val family: String,
    val religiousStrict: String,
) {
    fun replacement(profile: FilterProfile): String =
        when (profile) {
            FilterProfile.FAMILY -> family
            FilterProfile.RELIGIOUS_STRICT -> religiousStrict
        }
}

data class FilterConfig(
    val compounds: Set<String>,
    val contractions: Set<String>,
    val tier1Words: Set<String>,
    val wordList: List<String>,
    val leetMap: Map<String, String>,
    val tier1Safe: Map<String, List<String>>,
    val ambiguous: Map<String, AmbiguousRules>,
    val phrases: List<PhraseRule>,
    val substitutions: Map<String, Map<String, String>>
)
