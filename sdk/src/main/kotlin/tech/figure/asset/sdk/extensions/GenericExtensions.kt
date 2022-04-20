package tech.figure.asset.sdk.extensions

fun <T> T?.elvis(lazyDefault: () -> T): T = this ?: lazyDefault()
