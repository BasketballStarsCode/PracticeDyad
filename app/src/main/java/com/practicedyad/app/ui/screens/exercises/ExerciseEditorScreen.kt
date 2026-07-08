package com.practicedyad.app.ui.screens.exercises

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.practicedyad.app.data.model.ExerciseTemplate
import com.practicedyad.app.ui.components.*
import com.practicedyad.app.ui.theme.*
import com.practicedyad.app.viewmodel.AuthViewModel
import com.practicedyad.app.viewmodel.ExerciseViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import kotlin.math.*

// ─── Stick Figure Data ────────────────────────────────────────────────────────

data class JointPos(val x: Float, val y: Float) {
    operator fun plus(o: JointPos) = JointPos(x + o.x, y + o.y)
    operator fun minus(o: JointPos) = JointPos(x - o.x, y - o.y)
    fun distanceTo(o: JointPos) = sqrt((x - o.x).pow(2) + (y - o.y).pow(2))
    fun toOffset() = Offset(x, y)
    fun lerp(to: JointPos, t: Float) = JointPos(x + (to.x - x) * t, y + (to.y - y) * t)
}

data class StickFigure(
    val center: JointPos = JointPos(180f, 250f),
    val head: JointPos = JointPos(180f, 58f),
    val neck: JointPos = JointPos(180f, 110f),
    val belly: JointPos = JointPos(180f, 223f),
    val pelvis: JointPos = JointPos(180f, 283f),
    val lShoulder: JointPos = JointPos(128f, 133f),
    val lElbow: JointPos = JointPos(83f, 193f),
    val lHand: JointPos = JointPos(48f, 238f),
    val rShoulder: JointPos = JointPos(232f, 133f),
    val rElbow: JointPos = JointPos(277f, 193f),
    val rHand: JointPos = JointPos(312f, 238f),
    val lHip: JointPos = JointPos(147f, 283f),
    val lKnee: JointPos = JointPos(128f, 380f),
    val lFoot: JointPos = JointPos(110f, 455f),
    val rHip: JointPos = JointPos(213f, 283f),
    val rKnee: JointPos = JointPos(232f, 380f),
    val rFoot: JointPos = JointPos(250f, 455f),
    val scale: Float = 1f,
    val rotation: Float = 0f
)

data class DrawingPath(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float
)

data class DrawingShape(
    val type: ShapeType, val topLeft: Offset, val size: androidx.compose.ui.geometry.Size,
    val color: Color, val selected: Boolean = false
)

enum class ShapeType { CIRCLE, RECTANGLE }
enum class DrawingTool { NONE, PEN, CIRCLE, RECTANGLE }
enum class EditorMode { FIGURE, UPLOAD, BLANK }

val figureDrawColors = listOf(
    Color.Black, Color(0xFF555555), Color(0xFF999999), Color(0xFFCCCCCC),
    TealPrimary, TealDark, TealLight, TealUltraLight,
    Color.Red, Color(0xFFFF4444), Color(0xFFFF8888), Color(0xFFFFCCCC)
)

