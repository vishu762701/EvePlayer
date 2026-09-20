package com.myplayer.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.text.format.Formatter
import android.util.LruCache
import android.util.Size
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.inputmethod.InputMethodManager
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

private const val MP = ViewGroup.LayoutParams.MATCH_PARENT
private const val WC = ViewGroup.LayoutParams.WRAP_CONTENT

class Vid(
    val id: Long,
    val uri: Uri,
    val name: String,
    val dur: Long,
    val size: Long,
    val date: Long,
    val folder: String,
    val w: Int,
    val h: Int
)

class Cell(
    val type: Int,
    val name: String,
    val vids: List<Vid>
)

class Holder(
    val iv: ImageView,
    val q: TextView,
    val dots: ImageView,
    val prog: View,
    val title: TextView,
    val sub: TextView
)

class MainActivity : Activity() {

    private val all = ArrayList<Vid>()
    private val cells = ArrayList<Cell>()
    private var tab = 0
    private var page = 0
    private var pageName = ""
    private var query = ""
    private var sort = 0
    private var loaded = false
    private var colW = 0

    private val cache = LruCache<String, Bitmap>(150)
    private val pool = Executors.newFixedThreadPool(3)

    private lateinit var back: ImageView
    private lateinit var logo: ImageView
    private lateinit var title: TextView
    private lateinit var nav: LinearLayout
    private lateinit var search: EditText
    private lateinit var grid: GridView
    private lateinit var adapter: CellAdapter
    private lateinit var empty: TextView

    private val navIcons = ArrayList<ImageView>()
    private val navTexts = ArrayList<TextView>()

    private fun dp(v: Int) = Ui.dp(this, v)

    private fun toast(m: String) =
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show()

