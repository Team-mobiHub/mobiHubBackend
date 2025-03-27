package com.mobihub.repositories

import com.mobihub.model.TrafficModel
import com.mobihub.model.TrafficModelId
import com.mobihub.model.User
import com.mobihub.model.UserId
import com.mobihub.repositories.db.FavouriteTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

private const val TRAFFIC_MODEL_NOT_FOUND = "Traffic model with id %d not found"
private const val USER_NOT_FOUND = "User with id %d not found"

/**
 * Repository for managing favourite [TrafficModel] entities.
 *
 * @property repositoryProvider The [RepositoryProvider] instance.
 *
 * @author Team-MobiHub
 */
class FavouriteDbRepository(
    private val repositoryProvider: RepositoryProvider
) : FavouriteRepository {
    override fun addFavourite(trafficModelId: TrafficModelId, userId: UserId) {
        return transaction {
            FavouriteTable.insert {
                it[FavouriteTable.trafficModelId] = trafficModelId.id
                it[FavouriteTable.userId] = userId.id
            }
        }
    }

    override fun deleteFavourites(trafficModelId: TrafficModelId) {
        transaction {
            FavouriteTable.deleteWhere {
                FavouriteTable.trafficModelId eq trafficModelId.id
            }
        }
    }

    override fun deleteFavourite(trafficModelId: TrafficModelId, userId: UserId) {
        transaction {
            FavouriteTable.deleteWhere {
                (FavouriteTable.trafficModelId eq trafficModelId.id) and (FavouriteTable.userId eq userId.id)
            }
        }
    }

    override fun getFavouritesByUserId(userId: UserId): List<TrafficModel> {
        return transaction {
            FavouriteTable.selectAll().where { FavouriteTable.userId eq userId.id }
                .map {
                    repositoryProvider.trafficModelRepository.getById(TrafficModelId(it[FavouriteTable.trafficModelId]))
                    // An inconsistency in the data model, the traffic model should always exist
                        ?: error(TRAFFIC_MODEL_NOT_FOUND.format(it[FavouriteTable.trafficModelId]))
                }
        }
    }

    override fun getFavoritesByTrafficModelId(trafficModelId: TrafficModelId): List<User> {
        return transaction {
            FavouriteTable.selectAll().where { FavouriteTable.trafficModelId eq trafficModelId.id }
                .map {
                    repositoryProvider.userRepository.getById(UserId(it[FavouriteTable.userId]))
                    // An inconsistency in the data model, the user should always exist
                        ?: error(USER_NOT_FOUND.format(it[FavouriteTable.userId]))
                }
        }
    }
}