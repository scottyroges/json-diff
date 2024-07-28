package com.scottyroges.jsondiff.path

class CharacterIndex(private val charSequence: CharSequence) {
    private var position = 0
    private var endPosition: Int

    init {
        endPosition = charSequence.length - 1
    }

    fun length(): Int {
        return endPosition + 1
    }

    fun charAt(idx: Int): Char {
        return charSequence[idx]
    }

    fun currentChar(): Char {
        return charSequence[position]
    }

    fun currentCharIs(c: Char): Boolean {
        return charSequence[position] == c
    }

    fun lastCharIs(c: Char): Boolean {
        return charSequence[endPosition] == c
    }

    fun nextCharIs(c: Char): Boolean {
        return inBounds(position + 1) && charSequence[position + 1] == c
    }

    fun incrementPosition(charCount: Int): Int {
        return setPosition(position + charCount)
    }

    fun decrementEndPosition(charCount: Int): Int {
        return setEndPosition(endPosition - charCount)
    }

    fun setPosition(newPosition: Int): Int {
        // position = min(newPosition, charSequence.length() - 1);
        position = newPosition
        return position
    }

    private fun setEndPosition(newPosition: Int): Int {
        endPosition = newPosition
        return endPosition
    }

    fun position(): Int {
        return position
    }

    fun indexOfClosingSquareBracket(startPosition: Int): Int {
        var readPosition = startPosition
        while (inBounds(readPosition)) {
            if (charAt(readPosition) == CLOSE_SQUARE_BRACKET) {
                return readPosition
            }
            readPosition++
        }
        return -1
    }

    fun indexOfMatchingCloseChar(
        startPosition: Int,
        openChar: Char,
        closeChar: Char,
        skipStrings: Boolean,
        skipRegex: Boolean,
    ): Int {
        if (charAt(startPosition) != openChar) {
            throw InvalidPathException("Expected " + openChar + " but found " + charAt(startPosition))
        }
        var opened = 1
        var readPosition = startPosition + 1
        while (inBounds(readPosition)) {
            if (skipStrings) {
                val quoteChar = charAt(readPosition)
                if (quoteChar == SINGLE_QUOTE || quoteChar == DOUBLE_QUOTE) {
                    readPosition = nextIndexOfUnescaped(readPosition, quoteChar)
                    if (readPosition == -1) {
                        throw InvalidPathException("Could not find matching close quote for $quoteChar when parsing : $charSequence")
                    }
                    readPosition++
                }
            }
            if (skipRegex) {
                if (charAt(readPosition) == REGEX) {
                    readPosition = nextIndexOfUnescaped(readPosition, REGEX)
                    if (readPosition == -1) {
                        throw InvalidPathException(
                            "Could not find matching close for " + REGEX + " when parsing regex in : " + charSequence,
                        )
                    }
                    readPosition++
                }
            }
            if (charAt(readPosition) == openChar) {
                opened++
            }
            if (charAt(readPosition) == closeChar) {
                opened--
                if (opened == 0) {
                    return readPosition
                }
            }
            readPosition++
        }
        return -1
    }

    fun indexOfClosingBracket(
        startPosition: Int,
        skipStrings: Boolean,
        skipRegex: Boolean,
    ): Int {
        return indexOfMatchingCloseChar(startPosition, OPEN_PARENTHESIS, CLOSE_PARENTHESIS, skipStrings, skipRegex)
    }

    fun indexOfNextSignificantChar(c: Char): Int {
        return indexOfNextSignificantChar(position, c)
    }

    fun indexOfNextSignificantChar(
        startPosition: Int,
        c: Char,
    ): Int {
        var readPosition = startPosition + 1
        while (!isOutOfBounds(readPosition) && charAt(readPosition) == SPACE) {
            readPosition++
        }
        return if (charAt(readPosition) == c) {
            readPosition
        } else {
            -1
        }
    }

    fun nextIndexOf(c: Char): Int {
        return nextIndexOf(position + 1, c)
    }

    fun nextIndexOf(
        startPosition: Int,
        c: Char,
    ): Int {
        var readPosition = startPosition
        while (!isOutOfBounds(readPosition)) {
            if (charAt(readPosition) == c) {
                return readPosition
            }
            readPosition++
        }
        return -1
    }

    fun nextIndexOfUnescaped(c: Char): Int {
        return nextIndexOfUnescaped(position, c)
    }

