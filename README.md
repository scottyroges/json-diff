# Json Diff
[![](https://jitpack.io/v/scottyroges/json-diff.svg)](https://jitpack.io/#scottyroges/json-diff)

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

Create the `JsonDiff` class and then call the method `diff` on two Jackson `JsonNode` objects.
The result is a list of strings. An empty list means there are no differences, but if there are
results then the string will represent the actual differences. You can then use these to print
out a log statement with the diffs to understand where the difference is.
```json
{
  "field1": "test"
}
```
```json
{
  "field1": "test2"
}
```
```kotlin
val jsonA = jacksonObjectMapper.readTree(a)
val jsonB = jacksonObjectMapper.readTree(b)
val diffs = JsonDiff().diff(jsonA, jsonB)
```
Output
```dtd
$.field1: value mismatch test != test2
```

`JsonDiff` takes in an optional configuration object called `JsonDiffConfig`. This config object
has a bunch of properties that allow you to modify the comparison logic to suit
your specific use case.

### Ignore paths
```json
{
  "extensions": {
    "session_hash": "qwera234sdf2123dsf"
  },
  "name": "spiderman"
}
```
```json
{
  "extensions": {
    "session_hash": "lllknip425lkmknoi"
  },
  "name": "spiderman"
}
```
With the `ignorePaths` property you can provide a list of `JsonPath` for parts of the json tree
to ignore while doing comparisons. That entire part of the tree will be skipped over.
```kotlin
val jsonDiffConfig = JsonDiffConfig(
    ignorePaths = listOf(JsonPathMatcher.parse("\$.extensions"))
)
```

### Ignore Array Order
```json
{
  "name": "spiderman",
  "powers": ["spidey-sense", "super strength", "climb walls"]
}
```
```json
{
  "name": "spiderman",
  "powers": ["climb walls", "super strength", "spidey-sense"]
}
```
With `ignoreArrayOrder` you can decide if arrays in the json should maintain
order for the comparison or not. This is a global setting that will apply
to any array in the json. 

It's worth noting that to do the comparison when ignoring order, we do a `.toString()` on the objects
in the array and then sort those strings. This works fine in most cases, but it may not
be preferred in some cases, especially if there are fields within the array that are
meant to be ignored and may cause the sorting to get messed up. For more control over the 
sorting use a `customDiffer` described below.

```kotlin
val jsonDiffConfig = JsonDiffConfig(
    ignoreArrayOrder = true
)
```

### Ignore Array Order Only For Paths
```json
{
  "name": "spiderman",
  "powers": ["spidey-sense", "super strength", "climb walls"],
  "movieReleaseYears": [2002, 2004, 2007]
}
```
```json
{
  "name": "spiderman",
  "powers": ["climb walls", "super strength", "spidey-sense"],
  "movieReleaseYears": [2007, 2002, 2004]
}
```
With `ignoreArrayOrderOnlyForPaths` you can decide to ignore array order for
only specific paths in the json. This can be useful when you for most cases you
want `ignoreArrayOrder = false`, but there are one-off cases where you need to validate
order.

```kotlin
val jsonDiffConfig = JsonDiffConfig(
    ignoreArrayOrder = false,
    ignoreArrayOrderOnlyForPaths = listOf(JsonPathMatcher.parse("\$.powers"))
)
```

### Validate Array Order Only For Paths
```json
{
  "name": "spiderman",
  "powers": ["spidey-sense", "super strength", "climb walls"],
  "movieReleaseYears": [2002, 2004, 2007]
}
```
```json
{
  "name": "spiderman",
  "powers": ["climb walls", "super strength", "spidey-sense"],
  "movieReleaseYears": [2007, 2002, 2004]
}
```
`validateArrayOrderOnlyForPaths` has the opposite effect. If you have set
`ignoreArrayOrder = true` because in most cases you do not care about array
order, but there are a few one-off cases where you do, then this property will
let you set the paths for those exceptions.

```kotlin
val jsonDiffConfig = JsonDiffConfig(
    ignoreArrayOrder = true,
    validateArrayOrderOnlyForPaths = listOf(JsonPathMatcher.parse("\$.powers"))
)
```

### Relaxed Number Equality
```json
{
  "name": "spiderman",
  "powerLevel": 9
}
```
```json
{
  "name": "spiderman",
  "powerLevel": 9.0
}
```
With `relaxedNumberEquality` the above two numbers of `9` and `9.0` would be considered
equal. By default this is set to `false`, in which case the numbers would be treated like
strings and would have to match exactly.

```kotlin
val jsonDiffConfig = JsonDiffConfig(
    relaxedNumberEquality = false,
)
```

### Custom Diff Logic
`customDiffers` are extremely powerful. They allow you to insert custom logic
at a particular path of the json tree to perform some logic for the comparison.

For example, maybe you have to compare two URLs that need to be compared, but you 
are ok if the header string between them is different.

```json
{
  "name": "spiderman",
  "url": "https://www.wikipedia.com/Spiderman#Comics"
}
```
```json
{
  "name": "spiderman",
  "url": "https://www.wikipedia.com/Spiderman#Movies"
}
```

You can write a `JsonDiffer` object that will be executed for that path. Inside
the object you can determine how to do the comparison. 

```kotlin
val jsonDiffConfig = JsonDiffConfig(
    customDiffers = mapOf(
        JsonPathMatcher.parse("$.url") to object : JsonDiffer {
            override fun diff(jsonA: JsonNode, jsonB: JsonNode, currentPath: String, config: JsonDiffConfig,): List<String> {
                val valueA = jsonA.asText().split("#").first()
                val valueB = jsonB.asText().split("#").first()
                return if(valueA != valueB) {
                    listOf("$currentPath: value mismatch $valueA != $valueB")
                } else {
                    listOf()
                }
            }
        }
    )
)
```


It's worth noting that what's passed in is the `JsonNode`, so you will need to convert this to the
right type based on the value you are expecting. Sometimes this may throw an exception
if there is an unexpected type. The `JsonDiff` will catch the exception and include the message
as part of the error string for that path, but it won't give much more information. If you want you can 
customize the log message by catching any exceptions yourself and returning your custom message. 
It also will not continue down the rest of that part of the json tree.


Another example, might be having a custom sort order for an object when you have set `ignoreArrayOrder=true`
and also have a field that you are ignoring.

```json
{
  "superHeroes": [{
    "id": "123",
    "name": "spiderman",
    "sessionHash": "asdwqer31234"
  },{
    "id": "456",
    "name": "batman",
    "sessionHash": "xwe4455asdd"
  }]
}
```
```json
{
  "superHeroes": [{
    "id": "456",
    "name": "batman",
    "sessionHash": "sd23523sdfad"
  },{
    "id": "123",
    "name": "spiderman",
    "sessionHash": "lopasd445211a"
  }]
}
```

So in this case the `sessionHash` is a field we don't want to compare, but because we don't care about array order, by default `JsonDiff` will try to
sort by doing a `toString()` on each superHero object and sort those strings for comparison. The hash could
mess up the sort order and our comparison would not work. But with the `customDiffers`
we can supply the sort order ourselves.
```kotlin
data class SuperHero(val id: Int, val name: String)

val jsonDiffConfig = JsonDiffConfig(
    ignorePaths = listOf(JsonPathMatcher.parse("\$superHeroes[*].sessionHash")),
    ignoreArrayOrder = true,
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
```

As you can see, we parse the json objects into the `SuperHero` data class and then do a sort by the id.
This ensures that regardless of what any other fields are, as long as our list of objects have the same
ids, the array will be considered a match in terms of order. We then take this new sorted list
and convert it back to a `JsonNode`. We pass that into the `AnyDiff` differ which will continue on
down the tree but with these sorted nodes instead. So it will still do the comparisons between the objects
and will respect our ignored path.

### Gradle

```kotlin
repositories {
    mavenCentral()
    maven {  url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.scottyroges:json-diff:$version")
    // other deps here
}
```

## CHANGELOG
### 1.0.3
- jitpack

### 1.0.2
- fix

### 1.0.1
- Getting setup with jitpack

### 1.0.0
- Initial library setup
