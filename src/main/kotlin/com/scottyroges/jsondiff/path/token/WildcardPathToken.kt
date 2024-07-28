package com.scottyroges.jsondiff.path.token

class WildcardPathToken : PathToken() {
    override fun isTokenDefinite(): Boolean {
        return false
    }

    override fun getPathFragment(): String {
        return "[*]"
    }
}
