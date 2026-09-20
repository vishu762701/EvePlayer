package com.example.ui.library

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.local.PlaylistEntity
import com.example.data.mediastore.MediaFolder
import com.example.data.mediastore.MediaItemModel
import com.example.player.MediaPlaybackManager
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.FolderCard
import com.example.ui.components.MediaItemGridCard
import com.example.ui.components.MediaItemListRow
import com.example.ui.components.MiniPlayer
import com.example.ui.components.SortBottomSheet
import com.example.ui.theme.EveRedPrimary
import kotlinx.coroutines.launch

enum class LibraryTab(val title: String) {
    VIDEOS("Videos"),
    AUDIO("Audio"),
    FOLDERS("Folders"),
    PLAYLISTS("Playlists")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    playbackManager: MediaPlaybackManager,
    onNavigateToVideoPlayer: (MediaItemModel) -> Unit,
    onNavigateToAudioPlayer: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Permissions check
    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    var hasPermissions by remember {
        mutableStateOf(
            requiredPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermissions = results.values.any { it }
        if (hasPermissions) {
            viewModel.refreshLibrary()
        }
    }

    LaunchedEffect(hasPermissions) {
        if (hasPermissions) {
            viewModel.refreshLibrary()
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var isSearchActive by remember { mutableStateOf(false) }
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()

    val videos by viewModel.videos.collectAsState()
    val audios by viewModel.audios.collectAsState()
    val videoFolders by viewModel.videoFolders.collectAsState()
    val audioFolders by viewModel.audioFolders.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    // Player states
    val currentMedia by playbackManager.currentMediaItem.collectAsState()
    val isPlaying by playbackManager.isPlaying.collectAsState()
    val currentPosition by playbackManager.currentPosition.collectAsState()
    val duration by playbackManager.duration.collectAsState()

    // Dialog & Sheet states
    var showSortSheet by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var mediaItemForPlaylist by remember { mutableStateOf<MediaItemModel?>(null) }
    var activeFolderView by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // folderName to isVideo
    var folderItems by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        if (!isSearchActive) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_eve_logo),
                                    contentDescription = "eve logo",
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black)
                                        .padding(2.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "eve",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        } else {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = { Text("Search media...") },
                                singleLine = true,
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EveRedPrimary,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) viewModel.setSearchQuery("")
                        }) {
                            Icon(
                                imageVector = if (isSearchActive) Icons.Default.Clear else Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        }

                        if (selectedTab == 0 || selectedTab == 1) {
                            val isGrid = if (selectedTab == 0) userSettings.isVideoGridView else userSettings.isAudioGridView
                            IconButton(onClick = {
                                if (selectedTab == 0) viewModel.toggleVideoGridView() else viewModel.toggleAudioGridView()
                            }) {
                                Icon(
                                    imageVector = if (isGrid) Icons.Default.ViewList else Icons.Default.GridView,
                                    contentDescription = "Toggle Grid/List"
                                )
                            }

                            IconButton(onClick = { showSortSheet = true }) {
                                Icon(Icons.Default.Sort, contentDescription = "Sort")
                            }
                        }

                        IconButton(onClick = { viewModel.refreshLibrary() }) {
                            if (isScanning) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color = EveRedPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                            }
                        }

                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )

                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = EveRedPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = EveRedPrimary,
                            height = 3.dp
                        )
                    }
                ) {
                    LibraryTab.entries.forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = {
                                selectedTab = index
                                activeFolderView = null
                            },
                            text = {
                                Text(
                                    tab.title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            selectedContentColor = EveRedPrimary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        bottomBar = {
            Column {
                MiniPlayer(
                    currentMedia = currentMedia,
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    duration = duration,
                    onTogglePlayPause = { playbackManager.togglePlayPause() },
                    onPlayNext = { playbackManager.playNext() },
                    onClick = {
                        if (currentMedia?.isVideo == true) {
                            currentMedia?.let { onNavigateToVideoPlayer(it) }
                        } else {
                            onNavigateToAudioPlayer()
                        }
                    }
                )

                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    LibraryTab.entries.forEachIndexed { index, tab ->
                        val icon = when (tab) {
                            LibraryTab.VIDEOS -> Icons.Default.Movie
                            LibraryTab.AUDIO -> Icons.Default.Audiotrack
                            LibraryTab.FOLDERS -> Icons.Default.Folder
                            LibraryTab.PLAYLISTS -> Icons.Default.PlaylistPlay
                        }
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = {
                                selectedTab = index
                                activeFolderView = null
                            },
                            icon = { Icon(icon, contentDescription = tab.title) },
                            label = { Text(tab.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = EveRedPrimary,
                                selectedTextColor = EveRedPrimary,
                                indicatorColor = EveRedPrimary.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!hasPermissions) {
                // Permission Request State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = EveRedPrimary.copy(alpha = 0.15f),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = EveRedPrimary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        "Permission Required",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "eve needs permission to scan your device storage for offline video and audio files.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { permissionLauncher.launch(requiredPermissions) },
                        colors = ButtonDefaults.buttonColors(containerColor = EveRedPrimary)
                    ) {
                        Text("Grant Storage Permission")
                    }
                }
            } else {
                // Main Content
                when (LibraryTab.entries[selectedTab]) {
                    LibraryTab.VIDEOS -> {
                        VideosTabContent(
                            videos = videos,
                            isGridView = userSettings.isVideoGridView,
                            onPlayVideo = { item ->
                                playbackManager.playMedia(item, newQueue = videos)
                                onNavigateToVideoPlayer(item)
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onAddToPlaylist = { mediaItemForPlaylist = it },
                            onAddToQueue = { playbackManager.addToQueueNext(it) }
                        )
                    }
                    LibraryTab.AUDIO -> {
                        AudioTabContent(
                            audios = audios,
                            isGridView = userSettings.isAudioGridView,
                            onPlayAudio = { item ->
                                playbackManager.playMedia(item, newQueue = audios)
                                onNavigateToAudioPlayer()
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onAddToPlaylist = { mediaItemForPlaylist = it },
                            onAddToQueue = { playbackManager.addToQueueNext(it) }
                        )
                    }
                    LibraryTab.FOLDERS -> {
                        if (activeFolderView != null) {
                            val (fName, isVid) = activeFolderView!!
                            FolderDetailView(
                                folderName = fName,
                                isVideo = isVid,
                                items = folderItems,
                                onBack = { activeFolderView = null },
                                onPlay = { item ->
                                    playbackManager.playMedia(item, newQueue = folderItems)
                                    if (item.isVideo) {
                                        onNavigateToVideoPlayer(item)
                                    } else {
                                        onNavigateToAudioPlayer()
                                    }
                                },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onAddToPlaylist = { mediaItemForPlaylist = it },
                                onAddToQueue = { playbackManager.addToQueueNext(it) }
                            )
                        } else {
                            FoldersTabContent(
                                videoFolders = videoFolders,
                                audioFolders = audioFolders,
                                onSelectFolder = { folder ->
                                    coroutineScope.launch {
                                        folderItems = viewModel.getMediaInFolder(folder.folderName, folder.isVideo)
                                        activeFolderView = folder.folderName to folder.isVideo
                                    }
                                }
                            )
                        }
                    }
                    LibraryTab.PLAYLISTS -> {
                        PlaylistsTabContent(
                            playlists = playlists,
                            onCreatePlaylist = { showCreatePlaylistDialog = true },
                            onDeletePlaylist = { viewModel.deletePlaylist(it) },
                            onPlayPlaylist = { p ->
                                // play playlist
                            }
                        )
                    }
                }
            }
        }
    }

    // Sort Bottom Sheet
    if (showSortSheet) {
        val isVideoTab = selectedTab == 0
        val currentSort = if (isVideoTab) viewModel.videoSortBy.collectAsState().value else viewModel.audioSortBy.collectAsState().value
        val currentAsc = if (isVideoTab) viewModel.videoSortAsc.collectAsState().value else viewModel.audioSortAsc.collectAsState().value

        SortBottomSheet(
            currentSortBy = currentSort,
            currentSortAsc = currentAsc,
            onSortSelected = { sort, asc ->
                if (isVideoTab) {
                    viewModel.setVideoSorting(sort, asc)
                } else {
                    viewModel.setAudioSorting(sort, asc)
                }
                showSortSheet = false
            },
            onDismiss = { showSortSheet = false }
        )
    }

    // Create Playlist Dialog
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = {
                viewModel.createPlaylist(it)
                showCreatePlaylistDialog = false
            }
        )
    }

    // Add to Playlist Dialog
    if (mediaItemForPlaylist != null) {
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { mediaItemForPlaylist = null },
            onSelectPlaylist = { pId ->
                mediaItemForPlaylist?.let { viewModel.addMediaToPlaylist(pId, it) }
                mediaItemForPlaylist = null
            },
            onCreateNewPlaylist = {
                showCreatePlaylistDialog = true
            }
        )
    }
}

@Composable
private fun VideosTabContent(
    videos: List<MediaItemModel>,
    isGridView: Boolean,
    onPlayVideo: (MediaItemModel) -> Unit,
    onToggleFavorite: (MediaItemModel) -> Unit,
    onAddToPlaylist: (MediaItemModel) -> Unit,
    onAddToQueue: (MediaItemModel) -> Unit
) {
    if (videos.isEmpty()) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Movie,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "No videos found",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else if (isGridView) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(170.dp),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(videos, key = { it.uri.toString() }) { item ->
                MediaItemGridCard(
                    item = item,
                    onClick = { onPlayVideo(item) },
                    onToggleFavorite = { onToggleFavorite(item) },
                    onAddToPlaylist = { onAddToPlaylist(item) },
                    onAddToQueue = { onAddToQueue(item) }
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(videos, key = { it.uri.toString() }) { item ->
                MediaItemListRow(
                    item = item,
                    onClick = { onPlayVideo(item) },
                    onToggleFavorite = { onToggleFavorite(item) },
                    onAddToPlaylist = { onAddToPlaylist(item) },
                    onAddToQueue = { onAddToQueue(item) }
                )
            }
        }
    }
}

@Composable
private fun AudioTabContent(
    audios: List<MediaItemModel>,
    isGridView: Boolean,
    onPlayAudio: (MediaItemModel) -> Unit,
    onToggleFavorite: (MediaItemModel) -> Unit,
    onAddToPlaylist: (MediaItemModel) -> Unit,
    onAddToQueue: (MediaItemModel) -> Unit
) {
    if (audios.isEmpty()) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Audiotrack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "No audio files found",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else if (isGridView) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(audios, key = { it.uri.toString() }) { item ->
                MediaItemGridCard(
                    item = item,
                    onClick = { onPlayAudio(item) },
                    onToggleFavorite = { onToggleFavorite(item) },
                    onAddToPlaylist = { onAddToPlaylist(item) },
                    onAddToQueue = { onAddToQueue(item) }
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(audios, key = { it.uri.toString() }) { item ->
                MediaItemListRow(
                    item = item,
                    onClick = { onPlayAudio(item) },
                    onToggleFavorite = { onToggleFavorite(item) },
                    onAddToPlaylist = { onAddToPlaylist(item) },
                    onAddToQueue = { onAddToQueue(item) }
                )
            }
        }
    }
}

@Composable
private fun FoldersTabContent(
    videoFolders: List<MediaFolder>,
    audioFolders: List<MediaFolder>,
    onSelectFolder: (MediaFolder) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (videoFolders.isNotEmpty()) {
            item {
                Text(
                    "Video Folders",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            items(videoFolders) { folder ->
                FolderCard(folder = folder, onClick = { onSelectFolder(folder) })
            }
        }

        if (audioFolders.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Audio Folders",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            items(audioFolders) { folder ->
                FolderCard(folder = folder, onClick = { onSelectFolder(folder) })
            }
        }

        if (videoFolders.isEmpty() && audioFolders.isEmpty()) {
            item {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillParentMaxSize()) {
                    Text(
                        "No media folders found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderDetailView(
    folderName: String,
    isVideo: Boolean,
    items: List<MediaItemModel>,
    onBack: () -> Unit,
    onPlay: (MediaItemModel) -> Unit,
    onToggleFavorite: (MediaItemModel) -> Unit,
    onAddToPlaylist: (MediaItemModel) -> Unit,
    onAddToQueue: (MediaItemModel) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    folderName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${items.size} ${if (isVideo) "videos" else "tracks"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items, key = { it.uri.toString() }) { item ->
                MediaItemListRow(
                    item = item,
                    onClick = { onPlay(item) },
                    onToggleFavorite = { onToggleFavorite(item) },
                    onAddToPlaylist = { onAddToPlaylist(item) },
                    onAddToQueue = { onAddToQueue(item) }
                )
            }
        }
    }
}

@Composable
private fun PlaylistsTabContent(
    playlists: List<PlaylistEntity>,
    onCreatePlaylist: () -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onPlayPlaylist: (PlaylistEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = onCreatePlaylist,
            colors = ButtonDefaults.buttonColors(containerColor = EveRedPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create New Playlist")
        }

        if (playlists.isEmpty()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    "No playlists created yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlayPlaylist(playlist) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EveRedPrimary.copy(alpha = 0.15f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.PlaylistPlay,
                                        contentDescription = null,
                                        tint = EveRedPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    playlist.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Playlist",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(onClick = { onDeletePlaylist(playlist.id) }) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = "Delete Playlist",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
