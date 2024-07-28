package com.scottyroges.jsondiff.path.token

class ScanPathToken internal constructor() : PathToken() {
    override fun isTokenDefinite(): Boolean {
        return false
    }

    override fun getPathFragment(): String {
        return ".."
    }
}
