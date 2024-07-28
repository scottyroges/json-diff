package com.scottyroges.jsondiff.path

import java.io.Closeable
import java.io.IOException
import java.io.StringWriter
import java.util.Locale

object PathUtils {
    // accept a collection of objects, since all objects have toString()
    fun join(
        delimiter: String?,
        wrap: String?,
        objs: Iterable<*>,
    ): String {
        val iter = objs.iterator()
        if (!iter.hasNext()) {
            return ""
        }
        val buffer = StringBuilder()
        buffer.append(wrap).append(iter.next()).append(wrap)
        while (iter.hasNext()) {
            buffer.append(delimiter).append(wrap).append(iter.next()).append(wrap)
        }
        return buffer.toString()
    }

    // accept a collection of objects, since all objects have toString()
    fun join(
        delimiter: String?,
        objs: Iterable<*>,
    ): String {
        return join(delimiter, "", objs)
    }

    fun concat(vararg strings: CharSequence): String {
        if (strings.size == 0) {
            return ""
        }
        if (strings.size == 1) {
            return strings[0].toString()
        }
        var length = 0
        // -1 = no result, -2 = multiple results
        var indexOfSingleNonEmptyString = -1
        for (i in strings.indices) {
            val charSequence = strings[i]
            val len = charSequence.length
            length += len
            if (indexOfSingleNonEmptyString != -2 && len > 0) {
                indexOfSingleNonEmptyString =
                    if (indexOfSingleNonEmptyString == -1) {
                        i
                    } else {
                        -2
                    }
            }
        }
        if (length == 0) {
            return ""
        }
        if (indexOfSingleNonEmptyString > 0) {
            return strings[indexOfSingleNonEmptyString].toString()
        }
        val sb = StringBuilder(length)
        for (charSequence in strings) {
            sb.append(charSequence)
        }
        return sb.toString()
    }

    // ---------------------------------------------------------
    //
    // IO
    //
    // ---------------------------------------------------------
    fun closeQuietly(closeable: Closeable?) {
        try {
            closeable?.close()
        } catch (ignore: IOException) {
        }
    }

    fun escape(
        str: String?,
        escapeSingleQuote: Boolean,
    ): String? {
        if (str == null) {
            return null
        }
        val len = str.length
        val writer = StringWriter(len * 2)
        for (i in 0 until len) {
            val ch = str[i]

            // handle unicode
            if (ch.code > 0xfff) {
                writer.write("\\u" + hex(ch))
            } else if (ch.code > 0xff) {
                writer.write("\\u0" + hex(ch))
            } else if (ch.code > 0x7f) {
                writer.write("\\u00" + hex(ch))
            } else if (ch.code < 32) {
                when (ch) {
                    '\b' -> {
                        writer.write('\\'.code)
                        writer.write('b'.code)
                    }

                    '\n' -> {
                        writer.write('\\'.code)
                        writer.write('n'.code)
                    }

                    '\t' -> {
                        writer.write('\\'.code)
                        writer.write('t'.code)
                    }

                    '\u000c' -> {
                        writer.write('\\'.code)
                        writer.write('f'.code)
                    }

                    '\r' -> {
                        writer.write('\\'.code)
                        writer.write('r'.code)
                    }

                    else ->
                        if (ch.code > 0xf) {
                            writer.write("\\u00" + hex(ch))
                        } else {
                            writer.write("\\u000" + hex(ch))
                        }
                }
            } else {
                when (ch) {
                    '\'' -> {
                        if (escapeSingleQuote) {
                            writer.write('\\'.code)
                        }
                        writer.write('\''.code)
                    }

                    '"' -> {
                        writer.write('\\'.code)
                        writer.write('"'.code)
                    }

                    '\\' -> {
                        writer.write('\\'.code)
                        writer.write('\\'.code)
                    }

                    '/' -> {
                        writer.write('\\'.code)
                        writer.write('/'.code)
                    }

                    else -> writer.write(ch.code)
                }
            }
        }
        return writer.toString()
    }

