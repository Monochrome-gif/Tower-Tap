
package com.towertap.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class TapAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())

    private var receiverRegistered = false
    private var running = false

    private var tapX = 0f
    private var tapY = 0f
    private var interval = 1000L
    private var duration = 100L
    private var repeatCount = 10
    private var completedTaps = 0

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.getStringExtra("action")) {
                "START" -> {
                    tapX = intent.getFloatExtra("x", 0f)
                    tapY = intent.getFloatExtra("y", 0f)
                    interval = intent.getLongExtra("interval", 1000L)
                    duration = intent.getLongExtra("duration", 100L)
                    repeatCount = intent.getIntExtra("repeat", 10)

                    startTapping()
                }

                "PAUSE" -> pauseTapping()

                "STOP" -> stopTapping()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        val filter = IntentFilter("com.towertap.app.TAP_CONTROL")

        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(
                receiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(receiver, filter)
        }

        receiverRegistered = true
    }

    private fun startTapping() {
        if (tapX < 0 || tapY < 0 ||
            interval < 100 || duration < 1 ||
            duration > 1000 || repeatCount < 0
        ) {
            stopTapping()
            return
        }

        running = true
        completedTaps = 0
        handler.removeCallbacks(tapRunnable)
        handler.post(tapRunnable)
    }

    private fun pauseTapping() {
        running = false
        handler.removeCallbacks(tapRunnable)
    }

    private fun stopTapping() {
        running = false
        completedTaps = 0
        handler.removeCallbacks(tapRunnable)
    }

    private val tapRunnable = object : Runnable {
        override fun run() {
            if (!running) return

            if (repeatCount > 0 && completedTaps >= repeatCount) {
                stopTapping()
                return
            }

            val path = Path().apply {
                moveTo(tapX, tapY)
            }

            val gesture = GestureDescription.Builder()
                .addStroke(
                    GestureDescription.StrokeDescription(
                        path,
                        0,
                        duration
                    )
                )
                .build()

            val accepted = dispatchGesture(
                gesture,
                object : GestureResultCallback() {

                    override fun onCompleted(
                        gestureDescription: GestureDescription?
                    ) {
                        super.onCompleted(gestureDescription)

                        if (!running) return

                        completedTaps++

                        if (repeatCount > 0 &&
                            completedTaps >= repeatCount
                        ) {
                            stopTapping()
                        } else {
                            handler.postDelayed(
                                tapRunnable,
                                interval
                            )
                        }
                    }

                    override fun onCancelled(
                        gestureDescription: GestureDescription?
                    ) {
                        super.onCancelled(gestureDescription)
                        stopTapping()
                    }
                },
                handler
            )

            if (!accepted) {
                stopTapping()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No screen content is collected or inspected.
    }

    override fun onInterrupt() {
        stopTapping()
    }

    override fun onDestroy() {
        stopTapping()

        if (receiverRegistered) {
            unregisterReceiver(receiver)
            receiverRegistered = false
        }

        super.onDestroy()
    }
}
