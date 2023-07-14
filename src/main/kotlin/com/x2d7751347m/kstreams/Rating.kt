package com.x2d7751347m.kstreams

import kotlinx.serialization.Serializable

/**
 * Rating class for storing a movie rating
 */
@Serializable
data class Rating(val movieId: Long = 1L, val rating: Double = 0.0)

/**
 * intermediate holder of CountAndSum
 */
data class CountAndSum(var count: Long = 0L, var sum: Double = 0.0)
