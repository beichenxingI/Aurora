package com.beichenxing.aurora.domain.service

import com.beichenxing.aurora.domain.model.CharacterBook
import com.beichenxing.aurora.domain.model.Message
import com.beichenxing.aurora.domain.model.SelectiveLogic
import com.beichenxing.aurora.domain.model.WorldInfoEntry
import com.beichenxing.aurora.domain.model.WorldInfoPosition
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * World info activation engine.
 *
 * Phase 1: keyword matching, selective logic, constant/sticky/decorators,
 *          character filter, insertion positions.
 * Phase 2: probability, recursion, inclusion groups, token budget.
 */
@Singleton
class WorldInfoScanner @Inject constructor() {

    /**
     * Scan chat history and return activated entries grouped by position.
     *
     * @param book          The character book to scan.
     * @param messages      Chat history in chronological order.
     * @param characterId   The active character's ID for character filter.
     * @param globalScanDepth Default scan depth if entry doesn't specify.
     * @param globalCaseSensitive Default case sensitivity.
     * @param globalMatchWholeWords Default whole-word matching.
     * @return Map of position → activated entries in order-descending order.
     */
    fun scan(
        book: CharacterBook?,
        messages: List<Message>,
        characterId: String,
        globalScanDepth: Int = 2,
        globalCaseSensitive: Boolean = false,
        globalMatchWholeWords: Boolean = false
    ): Map<WorldInfoPosition, List<WorldInfoEntry>> {
        if (book == null || book.entries.isEmpty()) return emptyMap()

        val effectiveScanDepth = book.scanDepth ?: globalScanDepth
        val scanBuffer = buildScanBuffer(messages, effectiveScanDepth)

        val activated = mutableListOf<WorldInfoEntry>()

        for (entry in book.entries) {
            if (entry.disable) continue

            // Character filter: skip if automationId is set and doesn't match
            if (entry.automationId.isNotBlank() && entry.automationId != characterId) {
                continue
            }

            // Constant: always activate
            if (entry.constant) {
                activated.add(entry)
                continue
            }

            // Sticky: Phase 2
            // For Phase 1, treat sticky as always-on if sticky > 0
            if ((entry.sticky ?: 0) > 0) {
                activated.add(entry)
                continue
            }

            // Decorator: @@activate / @@dont_activate
            val decorator = detectDecorator(entry.comment) ?: detectDecorator(entry.content)
            when (decorator) {
                "@@activate" -> {
                    activated.add(entry)
                    continue
                }
                "@@dont_activate" -> continue
                else -> {}
            }

            // Keyword matching
            if (matchesEntry(entry, scanBuffer, globalCaseSensitive, globalMatchWholeWords)) {
                activated.add(entry)
            }
        }

        // Sort by order descending (higher order = higher priority)
        val sorted = activated.sortedByDescending { it.order }

        // Group by position
        return sorted.groupBy { it.position }
    }

    // ─── Scan buffer ──────────────────────────────────────────────────────

    private fun buildScanBuffer(messages: List<Message>, scanDepth: Int): String {
        // Take last N messages (scanDepth from end)
        val toScan = if (scanDepth > 0 && messages.size > scanDepth) {
            messages.takeLast(scanDepth)
        } else {
            messages
        }
        return toScan.joinToString("\n") { it.currentContent }
    }

    // ─── Decorator detection ──────────────────────────────────────────────

    private fun detectDecorator(comment: String): String? {
        val trimmed = comment.trim()
        return when {
            trimmed.startsWith("@@activate", ignoreCase = true) -> "@@activate"
            trimmed.startsWith("@@dont_activate", ignoreCase = true) -> "@@dont_activate"
            else -> null
        }
    }

    // ─── Keyword matching ─────────────────────────────────────────────────

