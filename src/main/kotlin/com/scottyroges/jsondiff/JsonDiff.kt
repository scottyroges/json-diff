package com.scottyroges.jsondiff

import com.fasterxml.jackson.databind.JsonNode
import com.scottyroges.jsondiff.path.JsonPath
import com.scottyroges.jsondiff.path.JsonPathMatcher

interface JsonDiffer {
    fun diff(
        jsonA: JsonNode,
        jsonB: JsonNode,
        currentPath: String,
        config: JsonDiffConfig,
    ): List<String>
}

data class JsonDiffConfig(
    val ignorePaths: List<JsonPathMatcher> = listOf(),
    val ignoreArrayOrder: Boolean = false,
    val ignoreArrayOrderOnlyForPaths: List<JsonPathMatcher> = listOf(),
    val validateArrayOrderOnlyForPaths: List<JsonPathMatcher> = listOf(),
    val relaxedNumberEquality: Boolean = false,
    val customDiffers: Map<JsonPathMatcher, JsonDiffer> = mapOf(),
)

class JsonDiff(
    private val config: JsonDiffConfig = JsonDiffConfig(),
) {
    fun diff(
        jsonA: JsonNode,
        jsonB: JsonNode,
    ): List<String> {
        return MainDiff.diff(jsonA, jsonB, "$", config)
    }
}

private object MainDiff : JsonDiffer {
    override fun diff(
        jsonA: JsonNode,
        jsonB: JsonNode,
        currentPath: String,
        config: JsonDiffConfig,
    ): List<String> {
        try {
            if (config.ignorePaths.isNotEmpty()) {
                val currentJsonPath = JsonPath.parse(currentPath)
                if (config.ignorePaths.any { it.matches(currentJsonPath) }) {
                    return emptyList()
                }
            }

            if (config.customDiffers.isNotEmpty()) {
                val currentJsonPath = JsonPath.parse(currentPath)
                val customDiffer = config.customDiffers.entries.firstOrNull { it.key.matches(currentJsonPath) }?.value
                if (customDiffer != null) {
                    return try {
                        customDiffer.diff(jsonA, jsonB, currentPath, config)
                    } catch (e: Exception) {
                        listOf("$currentPath: custom differ failed with exception ${e.message}")
                    }
                }
            }

            return AnyDiff.diff(jsonA, jsonB, currentPath, config)
        } catch (e: Exception) {
            return listOf("$currentPath: diff failed with exception ${e.message}")
        }
    }
}

object AnyDiff : JsonDiffer {
    override fun diff(
        jsonA: JsonNode,
        jsonB: JsonNode,
        currentPath: String,
        config: JsonDiffConfig,
    ): List<String> {
        if (jsonA.isValueNode && jsonB.isValueNode) {
            return ValueDiff.diff(jsonA, jsonB, currentPath, config)
        }

        if (jsonA.isObject && jsonB.isObject) {
            return ObjectDiff.diff(jsonA, jsonB, currentPath, config)
        }

        if (jsonA.isArray && jsonB.isArray) {
            return ArrayDiff.diff(jsonA, jsonB, currentPath, config)
        }

        if (jsonA.isEmpty && jsonB.isEmpty) {
            return emptyList()
        }

        return emptyList()
    }
}

object ValueDiff : JsonDiffer {
    override fun diff(
        jsonA: JsonNode,
        jsonB: JsonNode,
        currentPath: String,
        config: JsonDiffConfig,
    ): List<String> {
        return if (config.relaxedNumberEquality && jsonA.isNumber && jsonB.isNumber) {
            NumberDiff.diff(jsonA, jsonB, currentPath, config)
        } else {
            TextDiff.diff(jsonA, jsonB, currentPath, config)
        }
    }
}

object NumberDiff : JsonDiffer {
    override fun diff(
        jsonA: JsonNode,
        jsonB: JsonNode,
        currentPath: String,
        config: JsonDiffConfig,
    ): List<String> {
        // just converting everything to double for accurate comparison
        val valueA = jsonA.numberValue().toDouble()
        val valueB = jsonB.numberValue().toDouble()
        return if (valueA != valueB) {
            listOf("$currentPath: value mismatch $valueA != $valueB")
        } else {
            emptyList()
        }
    }
}

object TextDiff : JsonDiffer {
    override fun diff(
        jsonA: JsonNode,
        jsonB: JsonNode,
        currentPath: String,
        config: JsonDiffConfig,
    ): List<String> {
        val valueA = jsonA.asText()
        val valueB = jsonB.asText()
        return if (valueA != valueB) {
            listOf("$currentPath: value mismatch $valueA != $valueB")
        } else {
            emptyList()
        }
    }
}

