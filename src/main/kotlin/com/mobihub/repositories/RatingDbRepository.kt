package com.mobihub.repositories

import com.mobihub.model.Rating
import com.mobihub.model.TrafficModelId
import com.mobihub.model.UserId
import com.mobihub.repositories.db.RatingTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

private const val USER_NOT_FOUND = "User with id %d not found"
private const val TRAFFIC_MODEL_NOT_FOUND = "Traffic model with id %d not found"

/**
 * Repository for managing [Rating] entities.
 *
 * @property repositoryProvider The [RepositoryProvider] instance.
 *
 * @author Team-MobiHub
 */
class RatingDbRepository(
    private val repositoryProvider: RepositoryProvider
) : RatingRepository {

    override fun addRating(rating: Rating): Rating {
        return transaction {
            RatingTable.insert {
                it[userId] = rating.user.id?.id!!
                it[usersRating] = rating.rating
                it[trafficModelId] = rating.trafficModel.id?.id!!
            }
            rating
        }
    }

    override fun updateRating(rating: Rating): Rating {
        return transaction {
            RatingTable.update({(RatingTable.userId eq rating.user.id?.id!!) and (RatingTable.trafficModelId eq rating.trafficModel.id?.id!!)}) {
                it[userId] = rating.user.id?.id!!
                it[usersRating] = rating.rating
                it[trafficModelId] = rating.trafficModel.id?.id!!
            }
            rating
        }
    }

    override fun deleteRating(rating: Rating) {
        return transaction {
            RatingTable.deleteWhere {
                (userId eq rating.user.id?.id!!) and (trafficModelId eq rating.trafficModel.id?.id!!)
            }
        }
    }

    override fun getRatingsForTrafficModel(trafficModelId: TrafficModelId): List<Rating> {
        return transaction {
            RatingTable.selectAll().where {
                RatingTable.trafficModelId eq trafficModelId.id
            }.map { toRating(it) }
        }
    }

    override fun getRatingsForUser(userId: UserId): List<Rating> {
        return transaction {
            RatingTable.selectAll().where {
                RatingTable.userId eq userId.id
            }.map { toRating(it) }
        }
    }

    override fun getAverageRatingForTrafficModel(trafficModelId: TrafficModelId): Double {
        return transaction {
            val ratings = RatingTable.selectAll().where {
                RatingTable.trafficModelId eq trafficModelId.id
            }.map { it[RatingTable.usersRating] }

            return@transaction if (ratings.isEmpty()) {
                0.0
            } else {
                ratings.sum().toDouble() / ratings.size
            }
        }
    }

    /**
     * Converts a [ResultRow] to a [Rating].
     *
     * @param row The [ResultRow] to convert.
     *
     * @return The [Rating] object.
     */
    fun toRating(row: ResultRow): Rating {
        return Rating(
            rating = row[RatingTable.usersRating],
            trafficModelProvider = {
                return@Rating repositoryProvider.trafficModelRepository.getById(TrafficModelId(row[RatingTable.trafficModelId]))
                    ?: error(TRAFFIC_MODEL_NOT_FOUND.format({ row[RatingTable.trafficModelId] }))
            },
            userProvider = {
                return@Rating repositoryProvider.userRepository.getById(UserId(row[RatingTable.userId]))
                    ?: error(USER_NOT_FOUND.format({ row[RatingTable.userId] }))
            }
        )
    }
}
