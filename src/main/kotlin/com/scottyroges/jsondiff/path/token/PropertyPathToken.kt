package com.scottyroges.jsondiff.path.token

import com.scottyroges.jsondiff.path.InvalidPathException
import com.scottyroges.jsondiff.path.PathUtils

internal class PropertyPathToken(properties: List<String>, stringDelimiter: Char) : PathToken() {
    val properties: List<String>
    private val stringDelimiter: String

    init {
        if (properties.isEmpty()) {
            throw InvalidPathException("Empty properties")
        }
        this.properties = properties
        this.stringDelimiter = Character.toString(stringDelimiter)
    }

    fun singlePropertyCase(): Boolean {
        return properties.size == 1
    }

    fun multiPropertyMergeCase(): Boolean {
        return isLeaf() && properties.size > 1
    }

    fun multiPropertyIterationCase(): Boolean {
        // Semantics of this case is the same as semantics of ArrayPathToken with INDEX_SEQUENCE operation.
        return !isLeaf() && properties.size > 1
    }

    fun hasProperty(p: String): Boolean {
        return properties.contains(p)
    }

    override fun isTokenDefinite(): Boolean {
        // in case of leaf multiprops will be merged, so it's kinda definite
        return singlePropertyCase() || multiPropertyMergeCase()
    }

    override fun getPathFragment(): String {
        return StringBuilder()
            .append("[")
            .append(PathUtils.join(",", stringDelimiter, properties))
            .append("]").toString()
    }
}