    fun unescape(str: String?): String? {
        if (str == null) {
            return null
        }
        val len = str.length
        val writer = StringWriter(len)
        val unicode = StringBuilder(4)
        var hadSlash = false
        var inUnicode = false
        for (i in 0 until len) {
            val ch = str[i]
            if (inUnicode) {
                unicode.append(ch)
                if (unicode.length == 4) {
                    try {
                        val value = unicode.toString().toInt(16)
                        writer.write(value.toChar().code)
                        unicode.setLength(0)
                        inUnicode = false
                        hadSlash = false
                    } catch (nfe: NumberFormatException) {
                        throw JsonPathException("Unable to parse unicode value: $unicode", nfe)
                    }
                }
                continue
            }
            if (hadSlash) {
                hadSlash = false
                when (ch) {
                    '\\' -> writer.write('\\'.code)
                    '\'' -> writer.write('\''.code)
                    '\"' -> writer.write('"'.code)
                    'r' -> writer.write('\r'.code)
                    'f' -> writer.write('\u000c'.code)
                    't' -> writer.write('\t'.code)
                    'n' -> writer.write('\n'.code)
                    'b' -> writer.write('\b'.code)
                    'u' -> {
                        inUnicode = true
                    }

                    else -> writer.write(ch.code)
                }
                continue
            } else if (ch == '\\') {
                hadSlash = true
                continue
            }
            writer.write(ch.code)
        }
        if (hadSlash) {
            writer.write('\\'.code)
        }
        return writer.toString()
    }

    /**
     * Returns an upper case hexadecimal `String` for the given
     * character.
     *
     * @param ch The character to map.
     * @return An upper case hexadecimal `String`
     */
    fun hex(ch: Char): String {
        return Integer.toHexString(ch.code).uppercase(Locale.getDefault())
    }

    /**
     *
     * Checks if a CharSequence is empty ("") or null.
     *
     *
     * <pre>
     * StringUtils.isEmpty(null)      = true
     * StringUtils.isEmpty("")        = true
     * StringUtils.isEmpty(" ")       = false
     * StringUtils.isEmpty("bob")     = false
     * StringUtils.isEmpty("  bob  ") = false
     </pre> *
     *
     *
     *
     * NOTE: This method changed in Lang version 2.0.
     * It no longer trims the CharSequence.
     * That functionality is available in isBlank().
     *
     * @param cs the CharSequence to check, may be null
     * @return `true` if the CharSequence is empty or null
     * @since 3.0 Changed signature from isEmpty(String) to isEmpty(CharSequence)
     */
    fun isEmpty(cs: CharSequence?): Boolean {
        return cs == null || cs.length == 0
    }

    /**
     * Used by the indexOf(CharSequence methods) as a green implementation of indexOf.
     *
     * @param cs         the `CharSequence` to be processed
     * @param searchChar the `CharSequence` to be searched for
     * @param start      the start index
     * @return the index where the search sequence was found
     */
    fun indexOf(
        cs: CharSequence,
        searchChar: CharSequence,
        start: Int,
    ): Int {
        return cs.toString().indexOf(searchChar.toString(), start)
    }
    // ---------------------------------------------------------
    //
    // Validators
    //
    // ---------------------------------------------------------

    /**
     *
     * Validate that the specified argument is not `null`;
     * otherwise throwing an exception with the specified message.
     *
     *
     * <pre>Validate.notNull(myObject, "The object must not be null");</pre>
     *
     * @param <T>     the object type
     * @param object  the object to check
     * @param message the [String.format] exception message if invalid, not null
     * @return the validated object (never `null` for method chaining)
     * @throws NullPointerException if the object is `null`
     </T> */
    fun <T> notNull(
        o: T?,
        message: String,
    ): T {
        requireNotNull(o) { message }
        return o
    }

    /**
     *
     * Validate that the specified argument is not `null`;
     * otherwise throwing an exception with the specified message.
     *
     *
     * <pre>Validate.notNull(myObject, "The object must not be null");</pre>
     *
     * @param <T>     the object type
     * @param object  the object to check
     * @param message the [String.format] exception message if invalid, not null
     * @param values  the optional values for the formatted exception message
     * @return the validated object (never `null` for method chaining)
     * @throws NullPointerException if the object is `null`
     </T> */
    fun <T> notNull(
        `object`: T?,
        message: String?,
        vararg values: Any?,
    ): T {
        requireNotNull(`object`) { String.format(message!!, *values) }
        return `object`
    }

