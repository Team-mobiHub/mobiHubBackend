package com.mobihub.repositories

import com.mobihub.model.Comment
import com.mobihub.model.CommentId
import com.mobihub.model.TrafficModelId
import com.mobihub.model.UserId
import com.mobihub.repositories.db.CommentTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

private const val TRAFFIC_MODEL_NOT_FOUND = "Traffic model with id %d not found"
private const val USER_NOT_FOUND = "User with id %d not found"

/**
 * Repository for managing [Comment] entities.
 *
 * @property repositoryProvider The [RepositoryProvider] instance.
 *
 * @author Team-MobiHub
 */
class CommentDbRepository(
    private val repositoryProvider: RepositoryProvider
) : CommentRepository {

    override fun addComment(comment: Comment): Comment {
        return transaction {
            val commentId = CommentTable.insert {
                it[content] = comment.content
                it[dateTime] = comment.creationDate
                it[trafficModelId] = comment.trafficModel.id?.id!!
                it[userId] = comment.user.id?.id!!
            } get CommentTable.id
            comment.copy(id = CommentId(commentId))
        }
    }

    override fun updateComment(comment: Comment): Comment {
        return transaction {
            CommentTable.update({CommentTable.id eq comment.id.id}) {
                it[content] = comment.content
                it[dateTime] = comment.creationDate
            }
            comment
        }
    }

    override fun getCommentsForTrafficModel(trafficModelId: TrafficModelId): List<Comment> {
        return transaction {
            CommentTable.selectAll().where {
                CommentTable.trafficModelId eq trafficModelId.id
            }.map { toComment(it) }
        }
    }

    override fun getCommentsForUser(userId: UserId): List<Comment> {
        return transaction {
            CommentTable.selectAll().where {
                CommentTable.userId eq userId.id
            } .map { toComment((it)) }
        }
    }

    override fun deleteComment(id: CommentId) {
        transaction {
            CommentTable.deleteWhere { CommentTable.id eq id.id }
        }
    }

    override fun getCommentById(commentId: CommentId): Comment? {
        return transaction {
            CommentTable.selectAll().where {
                CommentTable.id eq commentId.id
            } .map { toComment(it) }
                .firstOrNull()
        }
    }

    private fun toComment(row: ResultRow): Comment {
        return Comment(
            id = CommentId(row[CommentTable.id]),
            content = row[CommentTable.content],
            creationDate = row[CommentTable.dateTime],
            trafficModelProvider = {
                return@Comment repositoryProvider.trafficModelRepository.getById(TrafficModelId(row[CommentTable.trafficModelId]))
                    ?: error(TRAFFIC_MODEL_NOT_FOUND.format({row[CommentTable.trafficModelId]}))
            },
            userProvider = {
                return@Comment repositoryProvider.userRepository.getById(UserId(row[CommentTable.userId]))
                    ?: error(USER_NOT_FOUND.format({row[CommentTable.userId]}))
            }
        )
    }
}
