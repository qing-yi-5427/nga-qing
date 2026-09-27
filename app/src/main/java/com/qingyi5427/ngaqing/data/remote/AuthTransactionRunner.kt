package com.qingyi5427.ngaqing.data.remote

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

/**
 * WebView account cookies must finish syncing after a DataStore revision change, even when
 * that change removes the ViewModel which called us. Each operation has a time bound so a
 * missing WebView callback cannot hold the account lock forever.
 */
internal class AuthTransactionRunner(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val timeoutMillis: Long = 45_000L
) {
    private val mutex = Mutex()

    suspend fun <T> run(block: suspend () -> T): T {
        currentCoroutineContext().ensureActive()
        return scope.async {
            withTimeout(timeoutMillis) {
                mutex.withLock { block() }
            }
        }.await()
    }
}
