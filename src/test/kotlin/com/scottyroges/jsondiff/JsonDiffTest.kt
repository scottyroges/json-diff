package com.scottyroges.jsondiff

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import com.scottyroges.jsondiff.path.JsonPathMatcher
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class JsonDiffTest {
    private val jacksonObjectMapper = jacksonObjectMapper()

    @Test
    fun `diff value change`() {
        val json1 =
            """
            {
               "field1": "test"            
            }
            """.trimIndent()

        val json2 =
            """
            {
               "field1": "test2"            
            }
            """.trimIndent()

        val diff = JsonDiff().diff(jacksonObjectMapper.readTree(json1), jacksonObjectMapper.readTree(json2))
        assertEquals(1, diff.size)
        assertEquals("\$.field1: value mismatch test != test2", diff[0])
    }

    @Test
    fun `diff numeric value change text compare`() {
        val json1 =
            """
            {
               "field1": 1.0            
            }
            """.trimIndent()

        val json2 =
            """
            {
               "field1": 1            
            }
            """.trimIndent()

        val diff = JsonDiff().diff(jacksonObjectMapper.readTree(json1), jacksonObjectMapper.readTree(json2))
        assertEquals(1, diff.size)
        assertEquals("\$.field1: value mismatch 1.0 != 1", diff[0])
    }

    @Test
    fun `diff numeric value change number compare`() {
        val json1 =
            """
            {
               "field1": 1.0            
            }
            """.trimIndent()

        val json2 =
            """
            {
               "field1": 1            
            }
            """.trimIndent()

        val config =
            JsonDiffConfig(
                relaxedNumberEquality = true,
            )
        val diff = JsonDiff(config).diff(jacksonObjectMapper.readTree(json1), jacksonObjectMapper.readTree(json2))
        assertEquals(0, diff.size)
    }

    @Test
    fun `diff value change, ignore`() {
        val json1 =
            """
            {
               "field1": "test"            
            }
            """.trimIndent()

        val json2 =
            """
            {
               "field1": "test2"            
            }
            """.trimIndent()

        val config =
            JsonDiffConfig(
                ignorePaths = listOf(JsonPathMatcher.parse("$.field1")),
            )
        val diff = JsonDiff(config).diff(jacksonObjectMapper.readTree(json1), jacksonObjectMapper.readTree(json2))
        assertEquals(0, diff.size)
    }

    @Test
    fun `diff value only in one, ignore`() {
        val json1 =
            """
            {
               "field1": "test",
               "field2": "ignore"
            }
            """.trimIndent()

        val json2 =
            """
            {
               "field1": "test"            
            }
            """.trimIndent()

        val config =
            JsonDiffConfig(
                ignorePaths = listOf(JsonPathMatcher.parse("$.field2")),
            )
        val diff = JsonDiff(config).diff(jacksonObjectMapper.readTree(json1), jacksonObjectMapper.readTree(json2))
        assertEquals(0, diff.size)
    }

    @Test
    fun `diff order change`() {
        val json1 =
            """
            {
               "field1": "test",
               "field2": "test2"
            }
            """.trimIndent()

        val json2 =
            """
            {
               "field2": "test2",
               "field1": "test"            
            }
            """.trimIndent()

        val diff = JsonDiff().diff(jacksonObjectMapper.readTree(json1), jacksonObjectMapper.readTree(json2))
        assertEquals(0, diff.size)
    }

    @Test
    fun `diff simple array order, no validation on order`() {
        val json1 =
            """
            {
               "field1": ["a", "b", "c"]
            }
            """.trimIndent()

        val json2 =
            """
            {
               "field1": ["c", "b", "a"]   
            }
            """.trimIndent()
        val config =
            JsonDiffConfig(
                ignoreArrayOrderOnlyForPaths = listOf(JsonPathMatcher.parse("$.field1")),
            )
        val diff = JsonDiff(config).diff(jacksonObjectMapper.readTree(json1), jacksonObjectMapper.readTree(json2))
        assertEquals(0, diff.size)
    }

    @Test
    fun `diff simple array order, validation on order`() {
        val json1 =
            """
            {
               "field1": ["a", "b", "c"]
            }
            """.trimIndent()

        val json2 =
            """
            {
               "field1": ["c", "b", "a"]   
            }
            """.trimIndent()

        val config =
            JsonDiffConfig(
                ignoreArrayOrder = true,
                validateArrayOrderOnlyForPaths = listOf(JsonPathMatcher.parse("$.field1")),
            )
        val diff = JsonDiff(config).diff(jacksonObjectMapper.readTree(json1), jacksonObjectMapper.readTree(json2))
        assertEquals(2, diff.size)
        assertEquals("\$.field1[0]: value mismatch a != c", diff[0])
        assertEquals("\$.field1[2]: value mismatch c != a", diff[1])
    }

    @Test
    fun `diff complex object array order`() {
        val json1 =
            """
            {
                "superHeroes": [
                {
                    "name": "Spider-man",
                    "powers": ["web", "spider-sense"],
                    "enemies": [
                    {
                        "name": "Green Goblin",
                        "powers": ["superhuman strength", "flying glider"]
                    },
                    {
                        "name": "Venom",
                        "powers": ["superhuman strength", "web"]
                    }]
                },
                {
                    "name": "Superman",
                    "powers": ["flight", "superhuman strength"],
                    "enemies": [
                    {
                        "name": "Lex Luthor",
                        "powers": ["superhuman intelligence"]
                    }]
                },
                {
                    "name": "Ironman",
                    "powers": ["rich", "smart"],
                    "enemies": [
                    {
                        "name": "Thanos",
                        "powers": ["superhuman strength"]
                    },
                    {
                        "name": "Ultron",
                        "powers": ["superhuman intelligence"]
                    }]
                }]
            }
            """.trimIndent()

        val json2 =
            """
            {
                "superHeroes": [
                {
                    "name": "Superman",
                    "powers": ["flight", "superhuman strength"],
                    "enemies": [
                    {
                        "name": "Lex Luthor",
                        "powers": ["superhuman intelligence"]
                    }]
                },
                {
                    "name": "Spider-man",
                    "powers": ["spider-sense", "web" ],
                    "enemies": [
                    {
                        "name": "Venom",
                        "powers": ["superhuman strength", "web"]
                    },
                    {
                        "name": "Green Goblin",
                        "powers": ["flying glider", "superhuman strength"]
                    }
                    ]
                },
                {
                    "name": "Ironman",
                    "powers": ["rich", "smart"],
                    "enemies": [
                    {
                        "name": "Thanos",
                        "powers": ["superhuman strength"]
                    },
                    {
                        "name": "Ultron",
                        "powers": ["superhuman intelligence"]
                    }]
                }]
            }
            """.trimIndent()

        val config =
            JsonDiffConfig(
                ignoreArrayOrder = true,
            )
        val diff = JsonDiff(config).diff(jacksonObjectMapper.readTree(json1), jacksonObjectMapper.readTree(json2))
        assertEquals(0, diff.size)
    }

    @Test
    fun `diff nest object`() {
        val json1 =
            """
            {
                "field1": "test",
                "parent":
                {
                    "child1": "test",
                    "childParent":
                    {
                        "childChild1": "test"
                    }
                }
            }
            """.trimIndent()

        val json2 =
            """
            {
                "field1": "test",
                "parent":
                {
                    "child1": "test",
                    "childParent":
                    {
                        "childChild1": "test2"
                    }
                }
            }
            """.trimIndent()
        val diff = JsonDiff().diff(jacksonObjectMapper.readTree(json1), jacksonObjectMapper.readTree(json2))
        println(diff)
        assertEquals(1, diff.size)
        assertEquals("\$.parent.childParent.childChild1: value mismatch test != test2", diff[0])
    }

    data class SuperHero(val id: Int, val name: String)

    @Test
    fun `diff complex object custom differ`() {
        val json1 =
            """
            {
                "superHeroes": [
                {
                    "id": 1,
                    "name": "Spider-man"
                },
                {
                    "id": 2,
                    "name": "Superman"
                },
                {
                    "id": 3,
                    "name": "Ironman"
                }]
            }
            """.trimIndent()

        val json2 =
            """
            {
                "superHeroes": [
                {
                    "id": 3,
                    "name": "Ironman"
                },
                {
                    "id": 1,
                    "name": "Spider-man"
                },
                {
                    "id": 2,
                    "name": "Superman"
                }]
                
            }
            """.trimIndent()

        val config =
            JsonDiffConfig(
                customDiffers =
                    mapOf(
                        JsonPathMatcher.parse("$.superHeroes") to
                            object : JsonDiffer {
                                override fun diff(
                                    jsonA: JsonNode,
                                    jsonB: JsonNode,
                                    currentPath: String,
                                    config: JsonDiffConfig,
                                ): List<String> {
                                    val sortedHeroesA = jacksonObjectMapper.treeToValue<List<SuperHero>>(jsonA).sortedBy { it.id }
                                    val sortedHeroesB = jacksonObjectMapper.treeToValue<List<SuperHero>>(jsonB).sortedBy { it.id }
                                    return AnyDiff.diff(
                                        jacksonObjectMapper.valueToTree(sortedHeroesA),
                                        jacksonObjectMapper.valueToTree(sortedHeroesB),
                                        currentPath,
                                        config,
                                    )
                                }
                            },
                    ),
            )
        val diff = JsonDiff(config).diff(jacksonObjectMapper.readTree(json1), jacksonObjectMapper.readTree(json2))
        assertEquals(0, diff.size)
    }

    @Test
    fun `diff complex object custom differ exception`() {
        val json1 =
            """
            {
                "superHeroes": [
                {
                    "id": 1,
                    "name": "Spider-man"
                },
                {
                    "id": 2,
                    "name": "Superman"
                },
                {
                    "id": 3,
                    "name": "Ironman"
                }]
            }
            """.trimIndent()

        val json2 =
            """
            {
                "superHeroes": [
                {
                    "id": 3,
                    "name": "Ironman"
                },
                {
                    "id": 1,
                    "name": "Spider-man"
                },
                {
                    "id": 2,
                    "name": "Superman"
                }]
                
            }
            """.trimIndent()

        val config =
            JsonDiffConfig(
                customDiffers =
                    mapOf(
                        JsonPathMatcher.parse("$.superHeroes") to
                            object : JsonDiffer {
                                override fun diff(
                                    jsonA: JsonNode,
                                    jsonB: JsonNode,
                                    currentPath: String,
                                    config: JsonDiffConfig,
                                ): List<String> {
                                    throw Exception("test exception")
                                }
                            },
                    ),
            )
        val diff = JsonDiff(config).diff(jacksonObjectMapper.readTree(json1), jacksonObjectMapper.readTree(json2))
        assertEquals(1, diff.size)
    }
}
