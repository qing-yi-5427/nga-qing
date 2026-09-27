package com.qingyi5427.ngaqing.data.remote

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthTransactionRunnerTest {
    @Test
    fun `account transaction finishes after caller is cancelled`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val runner = AuthTransactionRunner(scope, 5_000)
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val finished = CompletableDeferred<Unit>()
            val caller = launch {
                runner.run {
                    started.complete(Unit)
                    release.await()
                    finished.complete(Unit)
                }
            }
            withTimeout(2_000) { started.await() }
            caller.cancelAndJoin()
            release.complete(Unit)
            withTimeout(2_000) { finished.await() }
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `timed out transaction releases account lock`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val runner = AuthTransactionRunner(scope, 100)
            val never = CompletableDeferred<Unit>()
            try {
                runner.run { never.await() }
                throw AssertionError("Expected transaction timeout")
            } catch (_: TimeoutCancellationException) {
                // The stalled WebView callback cannot hold the lock.
            }
            assertEquals(42, withTimeout(2_000) { runner.run { 42 } })
        } finally {
            scope.cancel()
        }
    }
}
