package com.myplayer.app

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.OpenableColumns
import android.text.TextUtils
import android.util.Rational
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.app.Activity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private const val MP =
    ViewGroup.LayoutParams.MATCH_PARENT

private const val WC =
    ViewGroup.LayoutParams.WRAP_CONTENT

@SuppressLint("UnsafeOptInUsageError")
class PlayerActivity : Activity() {

    private lateinit var player: ExoPlayer
    private lateinit var view: PlayerView
    private lateinit var prefs: SharedPreferences
    private lateinit var am: AudioManager

    private lateinit var ui: FrameLayout
    private lateinit var topBar: LinearLayout
    private lateinit var title: TextView
    private lateinit var bPlay: ImageView
    private lateinit var seek: SeekBar
    private lateinit var tCur: TextView
    private lateinit var tDur: TextView
    private lateinit var skipL: TextView
    private lateinit var skipR: TextView
    private lateinit var toast: TextView
    private lateinit var unlockBtn: ImageView

    private var uiOn = true
    private var locked = false
    private var dragging = false
    private var released = false
    private var oriLocked = false
    private var fitMode = 0

    private val fitModes = intArrayOf(
        AspectRatioFrameLayout.RESIZE_MODE_FIT,
        AspectRatioFrameLayout.RESIZE_MODE_FILL,
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
        AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH,
        AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
    )

    private val fitNames = arrayOf(
        "Fit screen",
        "Stretch",
        "Crop",
        "100% Width",
        "100% Height"
    )

    private val h =
        Handler(Looper.getMainLooper())

    private val hideUiRun =
        Runnable { hideUi() }

    private val hideSkip =
        Runnable {
            skipL.alpha = 0f
            skipR.alpha = 0f
        }

    private val hideToast =
        Runnable {
            toast.animate().alpha(0f)
        }

    private val hideUnlock =
        Runnable {
            unlockBtn.animate().alpha(0f)
        }

    private val sleepRun =
        Runnable {
            finish()
        }

    private lateinit var scaler:
        ScaleGestureDetector

    private var zoom = 1f
    private var mode = 0
    private var downX = 0f
    private var downY = 0f
    private var startPos = 0L
    private var seekTarget = 0L
    private var startVol = 0f
    private var startBright = 0f

    private val slop = 20f

    private var lastTap = 0L
    private var lastSide = 0
    private var accum = 0

    private var longPress = false
    private var prevSpeed = 1f

    private val lpRun =
        Runnable {
            if (
                mode == 0 &&
                !locked &&
                player.isPlaying
            ) {
                longPress = true
                prevSpeed =
                    player.playbackParameters.speed

                player.setPlaybackSpeed(2f)
                say("Speed 2.0x")
                h.removeCallbacks(toggleRun)
            }
        }

    private val toggleRun =
        Runnable {
            if (!longPress) toggleUi()
        }

    override fun onCreate(
        b: Bundle?
    ) {
        super.onCreate(b)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        if (Build.VERSION.SDK_INT >= 28) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams
                    .LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        hideBars()

        prefs =
            getSharedPreferences(
                "prefs",
                MODE_PRIVATE
            )

        am =
            getSystemService(
                AUDIO_SERVICE
            ) as AudioManager

        val root =
            FrameLayout(this)

        root.setBackgroundColor(
            Color.BLACK
        )

        view =
            PlayerView(this)

        view.useController = false
        view.setBackgroundColor(
            Color.BLACK
        )

        root.addView(
            view,
            FrameLayout.LayoutParams(
                MP,
                MP,
                Gravity.CENTER
            )
        )

        buildUi(root)
        setContentView(root)

        scaler =
            ScaleGestureDetector(
                this,
                object :
                    ScaleGestureDetector.SimpleOnScaleGestureListener() {

                    override fun onScale(
                        d: ScaleGestureDetector
                    ): Boolean {
                        zoom =
                            (
                                zoom *
                                    d.scaleFactor
                                ).coerceIn(
                                    0.25f,
                                    4f
                                )

                        applyZoom()
                        return true
                    }
                }
            )

        root.setOnTouchListener { _, e ->
            onTouch(e)
            true
        }

        initPlayer()
    }

