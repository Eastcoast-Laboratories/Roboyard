package roboyard.ui.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import driftingdroids.model.Board
import roboyard.logic.core.Constants
import roboyard.logic.core.GameHistoryEntry
import roboyard.logic.core.GameState
import roboyard.logic.managers.GameHistoryManager
import roboyard.logic.managers.GameSession
import roboyard.logic.managers.ShareMapHelper
import roboyard.logic.network.RoboyardApiClient
import roboyard.logic.platform.openAutoLoginUrl
import roboyard.logic.platform.openUrl
import roboyard.logic.storage.PlatformStorage
import roboyard.logic.storage.getPlatformStorage
import roboyard.logic.ui.getStringProvider
import roboyard.ui.graphics.MinimapGenerator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val ITEMS_PER_PAGE = 20
private const val MAX_SAVE_SLOT = 34

/** Sort options mirroring Android SaveGameFragment.SortOption. */
private enum class SortOption(val stringKey: String, val fallback: String) {
    BY_DATE("sort_by_date", "By date"),
    LAST_SOLVED("sort_last_solved", "Last solved"),
    LONGEST_FIRST_ATTEMPT("sort_longest_first_attempt", "Longest first attempt"),
    OPTIMAL_NOT_FOUND("sort_optimal_not_found", "Optimal not found"),
    UNSOLVED("sort_unsolved", "Unsolved")
}

/** Filter options mirroring Android SaveGameFragment.FilterOption. */
private enum class FilterOption(val stringKey: String, val fallback: String) {
    ALL("filter_all", "All"),
    OPTIMAL_NOT_FOUND("filter_optimal_not_found", "Optimal not found"),
    UNSOLVED("filter_unsolved", "Unsolved")
}

/** Metadata extracted from a save file, mirrors Android SaveGameFragment.SaveSlotInfo. */
private data class SaveSlotInfo(
    val slotId: Int,
    val name: String,
    val dateMillis: Long?,
    val boardSize: String?,
    val difficulty: String?,
    val movesCount: String?,
    val completionStatus: String?,
    val saveData: String?
)

