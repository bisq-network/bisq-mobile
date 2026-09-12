package network.bisq.mobile.data.replicated.chat

import network.bisq.mobile.data.replicated.user.profile.UserProfileVO

/**
 * The mention token the caret is currently inside. Shared by the composer picker and the
 * insertion path so both agree on the replaced range — the same split desktop uses in
 * `ChatMentionParser` / `ChatMessageContainerController.onUserProfileSelected`.
 */
data class ChatMentionMatch(
    val query: String,
    val indicatorIndex: Int,
    val caretPosition: Int,
)

data class ChatMentionInsertion(
    val text: String,
    val caretPosition: Int,
)

/**
 * The mention token the user just hid or completed. [query] is the text after `@` at
 * dismiss time — Back keeps the typed query, a tap stores the inserted [UserProfileVO.userName]
 * — so a later edit at the same caret index can reopen without treating insert as a new type.
 */
data class DismissedMentionToken(
    val indicatorIndex: Int,
    val query: String,
)

fun ChatMentionMatch.isDismissedBy(dismissed: DismissedMentionToken?): Boolean =
    dismissed != null &&
        dismissed.indicatorIndex == indicatorIndex &&
        dismissed.query == query

/**
 * Port of desktop's caret-anchored mention parser at
 * `b03bcab3a3f678f4e0ca861f72537b8a0be74451`. Token characters are letters and digits
 * in any script, combining marks, and emoji — the same names [mentionRanges] can highlight —
 * not only ASCII. The `@` must sit at index 0 or after whitespace. Insertion applies
 * desktop's conditional trailing space rather than always appending one.
 */
object ChatMentionParser {
    private const val INDICATOR = '@'

    fun findMentionAtCaret(
        text: String,
        caretPosition: Int,
    ): ChatMentionMatch? {
        if (caretPosition < 1 || caretPosition > text.length) {
            return null
        }
        var indicatorIndex = -1
        for (i in caretPosition - 1 downTo 0) {
            val c = text[i]
            if (c == INDICATOR) {
                indicatorIndex = i
                break
            }
            if (!isTokenCharacter(c)) {
                return null
            }
        }
        if (indicatorIndex < 0) {
            return null
        }
        if (indicatorIndex > 0 && !text[indicatorIndex - 1].isWhitespace()) {
            return null
        }
        // Completion never replaces past the caret: text after it can be a pre-existing word
        // the user typed the mention in front of.
        return ChatMentionMatch(
            query = text.substring(indicatorIndex + 1, caretPosition),
            indicatorIndex = indicatorIndex,
            caretPosition = caretPosition,
        )
    }

    fun filterAndSort(
        candidates: Collection<UserProfileVO>,
        query: String,
    ): List<UserProfileVO> {
        val filtered =
            if (query.isEmpty()) {
                candidates
            } else {
                candidates.filter { it.userName.contains(query, ignoreCase = true) }
            }
        return filtered.sortedWith(
            compareByDescending<UserProfileVO> { it.userName.startsWith(query, ignoreCase = true) }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.userName },
        )
    }

    fun insertMention(
        text: String,
        match: ChatMentionMatch,
        userName: String,
    ): ChatMentionInsertion {
        if (match.indicatorIndex < 0 ||
            match.caretPosition > text.length ||
            match.indicatorIndex >= match.caretPosition
        ) {
            return ChatMentionInsertion(text, match.caretPosition.coerceIn(0, text.length))
        }
        val prefix = text.substring(0, match.indicatorIndex)
        val suffix = text.substring(match.caretPosition)
        val mention = "$INDICATOR$userName"
        var caretPosition = prefix.length + mention.length
        val separator =
            if (suffix.startsWith(" ")) {
                // Reuse the existing space and continue typing after it.
                caretPosition++
                ""
            } else if (suffix.isEmpty() || suffix[0].isLetterOrDigit()) {
                // A separating space is only needed before a word; punctuation and line
                // breaks stay attached to the mention.
                caretPosition++
                " "
            } else {
                ""
            }
        return ChatMentionInsertion(
            text = prefix + mention + separator + suffix,
            caretPosition = caretPosition,
        )
    }

    private fun isTokenCharacter(c: Char): Boolean {
        if (c == INDICATOR || c.isWhitespace()) {
            return false
        }
        if (c.isLetterOrDigit() || c.isSurrogate()) {
            return true
        }
        return when (c.category) {
            CharCategory.NON_SPACING_MARK,
            CharCategory.COMBINING_SPACING_MARK,
            CharCategory.ENCLOSING_MARK,
            CharCategory.OTHER_SYMBOL,
            CharCategory.MODIFIER_SYMBOL,
            CharCategory.FORMAT,
            -> true
            else -> false
        }
    }
}
