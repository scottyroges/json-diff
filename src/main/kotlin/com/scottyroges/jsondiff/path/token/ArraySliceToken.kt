package com.scottyroges.jsondiff.path.token

import com.scottyroges.jsondiff.path.InvalidPathException

class ArraySliceToken internal constructor(val operation: ArraySliceOperation) : ArrayPathToken() {
    override fun getPathFragment(): String {
        return operation.toString()
    }

    override fun isTokenDefinite(): Boolean {
        return false
    }
}

class ArraySliceOperation private constructor(
    private val from: Int?,
    private val to: Int?,
    private val operation: Operation,
) {
    enum class Operation {
        SLICE_FROM,
        SLICE_TO,
        SLICE_BETWEEN,
    }

    fun from(): Int? {
        return from
    }

    fun to(): Int? {
        return to
    }

    fun operation(): Operation {
        return operation
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("[")
        sb.append(from?.toString() ?: "")
        sb.append(":")
        sb.append(to?.toString() ?: "")
        sb.append("]")
        return sb.toString()
    }

    companion object {
        fun parse(operation: String): ArraySliceOperation {
            // check valid chars
            for (element in operation) {
                val c = element
                if (!Character.isDigit(c) && c != '-' && c != ':') {
                    throw InvalidPathException("Failed to parse SliceOperation: $operation")
                }
            }
            val tokens = operation.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val tempFrom = tryRead(tokens, 0)
            val tempTo = tryRead(tokens, 1)
            val tempOperation: Operation =
                if (tempFrom != null && tempTo == null) {
                    Operation.SLICE_FROM
                } else if (tempFrom != null) {
                    Operation.SLICE_BETWEEN
                } else if (tempTo != null) {
                    Operation.SLICE_TO
                } else {
                    throw InvalidPathException("Failed to parse SliceOperation: $operation")
                }
            return ArraySliceOperation(tempFrom, tempTo, tempOperation)
        }

        private fun tryRead(
            tokens: Array<String>,
            idx: Int,
        ): Int? {
            return if (tokens.size > idx) {
                if (tokens[idx] == "") {
                    null
                } else {
                    tokens[idx].toInt()
                }
            } else {
                null
            }
        }
    }
}
