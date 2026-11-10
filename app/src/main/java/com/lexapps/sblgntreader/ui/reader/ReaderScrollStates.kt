package com.lexapps.sblgntreader.ui.reader

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import com.lexapps.sblgntreader.data.model.ReadingLocation

internal class ReaderScrollStates {
    private val states = mutableMapOf<String, LazyListState>()

    fun forChapter(location: ReadingLocation, paragraphMode: Boolean): LazyListState {
        val key = "${location.key}:${if (paragraphMode) "paragraph" else "verse"}"
        return states.getOrPut(key) { LazyListState() }
    }

    companion object {
        val Saver: Saver<ReaderScrollStates, Any> = listSaver(
            save = { holder ->
                holder.states.flatMap { (key, state) ->
                    listOf(key, state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset)
                }
            },
            restore = { saved ->
                ReaderScrollStates().apply {
                    saved.chunked(3).forEach { position ->
                        states[position[0] as String] = LazyListState(position[1] as Int, position[2] as Int)
                    }
                }
            }
        )
    }
}
