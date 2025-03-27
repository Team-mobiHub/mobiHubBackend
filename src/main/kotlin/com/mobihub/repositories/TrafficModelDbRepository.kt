package com.mobihub.repositories

import com.mobihub.model.*
import com.mobihub.repositories.db.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.util.UUID


private const val PAGE_INDEX_ERROR = "Page index must not be less than zero!"
private const val PAGE_SIZE_ERROR = "Page size must be greater than zero!"

private const val LIKE_TEMPLATE = "%%%s%%"

/**
 * Repository class for managing [TrafficModel] entities in the database. Manipulates the [TrafficModelTable] and the [ImageMappingTable].
 * Implements the [TrafficModelRepository] interface.
 *
 * @property [repositoryProvider] The provider for accessing other repositories.
 *
 * @author Team-MobiHub
 */
class TrafficModelDbRepository(
    private val repositoryProvider: RepositoryProvider
) : TrafficModelRepository {

    override fun create(trafficModel: TrafficModel): TrafficModel {
        return transaction {
            val trafficModelId = TrafficModelTable.insert { statement ->
                insertOrUpdate(statement, trafficModel)
            } get (TrafficModelTable.id)

            trafficModel.copy(id = TrafficModelId(trafficModelId))
        }
    }

    override fun getById(id: TrafficModelId): TrafficModel? {
        return transaction {
            TrafficModelTable.selectAll().where { TrafficModelTable.id eq id.id }.map { toTrafficModel(it) }
                .firstOrNull()
        }
    }


    override fun getByTeam(teamId: TeamId): List<TrafficModel> {
        return transaction {
            TrafficModelTable.selectAll().where { TrafficModelTable.ownerTeamId eq teamId.id }
                .map { toTrafficModel(it) }
        }
    }


    override fun getByUser(userId: UserId): List<TrafficModel> {
        return transaction {
            TrafficModelTable.selectAll().where { TrafficModelTable.ownerUserId eq userId.id }
                .map { toTrafficModel(it) }
        }
    }

    override fun update(trafficModel: TrafficModel): TrafficModel {
        return transaction {
            TrafficModelTable.update({ TrafficModelTable.id eq trafficModel.id!!.id }) { statement ->
                insertOrUpdate(statement, trafficModel)
            }
            trafficModel
        }
    }

    override fun searchPaginated(
        page: Int,
        size: Int,
        name: String?,
        authorName: String?,
        region: Region?,
        modelLevels: List<ModelLevel>,
        modelMethods: List<ModelMethod>,
        frameworks: List<Framework>
    ): Pair<List<TrafficModel>, Long> {
        require(page >= 0) { PAGE_INDEX_ERROR }
        require(size > 0) { PAGE_SIZE_ERROR }

        return transaction {
            val averageRatingSubquery = createAverageRatingSubquery()
            val averageRatingAlias = RatingTable.usersRating.avg().alias("averageRating")

            val query = TrafficModelTable
                .join(
                    averageRatingSubquery,
                    JoinType.LEFT,
                    TrafficModelTable.id,
                    averageRatingSubquery[RatingTable.trafficModelId]
                )
                .selectAll()

            query.addFilters(name, authorName, region, modelLevels, modelMethods, frameworks)

            val totalResults = query.count()

            val sortedQuery = query.addSorting(averageRatingSubquery, averageRatingAlias)

            Pair(
                sortedQuery.limit(size).offset(start = (page * size).toLong()).map { toTrafficModel(it) },
                totalResults
            )
        }
    }

    private fun createAverageRatingSubquery(): QueryAlias {
        val averageRatingAlias = RatingTable.usersRating.avg().alias("averageRating")
        return RatingTable
            .select(RatingTable.trafficModelId, averageRatingAlias)
            .groupBy(RatingTable.trafficModelId)
            .alias("averageRatingSubquery")
    }

    private fun Query.addFilters(
        name: String?,
        authorName: String?,
        region: Region?,
        modelLevels: List<ModelLevel>,
        modelMethods: List<ModelMethod>,
        frameworks: List<Framework>
    ) {
        name?.let { addNameFilter(it) }
        authorName?.let { addAuthorFilter(it) }
        region?.let { addRegionFilter(it) }
        if (modelLevels.isNotEmpty()) {
            addModelLevelsFilter(modelLevels)
        }
        if (modelMethods.isNotEmpty()) {
            addModelMethodsFilter(modelMethods)
        }
        if (frameworks.isNotEmpty()) {
            addFrameworksFilter(frameworks)
        }
        useOnlyPublicModels()
    }

    private fun Query.addSorting(averageRatingSubquery: QueryAlias, averageRatingAlias: ExpressionWithColumnTypeAlias<BigDecimal?>): Query {
        return orderBy(
            averageRatingSubquery[averageRatingAlias].isNull() to SortOrder.ASC,
            averageRatingSubquery[averageRatingAlias] to SortOrder.DESC
        )
    }

    override fun delete(id: TrafficModelId) {
        return transaction {
            repositoryProvider.commentRepository.getCommentsForTrafficModel(id).forEach {
                repositoryProvider.commentRepository.deleteComment(it.id)
            }

            repositoryProvider.ratingRepository.getRatingsForTrafficModel(id).forEach {
                repositoryProvider.ratingRepository.deleteRating(it)
            }

            repositoryProvider.favouriteRepository.getFavoritesByTrafficModelId(id).forEach {
                repositoryProvider.favouriteRepository.deleteFavourite(id, it._id!!)
            }

            var uuidList = listOf<UUID>()

            ImageMappingTable.selectAll().where { ImageMappingTable.trafficModelId eq id.id }.forEach {
                uuidList = uuidList.plus(it[ImageMappingTable.imageToken])
            }

            ImageMappingTable.deleteWhere { trafficModelId eq id.id }

            uuidList.forEach {
                repositoryProvider.imageRepository.delete(it)
            }

            repositoryProvider.characteristicsMappingRepository.delete(id)

            TrafficModelTable.deleteWhere { TrafficModelTable.id eq id.id }
        }
    }

    override fun updateImages(trafficModelId: TrafficModelId, images: List<Image>) {
        transaction {
            val mappingEntries =
                ImageMappingTable.selectAll().where { ImageMappingTable.trafficModelId eq trafficModelId.id }
                    .map { it[ImageMappingTable.imageToken] }

            mappingEntries
                .forEach { entry ->
                    ImageMappingTable.deleteWhere { imageToken eq entry }
                    repositoryProvider.imageRepository.delete(entry)
                }

            for (image in images) {
                repositoryProvider.imageRepository.create(image)

                ImageMappingTable.insert {
                    it[this.trafficModelId] = trafficModelId.id
                    it[imageToken] = image.token
                }
            }
        }
    }

    override fun updateImage(trafficModelId: TrafficModelId, image: Image) {
        repositoryProvider.imageRepository.update(image)
    }

    /**
     * Converts a [ResultRow] to a [TrafficModel].
     *
     * @param row The [ResultRow] to be converted.
     * @return The converted [TrafficModel].
     */
    private fun toTrafficModel(row: ResultRow): TrafficModel {
        return TrafficModel(
            id = TrafficModelId(row[TrafficModelTable.id]),
            name = row[TrafficModelTable.name],
            description = row[TrafficModelTable.description],
            isVisibilityPublic = row[TrafficModelTable.isVisibilityPublic],
            dataSourceUrl = row[TrafficModelTable.dataSourceUrl],
            location = Location(
                Region(row[TrafficModelTable.region]), row[TrafficModelTable.coordinates]?.let { Coordinates(it) }),
            framework = Framework.fromId(FrameworkId(row[TrafficModelTable.frameworkId])),
            zipFileToken = row[TrafficModelTable.zipFileToken],
            isZipFileUploaded = row[TrafficModelTable.isZipFileUploaded],
            methodLevelPairProvider = {
                repositoryProvider.characteristicsMappingRepository.get(TrafficModelId(row[TrafficModelTable.id]))
            },
            authorProvider = {
                row[TrafficModelTable.ownerUserId]?.let { ownerId ->
                    repositoryProvider.userRepository.getById(UserId(ownerId))
                } ?: row[TrafficModelTable.ownerTeamId]?.let { teamId ->
                    repositoryProvider.teamRepository.getById(TeamId(teamId))
                } ?: throw IllegalStateException("Owner not found")
            },
            markdownFileUrlProvider = {
                return@TrafficModel null
            },
            imagesProvider = {
                transaction {
                    ImageMappingTable.selectAll()
                        .where { ImageMappingTable.trafficModelId eq row[TrafficModelTable.id] }.mapNotNull {
                            it[ImageMappingTable.imageToken].let { token ->
                                repositoryProvider.imageRepository.get(
                                    token
                                )
                            }
                        }
                }
            },
            averageRatingProvider = {
                repositoryProvider.ratingRepository.getAverageRatingForTrafficModel(TrafficModelId(row[TrafficModelTable.id]))
            },
            ratingsProvider = {
                repositoryProvider.ratingRepository.getRatingsForTrafficModel(TrafficModelId(row[TrafficModelTable.id]))
            },
            commentsProvider = {
                repositoryProvider.commentRepository.getCommentsForTrafficModel(TrafficModelId(row[TrafficModelTable.id]))
            },
            favoritesProvider = {
                repositoryProvider.favouriteRepository.getFavoritesByTrafficModelId(TrafficModelId(row[TrafficModelTable.id]))
            }
        )
    }

    /**
     * Inserts or updates a [TrafficModel] in the [TrafficModelTable].
     *
     * @param statement The [UpdateBuilder] used to build the SQL statement.
     * @param trafficModel The [TrafficModel] to be inserted or updated.
     */
    private fun TrafficModelTable.insertOrUpdate(
        statement: UpdateBuilder<Number>,
        trafficModel: TrafficModel
    ) {
        statement[name] = trafficModel.name
        statement[description] = trafficModel.description
        statement[ownerUserId] =
            if (trafficModel.author.getOwnerType() == OwnerType.USER) trafficModel.author.id!!.id else null
        statement[ownerTeamId] =
            if (trafficModel.author.getOwnerType() == OwnerType.TEAM) trafficModel.author.id!!.id else null
        statement[isVisibilityPublic] = trafficModel.isVisibilityPublic
        statement[dataSourceUrl] = trafficModel.dataSourceUrl
        statement[frameworkId] = trafficModel.framework.id.id
        statement[region] = trafficModel.location.region.name
        statement[coordinates] = trafficModel.location.coordinates!!.value
        statement[zipFileToken] = trafficModel.zipFileToken
        statement[isZipFileUploaded] = trafficModel.isZipFileUploaded
    }

    /**
     * Adds a filter to the query to search for [TrafficModel]s by name.
     *
     * @param name The name to search for.
     */
    private fun Query.addNameFilter(name: String) {
        andWhere { TrafficModelTable.name.lowerCase() like LIKE_TEMPLATE.format(name.lowercase()) }
    }

    /**
     * Adds a filter to the query to search for [TrafficModel]s by author name.
     */
    private fun Query.addAuthorFilter(authorName: String) {
        andWhere {
            (TrafficModelTable.ownerUserId inSubQuery UserTable.select(UserTable.id)
                .where { UserTable.name.lowerCase() like LIKE_TEMPLATE.format(authorName.lowercase()) }) or (TrafficModelTable.ownerTeamId inSubQuery TeamTable.select(
                TeamTable.id
            ).where { TeamTable.name.lowerCase() like LIKE_TEMPLATE.format(authorName.lowercase()) })
        }
    }

    /**
     * Adds a filter to the query to search for [TrafficModel]s by region.
     */
    private fun Query.addRegionFilter(region: Region) {
        andWhere { TrafficModelTable.region.lowerCase() like LIKE_TEMPLATE.format(region.name.lowercase()) }
    }

    /**
     * Adds a filter to the query to search for [TrafficModel]s by model levels.
     */
    private fun Query.addModelLevelsFilter(modelLevels: List<ModelLevel>) {
        andWhere {
            TrafficModelTable.id inSubQuery CharacteristicsMappingTable.select(CharacteristicsMappingTable.trafficModelId)
                .where { CharacteristicsMappingTable.modelLevelId inList modelLevels.map { it.id.id } }
        }
    }

    /**
     * Adds a filter to the query to search for [TrafficModel]s by model methods.
     */
    private fun Query.addModelMethodsFilter(modelMethods: List<ModelMethod>) {
        andWhere {
            TrafficModelTable.id inSubQuery CharacteristicsMappingTable.select(CharacteristicsMappingTable.trafficModelId)
                .where { CharacteristicsMappingTable.modelMethodId inList modelMethods.map { it.id.id } }
        }
    }

    /**
     * Adds a filter to the query to search for [TrafficModel]s by frameworks.
     */
    private fun Query.addFrameworksFilter(frameworks: List<Framework>) {
        andWhere { TrafficModelTable.frameworkId inList frameworks.map { it.id.id } }
    }

    /**
     * Adds a filter to the query to search for only public [TrafficModel]s.
     */
    private fun Query.useOnlyPublicModels() {
        andWhere { TrafficModelTable.isVisibilityPublic eq true }
    }
}