@Composable
fun ExerciseEditorScreen(
    navController: NavController,
    templateId: String,
    vm: ExerciseViewModel = hiltViewModel(),
    authVm: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser by authVm.currentUser.collectAsStateWithLifecycle()

    var nameDE by remember { mutableStateOf("") }
    var nameEN by remember { mutableStateOf("") }
    var descDE by remember { mutableStateOf("") }
    var descEN by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }

    var editorMode by remember { mutableStateOf(EditorMode.FIGURE) }
    var activeTool by remember { mutableStateOf(DrawingTool.NONE) }
    var selectedColor by remember { mutableStateOf(Color.Black) }
    var strokeWidth by remember { mutableStateOf(4f) }

    // Stick figure state
    var figure by remember { mutableStateOf(StickFigure()) }

    // Drawing state
    val paths = remember { mutableStateListOf<DrawingPath>() }
    val shapes = remember { mutableStateListOf<DrawingShape>() }
    var currentPath by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var shapeStart by remember { mutableStateOf<Offset?>(null) }
    var shapeEnd by remember { mutableStateOf<Offset?>(null) }

    // Existing remote photo URLs (already uploaded) + new local URIs
    val existingPhotoUrls = remember { mutableStateListOf<String>() }
    val uploadedImageUris = remember { mutableStateListOf<Uri>() }
    val uploadedVideoUris = remember { mutableStateListOf<Uri>() }

    // Image annotation state
    var annotatingIndex by remember { mutableIntStateOf(-1) } // -1 = not annotating; >=existingPhotoUrls.size = local URI
    val annotationPaths = remember { mutableStateListOf<DrawingPath>() }
    val annotationShapes = remember { mutableStateListOf<DrawingShape>() }
    var annotationCurrentPath by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var annotationShapeStart by remember { mutableStateOf<Offset?>(null) }
    var annotationShapeEnd by remember { mutableStateOf<Offset?>(null) }
    var annotationTool by remember { mutableStateOf(DrawingTool.NONE) }
    var annotationColor by remember { mutableStateOf(Color.Red) }
    var annotationStroke by remember { mutableStateOf(4f) }

    // Org save option
    val hasOrg = currentUser?.organizationId?.isNotEmpty() == true
    var saveToOrg by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { uploadedImageUris.add(it) } }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { uploadedVideoUris.add(it) } }

    val loading by vm.loading.collectAsStateWithLifecycle()
    val selectedTemplate by vm.selectedTemplate.collectAsStateWithLifecycle()

    LaunchedEffect(templateId) {
        if (templateId.isNotEmpty()) {
            vm.loadExercises()
            vm.loadTemplate(templateId)
        }
    }
    LaunchedEffect(selectedTemplate) {
        selectedTemplate?.let { t ->
            nameDE = t.nameDE
            nameEN = t.nameEN
            descDE = t.descriptionDE
            descEN = t.descriptionEN
            category = t.category
            // Load existing photos and switch to upload mode if any exist
            if (t.photoUrls.isNotEmpty()) {
                existingPhotoUrls.clear()
                existingPhotoUrls.addAll(t.photoUrls)
                editorMode = EditorMode.UPLOAD
            }
        }
    }

    // If annotating, show full annotation UI instead of main editor
    if (annotatingIndex >= 0) {
        val isExisting = annotatingIndex < existingPhotoUrls.size
        val imageSource: Any = if (isExisting) existingPhotoUrls[annotatingIndex]
                               else uploadedImageUris[annotatingIndex - existingPhotoUrls.size]
        Scaffold(
            topBar = {
                PDTopBar(
                    title = "Bild annotieren",
                    onBack = {
                        annotatingIndex = -1
                        annotationPaths.clear(); annotationShapes.clear()
                    }
                )
            },
            bottomBar = {
                PDButton(
                    text = "Annotierung übernehmen",
                    modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(),
                    onClick = {
                        scope.launch {
                            val uri = compositeAnnotation(
                                context, imageSource,
                                annotationPaths.toList(), annotationShapes.toList()
                            )
                            if (uri != null) {
                                // Remove original, add annotated version
                                if (isExisting) existingPhotoUrls.removeAt(annotatingIndex)
                                else uploadedImageUris.removeAt(annotatingIndex - existingPhotoUrls.size)
                                uploadedImageUris.add(uri)
                            }
                            annotatingIndex = -1
                            annotationPaths.clear(); annotationShapes.clear()
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ImageAnnotationCanvas(
                    imageSource = imageSource,
                    paths = annotationPaths,
                    shapes = annotationShapes,
                    currentPath = annotationCurrentPath,
                    onCurrentPathChange = { annotationCurrentPath = it },
                    shapeStart = annotationShapeStart,
                    shapeEnd = annotationShapeEnd,
                    onShapeStartChange = { annotationShapeStart = it },
                    onShapeEndChange = { annotationShapeEnd = it },
                    activeTool = annotationTool,
                    selectedColor = annotationColor,
                    strokeWidth = annotationStroke,
                    onAddPath = { annotationPaths.add(it) },
                    onAddShape = { annotationShapes.add(it) }
                )
                DrawingToolbar(
                    activeTool = annotationTool,
                    selectedColor = annotationColor,
                    strokeWidth = annotationStroke,
                    onToolChange = { annotationTool = it },
                    onColorChange = { annotationColor = it },
                    onStrokeChange = { annotationStroke = it },
                    onUndo = { if (annotationPaths.isNotEmpty()) annotationPaths.removeLast()
                               else if (annotationShapes.isNotEmpty()) annotationShapes.removeLast() },
                    onClear = { annotationPaths.clear(); annotationShapes.clear() }
                )
                Spacer(Modifier.height(80.dp))
            }
        }
        return
    }

    Scaffold(
        topBar = {
            PDTopBar(
                title = if (templateId.isEmpty()) "Übung erstellen" else "Übung bearbeiten",
                onBack = { navController.popBackStack() }
            )
        },
        bottomBar = {
            Column(modifier = Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
                if (hasOrg) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("In Organisationsdatenbank speichern",
                            style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = saveToOrg,
                            onCheckedChange = { saveToOrg = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = TealPrimary, checkedTrackColor = TealPrimary.copy(alpha = 0.5f))
                        )
                    }
                }
                PDButton(
                    text = "Speichern",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        vm.saveExercise(
                            template = ExerciseTemplate(
                                id = templateId.ifEmpty { UUID.randomUUID().toString() },
                                nameDE = nameDE, nameEN = nameEN,
                                descriptionDE = descDE, descriptionEN = descEN,
                                category = category, isCustom = true,
                                organizationId = if (saveToOrg) currentUser?.organizationId ?: "" else ""
                            ),
                            imageUris = uploadedImageUris.toList(),
                            keepUrls = existingPhotoUrls.toList(),
                            context = context
                        ) { navController.popBackStack() }
                    },
                    enabled = nameDE.isNotBlank()
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Basic info
            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PDTextField(nameDE, { nameDE = it }, "Name (Deutsch)")
                PDTextField(nameEN, { nameEN = it }, "Name (English)")
                PDTextField(category, { category = it }, "Kategorie (z.B. Beine, Rücken)")
                PDTextField(descDE, { descDE = it }, "Beschreibung (Deutsch)", singleLine = false, maxLines = 4)
                PDTextField(descEN, { descEN = it }, "Description (English)", singleLine = false, maxLines = 4)
            }

            PDDivider()

            // Editor mode tabs
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                PDSectionHeader("Übungsbild (Übungseditor)")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PDChip("Gelenkfigur", editorMode == EditorMode.FIGURE) { editorMode = EditorMode.FIGURE }
                    PDChip("Datei hochladen", editorMode == EditorMode.UPLOAD) { editorMode = EditorMode.UPLOAD }
                    PDChip("Freie Fläche", editorMode == EditorMode.BLANK) { editorMode = EditorMode.BLANK }
                }
            }

            when (editorMode) {
                EditorMode.FIGURE -> {
                    StickFigureEditor(
                        figure = figure,
                        onFigureChange = { figure = it },
                        paths = paths,
                        shapes = shapes,
                        currentPath = currentPath,
                        onCurrentPathChange = { currentPath = it },
                        shapeStart = shapeStart,
                        shapeEnd = shapeEnd,
                        onShapeStartChange = { shapeStart = it },
                        onShapeEndChange = { shapeEnd = it },
                        activeTool = activeTool,
                        selectedColor = selectedColor,
                        strokeWidth = strokeWidth,
                        onAddPath = { paths.add(it) },
                        onAddShape = { shapes.add(it) }
                    )
                    Text(
                        "Gelenk antippen → auswählen · im Kreis ziehen → bewegen · außerhalb ziehen → Figur verschieben",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    DrawingToolbar(
                        activeTool = activeTool,
                        selectedColor = selectedColor,
                        strokeWidth = strokeWidth,
                        onToolChange = { activeTool = it },
                        onColorChange = { selectedColor = it },
                        onStrokeChange = { strokeWidth = it },
                        onUndo = { if (paths.isNotEmpty()) paths.removeLast() else if (shapes.isNotEmpty()) shapes.removeLast() },
                        onClear = { paths.clear(); shapes.clear() },
                        onInsertShape = { shapes.add(it) }
                    )
                    // Figure controls
                    FigureControls(
                        figure = figure,
                        onFigureChange = { figure = it }
                    )
                }
                EditorMode.UPLOAD -> {
                    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Images
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PDButton("Bild hinzufügen", onClick = { imagePickerLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f), variant = ButtonVariant.SECONDARY)
                            PDButton("Video hinzufügen", onClick = { videoPickerLauncher.launch("video/*") },
                                modifier = Modifier.weight(1f), variant = ButtonVariant.SECONDARY)
                        }
                        // Existing remote photos
                        existingPhotoUrls.toList().forEachIndexed { idx, url ->
                            Box {
                                coil.compose.AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxWidth().height(220.dp)
                                        .background(Color.White, RoundedCornerShape(12.dp))
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Fit
                                )
                                // Annotate button
                                IconButton(
                                    onClick = { annotatingIndex = idx },
                                    modifier = Modifier.align(Alignment.BottomStart)
                                        .background(TealPrimary.copy(alpha = 0.85f), RoundedCornerShape(topEnd = 10.dp))
                                ) {
                                    Icon(Icons.Default.Edit, "Annotieren", tint = Color.White)
                                }
                                IconButton(
                                    onClick = { existingPhotoUrls.removeAt(idx) },
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Icon(Icons.Default.Close, "Bild entfernen", tint = Color.White)
                                }
                            }
                        }
                        // Newly added local photos
                        uploadedImageUris.forEachIndexed { idx, uri ->
                            Box {
                                coil.compose.AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxWidth().height(220.dp)
                                        .background(Color.White, RoundedCornerShape(12.dp))
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Fit
                                )
                                // Annotate button
                                IconButton(
                                    onClick = { annotatingIndex = existingPhotoUrls.size + idx },
                                    modifier = Modifier.align(Alignment.BottomStart)
                                        .background(TealPrimary.copy(alpha = 0.85f), RoundedCornerShape(topEnd = 10.dp))
                                ) {
                                    Icon(Icons.Default.Edit, "Annotieren", tint = Color.White)
                                }
                                IconButton(
                                    onClick = { uploadedImageUris.removeAt(idx) },
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Icon(Icons.Default.Close, "Bild entfernen", tint = Color.White)
                                }
                            }
                        }
                        // Videos (show filename + delete)
                        uploadedVideoUris.forEachIndexed { idx, uri ->
                            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.VideoLibrary, null, tint = TealPrimary)
                                    Spacer(Modifier.width(8.dp))
                                    Text(uri.lastPathSegment ?: "Video ${idx + 1}",
                                        modifier = Modifier.weight(1f), maxLines = 1)
                                    IconButton(onClick = { uploadedVideoUris.removeAt(idx) }) {
                                        Icon(Icons.Default.Close, null,
                                            tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                        DrawingToolbar(
                            activeTool = activeTool, selectedColor = selectedColor, strokeWidth = strokeWidth,
                            onToolChange = { activeTool = it }, onColorChange = { selectedColor = it },
                            onStrokeChange = { strokeWidth = it },
                            onUndo = { if (paths.isNotEmpty()) paths.removeLast() },
                            onClear = { paths.clear() }
                        )
                    }
                }
                EditorMode.BLANK -> {
                    BlankCanvas(
                        paths = paths, shapes = shapes, currentPath = currentPath,
                        onCurrentPathChange = { currentPath = it },
                        shapeStart = shapeStart, shapeEnd = shapeEnd,
                        onShapeStartChange = { shapeStart = it }, onShapeEndChange = { shapeEnd = it },
                        activeTool = activeTool, selectedColor = selectedColor, strokeWidth = strokeWidth,
                        onAddPath = { paths.add(it) }, onAddShape = { shapes.add(it) }
                    )
                    DrawingToolbar(
                        activeTool = activeTool, selectedColor = selectedColor, strokeWidth = strokeWidth,
                        onToolChange = { activeTool = it }, onColorChange = { selectedColor = it },
                        onStrokeChange = { strokeWidth = it },
                        onUndo = { if (paths.isNotEmpty()) paths.removeLast() else if (shapes.isNotEmpty()) shapes.removeLast() },
                        onClear = { paths.clear(); shapes.clear() },
                        onInsertShape = { shapes.add(it) }
                    )
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }

    if (loading) PDLoadingOverlay()
}

