package com.pedilo.app.core.result

sealed interface CoreResult<out T> {
    data class Success<T>(val value: T) : CoreResult<T>
    data class Failure(val error: CoreError) : CoreResult<Nothing>
}
