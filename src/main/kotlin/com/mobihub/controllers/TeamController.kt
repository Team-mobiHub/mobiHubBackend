package com.mobihub.controllers

import com.mobihub.dtos.TeamDTO
import com.mobihub.exceptions.DataWithIdentifierNotFoundException
import com.mobihub.model.UserId
import com.mobihub.services.TeamService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.reflect.*
import org.slf4j.LoggerFactory

private const val TEAM_FOR_USER_FETCHED_TEMPLATE = "Teams for user %s have been fetched."


/**
 * Controller for handling requests related to teams in the REST API.
 *
 * This class contains functions for getting adding, deleting and updating teams, changing team owners,
 * and adding and removing members of teams.
 *
 * @property [teamService] the [TeamService] object to use for team operations
 * @author Team-MobiHub
 */
class TeamController(val teamService: TeamService) {
    private val log = LoggerFactory.getLogger(TeamController::class.java)
    fun create(ctx: ApplicationCall) {
        TODO()
    }

    fun getById(ctx: ApplicationCall) {
        TODO()
    }

    fun update(ctx: ApplicationCall) {
        TODO()
    }

    /**
     * Retrieves the teams associated with the given userId.
     *
     * @param ctx the [ApplicationCall] context for the request
     */
    suspend fun getForUser(ctx: ApplicationCall) {
        try {
            val userId = ctx.parameters["userId"]?.toInt()
            ctx.respond(teamService.getForUser(UserId(userId!!)), typeInfo = typeInfo<List<TeamDTO>>())
            log.info(TEAM_FOR_USER_FETCHED_TEMPLATE.format(userId))
        } catch (e: Exception) {
            log.error(e.message, e)
            when(e) {
                is NumberFormatException -> ctx.respond(HttpStatusCode.BadRequest, typeInfo = typeInfo<HttpStatusCode>())
                is DataWithIdentifierNotFoundException -> ctx.respondText(e.message, status = HttpStatusCode.BadRequest)
                else -> ctx.respond(HttpStatusCode.InternalServerError, typeInfo = typeInfo<HttpStatusCode>())
            }
        }
    }

    fun delete(ctx: ApplicationCall) {
        TODO()
    }

    fun changeOwner(ctx: ApplicationCall) {
        TODO()
    }

    fun getTeamInvitationLink(ctx: ApplicationCall) {
        TODO()
    }

    fun sendTeamInvitation(ctx: ApplicationCall) {
        TODO()
    }

    fun useTeamInvitationLink(ctx: ApplicationCall) {
        TODO()
    }

}