    fun nextIndexOfUnescaped(
        startPosition: Int,
        c: Char,
    ): Int {
        var readPosition = startPosition + 1
        var inEscape = false
        while (!isOutOfBounds(readPosition)) {
            if (inEscape) {
                inEscape = false
            } else if ('\\' == charAt(readPosition)) {
                inEscape = true
            } else if (c == charAt(readPosition)) {
                return readPosition
            }
            readPosition++
        }
        return -1
    }

    fun charAtOr(
        postition: Int,
        defaultChar: Char,
    ): Char {
        return if (!inBounds(postition)) defaultChar else charAt(postition)
    }

    fun nextSignificantCharIs(
        startPosition: Int,
        c: Char,
    ): Boolean {
        var readPosition = startPosition + 1
        while (!isOutOfBounds(readPosition) && charAt(readPosition) == SPACE) {
            readPosition++
        }
        return !isOutOfBounds(readPosition) && charAt(readPosition) == c
    }

    fun nextSignificantCharIs(c: Char): Boolean {
        return nextSignificantCharIs(position, c)
    }

    @JvmOverloads
    fun nextSignificantChar(startPosition: Int = position): Char {
        var readPosition = startPosition + 1
        while (!isOutOfBounds(readPosition) && charAt(readPosition) == SPACE) {
            readPosition++
        }
        return if (!isOutOfBounds(readPosition)) {
            charAt(readPosition)
        } else {
            ' '
        }
    }

    fun readSignificantChar(c: Char) {
        if (skipBlanks().currentChar() != c) {
            throw InvalidPathException(String.format("Expected character: %c", c))
        }
        incrementPosition(1)
    }

    fun hasSignificantSubSequence(s: CharSequence): Boolean {
        skipBlanks()
        if (!inBounds(position + s.length - 1)) {
            return false
        }
        if (subSequence(position, position + s.length) != s) {
            return false
        }
        incrementPosition(s.length)
        return true
    }

    @JvmOverloads
    fun indexOfPreviousSignificantChar(startPosition: Int = position): Int {
        var readPosition = startPosition - 1
        while (!isOutOfBounds(readPosition) && charAt(readPosition) == SPACE) {
            readPosition--
        }
        return if (!isOutOfBounds(readPosition)) {
            readPosition
        } else {
            -1
        }
    }

    @JvmOverloads
    fun previousSignificantChar(startPosition: Int = position): Char {
        val previousSignificantCharIndex = indexOfPreviousSignificantChar(startPosition)
        return if (previousSignificantCharIndex == -1) ' ' else charAt(previousSignificantCharIndex)
    }

    fun currentIsTail(): Boolean {
        return position >= endPosition
    }

    fun hasMoreCharacters(): Boolean {
        return inBounds(position + 1)
    }

    @JvmOverloads
    fun inBounds(idx: Int = position): Boolean {
        return idx >= 0 && idx <= endPosition
    }

    fun isOutOfBounds(idx: Int): Boolean {
        return !inBounds(idx)
    }

    fun subSequence(
        start: Int,
        end: Int,
    ): CharSequence {
        return charSequence.subSequence(start, end)
    }

    fun charSequence(): CharSequence {
        return charSequence
    }

    override fun toString(): String {
        return charSequence.toString()
    }

    fun isNumberCharacter(readPosition: Int): Boolean {
        val c = charAt(readPosition)
        // workaround for issue: https://github.com/json-path/JsonPath/issues/590
        return Character.isDigit(c) || c == MINUS || c == PERIOD || c == SCI_UPPER_E || c == SCI_LOWER_E
    }

    fun skipBlanks(): CharacterIndex {
        while (inBounds() && position < endPosition && currentChar() == SPACE) {
            incrementPosition(1)
        }
        return this
    }

    private fun skipBlanksAtEnd(): CharacterIndex {
        while (inBounds() && position < endPosition && lastCharIs(SPACE)) {
            decrementEndPosition(1)
        }
        return this
    }

    fun trim(): CharacterIndex {
        skipBlanks()
        skipBlanksAtEnd()
        return this
    }

    companion object {
        private const val OPEN_PARENTHESIS = '('
        private const val CLOSE_PARENTHESIS = ')'
        private const val CLOSE_SQUARE_BRACKET = ']'
        private const val SPACE = ' '
        private const val ESCAPE = '\\'
        private const val SINGLE_QUOTE = '\''
        private const val DOUBLE_QUOTE = '"'
        private const val MINUS = '-'
        private const val PERIOD = '.'
        private const val REGEX = '/'

        // workaround for issue: https://github.com/json-path/JsonPath/issues/590
        private const val SCI_UPPER_E = 'E'
        private const val SCI_LOWER_E = 'e'
    }
}
