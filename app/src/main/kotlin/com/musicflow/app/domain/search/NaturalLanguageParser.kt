package com.musicflow.app.domain.search

/**
 * Parses natural language queries into structured search intents.
 *
 * Supports commands like "play some chill music", "find songs by
 * Artist Name", "discover new music", and falls back to normal
 * search when the query cannot be parsed.
 */
class NaturalLanguageParser {

    /**
     * Parsed query result containing the detected intent and extracted parameters.
     */
    data class ParsedQuery(
        val intent: Intent,
        val mood: String? = null,
        val language: String? = null,
        val timeRange: String? = null,
        val limit: Int = 20,
        val rawQuery: String = "",
        val searchTerm: String = "",
    )

    /**
     * Detected user intent from the natural language query.
     */
    enum class Intent {
        PLAY,
        SEARCH,
        MOOD,
        DISCOVERY,
        PLAYLIST,
    }

    private val playPatterns = listOf(
        Regex("""^(?:play|put on|start|queue up|listen to)\s+(.+)""", RegexOption.IGNORE_CASE),
        Regex("""^(?:i want to hear|let me hear|can you play)\s+(.+)""", RegexOption.IGNORE_CASE),
    )

    private val searchPatterns = listOf(
        Regex("""^(?:find|search for|look up|show me|search)\s+(.+)""", RegexOption.IGNORE_CASE),
        Regex("""^(?:who sings|what songs by|songs from)\s+(.+)""", RegexOption.IGNORE_CASE),
    )

    private val moodPatterns = listOf(
        Regex("""^(?:play|find|put on|get me)\s+(.+?)\s+(?:music|songs|tracks|vibes|tunes)""", RegexOption.IGNORE_CASE),
        Regex("""^(?:something|music|songs?)\s+(?:that is |that's )?(.+)""", RegexOption.IGNORE_CASE),
    )

    private val discoveryPatterns = listOf(
        Regex("""^(?:discover|surprise me|find something new|explore|what's new)""", RegexOption.IGNORE_CASE),
        RandomMixPattern(Regex("""^(?:random|shuffle|mix|shuffle play)\s*(.*)?""", RegexOption.IGNORE_CASE)),
    )

    private val playlistPatterns = listOf(
        Regex("""^(?:create|make|build)\s+(?:a\s+)?playlist\s+(?:called|named|titled)\s+(.+)""", RegexOption.IGNORE_CASE),
        Regex("""^(?:add to|save to)\s+(?:playlist\s+)?(.+)""", RegexOption.IGNORE_CASE),
    )

    private val moodKeywords = mapOf(
        "calm" to setOf("calm", "relax", "chill", "peaceful", "serene", "mellow", "gentle", "soothing"),
        "energetic" to setOf("energetic", "energy", "pump", "power", "hype", "intense", "fast"),
        "happy" to setOf("happy", "joy", "cheerful", "upbeat", "bright", "fun", "positive"),
        "sad" to setOf("sad", "melancholy", "grief", "heartbreak", "sorrow", "blue", "lonely"),
        "focus" to setOf("focus", "concentration", "study", "work", "deep", "flow", "productive"),
        "workout" to setOf("workout", "gym", "exercise", "fitness", "run", "cardio", "strength"),
    )

    private val languagePatterns = mapOf(
        "hindi" to "hi",
        "spanish" to "es",
        "french" to "fr",
        "german" to "de",
        "japanese" to "ja",
        "korean" to "ko",
        "portuguese" to "pt",
        "italian" to "it",
        "arabic" to "ar",
        "english" to "en",
    )

    private val timeRangePatterns = mapOf(
        Regex("""(?:from|of|in)\s+(?:the\s+)?(?:20)?(\d{2})s?""", RegexOption.IGNORE_CASE) to "decade",
        Regex("""(?:from|in)\s+(\d{4})""", RegexOption.IGNORE_CASE) to "year",
        Regex("""(?:old|classic|retro|vintage|throwback|oldies)""", RegexOption.IGNORE_CASE) to "classic",
        Regex("""(?:new|recent|latest|fresh|newest)""", RegexOption.IGNORE_CASE) to "recent",
    )

    /**
     * Parses a natural language query into a structured [ParsedQuery].
     *
     * @param query The raw user input.
     * @return A [ParsedQuery] with the detected intent and extracted parameters.
     */
    fun parse(query: String): ParsedQuery {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            return ParsedQuery(intent = Intent.SEARCH, rawQuery = trimmed)
        }

