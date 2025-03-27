package com.mobihub.dtos

import com.mobihub.model.Framework
import com.mobihub.model.ModelLevel
import com.mobihub.model.ModelMethod
import kotlinx.serialization.Serializable

/**
 * Data transfer object for search requests.
 *
 * @property page The page number.
 * @property pageSize The size of the page.
 * @property name The name of the traffic model.
 * @property authorName The name of the author.
 * @property region The region of the traffic model.
 * @property modelLevels The model levels of the traffic model.
 * @property modelMethods The model methods of the traffic model.
 * @property frameworks The frameworks of the traffic model.
 *
 * @author Team-MobiHub
 */
@Serializable
data class SearchRequestDTO (
    val page: Int,
    val pageSize: Int,
    val name: String?,
    val authorName: String?,
    val region: String?,
    val modelLevels: List<ModelLevel>,
    val modelMethods: List<ModelMethod>,
    val frameworks: List<Framework>,
)