package tech.figure.asset.fakes

import com.google.protobuf.Message
import tech.figure.asset.domain.data.stream.ProtoPublisher

class FakeProtoPublisher<T : Message> : ProtoPublisher<String, T> {

    val publishedMessages = mutableListOf<T>()

    override val topic: String = "fake.topic"

    override val keyMapping: (T) -> String = { "fake-key" }

    override suspend fun publish(message: T) {
        publishedMessages += message
    }
}
