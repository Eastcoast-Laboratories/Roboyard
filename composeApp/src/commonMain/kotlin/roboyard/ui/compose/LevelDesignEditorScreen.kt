package roboyard.ui.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import roboyard.logic.platform.openAutoLoginUrl
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.net.URLEncoder
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.imageResource
import roboyard.composeapp.generated.resources.Res
import roboyard.composeapp.generated.resources.grid_tiles
import roboyard.composeapp.generated.resources.robot_blue_right
import roboyard.composeapp.generated.resources.robot_green_right
import roboyard.composeapp.generated.resources.robot_pink_right
import roboyard.composeapp.generated.resources.robot_silver_right
import roboyard.composeapp.generated.resources.robot_yellow_right
import roboyard.composeapp.generated.resources.target_blue
import roboyard.composeapp.generated.resources.target_green
import roboyard.composeapp.generated.resources.target_multi
import roboyard.composeapp.generated.resources.target_pink
import roboyard.composeapp.generated.resources.target_silver
import roboyard.composeapp.generated.resources.target_yellow
import roboyard.logic.core.Constants
import roboyard.logic.core.GameElement
import roboyard.logic.core.GameState
import roboyard.logic.core.ResourceLoader
import roboyard.logic.core.WallPatternGenerator
import roboyard.logic.managers.LevelCompletionManager
import roboyard.logic.network.RoboyardApiClient
import roboyard.logic.solver.RRGetMap
import roboyard.logic.storage.getPlatformStorage
import roboyard.logic.ui.getStringProvider

/**
 * Level Design Editor — port of Android LevelDesignEditorFragment.
 *
 * Modes: place robots, place targets, horizontal/vertical walls, erase.
 * Supports board resize (center on grow, trim on shrink), shift, wall-pattern
 * generation, random robot/target placement, ASCII import, level-text export,
 * share URL, "save to sourcecode" via local receiver, and play-testing the map.
 */
