package tech.figure.asset.domain.usecase.common.extensions

import com.figure.util.proto.create
import com.figure.util.proto.merge
import com.google.protobuf.Message
import java.util.UUID

/**
 * Generic method to merge two lists of proto messages
 * [to] other list to merge
 * [modifiedBy] for audit record
 * [toKey] function to extract a key from the message
 */
fun <T : Message, K> List<T>.merge(to: List<T>, modifiedBy: String, toKey: (T) -> K): List<T> {
    fun List<T>.toMap() = map { toKey(it) to it }.toMap()

    val fromMap = toMap()
    val toMap = to.toMap()

    return fromMap.map {
        toMap[it.key]?.run {
            merge(it.value, this, modifiedBy = modifiedBy)
        } ?: it.value
    } + toMap
        .filter { !fromMap.containsKey(it.key) }
        .map { create(it.value, modifiedBy = modifiedBy) }
}

/**
 * Execute block if UUID is not UUID(0L, 0L)
 */
fun UUID.isNotNull(block: (UUID) -> Unit) {
    if (this != UUID(0L, 0L)) {
        block(this)
    }
}