        // Try PLAY intent
        for (pattern in playPatterns) {
            val match = pattern.find(trimmed)
            if (match != null) {
                val searchTerm = match.groupValues[1].trim()
                val detectedMood = detectMood(searchTerm)
                val detectedLang = detectLanguage(searchTerm)
                val cleanedSearch = cleanSearchTerm(searchTerm)
                return ParsedQuery(
                    intent = if (detectedMood != null) Intent.MOOD else Intent.PLAY,
                    mood = detectedMood,
                    language = detectedLang,
                    rawQuery = trimmed,
                    searchTerm = cleanedSearch,
                )
            }
        }

        // Try DISCOVERY intent
        for (pattern in discoveryPatterns) {
            if (pattern is RandomMixPattern) {
                val match = pattern.regex.find(trimmed)
                if (match != null) {
                    return ParsedQuery(
                        intent = Intent.DISCOVERY,
                        rawQuery = trimmed,
                        searchTerm = match.groupValues.getOrNull(1)?.trim() ?: "",
                    )
                }
            } else {
                @Suppress("UNCHECKED_CAST")
                val regex = pattern as Regex
                if (regex.containsMatchIn(trimmed)) {
                    return ParsedQuery(
                        intent = Intent.DISCOVERY,
                        rawQuery = trimmed,
                    )
                }
            }
        }

        // Try MOOD intent
        for (pattern in moodPatterns) {
            val match = pattern.find(trimmed)
            if (match != null) {
                val moodText = match.groupValues[1].trim()
                val detectedMood = detectMood(moodText) ?: moodText
                val detectedLang = detectLanguage(trimmed)
                return ParsedQuery(
                    intent = Intent.MOOD,
                    mood = detectedMood,
                    language = detectedLang,
                    rawQuery = trimmed,
                    searchTerm = detectedMood,
                )
            }
        }

        // Try PLAYLIST intent
        for (pattern in playlistPatterns) {
            val match = pattern.find(trimmed)
            if (match != null) {
                return ParsedQuery(
                    intent = Intent.PLAYLIST,
                    rawQuery = trimmed,
                    searchTerm = match.groupValues[1].trim(),
                )
            }
        }

        // Try SEARCH intent
        for (pattern in searchPatterns) {
            val match = pattern.find(trimmed)
            if (match != null) {
                val searchTerm = match.groupValues[1].trim()
                val detectedMood = detectMood(searchTerm)
                val detectedLang = detectLanguage(searchTerm)
                val cleanedSearch = cleanSearchTerm(searchTerm)
                return ParsedQuery(
                    intent = if (detectedMood != null) Intent.MOOD else Intent.SEARCH,
                    mood = detectedMood,
                    language = detectedLang,
                    rawQuery = trimmed,
                    searchTerm = cleanedSearch,
                )
            }
        }

        // Fallback: treat as a search query
        val detectedMood = detectMood(trimmed)
        val detectedLang = detectLanguage(trimmed)
        val cleanedSearch = cleanSearchTerm(trimmed)
        return ParsedQuery(
            intent = if (detectedMood != null) Intent.MOOD else Intent.SEARCH,
            mood = detectedMood,
            language = detectedLang,
            rawQuery = trimmed,
            searchTerm = cleanedSearch,
        )
    }

    private fun detectMood(text: String): String? {
        val lower = text.lowercase()
        for ((mood, keywords) in moodKeywords) {
            if (keywords.any { lower.contains(it) }) {
                return mood
            }
        }
        return null
    }

    private fun detectLanguage(text: String): String? {
        val lower = text.lowercase()
        for ((name, code) in languagePatterns) {
            if (lower.contains(name)) {
                return code
            }
        }
        return null
    }

    private fun cleanSearchTerm(term: String): String {
        var cleaned = term
        // Remove mood keywords that were detected
        for ((_, keywords) in moodKeywords) {
            for (keyword in keywords) {
                cleaned = cleaned.replace(keyword, "", ignoreCase = true)
            }
        }
        // Remove common filler words
        val fillers = listOf("some", "the", "a", "by", "with", "from", "called", "named")
        for (filler in fillers) {
            cleaned = cleaned.replace(Regex("""\b$filler\b""", RegexOption.IGNORE_CASE), "")
        }
        return cleaned.trim().ifBlank { term }
    }

    /** Wrapper for random/mix patterns that need special handling. */
    private class RandomMixPattern(val regex: Regex)
}
