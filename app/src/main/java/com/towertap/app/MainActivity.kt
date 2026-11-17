
package com.towertap.app

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.content.Context
import android.provider.Settings
import android.widget.*

class MainActivity : Activity() {

    private val purple = Color.rgb(124, 58, 237)
    private val background = Color.rgb(15, 12, 29)
    private val panel = Color.rgb(32, 25, 53)
    private val white = Color.WHITE

    private lateinit var xInput: EditText
    private lateinit var yInput: EditText
    private lateinit var intervalInput: EditText
    private lateinit var durationInput: EditText
    private lateinit var repeatInput: EditText
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(background)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 35, 24, 30)
        }

        scroll.addView(root)
        setContentView(scroll)

        val title = TextView(this).apply {
            text = "TowerTap"
            textSize = 32f
            setTextColor(white)
            gravity = Gravity.CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "YOUR PERSONAL AUTO TAPPER"
            textSize = 12f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 25)
        }
        root.addView(subtitle)

        statusText = TextView(this).apply {
            text = "● STOPPED"
            textSize = 14f
            setTextColor(Color.rgb(248, 113, 113))
            gravity = Gravity.CENTER
            setPadding(12, 15, 12, 15)
            setBackgroundColor(panel)
        }
        root.addView(statusText)

        addSection(root, "TAP POSITION")

        xInput = addInput(root, "X coordinate", "540")
        yInput = addInput(root, "Y coordinate", "960")

        addSection(root, "TAP SETTINGS")

        intervalInput = addInput(root, "Interval in milliseconds", "1000")
        durationInput = addInput(root, "Tap duration in milliseconds", "100")
        repeatInput = addInput(root, "Number of taps (0 = unlimited)", "10")

        val permissionButton = makeButton(
            "ENABLE ACCESSIBILITY SERVICE",
            purple
        )

        permissionButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        root.addView(permissionButton)

        val startButton = makeButton("START TAPPING", Color.rgb(22, 163, 74))
        startButton.setOnClickListener {
            if (!isAccessibilityEnabled()) {
                Toast.makeText(
                    this,
                    "Enable TowerTap accessibility first",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            val x = xInput.text.toString().toFloatOrNull()
            val y = yInput.text.toString().toFloatOrNull()
            val interval = intervalInput.text.toString().toLongOrNull()
            val duration = durationInput.text.toString().toLongOrNull()
            val repeat = repeatInput.text.toString().toIntOrNull()

            if (x == null || y == null || interval == null ||
                duration == null || repeat == null ||
                x < 0 || y < 0 || interval < 100 ||
                duration < 1 || duration > 1000 || repeat < 0
            ) {
                Toast.makeText(this, "Check your settings", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sendCommand("START", x, y, interval, duration, repeat)
            statusText.text = "● RUNNING"
            statusText.setTextColor(Color.rgb(74, 222, 128))
        }
        root.addView(startButton)

        val pauseButton = makeButton("PAUSE", Color.rgb(180, 83, 9))
        pauseButton.setOnClickListener {
            sendCommand("PAUSE")
            statusText.text = "● PAUSED"
            statusText.setTextColor(Color.rgb(251, 191, 36))
        }
        root.addView(pauseButton)

        val stopButton = makeButton("STOP", Color.rgb(185, 28, 28))
        stopButton.setOnClickListener {
            sendCommand("STOP")
            statusText.text = "● STOPPED"
            statusText.setTextColor(Color.rgb(248, 113, 113))
        }
        root.addView(stopButton)

        val note = TextView(this).apply {
            text = "TowerTap performs screen gestures at the coordinates you enter. It does not identify buttons or interpret what's happening in another app."
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(5, 22, 5, 5)
        }
        root.addView(note)
    }

    private fun addSection(root: LinearLayout, text: String) {
        val heading = TextView(this).apply {
            this.text = text
            textSize = 13f
            setTextColor(Color.rgb(196, 181, 253))
            setPadding(4, 24, 4, 10)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        root.addView(heading)
    }

    private fun addInput(
        root: LinearLayout,
        hintText: String,
        defaultValue: String
    ): EditText {
        val input = EditText(this).apply {
            hint = hintText
            setText(defaultValue)
            textSize = 15f
            setTextColor(white)
            setHintTextColor(Color.LTGRAY)
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setPadding(16, 12, 16, 12)
            setBackgroundColor(panel)
        }

        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.bottomMargin = 12
        root.addView(input, params)

        return input
    }

    private fun makeButton(label: String, color: Int): Button {
        return Button(this).apply {
            text = label
            textSize = 13f
            setTextColor(white)
            setBackgroundColor(color)
            isAllCaps = false
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = 10
            layoutParams = params
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val manager = getSystemService(Context.ACCESSIBILITY_SERVICE)
            as AccessibilityManager

        return manager.getEnabledAccessibilityServiceList(
            android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        ).any {
            it.resolveInfo.serviceInfo.packageName == packageName &&
            it.resolveInfo.serviceInfo.name.endsWith("TapAccessibilityService")
        }
    }

    private fun sendCommand(
        action: String,
        x: Float = 0f,
        y: Float = 0f,
        interval: Long = 1000,
        duration: Long = 100,
        repeat: Int = 0
    ) {
        val intent = Intent("com.towertap.app.TAP_CONTROL").apply {
            setPackage(packageName)
            putExtra("action", action)
            putExtra("x", x)
            putExtra("y", y)
            putExtra("interval", interval)
            putExtra("duration", duration)
            putExtra("repeat", repeat)
        }
        sendBroadcast(intent)
    }
}
