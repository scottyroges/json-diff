# Json Diff

Provides a library to diff two json objects

Uses Json Paths for customization of different diff configs which is heavily borrowed from https://github.com/json-path/JsonPath and modified to fit needs.

Paths supported:

| Operator                  | Description                                                  |
|---------------------------|--------------------------------------------------------------|
| $	                        | The root element to query. This starts all path expressions. |
| *	                        | Wildcard. Available anywhere a name or numeric are required. |
| ..	                       | Deep scan. Available anywhere a name is required.            |
| .\<name>	                 | Dot-notated child                                            |
| ['\<name>' (, '\<name>')] | 	Bracket-notated child or children                           |
| [\<number> (, \<number>)] | 	Array index or indexes                                      |
| [start:end]	              | Array slice operator                                         |

Examples:
- $.store.book[*].author
- $..author
- $.store.*
- $['store','website']['book','magazine'][*]['author']
- $.store.book[2:]
- $.store.book[:2]
- $.store.book[1:2]
- $.store.book[1,5,9]


## Using this Library

```kotlin
val jsonDiffConfig = JsonDiffConfig(
    ignorePaths = listOf(JsonPathMatcher.parse("\$.extensions")),
    ignoreArrayOrder = false,
    ignoreArrayOrderOnlyForPaths = listOf(JsonPathMatcher.parse("\$.store.books")),
    relaxedNumberEquality = false,
    customDiffers = mapOf(
        JsonPathMatcher.parse("$.superHeroes") to object : JsonDiffer {
            override fun diff(jsonA: JsonNode, jsonB: JsonNode, currentPath: String, config: JsonDiffConfig,): List<String> {
                val sortedHeroesA = jacksonObjectMapper.treeToValue<List<SuperHero>>(jsonA).sortedBy { it.id }
                val sortedHeroesB = jacksonObjectMapper.treeToValue<List<SuperHero>>(jsonB).sortedBy { it.id }
                return AnyDiff.diff(
                    jacksonObjectMapper.valueToTree(sortedHeroesA),
                    jacksonObjectMapper.valueToTree(sortedHeroesB),
                    currentPath,
                    config
                )
            }
        }
    )
)
val jsonA = jacksonObjectMapper.readTree(a)
val jsonB = jacksonObjectMapper.readTree(b)
val diffs = JsonDiff(jsonDiffConfig).diff(jsonA, jsonB)
println(diffs)

```

### Gradle

```kotlin
dependencies {
    implementation("com.scottyroges:json-diff:$version")
    // other deps here
}
```

## CHANGELOG
### 1.0.2
- fix

### 1.0.1
- Getting setup with jitpack

### 1.0.0
- Initial library setup
