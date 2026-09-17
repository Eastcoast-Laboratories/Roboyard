package roboyard.logic.network

import com.google.gson.JsonArray
import com.google.gson.JsonObject

/**
 * org.json opt*-style accessors for gson JsonObject, used by the shared
 * API client and sync manager.
 */

fun JsonObject.optBoolean(name: String, default: Boolean = false): Boolean =
    if (has(name) && !get(name).isJsonNull) get(name).asBoolean else default

fun JsonObject.optString(name: String, default: String? = null): String? =
    if (has(name) && !get(name).isJsonNull) get(name).asString else default

fun JsonObject.optInt(name: String, default: Int = 0): Int =
    if (has(name) && !get(name).isJsonNull) get(name).asInt else default

fun JsonObject.optLong(name: String, default: Long = 0L): Long =
    if (has(name) && !get(name).isJsonNull) get(name).asLong else default

fun JsonObject.optJsonObject(name: String): JsonObject? =
    if (has(name) && get(name).isJsonObject) getAsJsonObject(name) else null

fun JsonObject.optJsonArray(name: String): JsonArray? =
    if (has(name) && get(name).isJsonArray) getAsJsonArray(name) else null
