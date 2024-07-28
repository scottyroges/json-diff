package com.scottyroges.jsondiff.path.token

abstract class PathToken {
    var prev: PathToken? = null
    var next: PathToken? = null

    private var definite: Boolean? = null
    private var upstreamDefinite: Boolean? = null
    private var upstreamArrayIndex = -1

    abstract fun isTokenDefinite(): Boolean

    abstract fun getPathFragment(): String

    fun isUpstreamDefinite(): Boolean {
        if (upstreamDefinite == null) {
            upstreamDefinite = isRoot() || prev!!.isTokenDefinite() && prev!!.isUpstreamDefinite()
        }
        return upstreamDefinite!!
    }

    fun setUpstreamArrayIndex(idx: Int) {
        upstreamArrayIndex = idx
    }

    fun appendTailToken(next: PathToken?): PathToken? {
        this.next = next
        this.next!!.prev = this
        return next
    }

    fun isLeaf(): Boolean = next == null

    fun isRoot(): Boolean = prev == null

    open fun getTokenCount(): Int {
        var cnt = 1
        var token: PathToken? = this
        while (!token!!.isLeaf()) {
            token = token.next
            cnt++
        }
        return cnt
    }

    open fun isPathDefinite(): Boolean {
        if (definite != null) {
            return definite!!
        }
        var isDefinite: Boolean = isTokenDefinite()
        if (isDefinite && !isLeaf()) {
            isDefinite = next!!.isPathDefinite()
        }
        definite = isDefinite
        return isDefinite
    }

    override fun toString(): String {
        return if (isLeaf()) {
            getPathFragment()
        } else {
            getPathFragment() + next.toString()
        }
    }

    override fun hashCode(): Int {
        return toString().hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }
}
