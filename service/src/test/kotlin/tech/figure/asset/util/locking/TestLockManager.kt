package tech.figure.asset.util.locking

import io.provenance.core.locking.LockManager
import java.time.Duration

object TestLockManager : LockManager {
    override fun lock(key: String, lockFor: Duration, waitFor: Duration, tries: Int) = Unit

    override fun unlock(key: String) = Unit

    override fun <T> withLock(
        key: String,
        lockFor: Duration,
        waitFor: Duration,
        tries: Int,
        block: () -> T
    ): T = block()
}
