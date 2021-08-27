package tech.figure.asset.config

import org.springframework.util.ClassUtils
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation

@Target(AnnotationTarget.CLASS, AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Secret

open class LoggableProperties {

    open fun toLogMessages(): List<String> =
        logifyObject(this, ClassUtils.getUserClass(this::class.java).simpleName ?: "Unknown")

    private fun logifyObject(obj: Any?, prefix: String): List<String> {
        val logMessages: MutableList<String> = mutableListOf()

        if (obj != null) {
            if (!ClassUtils.getUserClass(obj::class.java).isAnnotationPresent(Secret::class.java)) {
                obj::class.declaredMemberProperties.forEach {
                    if (it.visibility == KVisibility.PUBLIC) {
                        if (it.findAnnotation<Secret>() == null) {
                            val value = it.getter.call(obj)
                            if (value != null) {
                                logMessages.addAll(logify(value::class, value, prefix, ".${it.name}"))
                            } else {
                                logMessages.add("$prefix.${it.name} : <null>")
                            }
                        } else {
                            logMessages.add("$prefix.${it.name} : <secret>")
                        }
                    }
                }
            } else {
                logMessages.add("$prefix : <secret>")
            }
        } else {
            logMessages.add("$prefix: <null>")
        }

        return logMessages
    }

    private fun logify(clazz: KClass<*>, value: Any?, prefix: String, suffix: String): List<String> {
        return if (value != null) {
            val valueAsString = value.toString()
            if (valueAsString.indexOf(clazz.toString().replace("class ", "")) == 0) {
                // we're dealing with an object
                logifyObject(value, "$prefix$suffix")
            } else {
                if (value is Collection<*>) {
                    // we're dealing with a collection
                    val logMessages: MutableList<String> = mutableListOf()
                    value.withIndex().forEach {
                        logMessages.addAll(logify(it.value!!::class, it.value, "$prefix$suffix", "[${it.index}]"))
                    }
                    logMessages
                } else {
                    // we're dealing with a basic data type
                    listOf("$prefix$suffix : $valueAsString")
                }
            }
        } else {
            listOf("$prefix$suffix : <null>")
        }
    }
}
