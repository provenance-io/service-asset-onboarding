package tech.figure.asset.domain.usecase.common.errors

class NotFoundError(message: String, cause: Throwable? = null) : Exception(message, cause)
