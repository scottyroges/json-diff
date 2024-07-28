package com.scottyroges.jsondiff.path

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class JsonPathMatcherTest {
    @Test
    fun `test json path matcher dot notated child`() {
        val match = JsonPathMatcher.parse("$.field1").matches(JsonPath.parse("$.field1"))
        assertEquals(true, match)

        val match2 = JsonPathMatcher.parse("$.field1").matches(JsonPath.parse("$.field2"))
        assertEquals(false, match2)
    }

    @Test
    fun `test json path matcher dot notated child more specific`() {
        val match = JsonPathMatcher.parse("$.field1").matches(JsonPath.parse("$.field1.field3"))
        assertEquals(false, match)

        val match2 = JsonPathMatcher.parse("$.field1").matches(JsonPath.parse("$.field2.field3"))
        assertEquals(false, match2)
    }

    @Test
    fun `test json path matcher bracket notated child`() {
        val match = JsonPathMatcher.parse("$['field1']").matches(JsonPath.parse("$.field1"))
        assertEquals(true, match)

        val match2 = JsonPathMatcher.parse("$['field1']").matches(JsonPath.parse("$.field2"))
        assertEquals(false, match2)
    }

    @Test
    fun `test json path matcher bracket notated child, multiple fields`() {
        val match = JsonPathMatcher.parse("$['field1','field3']").matches(JsonPath.parse("$.field1"))
        assertEquals(true, match)

        val match2 = JsonPathMatcher.parse("$['field1','field3']").matches(JsonPath.parse("$.field2"))
        assertEquals(false, match2)

        val match3 = JsonPathMatcher.parse("$['field1','field3']").matches(JsonPath.parse("$.field3"))
        assertEquals(true, match3)
    }

    @Test
    fun `test json path matcher dot notated child nested`() {
        val match = JsonPathMatcher.parse("$.field1.nested1").matches(JsonPath.parse("$.field1.nested1"))
        assertEquals(true, match)

        val match2 = JsonPathMatcher.parse("$.field1.nested1").matches(JsonPath.parse("$.field1.nested2"))
        assertEquals(false, match2)
    }

    @Test
    fun `test json path matcher bracket notated child nested`() {
        val match = JsonPathMatcher.parse("$['field1']['nested1']").matches(JsonPath.parse("$.field1.nested1"))
        assertEquals(true, match)

        val match2 = JsonPathMatcher.parse("$['field1']['nested1']").matches(JsonPath.parse("$.field1.nested2"))
        assertEquals(false, match2)
    }

    @Test
    fun `test json path matcher mix notated child nested`() {
        val match = JsonPathMatcher.parse("$.field1['nested1']").matches(JsonPath.parse("$.field1.nested1"))
        assertEquals(true, match)

        val match2 = JsonPathMatcher.parse("$.field1['nested1']").matches(JsonPath.parse("$.field1.nested2"))
        assertEquals(false, match2)
    }

    @Test
    fun `test json path matcher wildcard dot notated child`() {
        val match = JsonPathMatcher.parse("$.field1.*").matches(JsonPath.parse("$.field1.field2"))
        assertEquals(true, match)

        val match2 = JsonPathMatcher.parse("$.field1.*").matches(JsonPath.parse("$.field1.field3"))
        assertEquals(true, match2)
    }

    @Test
    fun `test json path matcher wildcard bracket notated child`() {
        val match = JsonPathMatcher.parse("$.field1[*]").matches(JsonPath.parse("$.field1.field2"))
        assertEquals(true, match)

        val match2 = JsonPathMatcher.parse("$.field1[*]").matches(JsonPath.parse("$.field1.field3"))
        assertEquals(true, match2)
    }

    @Test
    fun `test json path matcher array index`() {
        val match = JsonPathMatcher.parse("$.field1[1]").matches(JsonPath.parse("$.field1[1]"))
        assertEquals(true, match)

        val match2 = JsonPathMatcher.parse("$.field1[1]").matches(JsonPath.parse("$.field1[2]"))
        assertEquals(false, match2)
    }

    @Test
    fun `test json path matcher array index, multiple indexes`() {
        val match = JsonPathMatcher.parse("$.field1[1,3]").matches(JsonPath.parse("$.field1[1]"))
        assertEquals(true, match)

        val match2 = JsonPathMatcher.parse("$.field1[1,3]").matches(JsonPath.parse("$.field1[2]"))
        assertEquals(false, match2)

        val match3 = JsonPathMatcher.parse("$.field1[1,3]").matches(JsonPath.parse("$.field1[3]"))
        assertEquals(true, match3)
    }

    @Test
    fun `test json path matcher array index, array slice between`() {
        val match = JsonPathMatcher.parse("$.field1[2:4]").matches(JsonPath.parse("$.field1[1]"))
        assertEquals(false, match)

        val match2 = JsonPathMatcher.parse("$.field1[2:4]").matches(JsonPath.parse("$.field1[2]"))
        assertEquals(true, match2)

        val match3 = JsonPathMatcher.parse("$.field1[2:4]").matches(JsonPath.parse("$.field1[3]"))
        assertEquals(true, match3)

        val match4 = JsonPathMatcher.parse("$.field1[2:4]").matches(JsonPath.parse("$.field1[4]"))
        assertEquals(true, match4)

        val match5 = JsonPathMatcher.parse("$.field1[2:4]").matches(JsonPath.parse("$.field1[5]"))
        assertEquals(false, match5)
    }

    @Test
    fun `test json path matcher array index, array slice from`() {
        val match = JsonPathMatcher.parse("$.field1[2:]").matches(JsonPath.parse("$.field1[1]"))
        assertEquals(false, match)

        val match2 = JsonPathMatcher.parse("$.field1[2:]").matches(JsonPath.parse("$.field1[2]"))
        assertEquals(true, match2)

        val match3 = JsonPathMatcher.parse("$.field1[2:]").matches(JsonPath.parse("$.field1[3]"))
        assertEquals(true, match3)
    }

    @Test
    fun `test json path matcher array index, array slice to`() {
        val match = JsonPathMatcher.parse("$.field1[:2]").matches(JsonPath.parse("$.field1[1]"))
        assertEquals(true, match)

        val match2 = JsonPathMatcher.parse("$.field1[:2]").matches(JsonPath.parse("$.field1[2]"))
        assertEquals(true, match2)

        val match3 = JsonPathMatcher.parse("$.field1[:2]").matches(JsonPath.parse("$.field1[3]"))
        assertEquals(false, match3)
    }

    @Test
    fun `test json path matcher wildcard array`() {
        val match = JsonPathMatcher.parse("$.field1[*]").matches(JsonPath.parse("$.field1[1]"))
        assertEquals(true, match)

        val match2 = JsonPathMatcher.parse("$.field1[*]").matches(JsonPath.parse("$.field1[6]"))
        assertEquals(true, match2)
    }

    @Test
    fun `test json path matcher wildcard array complex`() {
        val match = JsonPathMatcher.parse("$.field1[*].field2").matches(JsonPath.parse("$.field1[5].field2"))
        assertEquals(true, match)

        val match2 = JsonPathMatcher.parse("$.field1[*].field2").matches(JsonPath.parse("$.field1[10].field2"))
        assertEquals(true, match2)
    }

    @Test
    fun `test json path matcher deep scan`() {
        val match = JsonPathMatcher.parse("$..field2").matches(JsonPath.parse("$.field1[5].field2"))
        assertEquals(true, match)

        val match2 = JsonPathMatcher.parse("$..field2").matches(JsonPath.parse("$.field1.field3.field4.field2"))
        assertEquals(true, match2)

        val match3 = JsonPathMatcher.parse("$..field2").matches(JsonPath.parse("$.field1.field3.field4.field8"))
        assertEquals(false, match3)
    }

    @Test
    fun `test json path matcher deep scan, multiple`() {
        val match =
            JsonPathMatcher.parse(
                "$..field2..field7[0]",
            ).matches(JsonPath.parse("$.field1[5].field2.field3['field4'][0].field7[0]"))
        assertEquals(true, match)

        val match2 =
            JsonPathMatcher.parse(
                "$..field2..field7[0]",
            ).matches(JsonPath.parse("$.field1.field3.field4.field2.field6.field5.field7[0]"))
        assertEquals(true, match2)

        val match3 =
            JsonPathMatcher.parse(
                "$..field2..field7[0]",
            ).matches(JsonPath.parse("$.field1.field3.field4.field2.field6.field5.field7[1]"))
        assertEquals(false, match3)
    }

    @Test
    fun `test json path matcher deep scan more specific`() {
        val match =
            JsonPathMatcher.parse(
                "$..field2..field7[0]",
            ).matches(JsonPath.parse("$.field1[5].field2.field3['field4'][0].field7[0].field8"))
        assertEquals(false, match)
    }
}
