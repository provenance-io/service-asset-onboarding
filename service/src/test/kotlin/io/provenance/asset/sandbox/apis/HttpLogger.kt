package io.provenance.asset.sandbox.apis

import mu.KotlinLogging
import okhttp3.logging.HttpLoggingInterceptor

object HttpLogger : HttpLoggingInterceptor.Logger {
    private val log = KotlinLogging.logger("RETROFIT-SANDBOX")
    override fun log(message: String) = log.info { message }
}
