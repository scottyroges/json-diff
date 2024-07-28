package com.scottyroges.jsondiff.path.token

interface PathTokenAppender {
    fun appendPathToken(next: PathToken): PathTokenAppender?
}
