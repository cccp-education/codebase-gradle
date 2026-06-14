package codebase.koog.discovery

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class OpenApiSpec(
    val openapi: String = "3.0.0",
    val info: OpenApiInfo,
    val paths: Map<String, PathItem>
)

@Serializable
data class OpenApiInfo(
    val title: String,
    val version: String,
    val description: String = ""
)

@Serializable
data class PathItem(
    val post: Operation? = null
)

@Serializable
data class Operation(
    val summary: String,
    val operationId: String,
    val description: String = "",
    val parameters: List<Parameter> = emptyList(),
    val responses: Map<String, Response> = mapOf(
        "200" to Response(description = "Task executed successfully"),
        "400" to Response(description = "Task execution failed")
    )
)

@Serializable
data class Parameter(
    val name: String,
    val `in`: String = "query",
    val required: Boolean = false,
    val description: String = "",
    val schema: ParameterSchema
)

@Serializable
data class ParameterSchema(
    val type: String = "string"
)

@Serializable
data class Response(
    val description: String
)

class OpenApiSpecGenerator {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    fun generate(schemas: List<TaskSchema>, title: String = "Gradle Tasks API", version: String = "1.0.0"): OpenApiSpec {
        val paths = schemas.associate { schema ->
            val path = "/tasks/${schema.name}"
            val parameters = schema.options.map { option ->
                Parameter(
                    name = option.name,
                    `in` = "query",
                    required = option.required,
                    description = option.description,
                    schema = ParameterSchema(type = mapType(option.type))
                )
            }
            path to PathItem(
                post = Operation(
                    summary = schema.description.ifBlank { "Execute ${schema.name} task" },
                    operationId = "execute_${schema.name}",
                    description = "Group: ${schema.group}. Type: ${schema.type}",
                    parameters = parameters
                )
            )
        }
        return OpenApiSpec(
            info = OpenApiInfo(
                title = title,
                version = version,
                description = "Auto-generated OpenAPI spec from Gradle task graph"
            ),
            paths = paths
        )
    }

    fun generateJson(schemas: List<TaskSchema>, title: String = "Gradle Tasks API", version: String = "1.0.0"): String =
        json.encodeToString(generate(schemas, title, version))

    private fun mapType(kotlinType: String): String = when {
        kotlinType.equals("String", ignoreCase = true) -> "string"
        kotlinType.equals("Boolean", ignoreCase = true) -> "boolean"
        kotlinType.equals("Int", ignoreCase = true) || kotlinType.equals("Integer", ignoreCase = true) -> "integer"
        kotlinType.equals("Long", ignoreCase = true) -> "integer"
        kotlinType.equals("Double", ignoreCase = true) || kotlinType.equals("Float", ignoreCase = true) -> "number"
        kotlinType.contains("Property") -> "string"
        else -> "string"
    }
}
