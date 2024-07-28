package com.scottyroges.jsondiff.path

import com.scottyroges.jsondiff.path.token.ArrayIndexOperation
import com.scottyroges.jsondiff.path.token.ArraySliceOperation
import com.scottyroges.jsondiff.path.token.PathTokenAppender
import com.scottyroges.jsondiff.path.token.PathTokenFactory
import com.scottyroges.jsondiff.path.token.RootPathToken
import java.lang.Character.isDigit

class PathCompiler private constructor(
    private val path: CharacterIndex,
) {
    companion object {
        private val DOC_CONTEXT = '$'
        private val EVAL_CONTEXT = '@'

        private val OPEN_SQUARE_BRACKET = '['
        private val CLOSE_SQUARE_BRACKET = ']'
        private val OPEN_PARENTHESIS = '('
        private val CLOSE_PARENTHESIS = ')'
        private val OPEN_BRACE = '{'
        private val CLOSE_BRACE = '}'

        private val WILDCARD = '*'
        private val PERIOD = '.'
        private val SPACE = ' '
        private val TAB = '\t'
        private val CR = '\r'
        private val LF = '\n'
        private val BEGIN_FILTER = '?'
        private val COMMA = ','
        private val SPLIT = ':'
        private val MINUS = '-'
        private val SINGLE_QUOTE = '\''
        private val DOUBLE_QUOTE = '"'

        fun compile(path: String): JsonPath? {
            return try {
                var ci = CharacterIndex(path)
                ci.trim()
                if (ci.charAt(0) != DOC_CONTEXT && ci.charAt(0) != EVAL_CONTEXT) {
                    ci = CharacterIndex("$.$path")
                    ci.trim()
                }
                if (ci.lastCharIs('.')) {
                    fail("Path must not end with a '.' or '..'")
                }
                PathCompiler(ci).compile()
            } catch (e: Exception) {
                val ipe =
                    if (e is InvalidPathException) {
                        e
                    } else {
                        InvalidPathException(e)
                    }
                throw ipe
            }
        }

        fun fail(message: String?): Boolean {
            throw InvalidPathException(message)
        }
    }

    private fun compile(): JsonPath {
        val root = readContextToken()
        if (root.getPathFragment() != "$") {
            throw JsonPathException("Path must start with '$'")
        }
        return JsonPath(root)
    }

    private fun readWhitespace() {
        while (path.inBounds()) {
            val c = path.currentChar()
            if (!isWhitespace(c)) {
                break
            }
            path.incrementPosition(1)
        }
    }

    private fun isWhitespace(c: Char): Boolean {
        return c == SPACE || c == TAB || c == LF || c == CR
    }

    private fun readContextToken(): RootPathToken {
        readWhitespace()
        if (!isPathContext(path.currentChar())) {
            throw InvalidPathException("Path must start with '$' or '@'")
        }
        val pathToken: RootPathToken = PathTokenFactory.createRootPathToken(path.currentChar())
        if (path.currentIsTail()) {
            return pathToken
        }
        path.incrementPosition(1)
        if (path.currentChar() != PERIOD && path.currentChar() != OPEN_SQUARE_BRACKET) {
            fail("Illegal character at position " + path.position() + " expected '.' or '['")
        }
        val appender: PathTokenAppender = pathToken.getPathTokenAppender()
        readNextToken(appender)
        return pathToken
    }

    private fun isPathContext(c: Char): Boolean {
        return c == DOC_CONTEXT || c == EVAL_CONTEXT
    }

    private fun readDotToken(appender: PathTokenAppender): Boolean {
        if (path.currentCharIs(PERIOD) && path.nextCharIs(PERIOD)) {
            appender.appendPathToken(PathTokenFactory.createScanToken())
            path.incrementPosition(2)
        } else if (!path.hasMoreCharacters()) {
            throw InvalidPathException("Path must not end with a '.")
        } else {
            path.incrementPosition(1)
        }
        if (path.currentCharIs(PERIOD)) {
            throw InvalidPathException("Character '.' on position " + path.position() + " is not valid.")
        }
        return readNextToken(appender)
    }

    private fun readBracketPropertyToken(appender: PathTokenAppender): Boolean {
        if (!path.currentCharIs(OPEN_SQUARE_BRACKET)) {
            return false
        }
        val potentialStringDelimiter = path.nextSignificantChar()
        if (potentialStringDelimiter != SINGLE_QUOTE && potentialStringDelimiter != DOUBLE_QUOTE) {
            return false
        }
        val properties = mutableListOf<String>()
        var startPosition = path.position() + 1
        var readPosition = startPosition
        var endPosition = 0
        var inProperty = false
        var inEscape = false
        var lastSignificantWasComma = false
        while (path.inBounds(readPosition)) {
            val c = path.charAt(readPosition)
            if (inEscape) {
                inEscape = false
            } else if ('\\' == c) {
                inEscape = true
            } else if (c == CLOSE_SQUARE_BRACKET && !inProperty) {
                if (lastSignificantWasComma) {
                    fail("Found empty property at index $readPosition")
                }
                break
            } else if (c == potentialStringDelimiter) {
                if (inProperty) {
                    val nextSignificantChar = path.nextSignificantChar(readPosition)
                    if (nextSignificantChar != CLOSE_SQUARE_BRACKET && nextSignificantChar != COMMA) {
                        fail(
                            "Property must be separated by comma or Property must be terminated close square bracket at index $readPosition",
                        )
                    }
                    endPosition = readPosition
                    val prop = path.subSequence(startPosition, endPosition).toString()
                    val p = PathUtils.unescape(prop)
                    if (p != null) {
                        properties.add(p)
                    }
                    inProperty = false
                } else {
                    startPosition = readPosition + 1
                    inProperty = true
                    lastSignificantWasComma = false
                }
            } else if (c == COMMA && !inProperty) {
                if (lastSignificantWasComma) {
                    fail("Found empty property at index $readPosition")
                }
                lastSignificantWasComma = true
            }
            readPosition++
        }
        if (inProperty) {
            fail("Property has not been closed - missing closing $potentialStringDelimiter")
        }
        val endBracketIndex = path.indexOfNextSignificantChar(endPosition, CLOSE_SQUARE_BRACKET) + 1
        path.setPosition(endBracketIndex)
        appender.appendPathToken(PathTokenFactory.createPropertyPathToken(properties, potentialStringDelimiter))
        return path.currentIsTail() || readNextToken(appender)
    }

    private fun readArrayToken(appender: PathTokenAppender): Boolean {
        if (!path.currentCharIs(OPEN_SQUARE_BRACKET)) {
            return false
        }
        val nextSignificantChar = path.nextSignificantChar()
        if (!isDigit(nextSignificantChar) && nextSignificantChar != MINUS && nextSignificantChar != SPLIT) {
            return false
        }
        val expressionBeginIndex = path.position() + 1
        val expressionEndIndex = path.nextIndexOf(expressionBeginIndex, CLOSE_SQUARE_BRACKET)
        if (expressionEndIndex == -1) {
            return false
        }
        val expression = path.subSequence(expressionBeginIndex, expressionEndIndex).toString().trim { it <= ' ' }
        if ("*" == expression) {
            return false
        }

        // check valid chars
        for (element in expression) {
            val c = element
            if (!isDigit(c) && c != COMMA && c != MINUS && c != SPLIT && c != SPACE) {
                return false
            }
        }
        val isSliceOperation = expression.contains(":")
        if (isSliceOperation) {
            val arraySliceOperation: ArraySliceOperation = ArraySliceOperation.parse(expression)
            appender.appendPathToken(PathTokenFactory.createSliceArrayPathToken(arraySliceOperation))
        } else {
            val arrayIndexOperation: ArrayIndexOperation = ArrayIndexOperation.parse(expression)
            appender.appendPathToken(PathTokenFactory.createIndexArrayPathToken(arrayIndexOperation))
        }
        path.setPosition(expressionEndIndex + 1)
        return path.currentIsTail() || readNextToken(appender)
    }

    private fun readWildCardToken(appender: PathTokenAppender): Boolean {
        val inBracket = path.currentCharIs(OPEN_SQUARE_BRACKET)
        if (inBracket && !path.nextSignificantCharIs(WILDCARD)) {
            return false
        }
        if (!path.currentCharIs(WILDCARD) && path.isOutOfBounds(path.position() + 1)) {
            return false
        }
        if (inBracket) {
            val wildCardIndex = path.indexOfNextSignificantChar(WILDCARD)
            if (!path.nextSignificantCharIs(wildCardIndex, CLOSE_SQUARE_BRACKET)) {
                val offset = wildCardIndex + 1
                throw InvalidPathException("Expected wildcard token to end with ']' on position $offset")
            }
            val bracketCloseIndex = path.indexOfNextSignificantChar(wildCardIndex, CLOSE_SQUARE_BRACKET)
            path.setPosition(bracketCloseIndex + 1)
        } else {
            path.incrementPosition(1)
        }
        appender.appendPathToken(PathTokenFactory.createWildCardPathToken())
        return path.currentIsTail() || readNextToken(appender)
    }

    private fun readPropertyToken(appender: PathTokenAppender): Boolean {
        if (path.currentCharIs(OPEN_SQUARE_BRACKET) || path.currentCharIs(OPEN_PARENTHESIS) ||
            path.currentCharIs(
                WILDCARD,
            ) || path.currentCharIs(PERIOD) || path.currentCharIs(SPACE)
        ) {
            return false
        }
        val startPosition = path.position()
        var readPosition = startPosition
        var endPosition = 0
        while (path.inBounds(readPosition)) {
            val c = path.charAt(readPosition)
            if (c == SPACE) {
                throw InvalidPathException(
                    "Use bracket notion ['my prop'] if your property contains blank characters. position: " + path.position(),
                )
            } else if (c == PERIOD || c == OPEN_SQUARE_BRACKET) {
                endPosition = readPosition
                break
            }
            readPosition++
        }
        if (endPosition == 0) {
            endPosition = path.length()
        }
        path.setPosition(endPosition)

        val property = path.subSequence(startPosition, endPosition).toString()
        appender.appendPathToken(PathTokenFactory.createSinglePropertyPathToken(property, SINGLE_QUOTE))
        return path.currentIsTail() || readNextToken(appender)
    }

    private fun readNextToken(appender: PathTokenAppender): Boolean {
        val c = path.currentChar()
        return when (c) {
            OPEN_SQUARE_BRACKET -> {
                if (!readBracketPropertyToken(appender) && !readArrayToken(appender) && !readWildCardToken(appender)) {
                    fail("Could not parse token starting at position " + path.position() + ". Expected ?, ', 0-9, * ")
                }
                true
            }

            PERIOD -> {
                if (!readDotToken(appender)) {
                    fail("Could not parse token starting at position " + path.position())
                }
                true
            }

            WILDCARD -> {
                if (!readWildCardToken(appender)) {
                    fail("Could not parse token starting at position " + path.position())
                }
                true
            }

            else -> {
                if (!readPropertyToken(appender)) {
                    fail("Could not parse token starting at position " + path.position())
                }
                return true
            }
        }
    }
}
