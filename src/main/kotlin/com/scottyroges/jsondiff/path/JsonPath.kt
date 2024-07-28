package com.scottyroges.jsondiff.path

import com.scottyroges.jsondiff.path.token.RootPathToken

class JsonPath(val root: RootPathToken) {
    override fun toString(): String {
        return root.toString()
    }

    companion object {
        fun parse(path: String): JsonPath? {
            return PathCompiler.compile(path)
        }
    }
}