@Composable
fun StickFigureEditor(
    figure: StickFigure,
    onFigureChange: (StickFigure) -> Unit,
    paths: List<DrawingPath>,
    shapes: List<DrawingShape>,
    currentPath: List<Offset>,
    onCurrentPathChange: (List<Offset>) -> Unit,
    shapeStart: Offset?,
    shapeEnd: Offset?,
    onShapeStartChange: (Offset?) -> Unit,
    onShapeEndChange: (Offset?) -> Unit,
    activeTool: DrawingTool,
    selectedColor: Color,
    strokeWidth: Float,
    onAddPath: (DrawingPath) -> Unit,
    onAddShape: (DrawingShape) -> Unit
) {
    // Magic Poser interaction: tap joint to select, drag selected ring to move joint,
    // drag anywhere else to move whole figure
    var selectedJoint by remember { mutableStateOf<String?>(null) }
    val jointSelectRadius = 36f   // tap radius to select a joint
    val jointControlRadius = 60f  // radius of the control ring around selected joint

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(520.dp)
            .padding(horizontal = 16.dp)
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(activeTool) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val pos = down.position

                        if (activeTool == DrawingTool.NONE) {
                            val currentJoints = buildJointMap(figure)

                            // Is the touch inside the selected joint's control ring?
                            val selJointPos = selectedJoint?.let { currentJoints[it] }
                            val insideControlRing = selJointPos != null &&
                                (pos - selJointPos.toOffset()).getDistance() <= jointControlRadius

                            // Which joint (if any) was tapped?
                            val tappedJoint = currentJoints.entries
                                .minByOrNull { (_, p) -> (pos - p.toOffset()).getDistance() }
                                ?.takeIf { (_, p) -> (pos - p.toOffset()).getDistance() < jointSelectRadius }
                                ?.key

                            var prevPos = pos
                            var hasDragged = false

                            var event = awaitPointerEvent()
                            while (event.changes.any { it.pressed }) {
                                val change = event.changes.first()
                                val delta = change.position - prevPos
                                if (delta.getDistance() > 3f) hasDragged = true

                                when {
                                    // Drag selected joint via its control ring
                                    insideControlRing && selectedJoint != null -> {
                                        onFigureChange(updateJoint(figure, selectedJoint!!, JointPos(change.position.x, change.position.y)))
                                    }
                                    // Drag anywhere else → move whole figure
                                    !insideControlRing -> {
                                        onFigureChange(moveFigure(figure, delta.x, delta.y))
                                    }
                                }
                                prevPos = change.position
                                change.consume()
                                event = awaitPointerEvent()
                            }

                            // Tap (no drag): select/deselect joint
                            if (!hasDragged) {
                                selectedJoint = if (tappedJoint != null && tappedJoint != selectedJoint) {
                                    tappedJoint
                                } else {
                                    null // deselect on tap outside or re-tap same joint
                                }
                            }
                        } else if (activeTool == DrawingTool.PEN) {
                            val newPoints = mutableListOf(pos)
                            onCurrentPathChange(newPoints.toList())
                            var event = awaitPointerEvent()
                            while (event.changes.any { it.pressed }) {
                                val change = event.changes.first()
                                newPoints.add(change.position)
                                onCurrentPathChange(newPoints.toList())
                                change.consume()
                                event = awaitPointerEvent()
                            }
                            if (newPoints.size >= 2) {
                                onAddPath(DrawingPath(newPoints.toList(), selectedColor, strokeWidth))
                            }
                            onCurrentPathChange(emptyList())
                        } else {
                            onShapeStartChange(pos)
                            var lastPos = pos
                            var event = awaitPointerEvent()
                            while (event.changes.any { it.pressed }) {
                                val change = event.changes.first()
                                lastPos = change.position
                                onShapeEndChange(lastPos)
                                change.consume()
                                event = awaitPointerEvent()
                            }
                            val tl = Offset(minOf(pos.x, lastPos.x), minOf(pos.y, lastPos.y))
                            val sz = androidx.compose.ui.geometry.Size(
                                abs(lastPos.x - pos.x), abs(lastPos.y - pos.y)
                            )
                            if (sz.width > 5f || sz.height > 5f) {
                                onAddShape(DrawingShape(
                                    if (activeTool == DrawingTool.CIRCLE) ShapeType.CIRCLE else ShapeType.RECTANGLE,
                                    tl, sz, selectedColor
                                ))
                            }
                            onShapeStartChange(null); onShapeEndChange(null)
                        }
                    }
                }
        ) {
            // Draw shapes
            shapes.forEach { s ->
                when (s.type) {
                    ShapeType.CIRCLE -> drawOval(s.color, topLeft = s.topLeft, size = s.size, style = Stroke(3f))
                    ShapeType.RECTANGLE -> drawRect(s.color, topLeft = s.topLeft, size = s.size, style = Stroke(3f))
                }
            }

            // Draw paths
            paths.forEach { p ->
                if (p.points.size >= 2) {
                    val path = Path().apply {
                        moveTo(p.points.first().x, p.points.first().y)
                        p.points.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(path, p.color, style = Stroke(p.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }

            // Current drawing path
            if (currentPath.size >= 2) {
                val path = Path().apply {
                    moveTo(currentPath.first().x, currentPath.first().y)
                    currentPath.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(path, selectedColor, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }

            // Live shape preview while dragging
            val start = shapeStart
            val end = shapeEnd
            if (start != null && end != null && (activeTool == DrawingTool.CIRCLE || activeTool == DrawingTool.RECTANGLE)) {
                val tl = Offset(minOf(start.x, end.x), minOf(start.y, end.y))
                val sz = androidx.compose.ui.geometry.Size(abs(end.x - start.x), abs(end.y - start.y))
                if (activeTool == DrawingTool.CIRCLE)
                    drawOval(selectedColor, topLeft = tl, size = sz, style = Stroke(3f))
                else
                    drawRect(selectedColor, topLeft = tl, size = sz, style = Stroke(3f))
            }

            // Draw stick figure
            drawStickFigure(figure, selectedJoint)

            // Draw control ring around selected joint (Magic Poser style)
            val selPos = selectedJoint?.let { buildJointMap(figure)[it] }
            if (selPos != null) {
                // Outer faint circle — drag zone indicator
                drawCircle(
                    TealPrimary.copy(alpha = 0.18f),
                    jointControlRadius,
                    selPos.toOffset()
                )
                // Ring border
                drawCircle(
                    TealPrimary,
                    jointControlRadius,
                    selPos.toOffset(),
                    style = Stroke(2.5f)
                )
                // Cross-hair arrows inside ring to indicate drag direction
                val r = jointControlRadius * 0.55f
                val cx = selPos.x; val cy = selPos.y
                val arrowColor = TealPrimary.copy(alpha = 0.85f)
                val ah = 7f
                // horizontal arrow
                drawLine(arrowColor, Offset(cx - r, cy), Offset(cx + r, cy), 2.5f)
                drawLine(arrowColor, Offset(cx + r, cy), Offset(cx + r - ah, cy - ah * 0.6f), 2.5f)
                drawLine(arrowColor, Offset(cx + r, cy), Offset(cx + r - ah, cy + ah * 0.6f), 2.5f)
                drawLine(arrowColor, Offset(cx - r, cy), Offset(cx - r + ah, cy - ah * 0.6f), 2.5f)
                drawLine(arrowColor, Offset(cx - r, cy), Offset(cx - r + ah, cy + ah * 0.6f), 2.5f)
                // vertical arrow
                drawLine(arrowColor, Offset(cx, cy - r), Offset(cx, cy + r), 2.5f)
                drawLine(arrowColor, Offset(cx, cy - r), Offset(cx - ah * 0.6f, cy - r + ah), 2.5f)
                drawLine(arrowColor, Offset(cx, cy - r), Offset(cx + ah * 0.6f, cy - r + ah), 2.5f)
                drawLine(arrowColor, Offset(cx, cy + r), Offset(cx - ah * 0.6f, cy + r - ah), 2.5f)
                drawLine(arrowColor, Offset(cx, cy + r), Offset(cx + ah * 0.6f, cy + r - ah), 2.5f)
            }
        }
    }
}

fun DrawScope.drawStickFigure(f: StickFigure, selectedJoint: String? = null) {
    val black = Color.Black
    val sw = 3f
    val pivot = Offset(
        (f.lShoulder.x + f.rShoulder.x) / 2f,
        (f.head.y + f.lFoot.y) / 2f
    )
    withTransform({
        scale(f.scale, f.scale, pivot)
        rotate(f.rotation, pivot)
    }) {
    // Head
    drawOval(black, topLeft = Offset(f.head.x - 14f, f.head.y - 22f),
        size = androidx.compose.ui.geometry.Size(28f, 38f), style = Stroke(sw))

    // Upper body (neck to belly)
    drawLine(black, f.neck.toOffset(), f.belly.toOffset(), sw)
    // Lower body (belly to pelvis)
    drawLine(black, f.belly.toOffset(), f.pelvis.toOffset(), sw)

    // Left arm
    drawLine(black, f.lShoulder.toOffset(), f.lElbow.toOffset(), sw)
    drawLine(black, f.lElbow.toOffset(), f.lHand.toOffset(), sw)
    // Left hand (triangle)
    drawHandTriangle(f.lHand, f.lElbow, black, sw)

    // Right arm
    drawLine(black, f.rShoulder.toOffset(), f.rElbow.toOffset(), sw)
    drawLine(black, f.rElbow.toOffset(), f.rHand.toOffset(), sw)
    drawHandTriangle(f.rHand, f.rElbow, black, sw)

    // Shoulders connector
    drawLine(black, f.lShoulder.toOffset(), f.rShoulder.toOffset(), sw)

    // Left leg
    drawLine(black, f.lHip.toOffset(), f.lKnee.toOffset(), sw)
    drawLine(black, f.lKnee.toOffset(), f.lFoot.toOffset(), sw)
    drawFootTriangle(f.lFoot, f.lKnee, black, sw)

    // Right leg
    drawLine(black, f.rHip.toOffset(), f.rKnee.toOffset(), sw)
    drawLine(black, f.rKnee.toOffset(), f.rFoot.toOffset(), sw)
    drawFootTriangle(f.rFoot, f.rKnee, black, sw)

    // Hip connector
    drawLine(black, f.lHip.toOffset(), f.rHip.toOffset(), sw)
    drawLine(black, f.pelvis.toOffset(), Offset((f.lHip.x + f.rHip.x) / 2, (f.lHip.y + f.rHip.y) / 2), sw)

    // Draw joints (interactive circles — Magic Poser style)
    val joints = buildJointMap(f)
    joints.entries.forEach { (name, jp) ->
        val isSelected = name == selectedJoint
        if (isSelected) {
            // Selected: larger, brighter highlight
            drawCircle(TealPrimary, 15f, jp.toOffset())
            drawCircle(Color.White, 10f, jp.toOffset())
            drawCircle(TealPrimary, 10f, jp.toOffset(), style = Stroke(2.5f))
        } else {
            drawCircle(TealPrimary, 11f, jp.toOffset())
            drawCircle(Color.White, 7f, jp.toOffset())
            drawCircle(TealPrimary, 7f, jp.toOffset(), style = Stroke(2f))
        }
    }
    } // end withTransform
}

fun DrawScope.drawHandTriangle(hand: JointPos, elbow: JointPos, color: Color, sw: Float) {
    val dir = JointPos(hand.x - elbow.x, hand.y - elbow.y)
    val len = sqrt(dir.x.pow(2) + dir.y.pow(2)).coerceAtLeast(0.001f)
    val norm = JointPos(dir.x / len, dir.y / len)
    val perp = JointPos(-norm.y * 5f, norm.x * 5f)
    val tip = JointPos(hand.x + norm.x * 10f, hand.y + norm.y * 10f)
    val p = Path()
    p.moveTo(tip.x, tip.y)
    p.lineTo(hand.x + perp.x, hand.y + perp.y)
    p.lineTo(hand.x - perp.x, hand.y - perp.y)
    p.close()
    drawPath(p, color, style = Stroke(sw))
}

fun DrawScope.drawFootTriangle(foot: JointPos, knee: JointPos, color: Color, sw: Float) {
    val perp = JointPos(10f, 0f) // feet pointing outward
    val tip = JointPos(foot.x + perp.x, foot.y + 5f)
    val p = Path()
    p.moveTo(foot.x - 8f, foot.y)
    p.lineTo(tip.x + 8f, foot.y)
    p.lineTo(foot.x, foot.y - 6f)
    p.close()
    drawPath(p, color, style = Stroke(sw))
}

fun buildJointMap(f: StickFigure): Map<String, JointPos> = mapOf(
    "head" to f.head,
    "neck" to f.neck, "belly" to f.belly, "pelvis" to f.pelvis,
    "lShoulder" to f.lShoulder, "lElbow" to f.lElbow, "lHand" to f.lHand,
    "rShoulder" to f.rShoulder, "rElbow" to f.rElbow, "rHand" to f.rHand,
    "lHip" to f.lHip, "lKnee" to f.lKnee, "lFoot" to f.lFoot,
    "rHip" to f.rHip, "rKnee" to f.rKnee, "rFoot" to f.rFoot
)

fun moveFigure(f: StickFigure, dx: Float, dy: Float): StickFigure {
    fun JointPos.move() = JointPos(x + dx, y + dy)
    return f.copy(
        center = f.center.move(), head = f.head.move(), neck = f.neck.move(),
        belly = f.belly.move(), pelvis = f.pelvis.move(),
        lShoulder = f.lShoulder.move(), lElbow = f.lElbow.move(), lHand = f.lHand.move(),
        rShoulder = f.rShoulder.move(), rElbow = f.rElbow.move(), rHand = f.rHand.move(),
        lHip = f.lHip.move(), lKnee = f.lKnee.move(), lFoot = f.lFoot.move(),
        rHip = f.rHip.move(), rKnee = f.rKnee.move(), rFoot = f.rFoot.move()
    )
}

fun updateJoint(f: StickFigure, joint: String, pos: JointPos): StickFigure = when (joint) {
    "head" -> f.copy(head = pos)
    "neck" -> f.copy(neck = pos)
    "belly" -> f.copy(belly = pos)
    "pelvis" -> f.copy(pelvis = pos)
    "lShoulder" -> f.copy(lShoulder = pos)
    "lElbow" -> f.copy(lElbow = pos)
    "lHand" -> f.copy(lHand = pos)
    "rShoulder" -> f.copy(rShoulder = pos)
    "rElbow" -> f.copy(rElbow = pos)
    "rHand" -> f.copy(rHand = pos)
    "lHip" -> f.copy(lHip = pos)
    "lKnee" -> f.copy(lKnee = pos)
    "lFoot" -> f.copy(lFoot = pos)
    "rHip" -> f.copy(rHip = pos)
    "rKnee" -> f.copy(rKnee = pos)
    "rFoot" -> f.copy(rFoot = pos)
    else -> f
}

@Composable
fun FigureControls(figure: StickFigure, onFigureChange: (StickFigure) -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Größe", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = figure.scale,
            onValueChange = { onFigureChange(figure.copy(scale = it)) },
            valueRange = 0.5f..2f,
            colors = SliderDefaults.colors(activeTrackColor = TealPrimary, thumbColor = TealPrimary)
        )
        Text("Drehung", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = figure.rotation,
            onValueChange = { onFigureChange(figure.copy(rotation = it)) },
            valueRange = -180f..180f,
            colors = SliderDefaults.colors(activeTrackColor = TealPrimary, thumbColor = TealPrimary)
        )
    }
}

@Composable
fun DrawingToolbar(
    activeTool: DrawingTool,
    selectedColor: Color,
    strokeWidth: Float,
    onToolChange: (DrawingTool) -> Unit,
    onColorChange: (Color) -> Unit,
    onStrokeChange: (Float) -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onInsertShape: ((DrawingShape) -> Unit)? = null
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Tools
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                DrawingTool.NONE to Icons.Default.TouchApp,
                DrawingTool.PEN to Icons.Default.Edit,
                DrawingTool.CIRCLE to Icons.Default.RadioButtonUnchecked,
                DrawingTool.RECTANGLE to Icons.Default.CheckBoxOutlineBlank
            ).forEach { (tool, icon) ->
                IconButton(
                    onClick = { onToolChange(if (activeTool == tool) DrawingTool.NONE else tool) },
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (activeTool == tool) TealPrimary.copy(alpha = 0.2f) else Color.Transparent,
                            CircleShape
                        )
                ) { Icon(icon, null, tint = if (activeTool == tool) TealPrimary else MaterialTheme.colorScheme.onSurface) }
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onUndo) { Icon(Icons.Default.Undo, "Rückgängig") }
            IconButton(onClick = onClear) { Icon(Icons.Default.DeleteForever, "Alles löschen", tint = MaterialTheme.colorScheme.error) }
        }

        // Stroke width
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Stärke", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(40.dp))
            Slider(
                value = strokeWidth, onValueChange = onStrokeChange,
                valueRange = 1f..12f, modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(activeTrackColor = TealPrimary, thumbColor = TealPrimary)
            )
        }

        // Colors
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            figureDrawColors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            if (selectedColor == color) 3.dp else 0.dp,
                            Color.White, CircleShape
                        )
                        .border(
                            if (selectedColor == color) 2.dp else 1.dp,
                            if (selectedColor == color) TealPrimary else Color.Gray.copy(alpha = 0.5f), CircleShape
                        )
                        .clickable { onColorChange(color) }
                )
            }
        }
    }

}

