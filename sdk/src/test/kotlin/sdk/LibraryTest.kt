package tech.figure.asset.sdk

import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class LibraryTest {

    @Test
    fun `someLibraryMethod test`() {
        runBlockingTest {
            val classUnderTest = Library()
            Assertions.assertTrue(classUnderTest.someLibraryMethod(), "someLibraryMethod should return 'true'")
        }
    }
}
