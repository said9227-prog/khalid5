package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.KhalidStudioDatabase
import com.example.data.ProjectEntity
import com.example.data.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class StudioTab {
    TEXT,
    TERRAIN,
    SURFACE_PAINT,
    GREEN_SCREEN,
    MATERIALS,
    LIGHTING,
    CAMERA,
    ANIMATION,
    AUDIO,
    EXPORT,
    PROJECTS
}

data class CameraMotionStep(
    val id: String = "step_" + System.currentTimeMillis() + "_" + (100..999).random(),
    val preset: String,
    val durationSec: Float = 5.0f,
    val startX: Float = 0f,
    val startY: Float = 1.8f,
    val startZ: Float = 9.5f,
    val endX: Float = 0f,
    val endY: Float = 0.5f,
    val endZ: Float = 4.0f
)

enum class SceneAssetType {
    TEXT,
    IMAGE,
    MODEL
}

data class SceneAssetItem(
    val id: String,
    val type: SceneAssetType,
    var label: String,
    var text: String = "",
    var imageBase64: String = "",
    var modelType: String = "cube", // cube, sphere, cylinder, cone, torus, star, pyramid, ring
    var posX: Float = 0f,
    var posY: Float = 0f,
    var posZ: Float = 0f,
    var rotX: Float = 0f,
    var rotY: Float = 0f,
    var rotZ: Float = 0f,
    var scaleX: Float = 1.0f,
    var scaleY: Float = 1.0f,
    var scaleZ: Float = 1.0f,
    var size: Float = 1.4f,
    var depth: Float = 0.5f,
    var bevel: Float = 0.08f,
    var spacing: Float = 1.25f,
    var materialPreset: String = "gold"
)

data class Imported3DModelItem(
    val id: String,
    val name: String,
    val format: String, // GLB, GLTF, OBJ
    val base64Data: String = "",
    val brushId: String = "brush_$id",
    val isPreset: Boolean = false,
    val presetKey: String = ""
)

