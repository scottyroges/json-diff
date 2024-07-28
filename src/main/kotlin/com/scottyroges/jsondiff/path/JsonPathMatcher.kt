package com.scottyroges.jsondiff.path

import com.scottyroges.jsondiff.path.token.ArrayIndexToken
import com.scottyroges.jsondiff.path.token.ArraySliceOperation
import com.scottyroges.jsondiff.path.token.ArraySliceToken
import com.scottyroges.jsondiff.path.token.PathToken
import com.scottyroges.jsondiff.path.token.PropertyPathToken
import com.scottyroges.jsondiff.path.token.ScanPathToken
import com.scottyroges.jsondiff.path.token.WildcardPathToken
import mu.KotlinLogging

/**
 This (and the down tree classes) was heavily borrowed from https://github.com/json-path/JsonPath
 and modified to fit our needs.

 Supports the following:
 Operator                  |    Description
 $	                       |    The root element to query. This starts all path expressions.
 *	                       |    Wildcard. Available anywhere a name or numeric are required.
 ..	                       |    Deep scan. Available anywhere a name is required.
 .<name>	               |    Dot-notated child
 ['<name>' (, '<name>')]   |	Bracket-notated child or children
 [<number> (, <number>)]   |	Array index or indexes
 [start:end]	           |    Array slice operator

 Examples:
 $.store.book[*].author
 $..author
 $.store.*
 $['store','website']['book','magazine'][*]['author']
 $.store.book[2:]
 $.store.book[:2]
 $.store.book[1:2]
 $.store.book[1,5,9]
 */
data class JsonPathMatcher(
    val matcherPath: JsonPath?,
) {
    private val logger = KotlinLogging.logger {}

    fun matches(path: JsonPath?): Boolean {
        return try {
            if (path == null || matcherPath == null) {
                return false
            }
            var matcherPathPointer: PathToken? = matcherPath.root
            var currentPathPointer: PathToken? = path.root

            var isMatched = true
            while (matcherPathPointer?.next != null) {
                matcherPathPointer = matcherPathPointer.next!!
                when (matcherPathPointer) {
                    is PropertyPathToken -> {
                        currentPathPointer = currentPathPointer?.next
                        if (currentPathPointer is PropertyPathToken) {
                            val currentPropertyPathToken = currentPathPointer
                            if (matcherPathPointer.properties.none { currentPropertyPathToken.hasProperty(it) }) {
                                isMatched = false
                            }
                        } else {
                            isMatched = false
                        }
                    }
                    is ArrayIndexToken -> {
                        currentPathPointer = currentPathPointer?.next
                        if (currentPathPointer is ArrayIndexToken) {
                            val currentPropertyPathToken = currentPathPointer
                            if (matcherPathPointer.arrayIndexOperation.indexes.none {
                                    currentPropertyPathToken.arrayIndexOperation.indexes.contains(
                                        it,
                                    )
                                }
                            ) {
                                isMatched = false
                            }
                        } else {
                            isMatched = false
                        }
                    }
                    is ArraySliceToken -> {
                        currentPathPointer = currentPathPointer?.next
                        if (currentPathPointer is ArrayIndexToken) {
                            val currentPropertyPathToken = currentPathPointer
                            when (matcherPathPointer.operation.operation()) {
                                ArraySliceOperation.Operation.SLICE_FROM -> {
                                    if (currentPropertyPathToken.arrayIndexOperation.indexes.first() < matcherPathPointer.operation.from()!!) {
                                        isMatched = false
                                    }
                                }
                                ArraySliceOperation.Operation.SLICE_TO -> {
                                    if (currentPropertyPathToken.arrayIndexOperation.indexes.first() > matcherPathPointer.operation.to()!!) {
                                        isMatched = false
                                    }
                                }
                                ArraySliceOperation.Operation.SLICE_BETWEEN -> {
                                    if (currentPropertyPathToken.arrayIndexOperation.indexes.first() < matcherPathPointer.operation.from()!! ||
                                        currentPropertyPathToken.arrayIndexOperation.indexes.first() > matcherPathPointer.operation.to()!!
                                    ) {
                                        isMatched = false
                                    }
                                }
                            }
                        } else {
                            isMatched = false
                        }
                    }
                    is WildcardPathToken -> {
                        currentPathPointer = currentPathPointer?.next
                        if (currentPathPointer !is PropertyPathToken && currentPathPointer !is ArrayIndexToken) {
                            isMatched = false
                        }
                    }
                    is ScanPathToken -> {
                        val tokenAfterScan = matcherPathPointer.next
                        if (tokenAfterScan is PropertyPathToken) {
                            var foundNextToken = false
                            currentPathPointer = currentPathPointer?.next
                            while (!foundNextToken && currentPathPointer != null) {
                                if (currentPathPointer is PropertyPathToken) {
                                    val currentPropertyPathToken = currentPathPointer
                                    if (tokenAfterScan.properties.any { currentPropertyPathToken.hasProperty(it) }) {
                                        foundNextToken = true
                                        break
                                    }
                                }
                                currentPathPointer = currentPathPointer.next
                            }

                            if (!foundNextToken) {
                                isMatched = false
                            }
                            matcherPathPointer = matcherPathPointer.next
                        } else {
                            throw InvalidPathException("Scan token must be followed by a property token")
                        }
                    }
                }
            }
            // still more tokens on the current path
            if (currentPathPointer?.next != null) {
                isMatched = false
            }
            isMatched
        } catch (e: Exception) {
            logger.info("issue evaluating path $matcherPath and $path", e)
            false
        }
    }

    companion object {
        fun parse(path: String): JsonPathMatcher {
            return JsonPathMatcher(JsonPath.parse(path))
        }
    }
}
