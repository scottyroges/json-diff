package com.scottyroges.jsondiff.path.token

class RootPathToken(
    rootToken: Char,
) : PathToken() {
    private var tail: PathToken = this
    private var tokenCount = 0
    private val rootToken: String = rootToken.toString()

    fun getTail(): PathToken {
        return tail
    }

    fun setTail(token: PathToken?) {
        tail = token!!
    }

    override fun getTokenCount(): Int {
        return tokenCount
    }

    fun append(next: PathToken?): RootPathToken {
        tail = tail.appendTailToken(next)!!
        tokenCount++
        return this
    }

    fun getPathTokenAppender(): PathTokenAppender {
        return object : PathTokenAppender {
            override fun appendPathToken(next: PathToken): PathTokenAppender {
                append(next)
                return this
            }
        }
    }

    override fun getPathFragment(): String {
        return rootToken
    }

    override fun isTokenDefinite(): Boolean {
        return true
    }
}
