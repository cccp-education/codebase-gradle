package codebase.koog.discovery

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.options.Option

class TaskSchemaScanner(private val project: Project) {

    fun scanAll(): List<TaskSchema> =
        project.tasks.mapNotNull { task ->
            fromTask(task)
        }

    fun scanByGroup(group: String): List<TaskSchema> =
        scanAll().filter { it.group == group }

    private fun fromTask(task: Task): TaskSchema? {
        val group = task.group ?: return null
        val options = extractOptions(task)
        return TaskSchema(
            name = task.name,
            description = task.description ?: "",
            group = group,
            type = task.javaClass.simpleName,
            options = options
        )
    }

    private fun extractOptions(task: Task): List<TaskOption> {
        val allMethods = mutableListOf<java.lang.reflect.Method>()
        var cls: Class<*>? = task.javaClass
        while (cls != null) {
            allMethods.addAll(cls.declaredMethods.toList())
            cls = cls.superclass
        }
        return allMethods
            .filter { method ->
                method.isAnnotationPresent(Option::class.java) &&
                method.parameterCount == 0
            }
            .map { method ->
                val annotation = method.getAnnotation(Option::class.java)
                TaskOption(
                    name = annotation.option,
                    description = annotation.description,
                    required = false,
                    type = resolveType(method.genericReturnType)
                )
            }
            .distinctBy { it.name }
    }

    private fun resolveType(genericType: java.lang.reflect.Type): String {
        if (genericType is java.lang.reflect.ParameterizedType) {
            val rawType = genericType.rawType
            val rawName = (rawType as Class<*>).simpleName
            if (rawName == "Property") {
                val typeArgs = genericType.actualTypeArguments
                if (typeArgs.isNotEmpty()) {
                    val arg = typeArgs[0]
                    return when (arg) {
                        is Class<*> -> arg.simpleName
                        is java.lang.reflect.ParameterizedType -> (arg.rawType as Class<*>).simpleName
                        else -> "String"
                    }
                }
                return "String"
            }
            return rawName
        }
        if (genericType is Class<*>) {
            return genericType.simpleName
        }
        return "String"
    }
}