@Composable
fun SaveLoadScreen(
    session: GameSession,
    saveMode: Boolean = false,
    isLevelGame: Boolean = false,
    onBack: () -> Unit = {},
    onLoadGame: (Int) -> Unit = {},
    onLoadHistory: (String) -> Unit = {},
    onProfile: () -> Unit = {}
) {
    val storage = remember { getPlatformStorage() }
    val stringProvider = remember { getStringProvider() }
    val apiClient = remember { RoboyardApiClient.getInstance(storage) }
    val listDateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US) }

    fun s(key: String, fallback: String, vararg args: Any): String =
        stringProvider.getString(key, *args) ?: formatArgs(fallback, *args)

    var selectedTab by remember { mutableStateOf(0) }
    // Android parity: save mode shows [Save|History], load mode shows [Load|History]
    val tabs = listOf(
        if (saveMode) s("save_tab_title", "Save") else s("load_tab_title", "Load"),
        s("history_tab_title", "History")
    )

    var saveSlots by remember { mutableStateOf<List<SaveSlotInfo>>(emptyList()) }
    var allHistoryEntries by remember { mutableStateOf<List<GameHistoryEntry>>(emptyList()) }
    var filteredHistoryEntries by remember { mutableStateOf<List<GameHistoryEntry>>(emptyList()) }
    var currentSort by remember { mutableStateOf(SortOption.BY_DATE) }
    var currentFilter by remember { mutableStateOf(FilterOption.ALL) }
    var currentPage by remember { mutableStateOf(0) }
    var historyReload by remember { mutableStateOf(0) }

    var infoSlot by remember { mutableStateOf<SaveSlotInfo?>(null) }
    var infoEntry by remember { mutableStateOf<GameHistoryEntry?>(null) }
    var shareSlotId by remember { mutableStateOf<Int?>(null) }
    var shareMessage by remember { mutableStateOf<String?>(null) }

    // Derived: level entries are filtered out of history (Android parity)
    fun applyFilterAndSort() {
        val nonLevel = allHistoryEntries.filter { entry ->
            val name = entry.mapName
            name == null || !name.matches(Regex("(?i)^Level\\s+\\d+.*"))
        }
        val filtered = nonLevel.filter { entry ->
            when (currentFilter) {
                FilterOption.ALL -> true
                FilterOption.OPTIMAL_NOT_FOUND -> entry.optimalMoves == 0
                FilterOption.UNSOLVED -> entry.completionCount == 0
            }
        }
        filteredHistoryEntries = when (currentSort) {
            SortOption.BY_DATE -> filtered.sortedByDescending { it.timestamp }
            SortOption.LAST_SOLVED -> filtered.sortedByDescending { it.lastCompletionTimestamp }
            SortOption.LONGEST_FIRST_ATTEMPT -> filtered.sortedByDescending { it.bestTime }
            SortOption.OPTIMAL_NOT_FOUND -> filtered.sortedBy { it.optimalMoves }
            SortOption.UNSOLVED -> filtered.sortedBy { it.completionCount }
        }
    }

    LaunchedEffect(Unit) {
        saveSlots = loadSaveSlots(storage, ::s)
    }
    LaunchedEffect(historyReload) {
        allHistoryEntries = try {
            GameHistoryManager.initialize(storage)
            GameHistoryManager.getHistoryEntries(storage) ?: mutableListOf()
        } catch (e: Exception) {
            println("[SAVE_LOAD_SCREEN] Error loading history entries: ${e.message}")
            mutableListOf()
        }
        applyFilterAndSort()
    }
    LaunchedEffect(currentSort, currentFilter, allHistoryEntries) {
        currentPage = 0
        applyFilterAndSort()
    }

    val totalPages = ((filteredHistoryEntries.size + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE).coerceAtLeast(1)
    val pageEntries = filteredHistoryEntries.subList(
        (currentPage * ITEMS_PER_PAGE).coerceAtMost(filteredHistoryEntries.size),
        ((currentPage + 1) * ITEMS_PER_PAGE).coerceAtMost(filteredHistoryEntries.size)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // Title and profile button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when {
                    selectedTab == 1 -> s("history_screen_title", "Game History")
                    saveMode -> s("save_screen_title", "Save Game")
                    else -> s("load_screen_title", "Load Game")
                },
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            CircularButton(
                text = "👤",
                color = CircularButtonColor.TURQUOISE,
                onClick = onProfile,
                modifier = Modifier.size(48.dp)
            )
        }

        // Tab layout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEachIndexed { index, tab ->
                FancyButton(
                    text = tab,
                    color = if (selectedTab == index) FancyButtonColor.BLUE else FancyButtonColor.GRAY,
                    onClick = { selectedTab = index },
                    modifier = Modifier.weight(1f).semantics { testTag = "tab_$index" }
                )
            }
        }

        if (selectedTab == 1) {
            // Sort / filter spinners (Android parity: shown only on history tab)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SpinnerDropdown(
                    options = SortOption.entries.map { s(it.stringKey, it.fallback) },
                    selectedIndex = SortOption.entries.indexOf(currentSort),
                    onSelected = { currentSort = SortOption.entries[it] },
                    modifier = Modifier.weight(1f).semantics { testTag = "sort_spinner" }
                )
                Spacer(modifier = Modifier.width(8.dp))
                SpinnerDropdown(
                    options = FilterOption.entries.map { s(it.stringKey, it.fallback) },
                    selectedIndex = FilterOption.entries.indexOf(currentFilter),
                    onSelected = { currentFilter = FilterOption.entries[it] },
                    modifier = Modifier.weight(1f).semantics { testTag = "filter_spinner" }
                )
            }

            // Top pagination (Android: visible from page 2 onwards)
            if (filteredHistoryEntries.size >= ITEMS_PER_PAGE && currentPage >= 1) {
                PaginationRow(
                    currentPage = currentPage,
                    totalPages = totalPages,
                    totalEntries = filteredHistoryEntries.size,
                    s = ::s,
                    onPrev = { if (currentPage > 0) currentPage-- },
                    onNext = { if (currentPage < totalPages - 1) currentPage++ },
                    testTagPrefix = "top"
                )
            }
        }

        // Save slots or history entries
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp)
        ) {
            if (selectedTab == 1) {
                // History tab
                if (pageEntries.isEmpty()) {
                    Text(
                        text = s("no_history_entries", "No history entries yet"),
                        color = Color.Gray,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    pageEntries.forEach { entry ->
                        HistoryItem(
                            entry = entry,
                            storage = storage,
                            dateFormat = listDateFormat,
                            s = ::s,
                            onClick = {
                                println("[SAVE_LOAD_SCREEN] Loading history entry: ${entry.getMapPath()}")
                                onLoadHistory(entry.getMapPath())
                            },
                            onInfoClick = { infoEntry = entry },
                            onDeleteClick = {
                                val mapPath = entry.getMapPath()
                                if (mapPath.isNotEmpty()) {
                                    val success = GameHistoryManager.deleteHistoryEntry(storage, mapPath)
                                    if (success) {
                                        historyReload++
                                    } else {
                                        shareMessage = s("history_delete_failed", "Failed to delete history entry")
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            } else {
                // Save/Load tabs
                saveSlots.forEach { slot ->
                    SaveSlotItem(
                        slot = slot,
                        storage = storage,
                        dateFormat = listDateFormat,
                        onClick = {
                            if (selectedTab == 0 && saveMode && session.currentState.value != null) {
                                println("[SAVE_LOAD_SCREEN] Saving game to slot ${slot.slotId}")
                                val result = session.saveGame(slot.slotId, isAutoSave = slot.slotId == 0)
                                println("[SAVE_LOAD_SCREEN] Save result: $result")
                                if (result) {
                                    saveSlots = loadSaveSlots(storage, ::s)
                                }
                            } else if (slot.saveData != null && selectedTab == 0 && !saveMode) {
                                println("[SAVE_LOAD_SCREEN] Loading game from slot ${slot.slotId}")
                                onLoadGame(slot.slotId)
                            }
                        },
                        onShareClick = { shareSlotId = slot.slotId },
                        onInfoClick = { infoSlot = slot }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Bottom pagination (Android: shown when entries >= page size)
        if (selectedTab == 1 && filteredHistoryEntries.size >= ITEMS_PER_PAGE) {
            PaginationRow(
                currentPage = currentPage,
                totalPages = totalPages,
                totalEntries = filteredHistoryEntries.size,
                s = ::s,
                onPrev = { if (currentPage > 0) currentPage-- },
                onNext = { if (currentPage < totalPages - 1) currentPage++ },
                testTagPrefix = "bottom"
            )
        }

        shareMessage?.let {
            Text(it, color = Color.LightGray, fontSize = 14.sp, modifier = Modifier.padding(vertical = 4.dp))
        }

        // Back button
        FancyButton(
            text = "◂ " + s("back_button", "Back"),
            color = FancyButtonColor.GRAY,
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        )
    }

    // Info dialogs
    infoEntry?.let { entry ->
        HistoryInfoDialog(
            entry = entry,
            onDismiss = { infoEntry = null }
        )
    }
    infoSlot?.let { slot ->
        SaveSlotInfoDialog(
            slot = slot,
            storage = storage,
            s = ::s,
            onDismiss = { infoSlot = null }
        )
    }

    // Share flow: logged in -> choose account or URL; logged out -> URL share directly
    shareSlotId?.let { slotId ->
        if (apiClient.isLoggedIn) {
            AlertDialog(
                onDismissRequest = { shareSlotId = null },
                title = { Text(s("share_dialog_title", "Share Map")) },
                text = {
                    Text(
                        s("share_dialog_logged_in_message", "You are logged in as {0}. Share directly to your account?", apiClient.userName ?: "")
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        shareSlotId = null
                        shareToAccount(storage, apiClient, slotId, ::s) { msg -> shareMessage = msg }
                    }) { Text(s("share_to_account", "Share to Account")) }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = {
                            shareSlotId = null
                            shareViaUrl(storage, apiClient, slotId, ::s) { msg -> shareMessage = msg }
                        }) { Text(s("share_via_url", "Share via URL")) }
                        TextButton(onClick = { shareSlotId = null }) { Text(s("button_cancel", "Cancel")) }
                    }
                }
            )
        } else {
            // Not logged in - share via URL (Android parity)
            LaunchedEffect(slotId) {
                shareViaUrl(storage, apiClient, slotId, ::s) { msg -> shareMessage = msg }
                shareSlotId = null
            }
        }
    }
}

internal fun formatArgs(fallback: String, vararg args: Any): String {
    var result = fallback
    args.forEachIndexed { i, arg ->
        result = result.replace("{$i}", arg.toString())
    }
    return result
}

/** Load all save slots (autosave 0 + slots 1..34) with metadata, mirroring Android loadSaveSlots. */
private fun loadSaveSlots(storage: PlatformStorage, s: (String, String, Array<out Any>) -> String): List<SaveSlotInfo> {
    val slots = mutableListOf<SaveSlotInfo>()

    // Autosave slot
    try {
        val autosaveData = if (storage.fileExists("saves/save_0.dat")) storage.readFile("saves/save_0.dat") else null
        if (autosaveData != null) {
            val meta = extractSaveMetadata(autosaveData, s)
            val name = if (meta.mapName != null) "${meta.mapName}    auto-save" else "Auto-save"
            slots.add(SaveSlotInfo(0, name, fileTimestamp(storage, "saves/save_0.dat"), meta.boardSize,
                meta.difficulty, meta.movesCount, meta.completionStatus, autosaveData))
        } else {
            slots.add(SaveSlotInfo(0, "${s("autosave_tag", "Auto-save", emptyArray())} (${s("save_empty", "empty", emptyArray())})",
                null, null, null, null, null, null))
        }
    } catch (e: Exception) {
        slots.add(SaveSlotInfo(0, "Auto-save", null, null, null, null, null, null))
    }

    for (i in 1..MAX_SAVE_SLOT) {
        try {
            val path = "saves/save_$i.dat"
            val saveData = if (storage.fileExists(path)) storage.readFile(path) else null
            if (saveData != null && saveData.isNotEmpty()) {
                val meta = extractSaveMetadata(saveData, s)
                slots.add(SaveSlotInfo(i, meta.mapName ?: "Slot $i", fileTimestamp(storage, path),
                    meta.boardSize, meta.difficulty, meta.movesCount, meta.completionStatus, saveData))
            } else {
                slots.add(SaveSlotInfo(i, "Slot $i (${s("empty_slot", "empty", emptyArray())})",
                    null, null, null, null, null, null))
            }
        } catch (e: Exception) {
            slots.add(SaveSlotInfo(i, "Slot $i (Error)", null, null, null, null, null, null))
        }
    }
    return slots
}

private fun fileTimestamp(storage: PlatformStorage, path: String): Long? = try {
    storage.getFileTimestamp(path)
} catch (e: Exception) { null }

private data class SaveDataMetadata(
    var mapName: String? = null,
    var boardSize: String? = null,
    var difficulty: String? = null,
    var movesCount: String? = null,
    var completionStatus: String? = null,
    var moves: Int = 0
)

/** Extract metadata from save data string (DRY port of Android extractSaveMetadata). */
private fun extractSaveMetadata(
    saveData: String,
    s: (String, String, Array<out Any>) -> String
): SaveDataMetadata {
    val meta = SaveDataMetadata()
    if (saveData.isEmpty()) return meta

    val metadata = GameSession.extractMetadataFromSaveData(saveData)
    if (metadata.containsKey("MAPNAME")) {
        meta.mapName = metadata["MAPNAME"]
    }

    // Board size from #SIZE:width,height or WIDTH/HEIGHT lines
    val sizeMatch = Regex("(#SIZE:|SIZE:)(\\d+),(\\d+)").find(saveData)
    if (sizeMatch != null) {
        meta.boardSize = s("board_size", "Board: {0}×{1}", arrayOf(sizeMatch.groupValues[2], sizeMatch.groupValues[3]))
    } else {
        val w = Regex("WIDTH:(\\d+);").find(saveData)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val h = Regex("HEIGHT:(\\d+);").find(saveData)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        if (w > 0 && h > 0) {
            meta.boardSize = s("board_size", "Board: {0}×{1}", arrayOf(w, h))
        }
    }

    // Difficulty
    val diffMatch = Regex("(DIFFICULTY:|#DIFFICULTY:)([^;\\n]+)").find(saveData)
    if (diffMatch != null) {
        val diffValue = diffMatch.groupValues[2]
        meta.difficulty = diffValue.trim().toIntOrNull()?.let { difficultyIntToString(it, s) } ?: diffValue.trim()
    } else {
        meta.difficulty = difficultyIntToString(Constants.DIFFICULTY_BEGINNER, s)
    }

    // Move count
    meta.moves = Regex("\\|MOVES:(\\d+)").find(saveData)?.groupValues?.get(1)?.toIntOrNull()
        ?: Regex("MOVES:(\\d+);").find(saveData)?.groupValues?.get(1)?.toIntOrNull()
        ?: Regex("#MOVES:(\\d+)").find(saveData)?.groupValues?.get(1)?.toIntOrNull()
        ?: 0

    if (meta.moves > 0) {
        meta.movesCount = s("moves_count", "Moves: {0}", arrayOf(meta.moves))
        val solvedMatch = Regex("(#SOLVED:|SOLVED:)(true|false)").find(saveData)
        if (solvedMatch != null) {
            meta.completionStatus = if (solvedMatch.groupValues[2] == "true") "Completed" else "Incomplete"
        }
    }
    return meta
}

private fun difficultyIntToString(difficulty: Int, s: (String, String, Array<out Any>) -> String): String = when (difficulty) {
    Constants.DIFFICULTY_BEGINNER -> s("difficulty_beginner", "Beginner", emptyArray())
    Constants.DIFFICULTY_ADVANCED -> s("difficulty_advanced", "Advanced", emptyArray())
    Constants.DIFFICULTY_INSANE -> s("difficulty_insane", "Insane", emptyArray())
    Constants.DIFFICULTY_IMPOSSIBLE -> s("difficulty_impossible", "Impossible", emptyArray())
    else -> s("difficulty_beginner", "Beginner", emptyArray())
}

@Composable
private fun SpinnerDropdown(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(options.getOrElse(selectedIndex) { "" }, color = Color.White, fontSize = 14.sp)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { index, label ->
                DropdownMenuItem(text = { Text(label) }, onClick = {
                    onSelected(index)
                    expanded = false
                })
            }
        }
    }
}

@Composable
private fun PaginationRow(
    currentPage: Int,
    totalPages: Int,
    totalEntries: Int,
    s: (String, String, Array<out Any>) -> String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    testTagPrefix: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onPrev, enabled = currentPage > 0,
            modifier = Modifier.semantics { testTag = "prev_page_$testTagPrefix" }) {
            Text(s("pagination_prev", "◂ Prev", emptyArray()), color = Color.White)
        }
        Text(
            s("pagination_page_info", "Page {0} of {1} ({2} entries)", arrayOf(currentPage + 1, totalPages, totalEntries)),
            color = Color.White,
            fontSize = 14.sp
        )
        TextButton(onClick = onNext, enabled = currentPage < totalPages - 1,
            modifier = Modifier.semantics { testTag = "next_page_$testTagPrefix" }) {
            Text(s("pagination_next", "Next ▸", emptyArray()), color = Color.White)
        }
    }
}

/** Board for a minimap from raw save data. */
internal fun boardFromSaveData(saveData: String?): Board? {
    if (saveData.isNullOrEmpty()) return null
    return try {
        val state = GameState.parseFromSaveData(saveData) ?: return null
        gridElementsToBoard(ArrayList(state.gridElements.filterNotNull()), state.width, state.height)
    } catch (e: Exception) {
        null
    }
}

@Composable
internal fun MinimapCanvas(saveData: String?, modifier: Modifier = Modifier) {
    val board = remember(saveData) { boardFromSaveData(saveData) }
    if (board != null) {
        Canvas(modifier = modifier) {
            MinimapGenerator.drawMinimap(this, board, size.width, size.height)
        }
    } else {
        Box(modifier = modifier.background(Color(0xFFC8E6FF)))
    }
}

@Composable
private fun SaveSlotItem(
    slot: SaveSlotInfo,
    storage: PlatformStorage,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit,
    onShareClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    val stringProvider = remember { getStringProvider() }
    val shareA11y = stringProvider.getString("share_button_a11y") ?: "Share map"
    val infoA11y = stringProvider.getString("info_button_a11y") ?: "Map info"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2C2C2C), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MinimapCanvas(
                saveData = slot.saveData,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(slot.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                slot.dateMillis?.let {
                    Text(dateFormat.format(Date(it)), color = Color.LightGray, fontSize = 14.sp)
                }
                slot.boardSize?.let { Text(it, color = Color.LightGray, fontSize = 14.sp) }
                slot.difficulty?.let { Text(it, color = Color.LightGray, fontSize = 14.sp) }
                Row {
                    slot.movesCount?.let { Text(it, color = Color.LightGray, fontSize = 14.sp) }
                    slot.completionStatus?.let {
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(it, color = Color.LightGray, fontSize = 14.sp)
                    }
                }
            }
            if (slot.saveData != null) {
                Column {
                    CircularButton(
                        text = "⇪",
                        color = CircularButtonColor.GRAY,
                        onClick = onShareClick,
                        modifier = Modifier.semantics { contentDescription = shareA11y; testTag = "share_slot_${slot.slotId}" }
                    )
                    CircularButton(
                        text = "i",
                        color = CircularButtonColor.GRAY,
                        onClick = onInfoClick,
                        modifier = Modifier.semantics { contentDescription = infoA11y; testTag = "info_slot_${slot.slotId}" }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryItem(
    entry: GameHistoryEntry,
    storage: PlatformStorage,
    dateFormat: SimpleDateFormat,
    s: (String, String, Array<out Any>) -> String,
    onClick: () -> Unit,
    onInfoClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val stringProvider = remember { getStringProvider() }
    val infoA11y = stringProvider.getString("info_button_a11y") ?: "Map info"
    val deleteA11y = stringProvider.getString("delete_button_a11y") ?: "Delete history entry"
    var saveData by remember(entry.getMapPath()) { mutableStateOf<String?>(null) }
    LaunchedEffect(entry.getMapPath()) {
        saveData = try { storage.readFile(entry.getMapPath()) } catch (e: Exception) { null }
    }

    val completionStatus = when {
        entry.completionCount == 1 -> s("history_completed_once", "Completed once", emptyArray())
        entry.completionCount > 1 -> s("history_completed_times", "Completed {0} times", arrayOf(entry.completionCount))
        else -> s("history_not_completed", "Not completed", emptyArray())
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2C2C2C), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MinimapCanvas(saveData = saveData, modifier = Modifier.size(80.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.mapName ?: "Unknown", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(dateFormat.format(Date(entry.timestamp)), color = Color.LightGray, fontSize = 14.sp)
                entry.boardSize?.let { Text(it, color = Color.LightGray, fontSize = 14.sp) }
                Row {
                    Text("Moves: ${entry.movesMade}", color = Color.LightGray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(completionStatus, color = Color.LightGray, fontSize = 14.sp)
                }
            }
            Column {
                CircularButton(
                    text = "i",
                    color = CircularButtonColor.GRAY,
                    onClick = onInfoClick,
                    modifier = Modifier.semantics {
                        contentDescription = infoA11y
                        testTag = "infoButton_${entry.getHistoryIndex()}"
                    }
                )
                CircularButton(
                    text = "🗑",
                    color = CircularButtonColor.RED,
                    onClick = onDeleteClick,
                    modifier = Modifier.semantics {
                        contentDescription = deleteA11y
                        testTag = "deleteButton_${entry.getHistoryIndex()}"
                    }
                )
            }
        }
    }
}

/** Save slot info popup — port of Android showSaveSlotInfoPopup. */
@OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
@Composable
private fun SaveSlotInfoDialog(
    slot: SaveSlotInfo,
    storage: PlatformStorage,
    s: (String, String, Array<out Any>) -> String,
    onDismiss: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()) }
    val message = remember(slot.slotId) {
        val saveData = slot.saveData
        val sb = StringBuilder()
        var maxHintUsed = -1
        var moves = -1
        var mapSig: String? = null
        var solved = false

        if (saveData != null && saveData.startsWith("#")) {
            val nl = saveData.indexOf('\n')
            val header = saveData.substring(1, if (nl > 0) nl else saveData.length)
            for (part in header.split(";")) {
                when {
                    part.startsWith("MAX_HINT_USED:") ->
                        maxHintUsed = part.substring("MAX_HINT_USED:".length).toIntOrNull() ?: -1
                    part.startsWith("MOVES:") ->
                        moves = part.substring("MOVES:".length).toIntOrNull() ?: -1
                    part.startsWith("MAP_SIG:") -> {
                        val raw = part.substring("MAP_SIG:".length)
                        mapSig = try {
                            kotlin.io.encoding.Base64.decode(raw).decodeToString()
                        } catch (e: Exception) {
                            raw // old unencoded saves
                        }
                    }
                    part.startsWith("SOLVED:") ->
                        solved = part.substring("SOLVED:".length) == "true"
                }
            }
        }

        slot.dateMillis?.let {
            sb.append(s("save_slot_saved", "Saved:", emptyArray())).append(" ").append(sdf.format(Date(it))).append("\n")
        }
        sb.append(s("save_slot_status", "Status:", emptyArray())).append(" ")
            .append(if (solved) s("save_slot_solved", "Solved", emptyArray()) else s("save_slot_in_progress", "In progress", emptyArray()))
            .append("\n")
        if (moves >= 0) sb.append(s("save_slot_moves_at_save", "Moves at save:", emptyArray())).append(" ").append(moves).append("\n")
        slot.difficulty?.let {
            sb.append(s("save_slot_difficulty", "Difficulty:", emptyArray())).append(" ").append(it).append("\n")
        }

        sb.append("\n").append(s("save_slot_hint_usage_at_save", "Hint usage at save:", emptyArray())).append(" ")
        when {
            maxHintUsed < 0 -> sb.append(s("history_detail_no_hints_used", "No hints used", emptyArray()))
            maxHintUsed == 0 -> sb.append(s("history_detail_pre_hint_viewed", "Pre-hint viewed", emptyArray()))
            else -> sb.append(s("history_detail_up_to_hint", "Up to hint {0}", arrayOf(maxHintUsed + 1)))
        }
        sb.append("\n")

        sb.append("\n").append(s("save_slot_history_header", "History:", emptyArray())).append("\n")
        if (!mapSig.isNullOrEmpty()) {
            val histEntry = GameHistoryManager.findByMapSignature(storage, mapSig)
            if (histEntry != null) {
                sb.append(buildHistoryInfoMessage(histEntry, s, sdf))
            } else {
                sb.append(s("save_slot_no_history_entry", "No history entry for this map", emptyArray())).append("\n")
            }
        } else {
            sb.append(s("save_slot_no_history_old_format", "No history data (old save format)", emptyArray())).append("\n")
        }
        sb.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(slot.name) },
        text = { Text(message, fontSize = 12.sp) },
        confirmButton = { Button(onClick = onDismiss) { Text("OK") } }
    )
}

/** Shared history-info message builder (used by history info + save slot info dialogs). */
internal fun buildHistoryInfoMessage(
    entry: GameHistoryEntry,
    s: (String, String, Array<out Any>) -> String,
    sdf: SimpleDateFormat
): String {
    val sb = StringBuilder()
    sb.append(s("history_detail_completions", "Completions:", emptyArray())).append(" ").append(entry.completionCount).append("\n")
    sb.append(s("history_detail_first_started", "First started:", emptyArray())).append(" ").append(sdf.format(Date(entry.timestamp))).append("\n")
    if (entry.lastCompletionTimestamp > 0) {
        sb.append(s("history_detail_last_played", "Last played:", emptyArray())).append(" ").append(sdf.format(Date(entry.lastCompletionTimestamp))).append("\n")
    }
    val timestamps = entry.getCompletionTimestamps()
    if (timestamps.size > 1) {
        val isLevelGame = entry.mapName?.startsWith("Level ") == true
        val completionStars = entry.getCompletionStars()
        val completionMoves = entry.getCompletionMoves()
        sb.append("\n").append(s("history_detail_all_completions", "All completions:", emptyArray())).append("\n")
        for (i in timestamps.indices) {
            sb.append("  ").append(i + 1).append(". ").append(sdf.format(Date(timestamps[i])))
            if (isLevelGame) {
                val stars = if (i < completionStars.size) completionStars[i] else entry.starsEarned
                val moves = if (i < completionMoves.size) completionMoves[i] else entry.movesMade
                if (stars == 0) sb.append(" ✓") else repeat(stars) { sb.append("★") }
                sb.append(" - ").append(moves)
            } else {
                val moves = if (i < completionMoves.size) completionMoves[i] else entry.movesMade
                sb.append(" - ").append(moves)
            }
            sb.append("\n")
        }
    }
    sb.append("\n").append(s("history_detail_best_time", "Best time:", emptyArray())).append(" ")
    if (entry.bestTime > 0) sb.append("${entry.bestTime / 60}m ${entry.bestTime % 60}s") else sb.append("—")
    sb.append("\n")
    sb.append(s("history_detail_best_moves", "Best moves:", emptyArray())).append(" ")
    sb.append(if (entry.bestMoves > 0) entry.bestMoves else "—").append("\n")
    sb.append(s("history_detail_optimal_moves", "Optimal moves:", emptyArray())).append(" ")
    if (entry.optimalMoves > 0) {
        sb.append(entry.optimalMoves)
        if (entry.bestMoves > 0 && entry.bestMoves == entry.optimalMoves) {
            sb.append(" ✓ (").append(s("history_detail_perfect", "Perfect", emptyArray())).append(")")
        } else if (entry.bestMoves > 0) {
            sb.append(" (").append(s("history_detail_extra_moves", "+{0} extra moves", arrayOf(entry.bestMoves - entry.optimalMoves))).append(")")
        }
    } else sb.append("—")
    sb.append("\n")
    sb.append("\n").append(s("history_detail_hint_usage_last", "Hint usage (last):", emptyArray())).append(" ")
    when {
        entry.maxHintUsed < 0 -> sb.append(s("history_detail_no_hints_used", "No hints used", emptyArray()))
        entry.maxHintUsed == 0 -> sb.append(s("history_detail_pre_hint_viewed", "Pre-hint viewed", emptyArray()))
        else -> sb.append(s("history_detail_up_to_hint", "Up to hint {0}", arrayOf(entry.maxHintUsed + 1)))
    }
    sb.append("\n")
    sb.append(s("history_detail_hints_ever_used", "Hints ever used:", emptyArray())).append(" ")
        .append(if (entry.isEverUsedHints()) s("history_detail_yes", "Yes", emptyArray()) else s("history_detail_no", "No", emptyArray())).append("\n")
    sb.append(s("history_detail_qualifies_no_hints", "Qualifies for no-hints achievement:", emptyArray())).append(" ")
        .append(if (entry.qualifiesForNoHintsAchievement()) s("history_detail_yes", "Yes", emptyArray()) else s("history_detail_no", "No", emptyArray())).append("\n")
    sb.append(s("history_detail_qualifies_no_hints_perfect", "Qualifies for perfect no-hints achievement:", emptyArray())).append(" ")
        .append(if (entry.qualifiesForPerfectNoHintsAchievement()) s("history_detail_yes", "Yes", emptyArray()) else s("history_detail_no", "No", emptyArray())).append("\n")
    sb.append(s("history_detail_last_solved_no_hints", "Last solved without hints:", emptyArray())).append(" ")
        .append(if (entry.lastSolvedWithoutHints > 0) sdf.format(Date(entry.lastSolvedWithoutHints)) else "—").append("\n")
    sb.append(s("history_detail_last_perfect_no_hints", "Last perfectly solved without hints:", emptyArray())).append(" ")
        .append(if (entry.lastPerfectlySolvedWithoutHints > 0) sdf.format(Date(entry.lastPerfectlySolvedWithoutHints)) else "—").append("\n")
    return sb.toString()
}

@Composable
fun HistoryInfoDialog(
    entry: GameHistoryEntry,
    onDismiss: () -> Unit
) {
    val stringProvider = remember { getStringProvider() }
    val sdf = remember { SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()) }
    fun s(key: String, fallback: String, vararg args: Any): String =
        stringProvider.getString(key, *args) ?: formatArgs(fallback, *args)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entry.mapName ?: "Unknown Map") },
        text = { Text(buildHistoryInfoMessage(entry, ::s, sdf), fontSize = 12.sp) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

// ---- Share helpers (port of Android SaveGameFragment share flow) ----

private fun shareToAccount(
    storage: PlatformStorage,
    apiClient: RoboyardApiClient,
    slotId: Int,
    s: (String, String, Array<out Any>) -> String,
    onMessage: (String) -> Unit
) {
    val saveData = try { storage.readFile("saves/save_$slotId.dat") } catch (e: Exception) { null }
    val mapData = ShareMapHelper.buildMapDataForShare(saveData)
    if (mapData == null) {
        onMessage("No data to share")
        return
    }
    val mapName = ShareMapHelper.getMapNameFromSaveData(saveData)
    apiClient.shareMap(mapData, mapName, object : RoboyardApiClient.ApiCallback<RoboyardApiClient.ShareResult?> {
        override fun onSuccess(result: RoboyardApiClient.ShareResult?) {
            if (result == null) return
            onMessage(
                if (result.isDuplicate) "Map already exists"
                else s("share_success", "Map shared successfully", emptyArray())
            )
            result.shareUrl?.let { openAutoLoginUrl(apiClient.buildAutoLoginUrl(it)) }
        }

        override fun onError(error: String?) {
            onMessage(s("share_failed", "Share failed: {0}", arrayOf(error ?: "Unknown error")))
        }
    })
}

private fun shareViaUrl(
    storage: PlatformStorage,
    apiClient: RoboyardApiClient,
    slotId: Int,
    s: (String, String, Array<out Any>) -> String,
    onMessage: (String) -> Unit
) {
    val saveData = try { storage.readFile("saves/save_$slotId.dat") } catch (e: Exception) { null }
    if (saveData.isNullOrEmpty()) {
        onMessage("Save file does not exist")
        return
    }
    val result = ShareMapHelper.parseSaveDataForShare(saveData)
    if (result == null) {
        onMessage("Cannot share - failed to parse save data")
        return
    }
    if (result.targetCount == 0) {
        onMessage("Cannot share - no target data found in save file")
        return
    }
    val shareUrl = ShareMapHelper.buildShareUrl(apiClient.baseUrl, result.formattedData)
    openAutoLoginUrl(apiClient.buildAutoLoginUrl(shareUrl))
    onMessage("Opening share URL in browser")
}
