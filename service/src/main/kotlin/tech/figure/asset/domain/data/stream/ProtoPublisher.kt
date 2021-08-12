package tech.figure.asset.domain.data.stream

import com.google.protobuf.Message

/**
 * Interface for publishing protobuf messages to topic-based message streams. While this is essentially guaranteed to be
 * a Kafka stream in practice, we use this to abstract that detail out of the domain layer.
 * @param K Key type for the topic
 * @param M Protobuf message type
 */
interface ProtoPublisher<K, M : Message> {
    /**
     * Topic associated with the message type.
     */
    val topic: String

    /**
     * Function for mapping the message to a unique key.
     */
    val keyMapping: (M) -> K

    /**
     * Publish the message to the [topic].
     */
    suspend fun publish(message: M)
}