    /**
     *
     * Validate that the argument condition is `true`; otherwise
     * throwing an exception with the specified message. This method is useful when
     * validating according to an arbitrary boolean expression, such as validating a
     * primitive number or using your own custom validation expression.
     *
     *
     * <pre>Validate.isTrue(i > 0.0, "The value must be greater than zero: %d", i);</pre>
     *
     *
     *
     * For performance reasons, the long value is passed as a separate parameter and
     * appended to the exception message only in the case of an error.
     *
     * @param expression the boolean expression to check
     * @param message
     * @throws IllegalArgumentException if expression is `false`
     */
    fun isTrue(
        expression: Boolean,
        message: String,
    ) {
        require(expression != false) { message }
    }

    /**
     * Check if one and only one condition is true; otherwise
     * throw an exception with the specified message.
     *
     * @param message     error describing message
     * @param expressions the boolean expressions to check
     * @throws IllegalArgumentException if zero or more than one expressions are true
     */
    fun onlyOneIsTrue(
        message: String,
        vararg expressions: Boolean,
    ) {
        require(onlyOneIsTrueNonThrow(*expressions)) { message }
    }

    fun onlyOneIsTrueNonThrow(vararg expressions: Boolean): Boolean {
        var count = 0
        for (expression in expressions) {
            if (expression && ++count > 1) {
                return false
            }
        }
        return 1 == count
    }

    /**
     *
     * Validate that the specified argument character sequence is
     * neither `null` nor a length of zero (no characters);
     * otherwise throwing an exception with the specified message.
     *
     *
     * <pre>Validate.notEmpty(myString, "The string must not be empty");</pre>
     *
     * @param <T>     the character sequence type
     * @param chars   the character sequence to check, validated not null by this method
     * @param message the [String.format] exception message if invalid, not null
     * @return the validated character sequence (never `null` method for chaining)
     * @throws NullPointerException     if the character sequence is `null`
     * @throws IllegalArgumentException if the character sequence is empty
     </T> */
    fun <T : CharSequence?> notEmpty(
        chars: T?,
        message: String,
    ): T {
        require(!(chars == null || chars.length == 0)) { message }
        return chars
    }

    /**
     *
     * Validate that the specified argument character sequence is
     * neither `null` nor a length of zero (no characters);
     * otherwise throwing an exception with the specified message.
     *
     *
     * <pre>Validate.notEmpty(myString, "The string must not be empty");</pre>
     *
     * @param bytes   the bytes to check, validated not null by this method
     * @param message the [String.format] exception message if invalid, not null
     * @return the validated character sequence (never `null` method for chaining)
     * @throws NullPointerException     if the character sequence is `null`
     * @throws IllegalArgumentException if the character sequence is empty
     */
    fun notEmpty(
        bytes: ByteArray?,
        message: String,
    ): ByteArray {
        require(!(bytes == null || bytes.size == 0)) { message }
        return bytes
    }

    /**
     *
     * Validate that the specified argument character sequence is
     * neither `null` nor a length of zero (no characters);
     * otherwise throwing an exception with the specified message.
     *
     *
     * <pre>Validate.notEmpty(myString, "The string must not be empty");</pre>
     *
     * @param <T>     the character sequence type
     * @param chars   the character sequence to check, validated not null by this method
     * @param message the [String.format] exception message if invalid, not null
     * @param values  the optional values for the formatted exception message, null array not recommended
     * @return the validated character sequence (never `null` method for chaining)
     * @throws NullPointerException     if the character sequence is `null`
     * @throws IllegalArgumentException if the character sequence is empty
     </T> */
    fun <T : CharSequence?> notEmpty(
        chars: T?,
        message: String?,
        vararg values: Any?,
    ): T {
        require(!(chars == null || chars.length == 0)) { String.format(message!!, *values) }
        return chars
    }

    // ---------------------------------------------------------
    //
    // Converters
    //
    // ---------------------------------------------------------
    fun toString(o: Any?): String? {
        return o?.toString()
    }
}