@Composable
fun LevelDesignEditorScreen(
    onBack: () -> Unit = {},
    onPlayMap: (GameState) -> Unit = {}
) {
    val storage = remember { getPlatformStorage() }
    val stringProvider = remember { getStringProvider() }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current

    fun s(key: String, fallback: String): String = stringProvider.getString(key) ?: fallback

    androidx.compose.runtime.LaunchedEffect(Unit) {
        println("[LEVEL_EDITOR] Opened")
    }

    // ---- Editor state ----
    var currentState by remember { mutableStateOf<GameState?>(null) }
    var currentLevelId by remember { mutableIntStateOf(0) }
    var currentEditMode by remember { mutableIntStateOf(EDIT_MODE_ROBOT) }
    var currentRobotColor by remember { mutableIntStateOf(Constants.COLOR_PINK) }
    var currentTargetColor by remember { mutableIntStateOf(Constants.COLOR_PINK) }
    var boardWidthText by remember { mutableStateOf("12") }
    var boardHeightText by remember { mutableStateOf("14") }
    var selectedPattern by remember { mutableIntStateOf(0) }
    var selectedLevelLabel by remember { mutableStateOf("") }
    var levelNameText by remember { mutableStateOf("") }
    var boardRedrawTick by remember { mutableIntStateOf(0) }

    var toastMessage by remember { mutableStateOf<String?>(null) }
    fun toast(msg: String) { toastMessage = msg }

    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    // ---- Level list (matches Android setupLevelSpinner) ----
    val levelOptions = remember {
        buildList<Pair<String, Int>> {
            add(s("editor_new_map", "New Map") to 0)
            for (id in 1..140) add("Level $id" to id)
            for (f in storage.listFiles("custom_level_", ".txt")) {
                f.removePrefix("custom_level_").removeSuffix(".txt").toIntOrNull()
                    ?.let { add("Custom Level $it" to it) }
            }
        }
    }

    // ---- Savegame list (matches Android setupSavegameSpinner) ----
    val saveOptions = remember {
        buildList<Pair<String, Int>> {
            add("—" to -1)
            for (slot in 1..10) {
                val fileName = "${Constants.SAVE_DIRECTORY}/${Constants.SAVE_FILENAME_PREFIX}$slot${Constants.SAVE_FILENAME_EXTENSION}"
                if (storage.fileExists(fileName)) {
                    val saveData = storage.readFile(fileName)
                    val mapName = saveData.lineSequence()
                        .firstOrNull { it.startsWith("#MAPNAME:") }
                        ?.removePrefix("#MAPNAME:")?.split(";")?.firstOrNull()?.trim()
                    add("$slot: ${mapName?.takeIf { it.isNotEmpty() } ?: "Save $slot"}" to slot)
                }
            }
        }
    }

    // ---- Level helpers ----

    fun applyLoadedState(state: GameState, levelId: Int, label: String) {
        currentState = state
        currentLevelId = levelId
        boardWidthText = state.width.toString()
        boardHeightText = state.height.toString()
        levelNameText = label
        boardRedrawTick++
    }

    fun loadLevel(levelId: Int) {
        if (levelId == 0) {
            createNewLevel(12, 14).let { applyLoadedState(it, 0, s("editor_new_map", "New Map")) }
            return
        }
        // Custom level file takes precedence (matches Android: filesDir first, then assets)
        val customFile = "custom_level_$levelId.txt"
        val plainFile = "level_$levelId.txt"
        val content = when {
            storage.fileExists(customFile) -> storage.readFile(customFile)
            storage.fileExists(plainFile) -> storage.readFile(plainFile)
            else -> ResourceLoader.loadLevelContent(levelId)
        }
        if (content != null) {
            val state = GameState.parseLevel(content, levelId)
            applyLoadedState(state, levelId,
                stringProvider.getString("editor_level_map", levelId) ?: "Level $levelId Map")
        } else {
            toast("Error loading level $levelId")
            applyLoadedState(createNewLevel(12, 14), 0, s("editor_new_map", "New Map"))
        }
    }

    fun loadSaveSlot(slot: Int) {
        val fileName = "${Constants.SAVE_DIRECTORY}/${Constants.SAVE_FILENAME_PREFIX}$slot${Constants.SAVE_FILENAME_EXTENSION}"
        val saveData = storage.readFile(fileName)
        if (saveData.isEmpty()) {
            toast(s("editor_save_slot_empty", "Save slot empty"))
            return
        }
        val state = GameState.parseFromSaveData(saveData)
        if (state == null) {
            toast(s("editor_could_not_load_save", "Could not load save"))
            return
        }
        applyLoadedState(state, -1, state.levelName ?: "Save $slot")
    }

    // Initial level: last played, else level 1 (matches Android onViewCreated)
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (currentState == null) {
            val lastPlayed = LevelCompletionManager.getInstance(storage).lastPlayedLevel
            if (lastPlayed in 1..140) loadLevel(lastPlayed) else loadLevel(1)
        }
    }

    // ---- Element manipulation (ports of the Android fragment methods) ----

    fun inCarree(x: Int, y: Int, w: Int, h: Int): Boolean {
        val cx = w / 2 - 1
        val cy = h / 2 - 1
        return x in cx..(cx + 1) && y in cy..(cy + 1)
    }

    fun redrawWalls() {
        val st = currentState ?: return
        st.createBorderWalls()
        st.createCenterCarree()
        boardRedrawTick++
    }

    fun addRobotAt(x: Int, y: Int) {
        val st = currentState ?: return
        if (inCarree(x, y, st.width, st.height)) {
            toast(s("editor_cannot_place_in_carree", "Cannot place robot or goal in the center carree"))
            return
        }
        st.gameElements.removeAll { it.type == GameElement.TYPE_ROBOT && it.color == currentRobotColor }
        st.addRobot(x, y, currentRobotColor)
        toast(stringProvider.getString("editor_added_robot", colorName(currentRobotColor), x, y)
            ?: "Added ${colorName(currentRobotColor)} robot at ($x, $y)")
        boardRedrawTick++
    }

    fun addTargetAt(x: Int, y: Int) {
        val st = currentState ?: return
        if (inCarree(x, y, st.width, st.height)) {
            toast(s("editor_cannot_place_in_carree", "Cannot place robot or goal in the center carree"))
            return
        }
        st.gameElements.removeAll { it.type == GameElement.TYPE_TARGET && it.color == currentTargetColor }
        st.addTarget(x, y, currentTargetColor)
        boardRedrawTick++
    }

    fun addHorizontalWallAt(x: Int, y: Int) {
        val st = currentState ?: return
        if (st.gameElements.any { it.x == x && it.y == y && it.type == GameElement.TYPE_HORIZONTAL_WALL }) {
            st.gameElements.removeAll { it.x == x && it.y == y && it.type == GameElement.TYPE_HORIZONTAL_WALL }
        } else {
            st.addHorizontalWall(x, y)
        }
        redrawWalls()
    }

    fun addVerticalWallAt(x: Int, y: Int) {
        val st = currentState ?: return
        if (st.gameElements.any { it.x == x && it.y == y && it.type == GameElement.TYPE_VERTICAL_WALL }) {
            st.gameElements.removeAll { it.x == x && it.y == y && it.type == GameElement.TYPE_VERTICAL_WALL }
        } else {
            st.addVerticalWall(x, y)
        }
        redrawWalls()
    }

    fun eraseAt(x: Int, y: Int) {
        val st = currentState ?: return
        var removed = false
        st.gameElements.removeAll {
            if (it.x == x && it.y == y) { removed = true; true } else false
        }
        if (st.getCellType(x, y) != 0) {
            st.setCellType(x, y, 0)
            removed = true
        }
        if (!removed) {
            toast(stringProvider.getString("editor_nothing_to_erase", x, y)
                ?: "Nothing to erase at ($x, $y)")
        }
        redrawWalls()
    }

    fun handleBoardClick(x: Int, y: Int) {
        val st = currentState ?: return
        if (x < 0 || y < 0 || x >= st.width || y >= st.height) return
        when (currentEditMode) {
            EDIT_MODE_ROBOT -> addRobotAt(x, y)
            EDIT_MODE_TARGET -> addTargetAt(x, y)
            EDIT_MODE_WALL_H -> addHorizontalWallAt(x, y)
            EDIT_MODE_WALL_V -> addVerticalWallAt(x, y)
            EDIT_MODE_ERASE -> eraseAt(x, y)
        }
    }

    fun updateBoardSize(newWidth: Int, newHeight: Int) {
        val st = currentState ?: return
        if (newWidth < 4 || newWidth > 24 || newHeight < 4 || newHeight > 24) {
            toast(s("editor_board_size_invalid", "Board size must be between 4x4 and 24x24"))
            return
        }
        val oldWidth = st.width
        val oldHeight = st.height

        st.removeOuterWalls()
        st.removeCenterCarree(oldWidth, oldHeight)

        if (newWidth > oldWidth || newHeight > oldHeight) {
            // Grow: center old content
            val offX = (newWidth - oldWidth) / 2
            val offY = (newHeight - oldHeight) / 2
            st.gameElements.forEach { it.x += offX; it.y += offY }
        } else if (newWidth < oldWidth || newHeight < oldHeight) {
            // Shrink: shift up, cut off right (matches Android trimContent)
            val shiftY = oldHeight - newHeight
            st.gameElements.forEach { it.y -= shiftY }
            st.gameElements.removeAll { it.x < 0 || it.y < 0 || it.x >= newWidth || it.y >= newHeight }
        }

        val newState = GameState(newWidth, newHeight)
        newState.gameElements.addAll(st.gameElements)
        newState.levelId = st.levelId
        newState.levelName = st.levelName
        newState.createBorderWalls()
        newState.createCenterCarree()
        currentState = newState
        boardRedrawTick++
    }

    fun shiftContent(dx: Int, dy: Int) {
        val st = currentState ?: return
        st.removeOuterWalls()
        st.removeCenterCarree(st.width, st.height)
        st.gameElements.forEach { it.x += dx; it.y += dy }
        st.gameElements.removeAll { it.x < 0 || it.y < 0 || it.x >= st.width || it.y >= st.height }
        st.createBorderWalls()
        st.createCenterCarree()
        boardRedrawTick++
    }

    fun generateWallsFromPattern() {
        val st = currentState ?: run {
            toast(s("editor_no_level_loaded", "No level loaded"))
            return
        }
        val robotsAndTargets = st.gameElements.filter {
            it.type == GameElement.TYPE_ROBOT || it.type == GameElement.TYPE_TARGET
        }
        val newState = WallPatternGenerator(st.width, st.height).generate(selectedPattern)
        newState.levelId = st.levelId
        newState.levelName = st.levelName
        newState.gameElements.addAll(robotsAndTargets)
        currentState = newState
        boardRedrawTick++
    }

    fun randomlyPlaceRobotsAndTargets() {
        val st = currentState ?: run {
            toast(s("editor_no_level_loaded", "No level loaded"))
            return
        }
        st.gameElements.removeAll {
            it.type == GameElement.TYPE_ROBOT || it.type == GameElement.TYPE_TARGET
        }
        val colors = intArrayOf(
            Constants.COLOR_PINK, Constants.COLOR_GREEN,
            Constants.COLOR_BLUE, Constants.COLOR_YELLOW
        )
        val usedColors = mutableListOf<Int>()
        for (i in 0 until min(4, colors.size)) {
            var x = 0; var y = 0; var attempts = 0
            do {
                x = (0 until st.width).random()
                y = (0 until st.height).random()
                attempts++
            } while (st.gameElements.any { it.x == x && it.y == y } && attempts < 100)
            if (attempts < 100) {
                st.addRobot(x, y, colors[i])
                usedColors.add(colors[i])
            }
        }
        if (usedColors.isNotEmpty()) {
            val targetColor = usedColors.random()
            var x = 0; var y = 0; var attempts = 0
            do {
                x = (0 until st.width).random()
                y = (0 until st.height).random()
                attempts++
            } while (st.gameElements.any { it.x == x && it.y == y } && attempts < 100)
            if (attempts < 100) st.addTarget(x, y, targetColor)
        }
        boardRedrawTick++
        toast("Placed ${usedColors.size} robots and 1 target randomly")
    }

    fun generateLevelText(): String {
        val st = currentState ?: return ""
        val sb = StringBuilder()
        sb.append("board:").append(st.width).append(",").append(st.height).append(";\n")
        val sorted = st.gameElements.sortedWith(compareBy({ it.x }, { it.y }))
        for (e in sorted.filter { it.type == GameElement.TYPE_HORIZONTAL_WALL })
            sb.append("h").append(e.x).append(",").append(e.y).append(";\n")
        for (e in sorted.filter { it.type == GameElement.TYPE_VERTICAL_WALL })
            sb.append("v").append(e.x).append(",").append(e.y).append(";\n")
        for (e in sorted.filter { it.type == GameElement.TYPE_TARGET })
            sb.append("t").append(colorChar(e.color)).append(e.x).append(",").append(e.y).append(";\n")
        for (e in sorted.filter { it.type == GameElement.TYPE_ROBOT })
            sb.append("r").append(colorChar(e.color)).append(e.x).append(",").append(e.y).append(";\n")
        return sb.toString()
    }

    fun playCurrentMap() {
        val st = currentState ?: run {
            toast(s("editor_no_level_loaded", "No level loaded"))
            return
        }
        val hasRobot = st.gameElements.any { it.type == GameElement.TYPE_ROBOT }
        val hasTarget = st.gameElements.any { it.type == GameElement.TYPE_TARGET }
        if (!hasRobot || !hasTarget) {
            toast(s("editor_map_needs_robot_target", "Map needs at least one robot and one goal to play"))
            return
        }
        val state = GameState.parseLevel(generateLevelText(), -1)
        onPlayMap(state)
    }

    // ---- UI ----

    val patternNames = listOf(
        s("editor_pattern_classic", "Classic"),
        s("editor_pattern_spiral", "Spiral"),
        s("editor_pattern_rooms", "Rooms"),
        s("editor_pattern_maze", "Maze"),
        s("editor_pattern_diagonal", "Diagonal"),
        s("editor_pattern_symmetric", "Symmetric"),
        s("editor_pattern_corridors", "Corridors"),
        s("editor_pattern_islands", "Islands"),
        s("editor_pattern_border_heavy", "Border Heavy"),
        s("editor_pattern_scatter", "Scatter")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(8.dp)
    ) {
        // Explicit shared scroll state so a visible VerticalScrollbar can be
        // attached — on Desktop, mouse drags do not scroll the content
        val editorScrollState = rememberScrollState()
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(editorScrollState)
        ) {
            // Title + current map name
            Text(
                text = s("editor_title", "Level Design Editor"),
                color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = levelNameText,
                color = Color(0xFFCCCCCC), fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            // Level selector
            EditorDropdownRow(
                label = s("editor_select_level", "Select Level: "),
                options = levelOptions.map { it.first },
                selected = selectedLevelLabel.ifEmpty { levelOptions.firstOrNull()?.first ?: "" },
                onSelect = { label ->
                    selectedLevelLabel = label
                    val id = levelOptions.firstOrNull { it.first == label }?.second ?: 0
                    loadLevel(id)
                }
            )

            // Savegame selector
            EditorDropdownRow(
                label = "Savegame: ",
                options = saveOptions.map { it.first },
                selected = saveOptions.first().first,
                onSelect = { label ->
                    val slot = saveOptions.firstOrNull { it.first == label }?.second ?: -1
                    if (slot >= 0) loadSaveSlot(slot)
                }
            )

            // Board size
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(s("editor_board_size", "Board Size: "), color = Color.White, fontSize = 14.sp)
                TextField(
                    value = boardWidthText,
                    onValueChange = { boardWidthText = it.filter(Char::isDigit).take(2) },
                    singleLine = true,
                    modifier = Modifier.width(70.dp).padding(horizontal = 4.dp)
                )
                Text("×", color = Color.White, fontSize = 14.sp)
                TextField(
                    value = boardHeightText,
                    onValueChange = { boardHeightText = it.filter(Char::isDigit).take(2) },
                    singleLine = true,
                    modifier = Modifier.width(70.dp).padding(horizontal = 4.dp)
                )
                FancyButton(
                    text = s("editor_apply", "Apply"),
                    color = FancyButtonColor.BLUE,
                    onClick = {
                        val w = boardWidthText.toIntOrNull()
                        val h = boardHeightText.toIntOrNull()
                        if (w == null || h == null) {
                            toast(s("editor_invalid_numbers", "Please enter valid numbers"))
                        } else updateBoardSize(w, h)
                    },
                    modifier = Modifier.padding(start = 8.dp).height(40.dp)
                )
            }

            // Edit mode radio group
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                listOf(
                    EDIT_MODE_ROBOT to s("editor_mode_robot", "Robot"),
                    EDIT_MODE_TARGET to s("editor_mode_target", "Goal"),
                    EDIT_MODE_WALL_H to "–",
                    EDIT_MODE_WALL_V to "|",
                    EDIT_MODE_ERASE to s("editor_mode_erase", "Erase")
                ).forEach { (mode, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { currentEditMode = mode }
                            .padding(end = 4.dp)
                    ) {
                        RadioButton(
                            selected = currentEditMode == mode,
                            onClick = { currentEditMode = mode },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF4CAF50))
                        )
                        Text(label, color = Color.White, fontSize = 12.sp)
                    }
                }
            }

            // Color pickers
            if (currentEditMode == EDIT_MODE_ROBOT) {
                EditorColorRow(
                    colors = robotColors(),
                    selected = currentRobotColor,
                    onSelect = { currentRobotColor = it }
                )
            } else if (currentEditMode == EDIT_MODE_TARGET) {
                EditorColorRow(
                    colors = targetColors(),
                    selected = currentTargetColor,
                    onSelect = { currentTargetColor = it }
                )
            }

            // Board preview — tap to edit (Android GameBoardView equivalent)
            Text(
                s("editor_board_preview_hint", "Board Preview (Tap to edit)"),
                color = Color(0xFFAAAAAA), fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            EditorBoardCanvas(
                state = currentState,
                redrawTick = boardRedrawTick,
                onCellClick = { x, y -> handleBoardClick(x, y) },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .border(2.dp, Color(0xFF555555))
            )

            // Shift buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                FancyButton(text = "↑", color = FancyButtonColor.GRAY, onClick = { shiftContent(0, -1) },
                    modifier = Modifier.padding(2.dp).width(48.dp).height(40.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                FancyButton(text = "←", color = FancyButtonColor.GRAY, onClick = { shiftContent(-1, 0) },
                    modifier = Modifier.padding(2.dp).width(48.dp).height(40.dp))
                FancyButton(text = "↓", color = FancyButtonColor.GRAY, onClick = { shiftContent(0, 1) },
                    modifier = Modifier.padding(2.dp).width(48.dp).height(40.dp))
                FancyButton(text = "→", color = FancyButtonColor.GRAY, onClick = { shiftContent(1, 0) },
                    modifier = Modifier.padding(2.dp).width(48.dp).height(40.dp))
            }

            // Wall pattern generation
            EditorDropdownRow(
                label = "",
                options = patternNames,
                selected = patternNames[selectedPattern],
                onSelect = { label -> selectedPattern = patternNames.indexOf(label).coerceAtLeast(0) }
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                FancyButton(
                    text = s("editor_generate", "Generate Walls"),
                    color = FancyButtonColor.BLUE,
                    onClick = { generateWallsFromPattern() },
                    modifier = Modifier.weight(1f).padding(2.dp).height(40.dp)
                )
                FancyButton(
                    text = s("editor_generate_border_stubs", "Border Stubs"),
                    color = FancyButtonColor.BLUE,
                    onClick = {
                        val st = currentState
                        if (st == null) toast(s("editor_no_level_loaded", "No level loaded"))
                        else {
                            WallPatternGenerator.generateBorderStubs(st)
                            boardRedrawTick++
                        }
                    },
                    modifier = Modifier.weight(1f).padding(2.dp).height(40.dp)
                )
            }
            FancyButton(
                text = s("editor_random_robots_targets", "Random Placement"),
                color = FancyButtonColor.BLUE,
                onClick = { randomlyPlaceRobotsAndTargets() },
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).height(40.dp)
            )

            // Import / Export / Play / Cancel
            Row(modifier = Modifier.fillMaxWidth()) {
                FancyButton(
                    text = s("editor_import_ascii_map_title", "Import ASCII"),
                    color = FancyButtonColor.GRAY,
                    onClick = { showImportDialog = true },
                    modifier = Modifier.weight(1f).padding(2.dp).height(44.dp)
                )
                FancyButton(
                    text = s("editor_export_level", "Export"),
                    color = FancyButtonColor.GRAY,
                    onClick = { showExportDialog = true },
                    modifier = Modifier.weight(1f).padding(2.dp).height(44.dp)
                )
                FancyButton(
                    text = s("editor_play_map", "▶ Play Map"),
                    color = FancyButtonColor.GREEN,
                    onClick = { playCurrentMap() },
                    modifier = Modifier.weight(1f).padding(2.dp).height(44.dp)
                )
            }
        }

        androidx.compose.foundation.VerticalScrollbar(
            adapter = androidx.compose.foundation.rememberScrollbarAdapter(editorScrollState),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
        )
        }

        FancyButton(
            text = "◂ " + s("back_button", "Back"),
            color = FancyButtonColor.GRAY,
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(44.dp)
        )
    }

    // ---- Export dialog (Android showLevelText) ----
    if (showExportDialog) {
        val levelText = generateLevelText()
        var shareName by remember { mutableStateOf("") }
        val apiClient = remember { RoboyardApiClient.getInstance(storage) }
        val shareUrl = apiClient.baseUrl + "/share_map?data=" +
            URLEncoder.encode(levelText, "UTF-8")
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Level Text Format") },
            text = {
                Column {
                    TextField(
                        value = levelText,
                        onValueChange = {},
                        readOnly = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace, fontSize = 10.sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                    // Save to Sourcecode — POST to the local level_receiver.py
                    // (desktop loopback 127.0.0.1:8787; Android uses 10.0.2.2).
                    // Button only visible when the receiver answers GET /ping.
                    var receiverReachable by remember { mutableStateOf(false) }
                    var savedToSource by remember { mutableStateOf(false) }
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        launch(Dispatchers.IO) {
                            try {
                                val (code, _) = roboyard.logic.network.PlatformHttp.request(
                                    "GET", "http://127.0.0.1:8787/ping", emptyMap(), null
                                )
                                if (code == 200) receiverReachable = true
                            } catch (_: Exception) {}
                        }
                    }
                    if (receiverReachable) {
                        FancyButton(
                            text = if (savedToSource) "✓ Saved to Sourcecode"
                                else "💾 " + s("editor_save_to_sourcecode", "Save to Sourcecode"),
                            color = FancyButtonColor.BLUE,
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val escaped = levelText
                                            .replace("\\", "\\\\").replace("\"", "\\\"")
                                            .replace("\n", "\\n").replace("\r", "\\r")
                                            .replace("\t", "\\t")
                                        val body = "{\"level_id\":$currentLevelId,\"level_data\":\"$escaped\"}"
                                        val (code, resp) = roboyard.logic.network.PlatformHttp.request(
                                            "POST", "http://127.0.0.1:8787/save-level",
                                            mapOf("Content-Type" to "application/json"), body
                                        )
                                        launch(Dispatchers.Main) {
                                            if (code in 200..299) {
                                                savedToSource = true
                                                toast("Level $currentLevelId saved to sourcecode!")
                                            } else {
                                                toast("Save failed: $resp")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        launch(Dispatchers.Main) {
                                            toast("Connection failed. Is level_receiver.py running?")
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(40.dp)
                        )
                    }
                    FancyButton(
                        text = s("editor_copied_to_clipboard", "Copy to Clipboard"),
                        color = FancyButtonColor.BLUE,
                        onClick = {
                            clipboard.setText(AnnotatedString(levelText))
                            toast(s("editor_copied_to_clipboard", "Level data copied to clipboard"))
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(40.dp)
                    )
                    Text("Share Link:", color = Color.Gray, fontSize = 12.sp)
                    Text(
                        shareUrl, color = Color(0xFF0000FF), fontSize = 10.sp,
                        modifier = Modifier.clickable {
                            try { uriHandler.openUri(shareUrl) } catch (_: Exception) {}
                        }
                    )
                    TextField(
                        value = shareName,
                        onValueChange = { shareName = it },
                        placeholder = { Text("Enter your name (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // Android shareLevelOnline: logged in -> API share, else -> URL share
                    if (apiClient.isLoggedIn) {
                        toast(s("editor_sharing_to_account", "Sharing to your account..."))
                        apiClient.shareMap(levelText, shareName.trim(), object : RoboyardApiClient.ApiCallback<RoboyardApiClient.ShareResult?> {
                            override fun onSuccess(result: RoboyardApiClient.ShareResult?) {
                                if (result == null) return
                                toast(
                                    if (result.isDuplicate)
                                        s("editor_map_already_exists", "Map already exists")
                                    else
                                        s("share_success", "Map shared successfully")
                                )
                                result.shareUrl?.let {
                                    openAutoLoginUrl(apiClient.buildAutoLoginUrl(it))
                                }
                            }

                            override fun onError(error: String?) {
                                toast(s("share_failed", "Share failed: {0}").replace("{0}", error ?: ""))
                            }
                        })
                    } else {
                        var url = shareUrl
                        if (shareName.isNotBlank()) {
                            url += "&name=" + URLEncoder.encode(shareName.trim(), "UTF-8")
                        }
                        openAutoLoginUrl(apiClient.buildAutoLoginUrl(url))
                        toast(s("editor_opening_share_url", "Opening share URL in browser"))
                    }
                    showExportDialog = false
                }) { Text("Share Online") }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) { Text("Close") }
            }
        )
    }

    // ---- Import ASCII dialog (Android showImportAsciiMapDialog) ----
    if (showImportDialog) {
        var asciiText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(s("editor_import_ascii_map_title", "Import ASCII Map")) },
            text = {
                TextField(
                    value = asciiText,
                    onValueChange = { asciiText = it },
                    placeholder = { Text(s("editor_import_ascii_map_hint", "Paste ASCII map here")) },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace, fontSize = 10.sp
                    ),
                    modifier = Modifier.fillMaxWidth().height(240.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val trimmed = asciiText.trim()
                    if (trimmed.isEmpty()) {
                        toast("No input provided")
                    } else {
                        val gridElements = RRGetMap.parseAsciiMap(trimmed)
                        if (gridElements.isNullOrEmpty()) {
                            toast("Could not parse ASCII map")
                        } else {
                            val state = GameState.createFromGridElements(gridElements)
                            applyLoadedState(state, -1, state.levelName ?: "Imported Map")
                            toast("Imported ${state.width}x${state.height} map with ${state.gameElements.size} elements")
                        }
                    }
                    showImportDialog = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Toast overlay
    toastMessage?.let { msg ->
        androidx.compose.runtime.LaunchedEffect(msg) {
            kotlinx.coroutines.delay(2500)
            if (toastMessage == msg) toastMessage = null
        }
        Column(modifier = Modifier.fillMaxSize()) {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
            Text(
                text = msg, color = Color.White, fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color(0xDD333333), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private const val EDIT_MODE_ROBOT = 0
private const val EDIT_MODE_TARGET = 1
private const val EDIT_MODE_WALL_H = 2
private const val EDIT_MODE_WALL_V = 3
private const val EDIT_MODE_ERASE = 4

private fun createNewLevel(width: Int, height: Int): GameState {
    val state = GameState(width, height)
    state.createBorderWalls()
    state.createCenterCarree()
    return state
}

private fun colorChar(colorId: Int): Char = when (colorId) {
    -1 -> 'm'
    0 -> 'r'
    1 -> 'g'
    2 -> 'b'
    3 -> 'y'
    4 -> 's'
    else -> '?'
}

private fun colorName(colorIndex: Int): String = when (colorIndex) {
    Constants.COLOR_MULTI -> "Multi"
    Constants.COLOR_PINK -> "Red"
    Constants.COLOR_GREEN -> "Green"
    Constants.COLOR_BLUE -> "Blue"
    Constants.COLOR_YELLOW -> "Yellow"
    Constants.COLOR_SILVER -> "Silver"
    else -> "Unknown"
}

private fun robotColors(): List<Pair<Int, String>> = listOf(
    Constants.COLOR_PINK to "Red",
    Constants.COLOR_GREEN to "Green",
    Constants.COLOR_BLUE to "Blue",
    Constants.COLOR_YELLOW to "Yellow",
    Constants.COLOR_SILVER to "Silver"
)

private fun targetColors(): List<Pair<Int, String>> = robotColors() +
    (Constants.COLOR_MULTI to "Multi")

@Composable
private fun EditorDropdownRow(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (label.isNotEmpty()) Text(label, color = Color.White, fontSize = 14.sp)
        Box {
            Text(
                text = selected,
                color = Color.White, fontSize = 14.sp,
                modifier = Modifier
                    .background(Color(0xFF333333), RoundedCornerShape(4.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = {
                        expanded = false
                        onSelect(option)
                    })
                }
            }
        }
    }
}

@Composable
private fun EditorColorRow(
    colors: List<Pair<Int, String>>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        colors.forEach { (color, name) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onSelect(color) }
            ) {
                RadioButton(
                    selected = selected == color,
                    onClick = { onSelect(color) },
                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF4CAF50))
                )
                Text(name, color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

/**
 * Board preview canvas — port of Android GameBoardView (editor variant).
 * Draws grid tiles, grid lines, black wall bars, robot/target sprites and
 * converts taps into board cell clicks.
 */
@Composable
private fun EditorBoardCanvas(
    state: GameState?,
    redrawTick: Int,
    onCellClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridTile = imageResource(Res.drawable.grid_tiles)
    val robotSprites = listOf(
        imageResource(Res.drawable.robot_pink_right),
        imageResource(Res.drawable.robot_green_right),
        imageResource(Res.drawable.robot_blue_right),
        imageResource(Res.drawable.robot_yellow_right),
        imageResource(Res.drawable.robot_silver_right)
    )
    val targetSprites = listOf(
        imageResource(Res.drawable.target_pink),
        imageResource(Res.drawable.target_green),
        imageResource(Res.drawable.target_blue),
        imageResource(Res.drawable.target_yellow),
        imageResource(Res.drawable.target_silver)
    )
    val targetMulti = imageResource(Res.drawable.target_multi)

    Canvas(
        modifier = modifier.pointerInput(state?.width, state?.height) {
            detectTapGestures { offset ->
                val st = state ?: return@detectTapGestures
                val cellW = size.width / st.width
                val cellH = size.height / st.height
                val cell = min(cellW, cellH)
                val offX = (size.width - cell * st.width) / 2f
                val offY = (size.height - cell * st.height) / 2f
                val bx = ((offset.x - offX) / cell).toInt()
                val by = ((offset.y - offY) / cell).toInt()
                if (bx in 0 until st.width && by in 0 until st.height) {
                    onCellClick(bx, by)
                }
            }
        }
    ) {
        val st = state ?: return@Canvas
        @Suppress("UNUSED_EXPRESSION") redrawTick

        val boardW = st.width
        val boardH = st.height
        val cellW = size.width / boardW
        val cellH = size.height / boardH
        val cell = min(cellW, cellH)
        val offX = (size.width - cell * boardW) / 2f
        val offY = (size.height - cell * boardH) / 2f

        // Black background (matches Android drawColor(Color.BLACK))
        drawRect(Color.Black)

        // Grid cells with texture
        for (y in 0 until boardH) {
            for (x in 0 until boardW) {
                drawImage(
                    image = gridTile,
                    dstOffset = IntOffset((offX + x * cell).toInt(), (offY + y * cell).toInt()),
                    dstSize = IntSize(cell.toInt().coerceAtLeast(1), cell.toInt().coerceAtLeast(1))
                )
            }
        }

        // Grid lines (dark gray)
        val gridColor = Color.DarkGray
        for (x in 0..boardW) {
            drawLine(gridColor, Offset(offX + x * cell, offY), Offset(offX + x * cell, offY + boardH * cell), 1f)
        }
        for (y in 0..boardH) {
            drawLine(gridColor, Offset(offX, offY + y * cell), Offset(offX + boardW * cell, offY + y * cell), 1f)
        }

        // Walls as black bars (matches Android editor rendering)
        for (e in st.gameElements) {
            when (e.type) {
                GameElement.TYPE_HORIZONTAL_WALL -> drawRect(
                    Color.Black,
                    topLeft = Offset(offX + e.x * cell, offY + e.y * cell - cell / 8),
                    size = Size(cell, cell / 4)
                )
                GameElement.TYPE_VERTICAL_WALL -> drawRect(
                    Color.Black,
                    topLeft = Offset(offX + e.x * cell - cell / 8, offY + e.y * cell),
                    size = Size(cell / 4, cell)
                )
            }
        }

        // Robots and targets (same sprites as the game board)
        for (e in st.gameElements) {
            val left = offX + e.x * cell
            val top = offY + e.y * cell
            val dstOffset = IntOffset(left.toInt(), top.toInt())
            val dstSize = IntSize(cell.toInt().coerceAtLeast(1), cell.toInt().coerceAtLeast(1))
            when (e.type) {
                GameElement.TYPE_ROBOT -> drawImage(
                    image = robotSprites.getOrElse(e.color) { robotSprites[0] },
                    dstOffset = dstOffset, dstSize = dstSize
                )
                GameElement.TYPE_TARGET -> drawImage(
                    image = if (e.color == Constants.COLOR_MULTI) targetMulti
                    else targetSprites.getOrElse(e.color) { targetSprites[0] },
                    dstOffset = dstOffset, dstSize = dstSize
                )
            }
        }
    }
}
