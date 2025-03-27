package com.mobihub.model

/**
 * Value classes for the Mobihub project.
 * These classes are used to represent IDs and other values.
 * They are used to make the code more readable and to prevent errors.
 */

@JvmInline
value class TrafficModelId(val id: Int)

@JvmInline
value class CommentId(val id: Int)

@JvmInline
value class IdentityId(val id: Int)

@JvmInline
value class TeamId(val id: Int)

@JvmInline
value class UserId(val id: Int)

@JvmInline
value class FrameworkId(val id: Int)

@JvmInline
value class Region(val name: String)

@JvmInline
value class Coordinates(val value: String)

@JvmInline
value class ModelLevelId(val id: Int)

@JvmInline
value class ModelMethodId(val id: Int)

@JvmInline
value class LinkTypeId(val id: Int)

@JvmInline
value class ShareToken(val value: String)
