package tech.figure.asset.domain.usecase

import kotlinx.coroutines.runBlocking

abstract class AbstractUseCase<Args, Res> {
    abstract suspend fun execute(args: Args): Res

    fun executeBlocking(args: Args): Res = runBlocking { execute(args) }
}

suspend fun <Res> AbstractUseCase<Unit, Res>.execute() = execute(Unit)

fun <Res> AbstractUseCase<Unit, Res>.executeBlocking() = executeBlocking(Unit)
