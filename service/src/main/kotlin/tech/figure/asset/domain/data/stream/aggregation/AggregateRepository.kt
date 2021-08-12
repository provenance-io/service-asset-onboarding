package tech.figure.asset.domain.data.stream.aggregation

import tech.figure.asset.proto.Aggregate
import java.util.UUID

interface AggregateRepository {

    fun fetch(appUuid: UUID): Aggregate?

    fun updateAgg(appUuid: UUID, update: Aggregate.Builder.() -> Unit): Aggregate
}
