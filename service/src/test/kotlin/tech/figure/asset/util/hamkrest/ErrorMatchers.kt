package tech.figure.asset.util.hamkrest

import com.natpryce.hamkrest.Matcher

private fun Throwable.hasLocalizedMessage(msg: String) = localizedMessage == msg

fun hasMessage(message: String) = Matcher(Throwable::hasLocalizedMessage, message)
