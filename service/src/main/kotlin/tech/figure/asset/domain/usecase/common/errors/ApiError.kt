package tech.figure.asset.domain.usecase.common.errors

class ApiError(message: String, cause: Throwable? = null) : Exception(message, cause)
