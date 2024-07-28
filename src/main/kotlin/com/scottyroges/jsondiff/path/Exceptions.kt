package com.scottyroges.jsondiff.path

open class JsonPathException(override val message: String?, override val cause: Throwable?) : Exception(message, cause) {
    constructor(message: String?) : this(message, null)
    constructor(cause: Throwable?) : this(null, cause)
}

class InvalidPathException(override val message: String?, override val cause: Throwable?) : JsonPathException(message, cause) {
    constructor(message: String?) : this(message, null)
    constructor(cause: Throwable?) : this(null, cause)
}
