package tech.figure.asset.extensions

import org.slf4j.Logger

fun Logger.trace(messages: List<String>) = messages.forEach { this.trace(it) }
fun Logger.debug(messages: List<String>) = messages.forEach { this.debug(it) }
fun Logger.info(messages: List<String>) = messages.forEach { this.info(it) }
fun Logger.warn(messages: List<String>) = messages.forEach { this.warn(it) }
fun Logger.error(messages: List<String>) = messages.forEach { this.error(it) }
