package com.scottyroges.jsondiff.path.token

import com.scottyroges.jsondiff.path.InvalidPathException
import com.scottyroges.jsondiff.path.PathUtils
import java.util.regex.Pattern

abstract class ArrayPathToken : PathToken()

class ArrayIndexToken internal constructor(val arrayIndexOperation: ArrayIndexOperation) : ArrayPathToken() {
    override fun getPathFragment(): String {
        return arrayIndexOperation.toString()
    }

    override fun isTokenDefinite(): Boolean {
        return arrayIndexOperation.isSingleIndexOperation()
    }
}

class ArrayIndexOperation private constructor(
    val indexes: List<Int>,
) {
    fun isSingleIndexOperation(): Boolean = indexes.size == 1

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("[")
        sb.append(PathUtils.join(",", indexes))
        sb.append("]")
        return sb.toString()
    }

    companion object {
        private val COMMA: Pattern = Pattern.compile("\\s*,\\s*")

        fun parse(operation: String): ArrayIndexOperation {
            // check valid chars
            for (element in operation) {
                val c = element
                if (!Character.isDigit(c) && c != ',' && c != ' ' && c != '-') {
                    throw InvalidPathException("Failed to parse ArrayIndexOperation: $operation")
                }
            }
            val tokens: Array<String> = COMMA.split(operation, -1)
            val tempIndexes: MutableList<Int> = ArrayList(tokens.size)
            for (token in tokens) {
                tempIndexes.add(parseInteger(token))
            }
            return ArrayIndexOperation(tempIndexes)
        }

        private fun parseInteger(token: String): Int {
            return try {
                token.toInt()
            } catch (e: java.lang.Exception) {
                throw InvalidPathException("Failed to parse token in ArrayIndexOperation: $token", e)
            }
        }
    }
}