    private fun perms(): Array<String> =
        if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
        } else if (Build.VERSION.SDK_INT >= 29) {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }

    private fun hasRead() =
        checkSelfPermission(perms()[0]) == PackageManager.PERMISSION_GRANTED

    private val press = View.OnTouchListener { v, e ->
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN ->
                v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(90)

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL ->
                v.animate().scaleX(1f).scaleY(1f).setDuration(120)
        }
        false
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)

        sort = getSharedPreferences("lib", MODE_PRIVATE)
            .getInt("sort", 0)

        val dm = resources.displayMetrics
        val pad = dp(16)
        val gap = dp(14)
        val cols = max(
            2,
            (dm.widthPixels / dm.density / 190f).toInt()
        )

        colW = (
            dm.widthPixels -
                2 * pad -
                (cols - 1) * gap
        ) / cols

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Ui.BG)

        root.addView(buildTop())

        search = EditText(this)
        search.hint = "Search in current list"
        search.setHintTextColor(Ui.GRAY)
        search.setTextColor(Color.WHITE)
        search.isSingleLine = true
        search.background = Ui.shape(
            this,
            0xFF262626.toInt(),
            12
        )
        search.setPadding(
            dp(14),
            dp(10),
            dp(14),
            dp(10)
        )
        search.visibility = View.GONE

        search.addTextChangedListener(
            object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    query = s?.toString() ?: ""
                    rebuild()
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    a: Int,
                    b: Int,
                    c: Int
                ) {}

                override fun onTextChanged(
                    s: CharSequence?,
                    a: Int,
                    b: Int,
                    c: Int
                ) {}
            }
        )

        root.addView(
            search,
            LinearLayout.LayoutParams(
                MP,
                WC
            ).apply {
                setMargins(
                    pad,
                    dp(4),
                    pad,
                    dp(4)
                )
            }
        )

        root.addView(
            buildContent(cols, pad, gap),
            LinearLayout.LayoutParams(
                MP,
                0,
                1f
            )
        )

        root.addView(buildNav())

        setContentView(root)
        header()

        if (hasRead()) {
            load()
        } else {
            requestPermissions(perms(), 10)
        }
    }

    private fun buildTop(): View {
        val bar = LinearLayout(this)
        bar.gravity = Gravity.CENTER_VERTICAL
        bar.setPadding(
            dp(16),
            dp(12),
            dp(4),
            dp(12)
        )

        back = Ui.icon(
            this,
            R.drawable.ic_back,
            11
        )
        back.setOnClickListener { goBack() }
        bar.addView(
            back,
            LinearLayout.LayoutParams(
                dp(48),
                dp(48)
            )
        )

        logo = Ui.icon(
            this,
            R.drawable.ic_logo,
            4
        )
        bar.addView(
            logo,
            LinearLayout.LayoutParams(
                dp(36),
                dp(36)
            ).apply {
                marginEnd = dp(8)
            }
        )

        title = TextView(this)
        title.typeface = Typeface.DEFAULT_BOLD
        title.isSingleLine = true
        title.ellipsize = TextUtils.TruncateAt.END

        bar.addView(
            title,
            LinearLayout.LayoutParams(
                0,
                WC,
                1f
            )
        )

        val s = Ui.icon(
            this,
            R.drawable.ic_search,
            11
        )
        s.setOnClickListener {
            toggleSearch()
        }

        val r = Ui.icon(
            this,
            R.drawable.ic_recent,
            11
        )
        r.setOnClickListener {
            page = 2
            pageName = "Recent"
            rebuild()
            grid.setSelection(0)
        }

        val m = Ui.icon(
            this,
            R.drawable.ic_more_v,
            11
        )
        m.setOnClickListener {
            moreMenu()
        }

        bar.addView(
            s,
            LinearLayout.LayoutParams(
                dp(48),
                dp(48)
            )
        )
        bar.addView(
            r,
            LinearLayout.LayoutParams(
                dp(48),
                dp(48)
            )
        )
        bar.addView(
            m,
            LinearLayout.LayoutParams(
                dp(48),
                dp(48)
            )
        )

        return bar
    }

    private fun buildContent(
        cols: Int,
        pad: Int,
        gap: Int
    ): View {
        val fl = FrameLayout(this)

        grid = GridView(this)
        grid.numColumns = cols
        grid.columnWidth = colW
        grid.stretchMode = GridView.NO_STRETCH
        grid.horizontalSpacing = gap
        grid.verticalSpacing = dp(14)
        grid.gravity = Gravity.CENTER_HORIZONTAL
        grid.setPadding(
            pad,
            dp(8),
            pad,
            dp(96)
        )
        grid.clipToPadding = false
        grid.selector = ColorDrawable(Color.TRANSPARENT)
        grid.overScrollMode = View.OVER_SCROLL_NEVER

        adapter = CellAdapter()
        grid.adapter = adapter

        fl.addView(
            grid,
            FrameLayout.LayoutParams(MP, MP)
        )

        empty = TextView(this)
        empty.setTextColor(Ui.GRAY)
        empty.textSize = 15f
        empty.gravity = Gravity.CENTER

        fl.addView(
            empty,
            FrameLayout.LayoutParams(
                WC,
                WC,
                Gravity.CENTER
            )
        )

        val fab = Ui.icon(
            this,
            R.drawable.ic_play,
            18
        )
        fab.background = Ui.circle(Ui.RED)
        fab.elevation = dp(6).toFloat()
        fab.setOnClickListener {
            playAll()
        }

        fl.addView(
            fab,
            FrameLayout.LayoutParams(
                dp(60),
                dp(60),
                Gravity.BOTTOM or Gravity.END
            ).apply {
                setMargins(
                    0,
                    0,
                    dp(18),
                    dp(18)
                )
            }
        )

        return fl
    }

    private fun buildNav(): View {
        nav = LinearLayout(this)
        nav.setBackgroundColor(
            0xFF141414.toInt()
        )
        nav.setPadding(
            0,
            dp(6),
            0,
            dp(6)
        )

        val res = intArrayOf(
            R.drawable.ic_video,
            R.drawable.ic_audio,
            R.drawable.ic_folder,
            R.drawable.ic_playlist,
            R.drawable.ic_more_h
        )

        val names = arrayOf(
            "Video",
            "Audio",
            "Browse",
            "Playlists",
            "More"
        )

        for (i in res.indices) {
            val col = LinearLayout(this)
            col.orientation = LinearLayout.VERTICAL
            col.gravity = Gravity.CENTER_HORIZONTAL

            val ic = Ui.icon(
                this,
                res[i],
                4
            )

            val t = TextView(this)
            t.text = names[i]
            t.textSize = 11f

            col.addView(
                ic,
                LinearLayout.LayoutParams(
                    dp(28),
                    dp(28)
                )
            )
            col.addView(t)

            col.setOnClickListener {
                navTap(i)
            }

            navIcons.add(ic)
            navTexts.add(t)

            nav.addView(
                col,
                LinearLayout.LayoutParams(
                    0,
                    WC,
                    1f
                )
            )
        }

        return nav
    }

    private fun header() {
        val home = page == 0

        back.visibility =
            if (home) View.GONE else View.VISIBLE

        logo.visibility =
            if (home) View.VISIBLE else View.GONE

        nav.visibility =
            if (home) View.VISIBLE else View.GONE

        if (home) {
            title.text = Ui.wordmark()
            title.textSize = 24f
            title.setTextColor(Color.WHITE)
        } else {
            title.text = pageName
            title.textSize = 20f
            title.setTextColor(Color.WHITE)
        }

        for (i in navIcons.indices) {
            val c =
                if (i == tab) Ui.RED
                else Ui.GRAY

            navIcons[i].setColorFilter(c)
            navTexts[i].setTextColor(c)
        }
    }

    private fun navTap(i: Int) {
        if (i == 4) {
            moreMenu()
            return
        }

        if (
            i == 2 &&
            tab == 2 &&
            page == 0
        ) {
            openFiles()
            return
        }

        setTab(i)
    }

    private fun setTab(i: Int) {
        tab = i
        page = 0
        rebuild()
        grid.setSelection(0)
    }

    private fun goBack() {
        page = 0
        rebuild()
        grid.setSelection(0)
    }

    private fun load() {
        Thread {
            val l = ArrayList<Vid>()

            try {
                val vUri =
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI

                val vCols = arrayOf(
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.DURATION,
                    MediaStore.Video.Media.SIZE,
                    MediaStore.Video.Media.DATE_ADDED,
                    MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                    MediaStore.Video.Media.WIDTH,
                    MediaStore.Video.Media.HEIGHT
                )

                contentResolver.query(
                    vUri,
                    vCols,
                    null,
                    null,
                    MediaStore.Video.Media.DATE_ADDED +
                        " DESC"
                )?.use { c ->
                    while (c.moveToNext()) {
                        val id = c.getLong(0)

                        l.add(
                            Vid(
                                id,
                                ContentUris.withAppendedId(
                                    vUri,
                                    id
                                ),
                                c.getString(1) ?: "Video",
                                c.getLong(2),
                                c.getLong(3),
                                c.getLong(4),
                                c.getString(5) ?: "Unknown",
                                c.getInt(6),
                                c.getInt(7)
                            )
                        )
                    }
                }

                val aUri =
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

                val aCols = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.SIZE,
                    MediaStore.Audio.Media.DATE_ADDED,
                    MediaStore.Audio.Media.ALBUM
                )

                contentResolver.query(
                    aUri,
                    aCols,
                    null,
                    null,
                    MediaStore.Audio.Media.DATE_ADDED +
                        " DESC"
                )?.use { c ->
                    while (c.moveToNext()) {
                        val id = c.getLong(0)

                        l.add(
                            Vid(
                                id,
                                ContentUris.withAppendedId(
                                    aUri,
                                    id
                                ),
                                c.getString(1) ?: "Audio",
                                c.getLong(2),
                                c.getLong(3),
                                c.getLong(4),
                                c.getString(5) ?: "Music",
                                0,
                                0
                            )
                        )
                    }
                }
            } catch (e: Exception) {
            }

            runOnUiThread {
                all.clear()
                all.addAll(l)
                loaded = true
                rebuild()
            }
        }.start()
    }

    @Suppress("DEPRECATION")
    private fun rawThumb(v: Vid): Bitmap? =
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                contentResolver.loadThumbnail(
                    v.uri,
                    Size(360, 225),
                    null
                )
            } else if (v.w > 0) {
                MediaStore.Video.Thumbnails.getThumbnail(
                    contentResolver,
                    v.id,
                    MediaStore.Video.Thumbnails.MINI_KIND,
                    null
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }

    private fun thumb(v: Vid): Bitmap? {
        val k = "v" + v.id
        val hit = cache.get(k)

        if (hit != null) return hit

        val bm = rawThumb(v)

        if (bm != null) {
            cache.put(k, bm)
        }

        return bm
    }

    private fun collage(l: List<Vid>): Bitmap? {
        if (l.isEmpty()) return null

        val bm = Bitmap.createBitmap(
            360,
            225,
            Bitmap.Config.ARGB_8888
        )

        val cv = Canvas(bm)
        cv.drawColor(Ui.BG)

        if (l.size == 1) {
            val t = thumb(l[0])

            if (t != null) {
                cv.drawBitmap(
                    t,
                    null,
                    Rect(0, 0, 360, 225),
                    null
                )
            }

            return bm
        }

        for (i in 0 until min(4, l.size)) {
            val t = thumb(l[i]) ?: continue

            val x = (i % 2) * 180
            val y = (i / 2) * 112

            cv.drawBitmap(
                t,
                null,
                Rect(
                    x,
                    y,
                    x + 180,
                    y + 112
                ),
                null
            )
        }

        return bm
    }

    private fun loadCell(
        c: Cell,
        iv: ImageView
    ) {
        val key =
            if (c.type == 0)
                "v" + c.vids[0].id
            else
                "c" + c.type + c.name + c.vids.size

        iv.tag = key

        val hit = cache.get(key)

        if (hit != null) {
            iv.setImageBitmap(hit)
            return
        }

        iv.setImageDrawable(null)

        pool.execute {
            val bm =
                if (c.type == 0)
                    thumb(c.vids[0])
                else
                    collage(c.vids)

            if (bm != null) {
                cache.put(key, bm)

                runOnUiThread {
                    if (iv.tag == key) {
                        iv.setImageBitmap(bm)
                        iv.alpha = 0f
                        iv.animate()
                            .alpha(1f)
                            .setDuration(200)
                    }
                }
            }
        }
    }

    private fun sorted(
        l: List<Vid>
    ): List<Vid> =
        when (sort) {
            0 -> l.sortedByDescending { it.date }
            1 -> l.sortedBy { it.date }
            2 -> l.sortedBy {
                it.name.lowercase()
            }
            3 -> l.sortedByDescending {
                it.name.lowercase()
            }
            4 -> l.sortedByDescending {
                it.size
            }
            5 -> l.sortedBy {
                it.size
            }
            6 -> l.sortedByDescending {
                it.dur
            }
            else -> l.sortedBy {
                it.dur
            }
        }

    private fun byUris(
        l: List<String>
    ): List<Vid> {
        val m = all.associateBy {
            it.uri.toString()
        }

        return l.mapNotNull {
            m[it]
        }
    }

    private fun noExt(s: String) =
        s.substringBeforeLast('.')

    private fun rebuild() {
        val q = query.trim()
        cells.clear()

        if (page == 0) {
            when (tab) {
                0 -> {
                    val vids = sorted(
                        all.filter { it.w > 0 }
                    )

                    for (v in vids) {
                        if (
                            q.isEmpty() ||
                            v.name.contains(q, true)
                        ) {
                            cells.add(
                                Cell(
                                    0,
                                    noExt(v.name),
                                    listOf(v)
                                )
                            )
                        }
                    }
                }

                1 -> {
                    val auds = sorted(
                        all.filter { it.w == 0 }
                    )

                    for (a in auds) {
                        if (
                            q.isEmpty() ||
                            a.name.contains(q, true)
                        ) {
                            cells.add(
                                Cell(
                                    0,
                                    noExt(a.name),
                                    listOf(a)
                                )
                            )
                        }
                    }
                }

                2 -> {
                    val g = all
                        .groupBy { it.folder }
                        .toSortedMap(
                            String.CASE_INSENSITIVE_ORDER
                        )

                    for ((k, l) in g) {
                        if (
                            q.isEmpty() ||
                            k.contains(q, true)
                        ) {
                            cells.add(
                                Cell(
                                    1,
                                    k,
                                    sorted(l)
                                )
                            )
                        }
                    }
                }

                3 -> {
                    for (
                        n in Lib.playlists(this)
                    ) {
                        if (
                            q.isEmpty() ||
                            n.contains(q, true)
                        ) {
                            cells.add(
                                Cell(
                                    2,
                                    n,
                                    byUris(
                                        Lib.items(
                                            this,
                                            n
                                        )
                                    )
                                )
                            )
                        }
                    }
                }
            }
        } else {
            var l: List<Vid> =
                when (page) {
                    1 -> sorted(
                        all.filter {
                            it.folder == pageName
                        }
                    )

                    2 -> byUris(
                        Lib.recent(this)
                    )

                    else -> byUris(
                        Lib.items(
                            this,
                            pageName
                        )
                    )
                }

            if (q.isNotEmpty()) {
                l = l.filter {
                    it.name.contains(q, true)
                }
            }

            for (v in l) {
                cells.add(
                    Cell(
                        0,
                        noExt(v.name),
                        listOf(v)
                    )
                )
            }
        }

        adapter.notifyDataSetChanged()

        empty.text =
            if (!loaded) {
                "Loading media..."
            } else if (
                !hasRead() &&
                all.isEmpty()
            ) {
                "Allow storage access to scan media"
            } else if (cells.isEmpty()) {
                "No media found"
            } else {
                ""
            }

        header()
    }

    private fun toggleSearch() {
        val imm =
            getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager

        if (search.visibility == View.VISIBLE) {
            search.setText("")
            search.visibility = View.GONE

            imm.hideSoftInputFromWindow(
                search.windowToken,
                0
            )
        } else {
            search.visibility = View.VISIBLE
            search.requestFocus()

            imm.showSoftInput(
                search,
                0
            )
        }
    }

    private val sortNames = arrayOf(
        "Newest first",
        "Oldest first",
        "Name A-Z",
        "Name Z-A",
        "Largest size",
        "Smallest size",
        "Longest",
        "Shortest"
    )

    private fun sortDialog() {
        AlertDialog.Builder(this)
            .setTitle("Sort by")
            .setSingleChoiceItems(
                sortNames,
                sort
            ) { d, w ->
                sort = w

                getSharedPreferences(
                    "lib",
                    MODE_PRIVATE
                ).edit()
                    .putInt("sort", w)
                    .apply()

                d.dismiss()
                rebuild()
            }
            .show()
    }

    private fun moreMenu() {
        val items = arrayOf(
            "Sort by",
            "New playlist",
            "Refresh"
        )

        AlertDialog.Builder(this)
            .setItems(items) { _, w ->
                when (w) {
                    0 -> sortDialog()
                    1 -> newPlaylist(null)
                    else -> load()
                }
            }
            .show()
    }

    private fun newPlaylist(v: Vid?) {
        val et = EditText(this)

        AlertDialog.Builder(this)
            .setTitle("New playlist")
            .setView(et)
            .setPositiveButton("Create") { _, _ ->
                val n =
                    et.text.toString().trim()

                if (n.isNotEmpty()) {
                    if (v != null) {
                        Lib.add(
                            this,
                            n,
                            v.uri.toString()
                        )
                    } else {
                        Lib.create(this, n)
                    }

                    toast("Saved")
                    rebuild()
                }
            }
            .setNegativeButton(
                "Cancel",
                null
            )
            .show()
    }

    private fun openCell(c: Cell) {
        if (c.type == 0) {
            playFrom(c.vids[0])
            return
        }

        page =
            if (c.type == 1) 1
            else 3

        pageName = c.name
        rebuild()
        grid.setSelection(0)
    }

    private fun cellMenu(c: Cell) {
        if (c.type == 0) {
            videoMenu(c.vids[0])
            return
        }

        val items = ArrayList<String>()
        items.add("Play all")

        if (
            c.type == 2 &&
            c.name != "Favorites"
        ) {
            items.add("Delete playlist")
        }

        AlertDialog.Builder(this)
            .setTitle(c.name)
            .setItems(items.toTypedArray()) { _, w ->
                if (w == 0) {
                    if (c.vids.isNotEmpty()) {
                        startPlayer(
                            c.vids,
                            0
                        )
                    }
                } else {
                    Lib.deletePlaylist(
                        this,
                        c.name
                    )
                    rebuild()
                }
            }
            .show()
    }

    private fun videoMenu(v: Vid) {
        val items =
            arrayListOf(
                "Play",
                "Add to playlist"
            )

        if (page == 3) {
            items.add(
                "Remove from playlist"
            )
        }

        items.addAll(
            listOf(
                "Share",
                "Rename",
                "Delete",
                "Info"
            )
        )

        AlertDialog.Builder(this)
            .setTitle(noExt(v.name))
            .setItems(
                items.toTypedArray()
            ) { _, w ->
                when (items[w]) {
                    "Play" -> playFrom(v)
                    "Add to playlist" ->
                        addToPlaylist(v)

                    "Remove from playlist" -> {
                        Lib.remove(
                            this,
                            pageName,
                            v.uri.toString()
                        )
                        rebuild()
                    }

                    "Share" -> share(v)
                    "Rename" -> askRename(v)
                    "Delete" -> askDelete(v)
                    "Info" -> infoDialog(v)
                }
            }
            .show()
    }

    private fun addToPlaylist(v: Vid) {
        val names = Lib.playlists(this)

        val items =
            (names + "+ New playlist")
                .toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Add to playlist")
            .setItems(items) { _, w ->
                if (w < names.size) {
                    Lib.add(
                        this,
                        names[w],
                        v.uri.toString()
                    )
                    toast("Added")
                } else {
                    newPlaylist(v)
                }
            }
            .show()
    }

    private fun playFrom(v: Vid) {
        val vids = cells
            .filter { it.type == 0 }
            .map { it.vids[0] }

        val i = vids.indexOfFirst {
            it.id == v.id
        }

        if (i >= 0) {
            startPlayer(vids, i)
        }
    }

    private fun playAll() {
        val l =
            if (
                cells.isNotEmpty() &&
                cells[0].type == 0
            ) {
                cells.map { it.vids[0] }
            } else {
                sorted(all)
            }

        if (l.isEmpty()) {
            toast("No media")
        } else {
            startPlayer(l, 0)
        }
    }

    private fun startPlayer(
        vids: List<Vid>,
        index: Int
    ) {
        val from =
            max(0, index - 50)

        val to =
            min(
                vids.size,
                index + 250
            )

        val l =
            ArrayList<String>()

        for (i in from until to) {
            l.add(
                vids[i].uri.toString()
            )
        }

        startUris(
            l,
            index - from
        )
    }

    private fun startUris(
        l: ArrayList<String>,
        index: Int
    ) {
        val n =
            Intent(
                this,
                PlayerActivity::class.java
            )

        n.putStringArrayListExtra(
            "uris",
            l
        )

        n.putExtra(
            "index",
            index
        )

        startActivity(n)
    }

    @Suppress("DEPRECATION")
    private fun openFiles() {
        val i =
            Intent(Intent.ACTION_OPEN_DOCUMENT)

        i.addCategory(
            Intent.CATEGORY_OPENABLE
        )

        i.type = "*/*"

        i.putExtra(
            Intent.EXTRA_MIME_TYPES,
            arrayOf(
                "video/*",
                "audio/*"
            )
        )

        i.putExtra(
            Intent.EXTRA_ALLOW_MULTIPLE,
            true
        )

        i.addFlags(
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        )

        startActivityForResult(i, 1)
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

        if (
            req == 1 &&
            res == Activity.RESULT_OK &&
            data != null
        ) {
            val l =
                ArrayList<String>()

            val c = data.clipData

            if (c != null) {
                for (
                    i in 0 until c.itemCount
                ) {
                    l.add(
                        c.getItemAt(i)
                            .uri
                            .toString()
                    )
                }
            } else {
                data.data?.let {
                    l.add(
                        it.toString()
                    )
                }
            }

            if (l.isNotEmpty()) {
                startUris(l, 0)
            }
        } else if (
            (req == 2 || req == 3) &&
            res == Activity.RESULT_OK
        ) {
            load()
        }
    }

    private fun share(v: Vid) {
        val i =
            Intent(Intent.ACTION_SEND)

        i.type =
            if (v.w > 0)
                "video/*"
            else
                "audio/*"

        i.putExtra(
            Intent.EXTRA_STREAM,
            v.uri
        )

        startActivity(
            Intent.createChooser(
                i,
                "Share media"
            )
        )
    }

    private fun askDelete(v: Vid) {
        AlertDialog.Builder(this)
            .setTitle(
                "Delete ${noExt(v.name)}"
            )
            .setMessage(
                "Are you sure? This cannot be undone."
            )
            .setPositiveButton(
                "Delete"
            ) { _, _ ->
                deleteFile(v)
            }
            .setNegativeButton(
                "Cancel",
                null
            )
            .show()
    }

    @Suppress("DEPRECATION")
    private fun deleteFile(v: Vid) {
        try {
            contentResolver.delete(
                v.uri,
                null,
                null
            )

            toast("Deleted")
            load()
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= 29) {
                val r =
                    e as? android.app.RecoverableSecurityException

                if (r != null) {
                    startIntentSenderForResult(
                        r.userAction
                            .actionIntent
                            .intentSender,
                        3,
                        null,
                        0,
                        0,
                        0
                    )
                }
            }
        } catch (e: Exception) {
            toast("Failed")
        }
    }

    private fun askRename(v: Vid) {
        val et = EditText(this)

        et.setText(
            noExt(v.name)
        )

        AlertDialog.Builder(this)
            .setTitle("Rename")
            .setView(et)
            .setPositiveButton(
                "Rename"
            ) { _, _ ->
                renameFile(
                    v,
                    et.text.toString().trim()
                )
            }
            .setNegativeButton(
                "Cancel",
                null
            )
            .show()
    }

    private fun renameFile(
        v: Vid,
        nn: String
    ) {
        if (
            nn.isEmpty() ||
            nn == noExt(v.name)
        ) {
            return
        }

        try {
            val ext =
                v.name.substringAfterLast(
                    '.',
                    ""
                )

            val full =
                if (ext.isNotEmpty())
                    "$nn.$ext"
                else
                    nn

            val cv =
                android.content.ContentValues()

            cv.put(
                MediaStore.MediaColumns.DISPLAY_NAME,
                full
            )

            if (Build.VERSION.SDK_INT >= 29) {
                cv.put(
                    MediaStore.MediaColumns.IS_PENDING,
                    1
                )
            }

            contentResolver.update(
                v.uri,
                cv,
                null,
                null
            )

            if (Build.VERSION.SDK_INT >= 29) {
                cv.clear()

                cv.put(
                    MediaStore.MediaColumns.IS_PENDING,
                    0
                )

                contentResolver.update(
                    v.uri,
                    cv,
                    null,
                    null
                )
            }

            toast("Renamed")
            load()
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= 29) {
                val r =
                    e as? android.app.RecoverableSecurityException

                if (r != null) {
                    startIntentSenderForResult(
                        r.userAction
                            .actionIntent
                            .intentSender,
                        2,
                        null,
                        0,
                        0,
                        0
                    )
                }
            }
        } catch (e: Exception) {
            toast("Failed")
        }
    }

    private fun fmt(ms: Long): String {
        val s = ms / 1000
        val hh = s / 3600
        val m = (s % 3600) / 60
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

    private fun sz(b: Long): String =
        Formatter.formatFileSize(
            this,
            b
        )

    private fun infoDialog(v: Vid) {
        val d =
            DateFormat
                .getDateTimeInstance(
                    DateFormat.MEDIUM,
                    DateFormat.SHORT
                )
                .format(
                    Date(v.date * 1000)
                )

        val msg =
            "Name: ${v.name}\n" +
            "Folder: ${v.folder}\n" +
            "Duration: ${fmt(v.dur)}\n" +
            "Size: ${sz(v.size)}\n" +
            "Date: $d" +
            if (v.w > 0) {
                "\nResolution: ${v.w}x${v.h}"
            } else {
                ""
            }

        AlertDialog.Builder(this)
            .setTitle("Information")
            .setMessage(msg)
            .setPositiveButton(
                "OK",
                null
            )
            .show()
    }

    override fun onBackPressed() {
        if (
            search.visibility ==
            View.VISIBLE
        ) {
            toggleSearch()
        } else if (page != 0) {
            goBack()
        } else {
            super.onBackPressed()
        }
    }

    inner class CellAdapter : BaseAdapter() {

        override fun getCount() =
            cells.size

        override fun getItem(p: Int) =
            cells[p]

        override fun getItemId(p: Int) =
            p.toLong()

        override fun getView(
            p: Int,
            v: View?,
            parent: ViewGroup?
        ): View {
            val c = cells[p]

            val h: Holder
            val root: LinearLayout

            if (v == null) {
                root =
                    LinearLayout(
                        this@MainActivity
                    )

                root.orientation =
                    LinearLayout.VERTICAL

                val top =
                    FrameLayout(
                        this@MainActivity
                    )

                val iv =
                    ImageView(
                        this@MainActivity
                    )

                iv.scaleType =
                    ImageView.ScaleType.CENTER_CROP

                iv.setBackgroundColor(
                    0xFF262626.toInt()
                )

                iv.outlineProvider =
                    object : ViewOutlineProvider() {
                        override fun getOutline(
                            view: View,
                            outline: android.graphics.Outline
                        ) =
                            outline.setRoundRect(
                                0,
                                0,
                                view.width,
                                view.height,
                                dp(8).toFloat()
                            )
                    }

                iv.clipToOutline = true

                top.addView(
                    iv,
                    FrameLayout.LayoutParams(
                        MP,
                        (colW * 0.62f).toInt()
                    )
                )

                val q =
                    TextView(
                        this@MainActivity
                    )

                q.setTextColor(
                    Color.WHITE
                )

                q.textSize = 12f

                q.background =
                    Ui.shape(
                        this@MainActivity,
                        0x88000000.toInt(),
                        4
                    )

                q.setPadding(
                    dp(6),
                    dp(2),
                    dp(6),
                    dp(2)
                )

                top.addView(
                    q,
                    FrameLayout.LayoutParams(
                        WC,
                        WC,
                        Gravity.BOTTOM or
                            Gravity.START
                    ).apply {
                        setMargins(
                            dp(6),
                            0,
                            0,
                            dp(6)
                        )
                    }
                )

                val prg =
                    View(
                        this@MainActivity
                    )

                prg.setBackgroundColor(
                    Ui.RED
                )

                top.addView(
                    prg,
                    FrameLayout.LayoutParams(
                        0,
                        dp(3),
                        Gravity.BOTTOM
                    ).apply {
                        setMargins(
                            dp(4),
                            0,
                            dp(4),
                            dp(3)
                        )
                    }
                )

                root.addView(top)

                val bot =
                    LinearLayout(
                        this@MainActivity
                    )

                bot.gravity =
                    Gravity.CENTER_VERTICAL

                val txt =
                    LinearLayout(
                        this@MainActivity
                    )

                txt.orientation =
                    LinearLayout.VERTICAL

                val title =
                    TextView(
                        this@MainActivity
                    )

                title.setTextColor(
                    Color.WHITE
                )

                title.textSize = 14f
                title.isSingleLine = true
                title.ellipsize =
                    TextUtils.TruncateAt.END

                val sub =
                    TextView(
                        this@MainActivity
                    )

                sub.setTextColor(
                    Ui.GRAY
                )

                sub.textSize = 12f
                sub.isSingleLine = true

                txt.addView(title)
                txt.addView(sub)

                bot.addView(
                    txt,
                    LinearLayout.LayoutParams(
                        0,
                        WC,
                        1f
                    )
                )

                val d =
                    Ui.icon(
                        this@MainActivity,
                        R.drawable.ic_more_v,
                        8,
                        Ui.GRAY
                    )

                bot.addView(
                    d,
                    LinearLayout.LayoutParams(
                        dp(32),
                        dp(32)
                    )
                )

                root.addView(
                    bot,
                    LinearLayout.LayoutParams(
                        MP,
                        WC
                    ).apply {
                        topMargin = dp(6)
                    }
                )

                h = Holder(
                    iv,
                    q,
                    d,
                    prg,
                    title,
                    sub
                )

                root.tag = h
            } else {
                root =
                    v as LinearLayout

                h = root.tag as Holder
            }

            h.title.text = c.name
            h.iv.setImageResource(0)

            if (c.type == 0) {
                val vid = c.vids[0]

                h.sub.text =
                    sz(vid.size)

                h.q.text =
                    fmt(vid.dur)

                h.q.visibility =
                    View.VISIBLE

                val played =
                    getSharedPreferences(
                        "prefs",
                        MODE_PRIVATE
                    ).getLong(
                        vid.uri.toString(),
                        0
                    )

                if (
                    played > 0 &&
                    vid.dur > 0
                ) {
                    val w =
                        (
                            played.toFloat() /
                                vid.dur *
                                colW
                        ).toInt()

                    h.prog.layoutParams =
                        FrameLayout.LayoutParams(
                            w,
                            dp(3),
                            Gravity.BOTTOM
                        )

                    h.prog.visibility =
                        View.VISIBLE
                } else {
                    h.prog.visibility =
                        View.GONE
                }
            } else {
                h.sub.text =
                    c.vids.size.toString() +
                        if (c.type == 1)
                            " media"
                        else
                            " items"

                h.q.visibility =
                    View.GONE

                h.prog.visibility =
                    View.GONE
            }

            loadCell(
                c,
                h.iv
            )

            root.setOnTouchListener(
                press
            )

            root.setOnClickListener {
                openCell(c)
            }

            h.dots.setOnClickListener {
                cellMenu(c)
            }

            return root
        }
    }
}
