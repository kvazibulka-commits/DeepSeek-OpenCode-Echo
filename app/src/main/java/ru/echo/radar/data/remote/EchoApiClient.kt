package ru.echo.radar.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLEncoder

sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Failure(val error: ApiError) : ApiResult<Nothing>
}

sealed class ApiError(message: String) : Exception(message) {
    data object Unauthorized : ApiError("Токен не принят")
    data class Http(val code: Int) : ApiError("Сервер ответил $code")
    data class Network(val detail: String?) : ApiError("Нет связи с сервером")
    data class Parse(val detail: String?) : ApiError("Не удалось разобрать ответ")
    data class Unexpected(val detail: String?) : ApiError("Неизвестная ошибка")
}

class EchoApiClient(private val http: OkHttpClient) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    suspend fun fetchState(host: String, token: String, deviceId: String): ApiResult<EchoStateDto> =
        withContext(Dispatchers.IO) {
            val base = EchoUrl.normalizeHost(host)
            if (base.isEmpty()) {
                return@withContext ApiResult.Failure(ApiError.Network("Адрес сервера не задан"))
            }
            val encodedId = URLEncoder.encode(deviceId, "UTF-8")
            val request = Request.Builder()
                .url(EchoUrl.endpoint(base, "/api/state?deviceId=$encodedId"))
                .get()
                .auth(token)
                .header("X-Device-Id", deviceId)
                .build()

            execute(request) { body -> json.decodeFromString<EchoStateDto>(body) }
        }

    suspend fun post(
        host: String,
        token: String,
        deviceId: String,
        path: String,
        payload: JsonObject,
    ): ApiResult<Unit> = withContext(Dispatchers.IO) {
        val base = EchoUrl.normalizeHost(host)
        if (base.isEmpty()) {
            return@withContext ApiResult.Failure(ApiError.Network("Адрес сервера не задан"))
        }
        val request = Request.Builder()
            .url(EchoUrl.endpoint(base, path))
            .post(payload.toString().toRequestBody(JSON_MEDIA))
            .auth(token)
            .header("X-Device-Id", deviceId)
            .build()

        execute(request) { Unit }
    }

    private fun <T> execute(request: Request, parse: (String) -> T): ApiResult<T> {
        return try {
            http.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                when {
                    response.code == 401 -> ApiResult.Failure(ApiError.Unauthorized)
                    !response.isSuccessful -> ApiResult.Failure(ApiError.Http(response.code))
                    else -> ApiResult.Success(parse(body))
                }
            }
        } catch (e: SerializationException) {
            ApiResult.Failure(ApiError.Parse(e.message))
        } catch (e: IOException) {
            ApiResult.Failure(ApiError.Network(e.message))
        } catch (e: Exception) {
            ApiResult.Failure(ApiError.Unexpected(e.message))
        }
    }

    private fun Request.Builder.auth(token: String): Request.Builder =
        if (token.isBlank()) this else addHeader("X-Echo-Token", token)

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}
