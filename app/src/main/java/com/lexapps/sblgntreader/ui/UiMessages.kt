package com.lexapps.sblgntreader.ui

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@Singleton
class UiMessages @Inject constructor() {
    private val channel = Channel<String>(Channel.BUFFERED)
    val messages = channel.receiveAsFlow()
    fun report(message: String, error: Exception) {
        Log.e("GntReader", message, error)
        channel.trySend(message)
    }
}

fun CoroutineScope.launchUserAction(messages: UiMessages, failureMessage: String, action: suspend () -> Unit) = launch {
    try { action() }
    catch (cancelled: CancellationException) { throw cancelled }
    catch (error: Exception) { messages.report(failureMessage, error) }
}
