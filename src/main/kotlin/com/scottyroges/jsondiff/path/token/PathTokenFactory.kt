package com.scottyroges.jsondiff.path.token

object PathTokenFactory {
    fun createRootPathToken(token: Char): RootPathToken {
        return RootPathToken(token)
    }

    fun createSinglePropertyPathToken(
        property: String,
        stringDelimiter: Char,
    ): PathToken {
        return PropertyPathToken(listOf(property), stringDelimiter)
    }

    fun createPropertyPathToken(
        properties: List<String>,
        stringDelimiter: Char,
    ): PathToken {
        return PropertyPathToken(properties, stringDelimiter)
    }

    fun createSliceArrayPathToken(arraySliceOperation: ArraySliceOperation): PathToken {
        return ArraySliceToken(arraySliceOperation)
    }

    fun createIndexArrayPathToken(arrayIndexOperation: ArrayIndexOperation): PathToken {
        return ArrayIndexToken(arrayIndexOperation)
    }

    fun createWildCardPathToken(): PathToken {
        return WildcardPathToken()
    }

    fun createScanToken(): PathToken {
        return ScanPathToken()
    }
}
