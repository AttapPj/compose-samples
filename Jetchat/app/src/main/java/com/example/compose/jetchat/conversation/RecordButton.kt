/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.compose.jetchat.conversation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType.Companion.LongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlin.math.abs

object TouchConstants {
    const val SWIPE_TO_CANCEL_THRESHOLD = 250
    const val TOUCH_SLOP = 100
}

/*
 * @param isRecording whether the user is currently recording
 * @param onClick executed when the recording button is tapped
 * @param onStartRecording executed when the recording button is long pressed
 * @param onFinishRecording executed when an input button touch down event is released
 * @param onCancelRecording executed when a voice input button is swiped to cancel recording
 */
@Composable
fun RecordButton(
    recording: MutableState<Boolean>,
    onClick: () -> Unit,
    onStartRecording: () -> Boolean,
    onFinishRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val passedSwipeThreshold = remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (recording.value) 2f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "record-scale"
    )

    Icon(
        Icons.Default.Mic,
        contentDescription = "Record voice message",
        tint = if (recording.value) Color.Red else LocalContentColor.current,
        modifier = modifier
            .sizeIn(minWidth = 56.dp, minHeight = 6.dp)
            .padding(18.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        val consumed = onStartRecording()
                        recording.value = !consumed
                        if (recording.value) haptics.performHapticFeedback(LongPress)
                    },
                    onTap = {
                        onClick()
                        recording.value = false
                    },
                    onPress = {
                        tryAwaitRelease()
                        if (
                            !passedSwipeThreshold.value &&
                            recording.value
                        ) {
                            onFinishRecording()
                        }
                        recording.value = false
                    }
                )
            }
            .addSwipeGesture(
                recording,
                passedSwipeThreshold,
                onCancelRecording,
            )
            .graphicsLayer { scaleX = scale; scaleY = scale }
    )
}

private fun Modifier.addSwipeGesture(
    isLongPressRecording: MutableState<Boolean>,
    cancelRecording: MutableState<Boolean>,
    onCancelRecording: () -> Unit,
): Modifier {
    var offsetX = 0f
    var offsetY = 0f
    return this.pointerInput(Unit) {
        detectDragGesturesAfterLongPress(
            onDragStart = {
                offsetX = 0f
                offsetY = 0f
                cancelRecording.value = false
            },
            onDragCancel = {
                cancelRecording.value = false
                if (isLongPressRecording.value) {
                    onCancelRecording()
                    isLongPressRecording.value = false
                }
            },
            onDragEnd = {}
        ) { _, dragAmount ->
            if (cancelRecording.value || !isLongPressRecording.value) {
                return@detectDragGesturesAfterLongPress
            }
            offsetX += dragAmount.x
            offsetY += dragAmount.y
            cancelRecording.value =
                (offsetX < 0) &&
                        abs(offsetX) >= TouchConstants.SWIPE_TO_CANCEL_THRESHOLD &&
                        abs(offsetY) <= TouchConstants.TOUCH_SLOP
            if (cancelRecording.value) {
                onCancelRecording()
                isLongPressRecording.value = false
            }
        }
    }
}