package com.mobihub.repositories

import com.mobihub.model.*

/**
 * Repository for comments.
 *
 * This interface defines the methods that a repository for comments must implement.
 * @see [CommentDbRepository]
 *
 * @author Team-MobiHub
 */
interface CommentRepository {

    /**
     * Adds a new comment.
     *
     * @param comment The comment to add.
     * @return The added comment.
     */
    fun addComment(comment: Comment): Comment

    /**
     * Updates an existing comment.
     *
     * @param comment The comment to update.
     * @return The updated comment.
     */
    fun updateComment(comment: Comment): Comment

    /**
     * Retrieves all comments for a given traffic model.
     *
     * @param trafficModelId The ID of the traffic model.
     * @return The comments for the given traffic model.
     */
    fun getCommentsForTrafficModel(trafficModelId: TrafficModelId): List<Comment>

    /**
     * Retrieves all comments from a given user.
     *
     * @param userId the ID on the user.
     * @return The comments from this user.
     */
    fun getCommentsForUser(userId: UserId): List<Comment>

    /**
     * Deletes an existing comment.
     *
     * @param id The ID of the given comment.
      */
    fun deleteComment(id: CommentId)

    /**
     * Returns the comment identified by the given id, if it exists
     *
     * @param id the id of the comment
     * @return the comment, if it exists
     */
    fun getCommentById(commentId: CommentId): Comment?
}