data class TextObjectItem(
    val id: String,
    var label: String,
    var text: String,
    var size: Float = 1.4f,
    var depth: Float = 0.5f,
    var bevel: Float = 0.08f,
    var spacing: Float = 1.25f,
    var posX: Float = 0f,
    var posY: Float = 0f,
    var posZ: Float = 0f,
    var materialPreset: String = "gold"
)

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProjectRepository
    val projectsList: StateFlow<List<ProjectEntity>>

    var webViewRef: WebView? = null

    val isArabic = MutableStateFlow(false)
    val activeTab = MutableStateFlow(StudioTab.TEXT)

    // Multi-Asset 3D Scene Manager (Texts, 3D Image Planes, & 3D Primitive Models)
    val isText3DAnimating = MutableStateFlow(true)

    val sceneAssetsList = MutableStateFlow<List<SceneAssetItem>>(
        listOf(
            SceneAssetItem(
                id = "text_main",
                type = SceneAssetType.TEXT,
                label = "النص الرئيسي",
                text = "KHALID 3D",
                posY = 0.5f
            )
        )
    )

    val selectedAssetId = MutableStateFlow("text_main")

    // Legacy State compatibility
    val textObjectsList = MutableStateFlow(
        listOf(
            TextObjectItem("text_main", "النص 1", "KHALID 3D", posY = 0.5f)
        )
    )
    val selectedTextObjectId = MutableStateFlow("text_main")

    // Selected Asset Active Parameters
    val currentText = MutableStateFlow("KHALID 3D")
    val textSize = MutableStateFlow(1.4f)
    val textDepth = MutableStateFlow(0.5f)
    val bevelDepth = MutableStateFlow(0.08f)
    val letterSpacing = MutableStateFlow(1.25f)

    // 3D Transform Parameters (X, Y, Z Position, Rotation in degrees, and Scale)
    val assetPosX = MutableStateFlow(0.0f)
    val assetPosY = MutableStateFlow(0.5f)
    val assetPosZ = MutableStateFlow(0.0f)

    val assetRotX = MutableStateFlow(0.0f)
    val assetRotY = MutableStateFlow(0.0f)
    val assetRotZ = MutableStateFlow(0.0f)

    val assetScaleX = MutableStateFlow(1.0f)
    val assetScaleY = MutableStateFlow(1.0f)
    val assetScaleZ = MutableStateFlow(1.0f)

    // Legacy position bindings
    val textPosX = assetPosX
    val textPosY = assetPosY
    val textPosZ = assetPosZ

    // Material state
    val selectedMaterial = MutableStateFlow("gold")
    val metalness = MutableStateFlow(0.9f)
    val roughness = MutableStateFlow(0.18f)
    val customColorHex = MutableStateFlow("#FFD700")
    val customEmissive = MutableStateFlow(0.2f)

    // Interactive 3D Light Bulb & Lighting Controls
    val lightPosX = MutableStateFlow(0.0f)
    val lightPosY = MutableStateFlow(4.0f)
    val lightPosZ = MutableStateFlow(3.5f)
    val lightIntensity = MutableStateFlow(3.5f)
    val lightColorHex = MutableStateFlow("#FFD700")
    val isLightBulbVisible = MutableStateFlow(true)
    val isCameraPathVisible = MutableStateFlow(true)

    // 3D Surface Painting Studio (3ds Max Object Paint & Scatter Engine)
    val isPaintModeEnabled = MutableStateFlow(false)
    val activeBrushType = MutableStateFlow("grass") // "grass", "trees", "flowers", "rocks", "stars"
    val activeBrushTool = MutableStateFlow("paint") // "paint", "erase"
    val brushRadius = MutableStateFlow(2.5f)
    val brushDensity = MutableStateFlow(8)
    val totalPaintedCount = MutableStateFlow(0)
    val imported3DModels = MutableStateFlow<List<Imported3DModelItem>>(
        listOf(
            Imported3DModelItem("preset_spaceship", "سفينة فضاء 3D (Spaceship)", "GLB", isPreset = true, presetKey = "spaceship"),
            Imported3DModelItem("preset_robot", "روبوت قتالي (Robot Droid)", "OBJ", isPreset = true, presetKey = "robot"),
            Imported3DModelItem("preset_car", "سيارة سباق (Sport Car)", "GLB", isPreset = true, presetKey = "car"),
            Imported3DModelItem("preset_trophy", "كأس ذهبي 3D (Golden Trophy)", "OBJ", isPreset = true, presetKey = "trophy")
        )
    )
    val importModelStatus = MutableStateFlow("")

    // Environment, Ground, Backdrop & Procedural 3D Terrain Studio
    val stageTheme = MutableStateFlow("3dsmax_white")
    val groundPreset = MutableStateFlow("grass_meadow")
    val backdropPreset = MutableStateFlow("mountains")
    val renderQuality = MutableStateFlow("balanced")
    val selectedLightPreset = MutableStateFlow("studio_gold")
    val isBloomEnabled = MutableStateFlow(true)
    val isGridVisible = MutableStateFlow(true)
    val isTextureAnimating = MutableStateFlow(false)

    // 3D Procedural Terrain Studio State
    val terrainPreset = MutableStateFlow("grass_meadow")
    val terrainDisplacementHeight = MutableStateFlow(1.0f)
    val terrainNoiseFreq = MutableStateFlow(1.0f)
    val waterWaveSpeed = MutableStateFlow(1.0f)
    val terrainQualityTier = MutableStateFlow("balanced")

    // Space & Sky Celestial Environment Engine (نجوم تلمع، قمر كامل/هلال، شمس وغيوم)
    val skyPreset = MutableStateFlow("deep_space")
    val moonPhase = MutableStateFlow("full_moon")
    val starDensity = MutableStateFlow(1000)
    val cloudSpeed = MutableStateFlow(0.05f)
    val starTwinkleSpeed = MutableStateFlow(1.0f)

    // Camera Studio & Multi-Shot Sequence Choreographer
    val isPreviewingCamera = MutableStateFlow(false)
    val isCameraPreviewComplete = MutableStateFlow(false)
    val isRecordingCamera = MutableStateFlow(false)
    val recordedCameraFrames = MutableStateFlow(0)
    val selectedCameraPreset = MutableStateFlow("orbit360")
    val cameraRecDurationSec = MutableStateFlow(5.0f)
    val cameraSequence = MutableStateFlow(
        listOf(
            CameraMotionStep(preset = "top", durationSec = 4.0f),
            CameraMotionStep(preset = "bottom", durationSec = 4.0f),
            CameraMotionStep(preset = "orbit360", durationSec = 5.0f)
        )
    )

    // Timeline & Animation
    val isPlaying = MutableStateFlow(false)
    val currentTime = MutableStateFlow(0.0f)
    val durationSec = MutableStateFlow(5.0f)
    val fps = MutableStateFlow(30)

    // Audio
    val selectedAudioTrackName = MutableStateFlow<String?>(null)
    val audioVolume = MutableStateFlow(0.8f)

    // Hollywood Green Screen 3D Composer State
    val isGreenScreenActive = MutableStateFlow(false)
    val isGreenScreenVideoLoaded = MutableStateFlow(false)
    val greenScreenVideoName = MutableStateFlow("الممثل السينمائي (Actor Video)")
    val greenScreenDuration = MutableStateFlow(10.0f)
    val greenScreenCurrentTime = MutableStateFlow(0.0f)
    val isGreenScreenPlaying = MutableStateFlow(true)
    val greenScreenPlaybackSpeed = MutableStateFlow(1.0f)
    val isGreenScreenLooping = MutableStateFlow(true)

    // Chroma Key GPU Tuning (GLSL Shader Parameters)
    val chromaKeyColor = MutableStateFlow("#00FF00") // Green or Blue
    val chromaSimilarity = MutableStateFlow(0.42f)   // Tolerance
    val chromaSmoothness = MutableStateFlow(0.08f)   // Edge softness
    val chromaSpill = MutableStateFlow(0.50f)        // Spill suppression
    val chromaOpacity = MutableStateFlow(1.0f)       // Actor alpha
    val chromaEnvTint = MutableStateFlow(0.35f)      // Environment lighting integration

    // 3D Actor World Placement & Ground Contact Shadow
    val actorPosX = MutableStateFlow(0.0f)
    val actorPosY = MutableStateFlow(-0.85f)
    val actorPosZ = MutableStateFlow(0.0f)
    val actorScale = MutableStateFlow(1.0f)
    val actorRotY = MutableStateFlow(0.0f)
    val isGroundLocked = MutableStateFlow(true)
    val hasContactShadow = MutableStateFlow(true)
    val isBillboardEnabled = MutableStateFlow(false)
    val greenScreenStatusMessage = MutableStateFlow("")

    // Export State
    val isExporting = MutableStateFlow(false)
    val exportProgress = MutableStateFlow(0)
    val exportedFilePath = MutableStateFlow<String?>(null)
    val exportFormat = MutableStateFlow("mp4")
    val exportResolution = MutableStateFlow("1080p")

    // Real-Time System RAM Usage Performance Monitoring Engine
    val ramUsageMb = MutableStateFlow(0L)
    val ramTotalMb = MutableStateFlow(0L)
    val ramPercent = MutableStateFlow(0)
    val isRamOverlayVisible = MutableStateFlow(true)

    // Device Thermal Monitoring & Smart Thermal Protection Engine (لوحة مراقبة حرارة الهاتف)
    val batteryTempCelsius = MutableStateFlow(35.0f)
    val cpuThermalStatus = MutableStateFlow("NORMAL") // NORMAL, LIGHT, MODERATE, SEVERE, CRITICAL
    val fpsVal = MutableStateFlow(60)
    val systemThermalHealth = MutableStateFlow("مستقرة") // مستقرة، دافئة، حماية حرارية، خطر حراري
    val isSmartThermalProtectionEnabled = MutableStateFlow(true)
    val isThermalPanelExpanded = MutableStateFlow(false)
    val currentThrottleLevel = MutableStateFlow("normal")

    init {
        val dao = KhalidStudioDatabase.getDatabase(application).projectDao()
        repository = ProjectRepository(dao)
        projectsList = repository.allProjects.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        startRamMonitoring()
        startThermalMonitoring()
    }

    private fun startRamMonitoring() {
        viewModelScope.launch {
            while (true) {
                try {
                    val runtime = Runtime.getRuntime()
                    val usedMemBytes = runtime.totalMemory() - runtime.freeMemory()
                    val maxMemBytes = runtime.maxMemory()

                    val usedMb = usedMemBytes / (1024 * 1024)
                    val maxMb = maxMemBytes / (1024 * 1024)
                    val pct = if (maxMb > 0) ((usedMb.toDouble() / maxMb.toDouble()) * 100).toInt() else 0

                    ramUsageMb.value = usedMb
                    ramTotalMb.value = maxMb
                    ramPercent.value = pct
                } catch (e: Exception) {
                    // Fallback
                }
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    private fun startThermalMonitoring() {
        val app = getApplication<Application>()

        // 1. Initial Battery Temperature reading via sticky intent
        try {
            val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = app.registerReceiver(null, batteryFilter)
            val tempRaw = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            if (tempRaw > 0) {
                batteryTempCelsius.value = tempRaw / 10.0f
            }
        } catch (e: Exception) {
            // fallback
        }

        // 2. CPU Thermal Throttling Status (Android 10+ / API 29+)
        val powerManager = app.getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null) {
            try {
                updateThermalStatusFromInt(powerManager.currentThermalStatus)
                powerManager.addThermalStatusListener(app.mainExecutor) { status ->
                    updateThermalStatusFromInt(status)
                }
            } catch (e: Exception) {
                cpuThermalStatus.value = "NORMAL"
            }
        }

        // 3. Periodic Background Thermal & Battery Polling loop
        viewModelScope.launch {
            while (true) {
                try {
                    val batteryStatus = app.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    val tempRaw = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
                    if (tempRaw > 0) {
                        batteryTempCelsius.value = tempRaw / 10.0f
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null) {
                        updateThermalStatusFromInt(powerManager.currentThermalStatus)
                    }

                    evaluateThermalProtectionRules()
                } catch (e: Exception) {
                    // Ignore
                }
                kotlinx.coroutines.delay(2000)
            }
        }
    }

    private fun updateThermalStatusFromInt(status: Int) {
        val label = when (status) {
            0 -> "NORMAL"   // PowerManager.THERMAL_STATUS_NONE
            1 -> "LIGHT"    // PowerManager.THERMAL_STATUS_LIGHT
            2 -> "MODERATE" // PowerManager.THERMAL_STATUS_MODERATE
            3 -> "SEVERE"   // PowerManager.THERMAL_STATUS_SEVERE
            4 -> "CRITICAL" // PowerManager.THERMAL_STATUS_CRITICAL
            5 -> "EMERGENCY"// PowerManager.THERMAL_STATUS_EMERGENCY
            6 -> "SHUTDOWN" // PowerManager.THERMAL_STATUS_SHUTDOWN
            else -> "NORMAL"
        }
        cpuThermalStatus.value = label
    }

    private fun evaluateThermalProtectionRules() {
        if (!isSmartThermalProtectionEnabled.value) {
            systemThermalHealth.value = if (isArabic.value) "الحماية الذكية معطلة" else "Protection Disabled"
            return
        }

        val temp = batteryTempCelsius.value
        val cpuStatus = cpuThermalStatus.value

        val (targetLevel, healthLabel) = when {
            temp >= 46.0f || cpuStatus == "CRITICAL" || cpuStatus == "EMERGENCY" || cpuStatus == "SHUTDOWN" -> {
                Pair("critical", if (isArabic.value) "خطر حراري! تبريد طارئ ❄️" else "Thermal Hazard! Emergency Cooling ❄️")
            }
            temp >= 43.0f || cpuStatus == "SEVERE" -> {
                Pair("severe", if (isArabic.value) "حماية نشطة (حرارة شديدة) 🔥" else "Protected (Severe Heat) 🔥")
            }
            temp >= 40.0f || cpuStatus == "MODERATE" -> {
                Pair("moderate", if (isArabic.value) "حماية نشطة (حرارة متوسطة) ⚠️" else "Protected (Moderate Heat) ⚠️")
            }
            temp >= 37.5f || cpuStatus == "LIGHT" -> {
                Pair("light", if (isArabic.value) "دافئة (موازنة استهلاك) 🟡" else "Warm (Power Balanced) 🟡")
            }
            else -> {
                Pair("normal", if (isArabic.value) "مستقرة وطبيعية 🟢" else "Stable & Optimal 🟢")
            }
        }

        systemThermalHealth.value = healthLabel

        if (currentThrottleLevel.value != targetLevel) {
            currentThrottleLevel.value = targetLevel
            evaluateJs("window.applyThermalThrottling && window.applyThermalThrottling('$targetLevel');")
        }
    }

    fun onFpsUpdate(fps: Int) {
        fpsVal.value = fps
    }

    fun toggleSmartThermalProtection(enabled: Boolean) {
        isSmartThermalProtectionEnabled.value = enabled
        if (!enabled) {
            currentThrottleLevel.value = "normal"
            evaluateJs("window.applyThermalThrottling && window.applyThermalThrottling('normal');")
            systemThermalHealth.value = if (isArabic.value) "الحماية الذكية معطلة" else "Protection Off"
        } else {
            evaluateThermalProtectionRules()
        }
    }

    fun triggerInstantCoolDown() {
        currentThrottleLevel.value = "critical"
        renderQuality.value = "draft"
        systemThermalHealth.value = if (isArabic.value) "تبريد طارئ نشط ❄️" else "Emergency Cool Down ❄️"
        evaluateJs("window.applyThermalThrottling && window.applyThermalThrottling('critical');")
    }

    fun resetBalancedThermal() {
        currentThrottleLevel.value = "normal"
        renderQuality.value = "balanced"
        evaluateJs("window.applyThermalThrottling && window.applyThermalThrottling('normal');")
        evaluateThermalProtectionRules()
    }

    fun toggleThermalPanelExpanded(expanded: Boolean) {
        isThermalPanelExpanded.value = expanded
    }

    fun toggleRamOverlay(visible: Boolean) {
        isRamOverlayVisible.value = visible
    }

    fun toggleLanguage() {
        isArabic.value = !isArabic.value
    }

    fun toggleText3DAnimation(enabled: Boolean) {
        isText3DAnimating.value = enabled
        val js = "window.studioEngine && window.studioEngine.toggleText3DAnimation($enabled);"
        evaluateJs(js)
    }

    fun selectAsset(id: String) {
        selectedAssetId.value = id
        selectedTextObjectId.value = id

        val asset = sceneAssetsList.value.find { it.id == id } ?: return
        currentText.value = asset.text
        textSize.value = asset.size
        textDepth.value = asset.depth
        bevelDepth.value = asset.bevel
        letterSpacing.value = asset.spacing

        assetPosX.value = asset.posX
        assetPosY.value = asset.posY
        assetPosZ.value = asset.posZ

        assetRotX.value = asset.rotX
        assetRotY.value = asset.rotY
        assetRotZ.value = asset.rotZ

        assetScaleX.value = asset.scaleX
        assetScaleY.value = asset.scaleY
        assetScaleZ.value = asset.scaleZ
    }

    fun selectTextObject(id: String) {
        selectAsset(id)
    }

    fun addNewTextAsset(text: String = "نص جديد 3D") {
        val count = sceneAssetsList.value.filter { it.type == SceneAssetType.TEXT }.size + 1
        val newId = "text_" + System.currentTimeMillis()
        val defaultY = if (count % 2 == 0) -1.2f * (count / 2) else 1.2f * (count / 2)
        val newItem = SceneAssetItem(
            id = newId,
            type = SceneAssetType.TEXT,
            label = "نص $count",
            text = text,
            posY = defaultY
        )

        sceneAssetsList.value = sceneAssetsList.value + newItem
        syncLegacyTextList()
        selectAsset(newId)

        val js = "window.studioEngine && window.studioEngine.addTextObject({ id: '$newId', text: '${escapeJs(text)}', size: 1.2, depth: 0.4, posY: $defaultY, posX: 0, posZ: 0, materialPreset: 'gold' });"
        evaluateJs(js)
    }

    fun addNewTextObject(text: String = "نص جديد 3D") {
        addNewTextAsset(text)
    }

    fun addNewImageAsset(base64Data: String = "", label: String = "صورة 3D") {
        val count = sceneAssetsList.value.filter { it.type == SceneAssetType.IMAGE }.size + 1
        val newId = "img_" + System.currentTimeMillis()
        val defaultY = 0.0f
        val defaultZ = -0.5f
        val displayLabel = if (label.isNotBlank()) label else "صورة $count"
        val newItem = SceneAssetItem(
            id = newId,
            type = SceneAssetType.IMAGE,
            label = displayLabel,
            imageBase64 = base64Data,
            posY = defaultY,
            posZ = defaultZ
        )

        sceneAssetsList.value = sceneAssetsList.value + newItem
        selectAsset(newId)

        val js = if (base64Data.isNotBlank()) {
            "window.studioEngine && window.studioEngine.addImageObject({ id: '$newId', label: '${escapeJs(displayLabel)}', base64Url: '$base64Data', posY: $defaultY, posZ: $defaultZ });"
        } else {
            "window.studioEngine && window.studioEngine.addImageObject({ id: '$newId', label: '${escapeJs(displayLabel)}', posY: $defaultY, posZ: $defaultZ });"
        }
        evaluateJs(js)
    }

    fun addNewModelAsset(modelType: String = "cube", label: String = "مجسم 3D") {
        val count = sceneAssetsList.value.filter { it.type == SceneAssetType.MODEL }.size + 1
        val newId = "model_" + System.currentTimeMillis()
        val defaultX = if (count % 2 == 0) -2.0f else 2.0f
        val displayLabel = "$label $count"
        val newItem = SceneAssetItem(
            id = newId,
            type = SceneAssetType.MODEL,
            label = displayLabel,
            modelType = modelType,
            posX = defaultX,
            posY = 0.0f,
            materialPreset = selectedMaterial.value
        )

        sceneAssetsList.value = sceneAssetsList.value + newItem
        selectAsset(newId)

        val js = "window.studioEngine && window.studioEngine.addModelObject({ id: '$newId', modelType: '$modelType', posX: $defaultX, posY: 0, posZ: 0, materialPreset: '${selectedMaterial.value}' });"
        evaluateJs(js)
    }

    fun duplicateSelectedAsset() {
        val currentId = selectedAssetId.value
        val item = sceneAssetsList.value.find { it.id == currentId } ?: return
        val count = sceneAssetsList.value.size + 1
        val newId = "${item.type.name.lowercase()}_" + System.currentTimeMillis()
        val newY = item.posY - 1.0f
        val newItem = item.copy(id = newId, label = "نسخة $count", posY = newY)

        sceneAssetsList.value = sceneAssetsList.value + newItem
        syncLegacyTextList()
        selectAsset(newId)

        when (item.type) {
            SceneAssetType.TEXT -> {
                val js = "window.studioEngine && window.studioEngine.addTextObject({ id: '$newId', text: '${escapeJs(newItem.text)}', size: ${newItem.size}, depth: ${newItem.depth}, posY: $newY, posX: ${newItem.posX}, posZ: ${newItem.posZ}, materialPreset: '${newItem.materialPreset}' });"
                evaluateJs(js)
            }
            SceneAssetType.IMAGE -> {
                val js = "window.studioEngine && window.studioEngine.addImageObject({ id: '$newId', label: '${escapeJs(newItem.label)}', base64Url: '${newItem.imageBase64}', posY: $newY, posX: ${newItem.posX}, posZ: ${newItem.posZ} });"
                evaluateJs(js)
            }
            SceneAssetType.MODEL -> {
                val js = "window.studioEngine && window.studioEngine.addModelObject({ id: '$newId', modelType: '${newItem.modelType}', posY: $newY, posX: ${newItem.posX}, posZ: ${newItem.posZ}, materialPreset: '${newItem.materialPreset}' });"
                evaluateJs(js)
            }
        }
    }

    fun duplicateSelectedTextObject() {
        duplicateSelectedAsset()
    }

    fun deleteSelectedAsset() {
        if (sceneAssetsList.value.size <= 1) return
        val currentId = selectedAssetId.value
        val newList = sceneAssetsList.value.filter { it.id != currentId }
        sceneAssetsList.value = newList
        syncLegacyTextList()

        val nextItem = newList.first()
        selectAsset(nextItem.id)

        val js = "window.studioEngine && window.studioEngine.removeObject('$currentId');"
        evaluateJs(js)
    }

    fun deleteSelectedTextObject() {
        deleteSelectedAsset()
    }

    fun updateSelectedAssetTransform(
        posX: Float = assetPosX.value,
        posY: Float = assetPosY.value,
        posZ: Float = assetPosZ.value,
        rotX: Float = assetRotX.value,
        rotY: Float = assetRotY.value,
        rotZ: Float = assetRotZ.value,
        scaleX: Float = assetScaleX.value,
        scaleY: Float = assetScaleY.value,
        scaleZ: Float = assetScaleZ.value
    ) {
        assetPosX.value = posX
        assetPosY.value = posY
        assetPosZ.value = posZ

        assetRotX.value = rotX
        assetRotY.value = rotY
        assetRotZ.value = rotZ

        assetScaleX.value = scaleX
        assetScaleY.value = scaleY
        assetScaleZ.value = scaleZ

        val activeId = selectedAssetId.value
        sceneAssetsList.value.find { it.id == activeId }?.let { item ->
            item.posX = posX
            item.posY = posY
            item.posZ = posZ

            item.rotX = rotX
            item.rotY = rotY
            item.rotZ = rotZ

            item.scaleX = scaleX
            item.scaleY = scaleY
            item.scaleZ = scaleZ
        }
        syncLegacyTextList()

        val js = "window.studioEngine && window.studioEngine.updateObjectTransform('$activeId', { posX: $posX, posY: $posY, posZ: $posZ, rotX: $rotX, rotY: $rotY, rotZ: $rotZ, scaleX: $scaleX, scaleY: $scaleY, scaleZ: $scaleZ });"
        evaluateJs(js)
    }

    fun updateSelectedTextPosition(x: Float = assetPosX.value, y: Float = assetPosY.value, z: Float = assetPosZ.value) {
        updateSelectedAssetTransform(posX = x, posY = y, posZ = z)
    }

    private fun syncLegacyTextList() {
        textObjectsList.value = sceneAssetsList.value
            .filter { it.type == SceneAssetType.TEXT }
            .map {
                TextObjectItem(
                    id = it.id,
                    label = it.label,
                    text = it.text,
                    size = it.size,
                    depth = it.depth,
                    bevel = it.bevel,
                    spacing = it.spacing,
                    posX = it.posX,
                    posY = it.posY,
                    posZ = it.posZ,
                    materialPreset = it.materialPreset
                )
            }
    }

    fun updateTextConfig(
        text: String = currentText.value,
        size: Float = textSize.value,
        depth: Float = textDepth.value,
        bevel: Float = bevelDepth.value,
        spacing: Float = letterSpacing.value
    ) {
        currentText.value = text
        textSize.value = size
        textDepth.value = depth
        bevelDepth.value = bevel
        letterSpacing.value = spacing

        val activeId = selectedAssetId.value
        sceneAssetsList.value.find { it.id == activeId }?.let { item ->
            item.text = text
            item.size = size
            item.depth = depth
            item.bevel = bevel
            item.spacing = spacing
        }
        syncLegacyTextList()

        val js = "window.studioEngine && window.studioEngine.updateTextObject('$activeId', { text: '${escapeJs(text)}', size: $size, depth: $depth, bevel: $bevel, letterSpacing: $spacing });"
        evaluateJs(js)
    }

    fun loadCustomTextureFromPhone(targetType: String, base64Url: String) {
        val js = "window.studioEngine && window.studioEngine.loadCustomTexture('$targetType', '$base64Url');"
        evaluateJs(js)
    }

    fun toggleTextureAnimation(enabled: Boolean) {
        isTextureAnimating.value = enabled
        val js = "window.studioEngine && window.studioEngine.toggleTextureAnimation($enabled);"
        evaluateJs(js)
    }

    fun setGroundTexture(preset: String) {
        groundPreset.value = preset
        val js = "window.studioEngine && window.studioEngine.setGroundTexture('$preset');"
        evaluateJs(js)
    }

    fun setTerrainPreset(preset: String) {
        terrainPreset.value = preset
        groundPreset.value = preset
        val js = "window.studioEngine && window.studioEngine.setTerrainPreset('$preset');"
        evaluateJs(js)
    }

    fun setTerrainDisplacementHeight(height: Float) {
        terrainDisplacementHeight.value = height
        val js = "window.studioEngine && window.studioEngine.setTerrainDisplacementHeight($height);"
        evaluateJs(js)
    }

    fun setTerrainNoiseFrequency(freq: Float) {
        terrainNoiseFreq.value = freq
        val js = "window.studioEngine && window.studioEngine.setTerrainNoiseFrequency($freq);"
        evaluateJs(js)
    }

    fun setWaterWaveSpeed(speed: Float) {
        waterWaveSpeed.value = speed
        val js = "window.studioEngine && window.studioEngine.setWaterWaveSpeed($speed);"
        evaluateJs(js)
    }

    fun setTerrainQualityTier(tier: String) {
        terrainQualityTier.value = tier
        setRenderQuality(tier)
    }

    fun setSkyPreset(preset: String) {
        skyPreset.value = preset
        val js = "window.studioEngine && window.studioEngine.setSkyPreset('$preset');"
        evaluateJs(js)
    }

    fun setMoonPhase(phase: String) {
        moonPhase.value = phase
        val js = "window.studioEngine && window.studioEngine.setMoonPhase('$phase');"
        evaluateJs(js)
    }

    fun setStarDensity(density: Int) {
        starDensity.value = density
        val js = "window.studioEngine && window.studioEngine.setStarDensity($density);"
        evaluateJs(js)
    }

    fun setCloudSpeed(speed: Float) {
        cloudSpeed.value = speed
        val js = "window.studioEngine && window.studioEngine.setCloudSpeed($speed);"
        evaluateJs(js)
    }

    fun setStarTwinkleSpeed(speed: Float) {
        starTwinkleSpeed.value = speed
        val js = "window.studioEngine && window.studioEngine.setStarTwinkleSpeed($speed);"
        evaluateJs(js)
    }

    fun setBackdropTexture(preset: String) {
        backdropPreset.value = preset
        val js = "window.studioEngine && window.studioEngine.setBackdropTexture('$preset');"
        evaluateJs(js)
    }

    fun setStageTheme(theme: String) {
        stageTheme.value = theme
        val js = "window.studioEngine && window.studioEngine.setStageTheme('$theme');"
        evaluateJs(js)
    }

    fun setRenderQuality(quality: String) {
        renderQuality.value = quality
        val js = "window.studioEngine && window.studioEngine.setRenderQuality('$quality');"
        evaluateJs(js)
    }

    fun setMaterialPreset(preset: String) {
        selectedMaterial.value = preset
        val js = "window.studioEngine && window.studioEngine.setMaterialPreset('text_main', '$preset');"
        evaluateJs(js)
    }

    fun setCustomMaterialColor(
        hexColor: String = customColorHex.value,
        metal: Float = metalness.value,
        rough: Float = roughness.value,
        emissive: Float = customEmissive.value
    ) {
        customColorHex.value = hexColor
        metalness.value = metal
        roughness.value = rough
        customEmissive.value = emissive
        selectedMaterial.value = "custom"

        val js = "window.studioEngine && window.studioEngine.setCustomMaterialColor('text_main', '$hexColor', $metal, $rough, $emissive);"
        evaluateJs(js)
    }

    fun updateLightPosition(x: Float = lightPosX.value, y: Float = lightPosY.value, z: Float = lightPosZ.value) {
        lightPosX.value = x
        lightPosY.value = y
        lightPosZ.value = z
        val js = "window.studioEngine && window.studioEngine.setLightPosition($x, $y, $z);"
        evaluateJs(js)
    }

    fun updateLightColor(hexColor: String) {
        lightColorHex.value = hexColor
        val js = "window.studioEngine && window.studioEngine.setLightColor('$hexColor');"
        evaluateJs(js)
    }

    fun updateLightIntensity(intensity: Float) {
        lightIntensity.value = intensity
        val js = "window.studioEngine && window.studioEngine.setLightIntensity($intensity);"
        evaluateJs(js)
    }

    fun toggleLightBulbMesh(visible: Boolean) {
        isLightBulbVisible.value = visible
        val js = "window.studioEngine && window.studioEngine.toggleLightBulbMesh($visible);"
        evaluateJs(js)
    }

    fun toggleCameraPathCurve(visible: Boolean) {
        isCameraPathVisible.value = visible
        if (visible) {
            updateCameraPathInEngine()
        } else {
            evaluateJs("window.studioEngine && window.studioEngine.updateCameraPathCurve('[]');")
        }
    }

    fun updateCameraPathInEngine() {
        if (!isCameraPathVisible.value) return
        val sequenceJson = serializeSequenceJson(cameraSequence.value)
        val js = "window.studioEngine && window.studioEngine.updateCameraPathCurve($sequenceJson);"
        evaluateJs(js)
    }

    fun togglePaintMode(enabled: Boolean) {
        isPaintModeEnabled.value = enabled
        val js = "window.studioEngine && window.studioEngine.togglePaintMode($enabled);"
        evaluateJs(js)
    }

    fun setBrushType(type: String) {
        activeBrushType.value = type
        val js = "window.studioEngine && window.studioEngine.setBrushType('$type');"
        evaluateJs(js)
    }

    fun setBrushTool(tool: String) {
        activeBrushTool.value = tool
        val js = "window.studioEngine && window.studioEngine.setBrushTool('$tool');"
        evaluateJs(js)
    }

    fun setBrushRadius(radius: Float) {
        brushRadius.value = radius
        val js = "window.studioEngine && window.studioEngine.setBrushRadius($radius);"
        evaluateJs(js)
    }

    fun setBrushDensity(density: Int) {
        brushDensity.value = density
        val js = "window.studioEngine && window.studioEngine.setBrushDensity($density);"
        evaluateJs(js)
    }

    fun scatterSelectedBrush(count: Int) {
        val type = activeBrushType.value
        val js = "window.studioEngine && window.studioEngine.scatterInstances('$type', $count);"
        evaluateJs(js)
    }

    fun clearPaintedSurface() {
        totalPaintedCount.value = 0
        val js = "window.studioEngine && window.studioEngine.clearAllPaintedInstances();"
        evaluateJs(js)
    }

    fun import3DModelFile(name: String, format: String, base64Data: String) {
        val id = "imported_" + System.currentTimeMillis()
        val cleanFormat = format.uppercase()
        val newItem = Imported3DModelItem(
            id = id,
            name = name,
            format = cleanFormat,
            base64Data = base64Data,
            brushId = "brush_$id"
        )
        imported3DModels.value = listOf(newItem) + imported3DModels.value
        activeBrushType.value = "brush_$id"
        isPaintModeEnabled.value = true

        val js = "window.loadImported3DModel && window.loadImported3DModel({ id: '$id', name: '${escapeJs(name)}', format: '${cleanFormat.lowercase()}', data: '${escapeJs(base64Data)}', spawnInScene: true, asBrush: true });"
        evaluateJs(js)
        importModelStatus.value = if (isArabic.value) "تم استيراد $name بنجاح!" else "Imported $name successfully!"
    }

    fun loadPreset3DModel(item: Imported3DModelItem) {
        activeBrushType.value = "brush_${item.id}"
        isPaintModeEnabled.value = true
        val js = "window.loadImported3DModel && window.loadImported3DModel({ id: '${item.id}', name: '${escapeJs(item.name)}', format: 'preset', presetName: '${item.presetKey}', spawnInScene: true, asBrush: true });"
        evaluateJs(js)
        importModelStatus.value = if (isArabic.value) "تم تفعيل النموذج ${item.name}" else "Loaded ${item.name}"
    }

    fun setModelAsBrush(modelId: String) {
        val brushId = if (modelId.startsWith("brush_")) modelId else "brush_$modelId"
        activeBrushType.value = brushId
        isPaintModeEnabled.value = true
        evaluateJs("window.setModelAsPaintBrush && window.setModelAsPaintBrush('$modelId');")
    }

    fun spawnModelInScene(modelId: String) {
        evaluateJs("window.spawnModelInScene && window.spawnModelInScene('$modelId');")
    }

    fun deleteImportedModel(id: String) {
        imported3DModels.value = imported3DModels.value.filter { it.id != id }
    }

    // Hollywood Green Screen 3D Composer Methods
    fun importGreenScreenVideo(name: String, fileUriOrPath: String) {
        isGreenScreenActive.value = true
        isGreenScreenVideoLoaded.value = true
        greenScreenVideoName.value = name
        greenScreenStatusMessage.value = if (isArabic.value) "تم تحميل فيديو $name بنجاح وتفعيل كروما كي GLSL" else "Loaded $name with GLSL Chroma Key"

        val js = "window.loadGreenScreenVideo && window.loadGreenScreenVideo('${escapeJs(fileUriOrPath)}', '${escapeJs(name)}');"
        evaluateJs(js)
    }

    fun loadSampleGreenScreenActor(sampleType: String = "action_hero") {
        isGreenScreenActive.value = true
        isGreenScreenVideoLoaded.value = true
        greenScreenVideoName.value = if (isArabic.value) "الممثل السينمائي (Green Screen Actor)" else "Cinematic Green Screen Actor"
        greenScreenStatusMessage.value = if (isArabic.value) "تم تجهيز ممثل هوليوود داخل البيئة 3D" else "Ready Hollywood Green Screen Actor in 3D"

        val js = "window.loadSampleGreenScreenActor && window.loadSampleGreenScreenActor('$sampleType');"
        evaluateJs(js)
    }

    fun toggleGreenScreenPlayPause() {
        isGreenScreenPlaying.value = !isGreenScreenPlaying.value
        val play = if (isGreenScreenPlaying.value) "true" else "false"
        evaluateJs("window.setGreenScreenPlayState && window.setGreenScreenPlayState($play);")
    }

    fun seekGreenScreenVideo(timeSec: Float) {
        greenScreenCurrentTime.value = timeSec
        evaluateJs("window.seekGreenScreenVideo && window.seekGreenScreenVideo($timeSec);")
    }

    fun setGreenScreenSpeed(speed: Float) {
        greenScreenPlaybackSpeed.value = speed
        evaluateJs("window.setGreenScreenPlaybackSpeed && window.setGreenScreenPlaybackSpeed($speed);")
    }

    fun toggleGreenScreenLoop(loop: Boolean) {
        isGreenScreenLooping.value = loop
        evaluateJs("window.setGreenScreenLoop && window.setGreenScreenLoop($loop);")
    }

    fun updateChromaKeySettings(
        color: String = chromaKeyColor.value,
        similarity: Float = chromaSimilarity.value,
        smoothness: Float = chromaSmoothness.value,
        spill: Float = chromaSpill.value,
        opacity: Float = chromaOpacity.value,
        envTint: Float = chromaEnvTint.value
    ) {
        chromaKeyColor.value = color
        chromaSimilarity.value = similarity
        chromaSmoothness.value = smoothness
        chromaSpill.value = spill
        chromaOpacity.value = opacity
        chromaEnvTint.value = envTint

        val js = "window.updateChromaKeyShader && window.updateChromaKeyShader({ keyColor: '$color', similarity: $similarity, smoothness: $smoothness, spill: $spill, opacity: $opacity, envTintIntensity: $envTint });"
        evaluateJs(js)
    }

    fun updateActor3DTransform(
        posX: Float = actorPosX.value,
        posY: Float = actorPosY.value,
        posZ: Float = actorPosZ.value,
        scale: Float = actorScale.value,
        rotY: Float = actorRotY.value,
        groundLock: Boolean = isGroundLocked.value,
        contactShadow: Boolean = hasContactShadow.value,
        billboard: Boolean = isBillboardEnabled.value
    ) {
        actorPosX.value = posX
        actorPosY.value = posY
        actorPosZ.value = posZ
        actorScale.value = scale
        actorRotY.value = rotY
        isGroundLocked.value = groundLock
        hasContactShadow.value = contactShadow
        isBillboardEnabled.value = billboard

        val js = "window.updateActorTransform && window.updateActorTransform({ posX: $posX, posY: $posY, posZ: $posZ, scale: $scale, rotY: $rotY, groundLock: $groundLock, contactShadow: $contactShadow, billboard: $billboard });"
        evaluateJs(js)
    }

    fun onGreenScreenVideoState(currentTime: Float, duration: Float, isPlaying: Boolean) {
        greenScreenCurrentTime.value = currentTime
        if (duration > 0f) {
            greenScreenDuration.value = duration
        }
        isGreenScreenPlaying.value = isPlaying
    }

    fun applyCameraPreset(preset: String) {
        selectedCameraPreset.value = preset
        val js = "window.studioEngine && window.studioEngine.applyCameraPreset('$preset');"
        evaluateJs(js)
    }

    fun addCameraSequenceStep(preset: String, durationSec: Float) {
        val newList = cameraSequence.value + CameraMotionStep(preset = preset, durationSec = durationSec)
        cameraSequence.value = newList
        updateCameraPathInEngine()
    }

    fun addCustomWaypointStep(
        durationSec: Float,
        startX: Float,
        startY: Float,
        startZ: Float,
        endX: Float,
        endY: Float,
        endZ: Float
    ) {
        val step = CameraMotionStep(
            preset = "custom_waypoint",
            durationSec = durationSec,
            startX = startX,
            startY = startY,
            startZ = startZ,
            endX = endX,
            endY = endY,
            endZ = endZ
        )
        val newList = cameraSequence.value + step
        cameraSequence.value = newList
        updateCameraPathInEngine()
    }

    fun removeCameraSequenceStep(id: String) {
        cameraSequence.value = cameraSequence.value.filter { it.id != id }
        updateCameraPathInEngine()
    }

    fun previewCameraSequence() {
        isPreviewingCamera.value = true
        isCameraPreviewComplete.value = false
        val sequenceJson = serializeSequenceJson(cameraSequence.value)
        val js = "window.studioEngine && window.studioEngine.playCameraSequence($sequenceJson, false);"
        evaluateJs(js)
    }

    fun stopCameraPreview() {
        isPreviewingCamera.value = false
        isRecordingCamera.value = false
        isExporting.value = false
        evaluateJs("window.emergencyStop ? window.emergencyStop() : (window.stopCameraPreview && window.stopCameraPreview());")
    }

    fun emergencyStopAll() {
        isPreviewingCamera.value = false
        isRecordingCamera.value = false
        isExporting.value = false
        isPlaying.value = false
        evaluateJs("window.emergencyStop && window.emergencyStop();")
    }

    fun recordCameraSequence() {
        isRecordingCamera.value = true
        isPreviewingCamera.value = false
        recordedCameraFrames.value = 0
        val sequenceJson = serializeSequenceJson(cameraSequence.value)
        val js = "window.studioEngine && window.studioEngine.playCameraSequence($sequenceJson, true);"
        evaluateJs(js)
    }

    fun recordCameraSequenceAndExport() {
        isExporting.value = true
        isPreviewingCamera.value = false
        exportProgress.value = 0
        exportedFilePath.value = null
        val fpsVal = fps.value
        val fmt = exportFormat.value
        val sequenceJson = serializeSequenceJson(cameraSequence.value)

        val js = "window.studioEngine && window.studioEngine.exportCameraSequenceVideo($sequenceJson, $fpsVal, '$fmt');"
        evaluateJs(js)
    }

    private fun serializeSequenceJson(steps: List<CameraMotionStep>): String {
        val array = JSONArray()
        steps.forEach { step ->
            val obj = JSONObject()
            obj.put("preset", step.preset)
            obj.put("duration", step.durationSec)
            if (step.preset == "custom_waypoint") {
                obj.put("startX", step.startX)
                obj.put("startY", step.startY)
                obj.put("startZ", step.startZ)
                obj.put("endX", step.endX)
                obj.put("endY", step.endY)
                obj.put("endZ", step.endZ)
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun startCameraRecording() {
        isRecordingCamera.value = true
        recordedCameraFrames.value = 0
        evaluateJs("window.studioEngine && window.studioEngine.startCameraRecording();")
    }

    fun stopCameraRecording() {
        isRecordingCamera.value = false
        evaluateJs("window.studioEngine && window.studioEngine.stopCameraRecording();")
    }

    fun replayCameraPath() {
        evaluateJs("window.studioEngine && window.studioEngine.replayCameraPath();")
    }

    fun togglePlayPause() {
        isPlaying.value = !isPlaying.value
        val state = if (isPlaying.value) "true" else "false"
        evaluateJs("if(window.studioEngine) window.studioEngine.isPlaying = $state;")
    }

    fun seekTimeline(time: Float) {
        currentTime.value = time
        evaluateJs("if(window.studioEngine) window.studioEngine.currentTime = $time;")
    }

    fun startVideoExport() {
        recordCameraSequenceAndExport()
    }

    fun onExportProgress(progress: Int) {
        exportProgress.value = progress
    }

    fun onExportComplete(savedPath: String) {
        isExporting.value = false
        exportedFilePath.value = savedPath
    }

    fun onCameraRecordingState(status: String, count: Int) {
        when (status) {
            "stopped" -> {
                isRecordingCamera.value = false
                recordedCameraFrames.value = count
                if (isPreviewingCamera.value) {
                    isPreviewingCamera.value = false
                    isCameraPreviewComplete.value = true
                }
            }
            "previewing" -> {
                isPreviewingCamera.value = true
                isRecordingCamera.value = false
            }
            "recording" -> {
                isRecordingCamera.value = true
                isPreviewingCamera.value = false
            }
        }
    }

    fun saveCurrentProject(title: String) {
        viewModelScope.launch {
            val project = ProjectEntity(
                title = title.ifBlank { "Cinematic Text" },
                isArabic = isArabic.value,
                jsonContent = "{}",
                durationSec = durationSec.value,
                fps = fps.value
            )
            repository.saveProject(project)
        }
    }

    fun loadProject(project: ProjectEntity) {
        viewModelScope.launch {
            currentText.value = project.title
            durationSec.value = project.durationSec
            fps.value = project.fps
            updateTextConfig(text = project.title)
        }
    }

    fun deleteProject(id: Long) {
        viewModelScope.launch {
            repository.deleteProject(id)
        }
    }

    private fun evaluateJs(code: String) {
        webViewRef?.post {
            webViewRef?.evaluateJavascript(code, null)
        }
    }

    private fun escapeJs(str: String): String {
        return str.replace("'", "\\'").replace("\n", "\\n")
    }
}
