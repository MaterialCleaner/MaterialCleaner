package me.gm.cleaner.net

sealed class NetworkConnectionState<T> {
    class Loading<T> : NetworkConnectionState<T>()
    class Success<T>(val result: T) : NetworkConnectionState<T>()
    class Failure<T>(val exception: Throwable) : NetworkConnectionState<T>()

    fun getOrNull(): T? = if (this is Success) this.result else null
}