    private fun matchesEntry(
        entry: WorldInfoEntry,
        scanBuffer: String,
        globalCaseSensitive: Boolean,
        globalMatchWholeWords: Boolean
    ): Boolean {
        // Primary keys: at least one must match
        val primaryMatch = entry.key.any { keyword ->
            matchKeyword(keyword, scanBuffer, entry, globalCaseSensitive, globalMatchWholeWords)
        }
        if (!primaryMatch) return false

        // No secondary keys: primary match is enough
        if (entry.keysecondary.isEmpty()) return true

        // Secondary keys: apply selective logic
        return applySelectiveLogic(
            entry.keysecondary,
            scanBuffer,
            entry,
            globalCaseSensitive,
            globalMatchWholeWords,
            entry.selectiveLogic
        )
    }

    private fun matchKeyword(
        keyword: String,
        haystack: String,
        entry: WorldInfoEntry,
        globalCaseSensitive: Boolean,
        globalMatchWholeWords: Boolean
    ): Boolean {
        if (keyword.isBlank()) return false

        // Regex detection: /pattern/flags
        val regex = parseRegex(keyword)
        if (regex != null) {
            return try {
                regex.containsMatchIn(haystack)
            } catch (e: Exception) {
                Timber.w(e, "Invalid regex in world info: $keyword")
                false
            }
        }

        // Case sensitivity
        val caseSensitive = entry.caseSensitive ?: globalCaseSensitive
        val transformedHaystack = if (caseSensitive) haystack else haystack.lowercase()
        val transformedKeyword = if (caseSensitive) keyword else keyword.lowercase()

        // Whole word matching
        val matchWholeWords = entry.matchWholeWords ?: globalMatchWholeWords
        if (matchWholeWords) {
            val words = transformedKeyword.split(Regex("\\s+"))
            return if (words.size > 1) {
                // Multi-word: check substring
                transformedHaystack.contains(transformedKeyword)
            } else {
                // Single word: use word boundary regex
                val pattern = Regex("(?:^|\\W)(${Regex.escape(transformedKeyword)})(?:$|\\W)")
                pattern.containsMatchIn(transformedHaystack)
            }
        } else {
            return transformedHaystack.contains(transformedKeyword)
        }
    }

    private fun parseRegex(keyword: String): Regex? {
        // Detect /pattern/ or /pattern/flags
        if (!keyword.startsWith('/')) return null
        val lastSlash = keyword.lastIndexOf('/')
        if (lastSlash <= 0) return null

        val pattern = keyword.substring(1, lastSlash)
        val flags = if (lastSlash < keyword.length - 1) {
            keyword.substring(lastSlash + 1)
        } else {
            ""
        }

        val options = mutableSetOf<RegexOption>()
        if ('i' in flags) options.add(RegexOption.IGNORE_CASE)
        if ('m' in flags) options.add(RegexOption.MULTILINE)
        // 's' (dotall) not directly supported in Kotlin Regex; ignore for Phase 1

        return try {
            Regex(pattern, options)
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse regex: $keyword")
            null
        }
    }

    // ─── Selective logic ──────────────────────────────────────────────────

    private fun applySelectiveLogic(
        secondaryKeys: List<String>,
        scanBuffer: String,
        entry: WorldInfoEntry,
        globalCaseSensitive: Boolean,
        globalMatchWholeWords: Boolean,
        logic: SelectiveLogic
    ): Boolean {
        var hasAnyMatch = false
        var hasAllMatch = true

        for (secondary in secondaryKeys) {
            val match = matchKeyword(
                secondary, scanBuffer, entry,
                globalCaseSensitive, globalMatchWholeWords
            )
            if (match) hasAnyMatch = true
            if (!match) hasAllMatch = false

            // Early exit optimization
            when (logic) {
                SelectiveLogic.AND_ANY -> if (match) return true
                SelectiveLogic.NOT_ALL -> if (!match) return true
                else -> {}
            }
        }

        return when (logic) {
            SelectiveLogic.AND_ANY -> hasAnyMatch
            SelectiveLogic.NOT_ALL -> !hasAllMatch
            SelectiveLogic.NOT_ANY -> !hasAnyMatch
            SelectiveLogic.AND_ALL -> hasAllMatch
        }
    }
}