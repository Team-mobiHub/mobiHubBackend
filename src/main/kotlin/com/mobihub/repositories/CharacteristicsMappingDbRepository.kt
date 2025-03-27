package com.mobihub.repositories

import com.mobihub.model.*
import com.mobihub.repositories.db.CharacteristicsMappingTable
import org.jetbrains.exposed.sql.*

import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

/**
 * Repository for characteristics mapping.
 *
 * This class implements the [CharacteristicsMappingRepository] interface.
 *
 * @property [repositoryProvider] The provider for accessing other repositories.
 *
 * @author Team-MobiHub
 */
class CharacteristicsMappingDbRepository(
    private val repositoryProvider: RepositoryProvider
) : CharacteristicsMappingRepository {

    override fun create(
        trafficModelId: TrafficModelId,
        mapping: List<Pair<ModelLevel, ModelMethod>>
    ): List<Pair<ModelLevel, ModelMethod>> {
        transaction {
            mapping.forEach { pair ->
                CharacteristicsMappingTable.insert {
                    it[CharacteristicsMappingTable.trafficModelId] = trafficModelId.id
                    it[modelLevelId] = pair.first.id.id
                    it[modelMethodId] = pair.second.id.id
                }
            }
        }
        return mapping
    }

    override fun update(trafficModelId: TrafficModelId, mapping: List<Pair<ModelLevel, ModelMethod>>) {
        transaction {
            CharacteristicsMappingTable.deleteWhere {
                CharacteristicsMappingTable.trafficModelId eq trafficModelId.id
            }
            create(trafficModelId, mapping)
        }
    }

    override fun get(trafficModelId: TrafficModelId): List<Pair<ModelLevel, ModelMethod>> {
        return transaction {
            CharacteristicsMappingTable.selectAll().where {
                CharacteristicsMappingTable.trafficModelId eq trafficModelId.id
            }.map {
                Pair(
                    ModelLevel.fromId(ModelLevelId(it[CharacteristicsMappingTable.modelLevelId])),
                    ModelMethod.fromId(ModelMethodId(it[CharacteristicsMappingTable.modelMethodId]))
                )
            }
        }
    }

    override fun delete(trafficModelId: TrafficModelId) {
        transaction {
            CharacteristicsMappingTable.deleteWhere {
                CharacteristicsMappingTable.trafficModelId eq trafficModelId.id
            }
        }
    }
}