@Composable
fun BlankCanvas(
    paths: List<DrawingPath>,
    shapes: List<DrawingShape>,
    currentPath: List<Offset>,
    onCurrentPathChange: (List<Offset>) -> Unit,
    shapeStart: Offset?,
    shapeEnd: Offset?,
    onShapeStartChange: (Offset?) -> Unit,
    onShapeEndChange: (Offset?) -> Unit,
    activeTool: DrawingTool,
    selectedColor: Color,
    strokeWidth: Float,
    onAddPath: (DrawingPath) -> Unit,
    onAddShape: (DrawingShape) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .padding(horizontal = 16.dp)
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize().pointerInput(activeTool) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val pos = down.position
                    if (activeTool == DrawingTool.PEN) {
                        val pts = mutableListOf(pos)
                        onCurrentPathChange(pts.toList())
                        var ev = awaitPointerEvent()
                        while (ev.changes.any { it.pressed }) {
                            val c = ev.changes.first()
                            pts.add(c.position); onCurrentPathChange(pts.toList()); c.consume()
                            ev = awaitPointerEvent()
                        }
                        if (pts.size >= 2) onAddPath(DrawingPath(pts.toList(), selectedColor, strokeWidth))
                        onCurrentPathChange(emptyList())
                    } else if (activeTool == DrawingTool.CIRCLE || activeTool == DrawingTool.RECTANGLE) {
                        onShapeStartChange(pos)
                        var lastPos = pos
                        var ev = awaitPointerEvent()
                        while (ev.changes.any { it.pressed }) {
                            val c = ev.changes.first()
                            lastPos = c.position
                            onShapeEndChange(lastPos)
                            c.consume()
                            ev = awaitPointerEvent()
                        }
                        val tl = Offset(minOf(pos.x, lastPos.x), minOf(pos.y, lastPos.y))
                        val sz = androidx.compose.ui.geometry.Size(abs(lastPos.x - pos.x), abs(lastPos.y - pos.y))
                        if (sz.width > 5f || sz.height > 5f) {
                            onAddShape(DrawingShape(if (activeTool == DrawingTool.CIRCLE) ShapeType.CIRCLE else ShapeType.RECTANGLE, tl, sz, selectedColor))
                        }
                        onShapeStartChange(null); onShapeEndChange(null)
                    }
                }
            }
        ) {
            shapes.forEach { s ->
                when (s.type) {
                    ShapeType.CIRCLE -> drawOval(s.color, s.topLeft, s.size, style = Stroke(3f))
                    ShapeType.RECTANGLE -> drawRect(s.color, s.topLeft, s.size, style = Stroke(3f))
                }
            }
            paths.forEach { p ->
                if (p.points.size >= 2) {
                    val path = Path().apply {
                        moveTo(p.points.first().x, p.points.first().y)
                        p.points.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(path, p.color, style = Stroke(p.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
            if (currentPath.size >= 2) {
                val path = Path().apply {
                    moveTo(currentPath.first().x, currentPath.first().y)
                    currentPath.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(path, selectedColor, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            // Live shape preview while dragging
            val bStart = shapeStart
            val bEnd = shapeEnd
            if (bStart != null && bEnd != null && (activeTool == DrawingTool.CIRCLE || activeTool == DrawingTool.RECTANGLE)) {
                val tl = Offset(minOf(bStart.x, bEnd.x), minOf(bStart.y, bEnd.y))
                val sz = androidx.compose.ui.geometry.Size(abs(bEnd.x - bStart.x), abs(bEnd.y - bStart.y))
                if (activeTool == DrawingTool.CIRCLE)
                    drawOval(selectedColor, topLeft = tl, size = sz, style = Stroke(3f))
                else
                    drawRect(selectedColor, topLeft = tl, size = sz, style = Stroke(3f))
            }
        }
    }
}

// ─── Image Annotation Canvas ──────────────────────────────────────────────────

@Composable
fun ImageAnnotationCanvas(
    imageSource: Any,
    paths: List<DrawingPath>,
    shapes: List<DrawingShape>,
    currentPath: List<Offset>,
    onCurrentPathChange: (List<Offset>) -> Unit,
    shapeStart: Offset?,
    shapeEnd: Offset?,
    onShapeStartChange: (Offset?) -> Unit,
    onShapeEndChange: (Offset?) -> Unit,
    activeTool: DrawingTool,
    selectedColor: Color,
    strokeWidth: Float,
    onAddPath: (DrawingPath) -> Unit,
    onAddShape: (DrawingShape) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(480.dp)
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, TealPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
    ) {
        coil.compose.AsyncImage(
            model = imageSource,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().background(Color.White),
            contentScale = ContentScale.Fit
        )
        Canvas(
            modifier = Modifier.fillMaxSize().pointerInput(activeTool) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val pos = down.position
                    if (activeTool == DrawingTool.PEN) {
                        val pts = mutableListOf(pos)
                        onCurrentPathChange(pts.toList())
                        var ev = awaitPointerEvent()
                        while (ev.changes.any { it.pressed }) {
                            val c = ev.changes.first()
                            pts.add(c.position); onCurrentPathChange(pts.toList()); c.consume()
                            ev = awaitPointerEvent()
                        }
                        if (pts.size >= 2) onAddPath(DrawingPath(pts.toList(), selectedColor, strokeWidth))
                        onCurrentPathChange(emptyList())
                    } else if (activeTool == DrawingTool.CIRCLE || activeTool == DrawingTool.RECTANGLE) {
                        onShapeStartChange(pos)
                        var lastPos = pos
                        var ev = awaitPointerEvent()
                        while (ev.changes.any { it.pressed }) {
                            val c = ev.changes.first()
                            lastPos = c.position; onShapeEndChange(lastPos); c.consume()
                            ev = awaitPointerEvent()
                        }
                        val tl = Offset(minOf(pos.x, lastPos.x), minOf(pos.y, lastPos.y))
                        val sz = androidx.compose.ui.geometry.Size(abs(lastPos.x - pos.x), abs(lastPos.y - pos.y))
                        if (sz.width > 5f || sz.height > 5f)
                            onAddShape(DrawingShape(if (activeTool == DrawingTool.CIRCLE) ShapeType.CIRCLE else ShapeType.RECTANGLE, tl, sz, selectedColor))
                        onShapeStartChange(null); onShapeEndChange(null)
                    }
                }
            }
        ) {
            paths.forEach { p ->
                if (p.points.size >= 2) {
                    val path = Path().apply {
                        moveTo(p.points.first().x, p.points.first().y)
                        p.points.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(path, p.color, style = Stroke(p.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
            shapes.forEach { s ->
                when (s.type) {
                    ShapeType.CIRCLE -> drawOval(s.color, topLeft = s.topLeft, size = s.size, style = Stroke(3f))
                    ShapeType.RECTANGLE -> drawRect(s.color, topLeft = s.topLeft, size = s.size, style = Stroke(3f))
                }
            }
            if (currentPath.size >= 2) {
                val path = Path().apply {
                    moveTo(currentPath.first().x, currentPath.first().y)
                    currentPath.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(path, selectedColor, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            val bStart = shapeStart; val bEnd = shapeEnd
            if (bStart != null && bEnd != null) {
                val tl = Offset(minOf(bStart.x, bEnd.x), minOf(bStart.y, bEnd.y))
                val sz = androidx.compose.ui.geometry.Size(abs(bEnd.x - bStart.x), abs(bEnd.y - bStart.y))
                if (activeTool == DrawingTool.CIRCLE) drawOval(selectedColor, topLeft = tl, size = sz, style = Stroke(3f))
                else if (activeTool == DrawingTool.RECTANGLE) drawRect(selectedColor, topLeft = tl, size = sz, style = Stroke(3f))
            }
        }
    }
}

// ─── Image Compositing ────────────────────────────────────────────────────────

suspend fun compositeAnnotation(
    context: android.content.Context,
    imageSource: Any,
    paths: List<DrawingPath>,
    shapes: List<DrawingShape>,
    canvasDp: Int = 480
): Uri? {
    return try {
        val density = context.resources.displayMetrics.density
        val canvasPx = (canvasDp * density).toInt()

        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(imageSource).allowHardware(false)
            .size(canvasPx, canvasPx).build()
        val result = loader.execute(request) as? SuccessResult ?: return null
        val srcBitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap ?: return null

        val outBitmap = android.graphics.Bitmap.createBitmap(canvasPx, canvasPx, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(outBitmap)
        canvas.drawColor(android.graphics.Color.WHITE)

        val scale = minOf(canvasPx.toFloat() / srcBitmap.width, canvasPx.toFloat() / srcBitmap.height)
        val left = (canvasPx - srcBitmap.width * scale) / 2f
        val top = (canvasPx - srcBitmap.height * scale) / 2f
        val matrix = Matrix().apply { setScale(scale, scale); postTranslate(left, top) }
        canvas.drawBitmap(srcBitmap, matrix, null)

        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        paths.forEach { p ->
            if (p.points.size < 2) return@forEach
            paint.color = p.color.toArgb(); paint.strokeWidth = p.strokeWidth
            val path = android.graphics.Path()
            path.moveTo(p.points.first().x, p.points.first().y)
            p.points.drop(1).forEach { path.lineTo(it.x, it.y) }
            canvas.drawPath(path, paint)
        }
        paint.strokeWidth = 3f
        shapes.forEach { s ->
            paint.color = s.color.toArgb()
            val r = android.graphics.RectF(s.topLeft.x, s.topLeft.y, s.topLeft.x + s.size.width, s.topLeft.y + s.size.height)
            when (s.type) {
                ShapeType.CIRCLE -> canvas.drawOval(r, paint)
                ShapeType.RECTANGLE -> canvas.drawRect(r, paint)
            }
        }

        val file = File(context.cacheDir, "annotated_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { outBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, it) }
        Uri.fromFile(file)
    } catch (e: Exception) { null }
}