object ObjectDiff : JsonDiffer {
    override fun diff(
        jsonA: JsonNode,
        jsonB: JsonNode,
        currentPath: String,
        config: JsonDiffConfig,
    ): List<String> {
        val diff = mutableListOf<String>()
        val fieldsA = jsonA.fieldNames().asSequence().toList()
        val fieldsB = jsonB.fieldNames().asSequence().toList()
        val fieldsInAOnly = fieldsA - fieldsB
        val fieldsInBOnly = fieldsB - fieldsA
        val fieldsInBoth = fieldsA.intersect(fieldsB)

        fieldsInAOnly.forEach { field ->
            if (config.ignorePaths.isNotEmpty()) {
                val tempPath = JsonPath.parse("$currentPath.$field")
                if (!config.ignorePaths.any { it.matches(tempPath) }) {
                    diff.add("$currentPath.$field: field only in A")
                }
            } else {
                diff.add("$currentPath.$field: field only in A")
            }
        }

        fieldsInBOnly.forEach { field ->
            if (config.ignorePaths.isNotEmpty()) {
                val tempPath = JsonPath.parse("$currentPath.$field")
                if (!config.ignorePaths.any { it.matches(tempPath) }) {
                    diff.add("$currentPath.$field: field only in B")
                }
            } else {
                diff.add("$currentPath.$field: field only in B")
            }
        }

        fieldsInBoth.forEach { field ->
            diff.addAll(MainDiff.diff(jsonA.get(field), jsonB.get(field), "$currentPath.$field", config))
        }

        return diff
    }
}

object ArrayDiff : JsonDiffer {
    override fun diff(
        jsonA: JsonNode,
        jsonB: JsonNode,
        currentPath: String,
        config: JsonDiffConfig,
    ): List<String> {
        val allDiffs = mutableListOf<String>()
        val sizeA = jsonA.size()
        val sizeB = jsonB.size()

        if (sizeA != sizeB) {
            allDiffs.add("$currentPath: array size mismatch")
        }

        val diffs =
            if (config.ignoreArrayOrder) {
                if (config.validateArrayOrderOnlyForPaths.isNotEmpty()) {
                    val currentJsonPath = JsonPath.parse(currentPath)
                    if (config.validateArrayOrderOnlyForPaths.any { it.matches(currentJsonPath) }) {
                        validateOrder(jsonA, jsonB, currentPath, config)
                    } else {
                        ignoreOrder(jsonA, jsonB, currentPath, config)
                    }
                } else {
                    ignoreOrder(jsonA, jsonB, currentPath, config)
                }
            } else {
                if (config.ignoreArrayOrderOnlyForPaths.isNotEmpty()) {
                    val currentJsonPath = JsonPath.parse(currentPath)
                    if (config.ignoreArrayOrderOnlyForPaths.any { it.matches(currentJsonPath) }) {
                        ignoreOrder(jsonA, jsonB, currentPath, config)
                    } else {
                        validateOrder(jsonA, jsonB, currentPath, config)
                    }
                } else {
                    validateOrder(jsonA, jsonB, currentPath, config)
                }
            }
        allDiffs.addAll(diffs)

        return allDiffs
    }

    private fun ignoreOrder(
        jsonA: JsonNode,
        jsonB: JsonNode,
        currentPath: String,
        config: JsonDiffConfig,
    ): List<String> {
        val size = minOf(jsonA.size(), jsonB.size())
        val diff = mutableListOf<String>()
        val sortedJsonA = jsonA.sortedBy { it.toString() }
        val sortedJsonB = jsonB.sortedBy { it.toString() }
        for (i in 0 until size) {
            diff.addAll(MainDiff.diff(sortedJsonA.get(i), sortedJsonB.get(i), "$currentPath[$i]", config))
        }
        return diff
    }

    private fun validateOrder(
        jsonA: JsonNode,
        jsonB: JsonNode,
        currentPath: String,
        config: JsonDiffConfig,
    ): List<String> {
        val size = minOf(jsonA.size(), jsonB.size())
        val diff = mutableListOf<String>()
        for (i in 0 until size) {
            diff.addAll(MainDiff.diff(jsonA.get(i), jsonB.get(i), "$currentPath[$i]", config))
        }
        return diff
    }
}
