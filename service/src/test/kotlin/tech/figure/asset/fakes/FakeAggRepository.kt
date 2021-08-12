package tech.figure.asset.fakes

import tech.figure.asset.domain.data.stream.aggregation.AggregateRepository
import tech.figure.asset.proto.Aggregate
import java.util.UUID
// import java.util.concurrent.ConcurrentHashMap

class FakeAggRepository : AggregateRepository {
    //    private val memoryStore = ConcurrentHashMap<UUID, Aggregate>()
//
//    override fun fetch(appUuid: UUID): Aggregate? =
//        memoryStore[appUuid]
//
//    override fun updateAgg(appUuid: UUID, update: Aggregate.Builder.() -> Unit): Aggregate =
//        requireNotNull(fetch(appUuid)).toBuilder()
//            .apply { update.invoke(this) }.build()
//            .also { memoryStore[appUuid] = it }
//
//    fun putAgg(appUuid: UUID, agg: Aggregate) {
//        memoryStore[appUuid] = agg
//    }
    override fun fetch(appUuid: UUID): Aggregate? {
        TODO("Not yet implemented")
    }

    override fun updateAgg(appUuid: UUID, update: Aggregate.Builder.() -> Unit): Aggregate {
        TODO("Not yet implemented")
    }
}