    private fun initPlayer() {
        player =
            ExoPlayer.Builder(this)
                .build()

        view.player = player

        val uris =
            intent.getStringArrayListExtra(
                "uris"
            )

        val idx =
            intent.getIntExtra(
                "index",
                0
            )

        if (
            uris != null &&
            uris.isNotEmpty()
        ) {
            val items =
                uris.map {
                    MediaItem.fromUri(it)
                }

            player.setMediaItems(
                items,
                idx,
                C.TIME_UNSET
            )
        } else {
            intent.data?.let {
                player.setMediaItem(
                    MediaItem.fromUri(it)
                )
            }
        }

        player.addListener(
            object : Player.Listener {

                override fun onMediaItemTransition(
                    mi: MediaItem?,
                    reason: Int
                ) {
                    if (mi == null) return

                    val u =
                        mi.localConfiguration
                            ?.uri
                            ?.toString()
                            ?: return

                    title.text =
                        nameOf(Uri.parse(u))

                    val p =
                        prefs.getLong(
                            u,
                            0
                        )

                    if (p > 0) {
                        player.seekTo(p)
                    }
                }

                override fun onPlaybackStateChanged(
                    s: Int
                ) {
                    bPlay.setImageResource(
                        if (player.isPlaying)
                            R.drawable.ic_pause
                        else
                            R.drawable.ic_play
                    )

                    if (
                        s ==
                        Player.STATE_READY
                    ) {
                        val d =
                            player.duration

                        if (d > 0) {
                            tDur.text =
                                fmt(d)

                            seek.max =
                                (d / 1000)
                                    .toInt()
                        }
                    }
                }

                override fun onIsPlayingChanged(
                    p: Boolean
                ) {
                    bPlay.setImageResource(
                        if (p)
                            R.drawable.ic_play
                        else
                            R.drawable.ic_play
                    )

                    if (p) {
                        resetUiTimer()
                    }
                }
            }
        )

        player.prepare()
        player.play()

        h.post(
            object : Runnable {
                override fun run() {
                    if (
                        ::player.isInitialized &&
                        !dragging
                    ) {
                        val p =
                            player.currentPosition

                        tCur.text =
                            fmt(p)

                        val d =
                            player.duration

                        if (d > 0) {
                            seek.progress =
                                (p / 1000)
                                    .toInt()
                        }
                    }

                    h.postDelayed(
                        this,
                        1000
                    )
                }
            }
        )
    }

    private fun nameOf(
        u: Uri
    ): String {
        try {
            contentResolver.query(
                u,
                arrayOf(
                    OpenableColumns.DISPLAY_NAME
                ),
                null,
                null,
                null
            )?.use { c ->
                if (c.moveToFirst()) {
                    return c.getString(0)
                }
            }
        } catch (e: Exception) {
        }

        return u.lastPathSegment
            ?: "Video"
    }

    private fun fmt(
        ms: Long
    ): String {
        val s =
            ms.coerceAtLeast(0) /
                1000

        val hh = s / 3600
        val m =
            (s % 3600) / 60

        val c = s % 60

        return if (hh > 0) {
            String.format(
                "%d:%02d:%02d",
                hh,
                m,
                c
            )
        } else {
            String.format(
                "%d:%02d",
                m,
                c
            )
        }
    }

    private fun say(
        m: String
    ) {
        toast.text = m
        toast.alpha = 1f

        h.removeCallbacks(
            hideToast
        )

        h.postDelayed(
            hideToast,
            2000
        )
    }

    private fun dp(
        v: Int
    ) =
        (
            v *
                resources
                    .displayMetrics
                    .density
            ).toInt()

