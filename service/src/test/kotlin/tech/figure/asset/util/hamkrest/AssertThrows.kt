package tech.figure.asset.util.hamkrest

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.isA
import org.junit.jupiter.api.fail
import org.opentest4j.AssertionFailedError

@Suppress("RethrowCaughtException")
inline fun <reified T : Throwable> assertThrows(logicThatThrows: () -> Unit): T {
    try {
        logicThatThrows()
        fail { "Expected an exception to be thrown, but none were." }
    } catch (e: AssertionFailedError) {
        throw e
    } catch (e: Throwable) {
        assertThat(e, isA<T>()) { "Expected exception type ${T::class}, but was ${e::class}" }
        return e as T
    }
}