    private fun buildUi(
        root: FrameLayout
    ) {
        ui =
            FrameLayout(this)

        topBar =
            LinearLayout(this)

        topBar.gravity =
            Gravity.CENTER_VERTICAL

        topBar.setBackgroundColor(
            0x66000000
        )

        topBar.setPadding(
            dp(16),
            dp(16),
            dp(16),
            dp(16)
        )

        val bck =
            ImageView(this).apply {
                setImageResource(
                    R.drawable.ic_back
                )
            }

        bck.setOnClickListener {
            finish()
        }

        topBar.addView(
            bck,
            LinearLayout.LayoutParams(
                dp(32),
                dp(32)
            ).apply {
                marginEnd = dp(16)
            }
        )

        title =
            TextView(this)

        title.setTextColor(
            Color.WHITE
        )

        title.textSize = 16f
        title.isSingleLine = true
        title.ellipsize =
            TextUtils.TruncateAt.MARQUEE
        title.isSelected = true

        topBar.addView(
            title,
            LinearLayout.LayoutParams(
                0,
                WC,
                1f
            )
        )

        val rot =
            ImageView(this).apply {
                setImageResource(
                    R.drawable.ic_rotate
                )
            }

        rot.setOnClickListener {
            rotate()
        }

        topBar.addView(
            rot,
            LinearLayout.LayoutParams(
                dp(32),
                dp(32)
            ).apply {
                marginStart = dp(16)
            }
        )

        val fit =
            ImageView(this).apply {
                setImageResource(
                    R.drawable.ic_fit
                )
            }

        fit.setOnClickListener {
            cycleFit()
        }

        topBar.addView(
            fit,
            LinearLayout.LayoutParams(
                dp(32),
                dp(32)
            ).apply {
                marginStart = dp(16)
            }
        )

        val more =
            ImageView(this).apply {
                setImageResource(
                    R.drawable.ic_more_v
                )
            }

        more.setOnClickListener {
            moreMenu()
        }

        topBar.addView(
            more,
            LinearLayout.LayoutParams(
                dp(32),
                dp(32)
            ).apply {
                marginStart = dp(16)
            }
        )

        ui.addView(
            topBar,
            FrameLayout.LayoutParams(
                MP,
                WC,
                Gravity.TOP
            )
        )

        val bot =
            LinearLayout(this)

        bot.orientation =
            LinearLayout.VERTICAL

        bot.setBackgroundColor(
            0x66000000
        )

        bot.setPadding(
            dp(16),
            dp(8),
            dp(16),
            dp(24)
        )

        val tRow =
            LinearLayout(this)

        tRow.gravity =
            Gravity.CENTER_VERTICAL

        tCur =
            TextView(this).apply {
                setTextColor(
                    Color.WHITE
                )
                textSize = 13f
                text = "0:00"
            }

        tDur =
            TextView(this).apply {
                setTextColor(
                    Color.WHITE
                )
                textSize = 13f
                text = "0:00"
            }

        seek =
            SeekBar(this)

        seek.progressDrawable =
            getDrawable(
                R.drawable.seek_progress
            )

        seek.thumb =
            getDrawable(
                R.drawable.seek_thumb
            )

        seek.setOnSeekBarChangeListener(
            object :
                SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(
                    s: SeekBar,
                    p: Int,
                    b: Boolean
                ) {
                    if (b) {
                        tCur.text =
                            fmt(
                                p * 1000L
                            )
                    }
                }

                override fun onStartTrackingTouch(
                    s: SeekBar
                ) {
                    dragging = true
                    h.removeCallbacks(
                        hideUiRun
                    )
                }

                override fun onStopTrackingTouch(
                    s: SeekBar
                ) {
                    dragging = false

                    player.seekTo(
                        s.progress *
                            1000L
                    )

                    resetUiTimer()
                }
            }
        )

        tRow.addView(tCur)

        tRow.addView(
            seek,
            LinearLayout.LayoutParams(
                0,
                WC,
                1f
            ).apply {
                setMargins(
                    dp(12),
                    0,
                    dp(12),
                    0
                )
            }
        )

        tRow.addView(tDur)

        bot.addView(
            tRow,
            LinearLayout.LayoutParams(
                MP,
                WC
            ).apply {
                bottomMargin = dp(16)
            }
        )

        val bRow =
            LinearLayout(this)

        bRow.gravity =
            Gravity.CENTER

        val lk =
            ImageView(this).apply {
                setImageResource(
                    R.drawable.ic_lock
                )
            }

        lk.setOnClickListener {
            lockScreen()
        }

        bRow.addView(
            lk,
            LinearLayout.LayoutParams(
                dp(36),
                dp(36)
            ).apply {
                marginEnd = dp(32)
            }
        )

        val sub =
            ImageView(this).apply {
                setImageResource(
                    R.drawable.ic_subs
                )
            }

        sub.setOnClickListener {
            subsDialog()
        }

        bRow.addView(
            sub,
            LinearLayout.LayoutParams(
                dp(36),
                dp(36)
            ).apply {
                marginEnd = dp(32)
            }
        )

        bPlay =
            ImageView(this).apply {
                setImageResource(
                    R.drawable.ic_play
                )
            }

        bPlay.setOnClickListener {
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }

            resetUiTimer()
        }

        bRow.addView(
            bPlay,
            LinearLayout.LayoutParams(
                dp(56),
                dp(56)
            )
        )

        val aud =
            ImageView(this).apply {
                setImageResource(
                    R.drawable.ic_audio
                )
            }

        aud.setOnClickListener {
            trackDialog(
                C.TRACK_TYPE_AUDIO,
                "Audio track"
            )
        }

        bRow.addView(
            aud,
            LinearLayout.LayoutParams(
                dp(36),
                dp(36)
            ).apply {
                marginStart = dp(32)
            }
        )

        val pip =
            ImageView(this).apply {
                setImageResource(
                    R.drawable.ic_video
                )
            }

        pip.setOnClickListener {
            enterPip()
        }

        bRow.addView(
            pip,
            LinearLayout.LayoutParams(
                dp(36),
                dp(36)
            ).apply {
                marginStart = dp(32)
            }
        )

        bot.addView(
            bRow,
            LinearLayout.LayoutParams(
                MP,
                WC
            )
        )

        ui.addView(
            bot,
            FrameLayout.LayoutParams(
                MP,
                WC,
                Gravity.BOTTOM
            )
        )

        root.addView(
            ui,
            FrameLayout.LayoutParams(
                MP,
                MP
            )
        )

        skipL =
            TextView(this).apply {
                setTextColor(
                    Color.WHITE
                )
                textSize = 16f
                alpha = 0f

                setPadding(
                    dp(32),
                    dp(16),
                    dp(32),
                    dp(16)
                )

                setBackgroundColor(
                    0x88000000.toInt()
                )
            }

        skipR =
            TextView(this).apply {
                setTextColor(
                    Color.WHITE
                )
                textSize = 16f
                alpha = 0f

                setPadding(
                    dp(32),
                    dp(16),
                    dp(32),
                    dp(16)
                )

                setBackgroundColor(
                    0x88000000.toInt()
                )
            }

        root.addView(
            skipL,
            FrameLayout.LayoutParams(
                WC,
                WC,
                Gravity.CENTER_VERTICAL or
                    Gravity.START
            ).apply {
                marginStart = dp(48)
            }
        )

        root.addView(
            skipR,
            FrameLayout.LayoutParams(
                WC,
                WC,
                Gravity.CENTER_VERTICAL or
                    Gravity.END
            ).apply {
                marginEnd = dp(48)
            }
        )

        toast =
            TextView(this).apply {
                setTextColor(
                    Color.WHITE
                )
                textSize = 14f
                alpha = 0f

                setPadding(
                    dp(16),
                    dp(8),
                    dp(16),
                    dp(8)
                )

                setBackgroundColor(
                    0xAA000000.toInt()
                )
            }

        root.addView(
            toast,
            FrameLayout.LayoutParams(
                WC,
                WC,
                Gravity.CENTER_HORIZONTAL or
                    Gravity.TOP
            ).apply {
                topMargin = dp(48)
            }
        )

        unlockBtn =
            ImageView(this).apply {
                setImageResource(
                    R.drawable.ic_lock
                )

                alpha = 0f

                setPadding(
                    dp(16),
                    dp(16),
                    dp(16),
                    dp(16)
                )

                setBackgroundColor(
                    0x88000000.toInt()
                )
            }

        unlockBtn.setOnClickListener {
            unlockScreen()
        }

        root.addView(
            unlockBtn,
            FrameLayout.LayoutParams(
                dp(64),
                dp(64),
                Gravity.CENTER_VERTICAL or
                    Gravity.START
            ).apply {
                marginStart = dp(48)
            }
        )
    }

    private fun toggleUi() {
        uiOn = !uiOn

        ui.visibility =
            if (uiOn)
                View.VISIBLE
            else
                View.GONE

        if (uiOn) {
            resetUiTimer()
        } else {
            h.removeCallbacks(
                hideUiRun
            )
        }

        hideBars()
    }

    private fun hideUi() {
        uiOn = false
        ui.visibility =
            View.GONE
        hideBars()
    }

    private fun resetUiTimer() {
        h.removeCallbacks(
            hideUiRun
        )

        if (player.isPlaying) {
            h.postDelayed(
                hideUiRun,
                3000
            )
        }
    }

    private fun onTouch(
        e: MotionEvent
    ) {
        scaler.onTouchEvent(e)

        if (scaler.isInProgress) {
            return
        }

        val x = e.x
        val y = e.y

        val w =
            ui.width.toFloat()

        val hView =
            ui.height.toFloat()

        when (e.actionMasked) {

            MotionEvent.ACTION_DOWN -> {
                downX = x
                downY = y

                mode = 0

                startPos =
                    player.currentPosition

                startVol =
                    am.getStreamVolume(
                        AudioManager.STREAM_MUSIC
                    ).toFloat() /
                        am.getStreamMaxVolume(
                            AudioManager.STREAM_MUSIC
                        )

                startBright =
                    window.attributes
                        .screenBrightness

                if (startBright < 0) {
                    startBright = 0.5f
                }

                longPress = false

                h.postDelayed(
                    lpRun,
                    600
                )

                if (locked) {
                    unlockBtn.alpha = 1f

                    h.removeCallbacks(
                        hideUnlock
                    )

                    h.postDelayed(
                        hideUnlock,
                        3000
                    )
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (locked) return

                val dx =
                    x - downX

                val dy =
                    y - downY

                if (
                    mode == 0 &&
                    (
                        abs(dx) > slop ||
                            abs(dy) > slop
                        )
                ) {
                    h.removeCallbacks(
                        lpRun
                    )

                    if (zoom > 1f) {
                        mode = 4
                    } else {
                        mode =
                            if (
                                abs(dx) >
                                abs(dy)
                            ) {
                                1
                            } else if (
                                downX >
                                w / 2
                            ) {
                                2
                            } else {
                                3
                            }
                    }
                }

                when (mode) {
                    1 -> {
                        val ms =
                            (
                                dx / w *
                                    180000
                                ).toLong()

                        seekTarget =
                            (
                                startPos + ms
                                ).coerceIn(
                                    0,
                                    player.duration
                                        .coerceAtLeast(0)
                                )

                        say(
                            "Seek: " +
                                fmt(seekTarget)
                        )
                    }

                    2 -> {
                        val v =
                            (
                                startVol -
                                    dy / hView
                                ).coerceIn(
                                    0f,
                                    1f
                                )

                        val maxVol =
                            am.getStreamMaxVolume(
                                AudioManager.STREAM_MUSIC
                            )

                        am.setStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            (v * maxVol)
                                .roundToInt(),
                            0
                        )

                        say(
                            "Volume: " +
                                (v * 100)
                                    .toInt() +
                                "%"
                        )
                    }

                    3 -> {
                        val b =
                            (
                                startBright -
                                    dy / hView
                                ).coerceIn(
                                    0f,
                                    1f
                                )

                        val lp =
                            window.attributes

                        lp.screenBrightness = b
                        window.attributes = lp

                        say(
                            "Brightness: " +
                                (b * 100)
                                    .toInt() +
                                "%"
                        )
                    }

                    4 -> {
                        view.translationX += dx
                        view.translationY += dy

                        downX = x
                        downY = y
                    }
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {

                h.removeCallbacks(
                    lpRun
                )

                if (longPress) {
                    player.setPlaybackSpeed(
                        prevSpeed
                    )

                    say(
                        "Speed " +
                            prevSpeed +
                            "x"
                    )

                    return
                }

                if (mode == 1) {
                    player.seekTo(
                        seekTarget
                    )
                } else if (
                    mode == 0 &&
                    !locked
                ) {
                    val now =
                        SystemClock.elapsedRealtime()

                    val side =
                        if (x > w / 2)
                            1
                        else
                            -1

                    if (
                        now - lastTap < 300
                    ) {
                        h.removeCallbacks(
                            toggleRun
                        )

                        if (
                            lastSide == side
                        ) {
                            accum += 10
                        } else {
                            accum = 10
                        }

                        lastSide = side

                        val diff =
                            accum *
                                side *
                                1000L

                        player.seekTo(
                            (
                                player.currentPosition +
                                    diff
                                ).coerceIn(
                                    0,
                                    player.duration
                                        .coerceAtLeast(0)
                                )
                        )

                        if (side == 1) {
                            skipR.text =
                                "+$accum s"

                            skipR.alpha = 1f
                        } else {
                            skipL.text =
                                "-$accum s"

                            skipL.alpha = 1f
                        }

                        h.removeCallbacks(
                            hideSkip
                        )

                        h.postDelayed(
                            hideSkip,
                            800
                        )
                    } else {
                        accum = 0

                        h.postDelayed(
                            toggleRun,
                            250
                        )
                    }

                    lastTap = now
                }

                mode = 0
            }
        }
    }

    private fun applyZoom() {
        if (zoom == 1f) {
            view.scaleX = 1f
            view.scaleY = 1f
            view.translationX = 0f
            view.translationY = 0f
        } else {
            view.scaleX = zoom
            view.scaleY = zoom
        }
    }

    private fun lockScreen() {
        locked = true

        hideUi()

        unlockBtn.visibility =
            View.VISIBLE

        unlockBtn.alpha = 1f

        h.postDelayed(
            hideUnlock,
            3000
        )

        say("Controls locked")
    }

    private fun unlockScreen() {
        locked = false

        unlockBtn.visibility =
            View.GONE

        toggleUi()

        say("Controls unlocked")
    }

    private fun cycleFit() {
        if (zoom != 1f) {
            zoom = 1f
            applyZoom()
            say("Zoom reset")
            return
        }

        fitMode =
            (fitMode + 1) %
                fitModes.size

        view.resizeMode =
            fitModes[fitMode]

        say(
            fitNames[fitMode]
        )
    }

    private fun rotate() {
        oriLocked = true

        requestedOrientation =
            if (
                resources.configuration
                    .orientation ==
                    Configuration.ORIENTATION_LANDSCAPE
            ) {
                ActivityInfo
                    .SCREEN_ORIENTATION_SENSOR_PORTRAIT
            } else {
                ActivityInfo
                    .SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
    }

    private fun moreMenu() {
        val items = arrayOf(
            "Playback speed",
            "Audio track",
            "Sleep timer",
            "Lock screen",
            "Picture in picture",
            "Video info"
        )

        AlertDialog.Builder(this)
            .setItems(items) { _, w ->
                when (w) {
                    0 -> speedDialog()

                    1 -> trackDialog(
                        C.TRACK_TYPE_AUDIO,
                        "Audio track"
                    )

                    2 -> sleepDialog()

                    3 -> lockScreen()

                    4 -> enterPip()

                    5 -> infoDialog()
                }
            }
            .show()
    }

    private fun subsDialog() {
        trackDialog(
            C.TRACK_TYPE_TEXT,
            "Subtitles"
        )
    }

    private fun speedDialog() {
        val v = floatArrayOf(
            0.25f,
            0.5f,
            0.75f,
            1f,
            1.25f,
            1.5f,
            2f,
            3f
        )

        val names =
            v.map {
                it.toString() + "x"
            }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(
                "Playback speed"
            )
            .setItems(names) { _, w ->
                player.setPlaybackSpeed(
                    v[w]
                )

                say(names[w])
            }
            .show()
    }

    private fun trackDialog(
        type: Int,
        ttl: String
    ) {
        val list =
            ArrayList<
                Triple<
                    Tracks.Group,
                    Int,
                    String
                >
            >()

        for (
            g in player.currentTracks.groups
        ) {
            if (g.type != type) {
                continue
            }

            for (
                i in 0 until g.length
            ) {
                val f =
                    g.getTrackFormat(i)

                val n =
                    f.label
                        ?: f.language?.let {
                            Locale(it)
                                .displayLanguage
                        }
                        ?: (
                            "Track " +
                                (list.size + 1)
                            )

                list.add(
                    Triple(
                        g,
                        i,
                        n
                    )
                )
            }
        }

        val isText =
            type == C.TRACK_TYPE_TEXT

        val names =
            ArrayList<String>()

        if (isText) {
            names.add("Off")
        }

        for (t in list) {
            names.add(t.third)
        }

        if (isText) {
            names.add(
                "Add subtitle file..."
            )
        }

        if (names.isEmpty()) {
            say("No tracks")
            return
        }

        AlertDialog.Builder(this)
            .setTitle(ttl)
            .setItems(
                names.toTypedArray()
            ) { _, w ->

                val off =
                    if (isText) 1
                    else 0

                if (
                    isText &&
                    w == 0
                ) {
                    player
                        .trackSelectionParameters =
                        player
                            .trackSelectionParameters
                            .buildUpon()
                            .setTrackTypeDisabled(
                                type,
                                true
                            )
                            .build()

                } else if (
                    isText &&
                    w == names.size - 1
                ) {
                    pickSubtitle()

                } else {
                    val t =
                        list[w - off]

                    player
                        .trackSelectionParameters =
                        player
                            .trackSelectionParameters
                            .buildUpon()
                            .setTrackTypeDisabled(
                                type,
                                false
                            )
                            .setOverrideForType(
                                TrackSelectionOverride(
                                    t.first.mediaTrackGroup,
                                    t.second
                                )
                            )
                            .build()
                }
            }
            .show()
    }

    private fun sleepDialog() {
        val names = arrayOf(
            "Off",
            "15 minutes",
            "30 minutes",
            "45 minutes",
            "60 minutes",
            "90 minutes"
        )

        val mins = intArrayOf(
            0,
            15,
            30,
            45,
            60,
            90
        )

        AlertDialog.Builder(this)
            .setTitle("Sleep timer")
            .setItems(names) { _, w ->
                h.removeCallbacks(
                    sleepRun
                )

                if (mins[w] > 0) {
                    h.postDelayed(
                        sleepRun,
                        mins[w] * 60000L
                    )

                    say(
                        "Sleep in " +
                            mins[w] +
                            " min"
                    )
                } else {
                    say(
                        "Sleep timer off"
                    )
                }
            }
            .show()
    }

    private fun infoDialog() {
        val f = player.videoFormat
        val a = player.audioFormat
        val vs = player.videoSize

        val br =
            if (
                f != null &&
                f.bitrate > 0
            ) {
                (f.bitrate / 1000)
                    .toString() +
                    " kbps"
            } else {
                "unknown"
            }

        val msg =
            "Name: " +
                title.text +
                "\nDuration: " +
                fmt(
                    player.duration
                        .coerceAtLeast(0)
                ) +
                "\nResolution: " +
                vs.width +
                " x " +
                vs.height +
                "\nVideo: " +
                (
                    f?.sampleMimeType
                        ?: "unknown"
                    ) +
                "\nBitrate: " +
                br +
                "\nAudio: " +
                (
                    a?.sampleMimeType
                        ?: "unknown"
                    )

        AlertDialog.Builder(this)
            .setTitle("Video info")
            .setMessage(msg)
            .setPositiveButton(
                "OK",
                null
            )
            .show()
    }

    @Suppress("DEPRECATION")
    private fun pickSubtitle() {
        val i =
            Intent(
                Intent.ACTION_OPEN_DOCUMENT
            )

        i.addCategory(
            Intent.CATEGORY_OPENABLE
        )

        i.type = "*/*"

        startActivityForResult(
            i,
            7
        )
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(
        req: Int,
        res: Int,
        data: Intent?
    ) {
        super.onActivityResult(
            req,
            res,
            data
        )

        val u = data?.data

        if (
            req == 7 &&
            res == RESULT_OK &&
            u != null
        ) {
            addSubtitle(u)
        }
    }

    private fun addSubtitle(
        u: Uri
    ) {
        val n =
            nameOf(u).lowercase()

        val mime =
            when {
                n.endsWith(".vtt") ->
                    MimeTypes.TEXT_VTT

                n.endsWith(".ass") ||
                    n.endsWith(".ssa") ->
                    MimeTypes.TEXT_SSA

                else ->
                    MimeTypes.APPLICATION_SUBRIP
            }

        val cur =
            player.currentMediaItem
                ?: return

        val sub =
            MediaItem.SubtitleConfiguration
                .Builder(u)
                .setMimeType(mime)
                .setLanguage("en")
                .setSelectionFlags(
                    C.SELECTION_FLAG_DEFAULT
                )
                .build()

        val idx =
            player.currentMediaItemIndex

        val pos =
            player.currentPosition

        player.replaceMediaItem(
            idx,
            cur.buildUpon()
                .setSubtitleConfigurations(
                    listOf(sub)
                )
                .build()
        )

        player.seekTo(
            idx,
            pos
        )

        player.play()

        say("Subtitle added")
    }

    private fun enterPip() {
        if (Build.VERSION.SDK_INT < 26) {
            return
        }

        try {
            val b =
                PictureInPictureParams.Builder()

            val vs =
                player.videoSize

            if (
                vs.width > 0 &&
                vs.height > 0
            ) {
                val r =
                    vs.width.toFloat() /
                        vs.height

                if (
                    r in
                    0.42f..2.39f
                ) {
                    b.setAspectRatio(
                        Rational(
                            vs.width,
                            vs.height
                        )
                    )
                }
            }

            enterPictureInPictureMode(
                b.build()
            )
        } catch (e: Exception) {
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()

        if (
            ::player.isInitialized &&
            player.isPlaying
        ) {
            enterPip()
        }
    }

    override fun onPictureInPictureModeChanged(
        inPip: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(
            inPip,
            newConfig
        )

        ui.visibility =
            if (inPip) {
                View.GONE
            } else if (
                uiOn &&
                !locked
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }

        if (inPip) {
            unlockBtn.visibility =
                View.GONE
        }
    }

    private fun savePos() {
        if (released) {
            return
        }

        val u =
            player.currentMediaItem
                ?.localConfiguration
                ?.uri
                ?.toString()
                ?: return

        val d =
            player.duration

        val p =
            player.currentPosition

        if (
            d != C.TIME_UNSET &&
            d > 0 &&
            d - p < 10000
        ) {
            prefs.edit()
                .remove(u)
                .apply()
        } else {
            prefs.edit()
                .putLong(u, p)
                .apply()
        }
    }

    private fun hideBars() {
        val c =
            WindowInsetsControllerCompat(
                window,
                window.decorView
            )

        c.systemBarsBehavior =
            WindowInsetsControllerCompat
                .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        c.hide(
            WindowInsetsCompat
                .Type.systemBars()
        )
    }

    override fun onWindowFocusChanged(
        f: Boolean
    ) {
        super.onWindowFocusChanged(f)

        if (f) {
            hideBars()
        }
    }

    override fun onStop() {
        super.onStop()

        if (
            ::player.isInitialized &&
            !released
        ) {
            savePos()
            player.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        h.removeCallbacksAndMessages(
            null
        )

        if (
            ::player.isInitialized &&
            !released
        ) {
            savePos()
            released = true
            player.release()
        }
    }
}
