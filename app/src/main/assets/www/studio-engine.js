/**
 * Khalid Super 3D Cinematic Studio - WebGL Core Engine
 * Custom Phone Texture Importing, Texture Panning Animation, Transparent Backgrounds, & Clean Recording Mode
 */

class StudioEngine {
    constructor() {
        this.container = document.getElementById('viewport-container');
        this.scene = new THREE.Scene();
        this.camera = new THREE.PerspectiveCamera(42, window.innerWidth / window.innerHeight, 0.1, 1000);
        this.renderer = new THREE.WebGLRenderer({ antialias: true, preserveDrawingBuffer: true, alpha: true });
        
        this.objects = new Map(); // id -> THREE.Group
        this.selectedObjectId = null;
        
        this.currentTime = 0;
        this.duration = 5.0; // seconds
        this.fps = 30;
        this.isPlaying = false;
        this.isText3DAnimating = true;
        this.stageTheme = '3dsmax_white';
        this.renderQuality = 'balanced';
        
        // Environment & Procedural 3D Terrain Studio Presets
        this.groundPreset = 'grass_meadow';
        this.terrainPreset = 'grass_meadow';
        this.terrainHeight = 1.0;
        this.terrainNoiseFreq = 1.0;
        this.waterSpeed = 1.0;
        this.waterTime = 0;

        // Space & Sky Celestial Environment Engine (نجوم تلمع، قمر كامل/هلال، شمس وغيوم)
        this.skyPreset = 'deep_space'; // 'deep_space', 'blue_sky_sun', 'sunset_golden', 'night_nebula', '3dsmax_white', 'transparent'
        this.moonPhase = 'full_moon';  // 'full_moon', 'crescent', 'none'
        this.starDensity = 1000;
        this.cloudSpeed = 0.05;
        this.starTwinkleSpeed = 1.0;
        this.sunAngle = 45; // degrees

        this.starFieldGroup = null;
        this.twinkleStarsList = [];
        this.moonMeshGroup = null;
        this.sunMeshGroup = null;
        this.cloudsMeshGroup = null;

        this.backdropPreset = 'mountains';
        this.groundTexture = null;
        this.backdropTexture = null;
        this.textCustomTexture = null;
        
        // Texture Panning Animation
        this.isTextureAnimating = false;
        this.textureScrollSpeedX = 0.002;
        this.textureScrollSpeedY = 0.0;
        
        // Camera recording track
        this.cameraKeyframes = [];
        this.isRecordingCamera = false;
        this.sequenceInterval = null;

        // Raycaster & Direct Viewport 3D Touch Dragging Engine
        this.raycaster = new THREE.Raycaster();
        this.mouse = new THREE.Vector2();
        this.dragPlane = new THREE.Plane();
        this.planeIntersect = new THREE.Vector3();
        this.isDraggingObject = false;
        this.dragTargetGroup = null;
        this.dragOffset = new THREE.Vector3();
        this.selectionBox = null;

        this.init();
    }

    init() {
        // Setup Renderer with Adaptive Pixel Ratio
        this.renderer.setSize(window.innerWidth, window.innerHeight);
        this.applyRenderQuality('balanced');

        this.renderer.shadowMap.enabled = true;
        this.renderer.shadowMap.type = THREE.PCFSoftShadowMap;
        this.renderer.toneMapping = THREE.ACESFilmicToneMapping;
        this.renderer.toneMappingExposure = 1.35;
        this.container.appendChild(this.renderer.domElement);

        // Camera Position
        this.camera.position.set(0, 1.8, 9.5);

        // Orbit Controls
        this.controls = new THREE.OrbitControls(this.camera, this.renderer.domElement);
        this.controls.enableDamping = true;
        this.controls.dampingFactor = 0.05;
        this.controls.target.set(0, 0, 0);
        this.controls.update();

        // Setup Studio Lighting
        this.setupLighting();

        // 3D Ground & Vertical Backdrop Environment
        this.setupEnvironmentStage();

        // Direct Viewport 3D Touch Selection & Dragging Event Listeners
        this.setupTouchDragListeners();

        // Window Resize Listener
        window.addEventListener('resize', () => this.onWindowResize());

        // Controls Change Listener for Camera Path Recording
        this.controls.addEventListener('change', () => {
            if (this.isRecordingCamera) {
                this.recordCameraFrame();
            }
        });

        // Initialize Offline Font
        this.initOfflineFont();

        // Add Initial Default 3D Text Object
        this.addTextObject({
            id: 'text_main',
            text: 'KHALID 3D',
            size: 1.4,
            depth: 0.5,
            bevel: 0.08,
            letterSpacing: 1.25,
            materialPreset: 'gold'
        });

        // Initialize Instanced 3D Surface Painting Engine (3ds Max Object Paint & Scatter)
        this.initInstancedSurfacePainter();

        // Initialize Hollywood Green Screen 3D Composer Engine (GLSL Chroma Key & Contact Shadows)
        this.initGreenScreenComposer();

        // Hide HTML loading spinner
        const overlay = document.getElementById('loading-overlay');
        if (overlay) {
            overlay.style.opacity = '0';
            setTimeout(() => overlay.remove(), 400);
        }

        // Start Animation Loop
        this.animate();
    }

    initInstancedSurfacePainter() {
        this.isPaintModeEnabled = false;
        this.activeBrushType = 'grass';
        this.activeBrushTool = 'paint';
        this.brushRadius = 2.5;
        this.brushDensity = 8;
        this.isPointerDown = false;

        this.instancedMeshes = new Map();
        this.instanceData = new Map();

        const maxCounts = {
            grass: 10000,
            trees: 2000,
            flowers: 5000,
            rocks: 3000,
            stars: 3000
        };

        // 1. Grass InstancedMesh Geometry & Material (🌱 Cone blade)
        const grassGeo = new THREE.ConeGeometry(0.12, 0.85, 5);
        grassGeo.translate(0, 0.42, 0);
        const grassMat = new THREE.MeshStandardMaterial({
            color: 0x22C55E,
            roughness: 0.6,
            metalness: 0.1,
            side: THREE.DoubleSide
        });
        const grassMesh = new THREE.InstancedMesh(grassGeo, grassMat, maxCounts.grass);
        grassMesh.count = 0;
        grassMesh.castShadow = true;
        grassMesh.receiveShadow = true;

        // 2. Tree InstancedMesh Geometry & Material (🌲 Pine Tree)
        const treeGroupGeo = new THREE.ConeGeometry(0.8, 2.5, 7);
        treeGroupGeo.translate(0, 1.25, 0);
        const treeMat = new THREE.MeshStandardMaterial({
            color: 0x15803D,
            roughness: 0.7,
            metalness: 0.05
        });
        const treeMesh = new THREE.InstancedMesh(treeGroupGeo, treeMat, maxCounts.trees);
        treeMesh.count = 0;
        treeMesh.castShadow = true;
        treeMesh.receiveShadow = true;

        // 3. Flower InstancedMesh Geometry & Material (🌸 Petal Disc)
        const flowerGeo = new THREE.DodecahedronGeometry(0.3, 0);
        flowerGeo.translate(0, 0.25, 0);
        const flowerMat = new THREE.MeshStandardMaterial({
            color: 0xEC4899,
            roughness: 0.3,
            metalness: 0.1,
            emissive: 0x831843,
            emissiveIntensity: 0.2
        });
        const flowerMesh = new THREE.InstancedMesh(flowerGeo, flowerMat, maxCounts.flowers);
        flowerMesh.count = 0;
        flowerMesh.castShadow = true;
        flowerMesh.receiveShadow = true;

        // 4. Rock InstancedMesh Geometry & Material (🪨 Boulder)
        const rockGeo = new THREE.DodecahedronGeometry(0.45, 1);
        rockGeo.translate(0, 0.2, 0);
        const rockMat = new THREE.MeshStandardMaterial({
            color: 0x78716C,
            roughness: 0.9,
            metalness: 0.1
        });
        const rockMesh = new THREE.InstancedMesh(rockGeo, rockMat, maxCounts.rocks);
        rockMesh.count = 0;
        rockMesh.castShadow = true;
        rockMesh.receiveShadow = true;

        // 5. Star / Particle InstancedMesh Geometry & Material (✨ Glowing Star)
        const starGeo = new THREE.OctahedronGeometry(0.3, 0);
        starGeo.translate(0, 0.3, 0);
        const starMat = new THREE.MeshStandardMaterial({
            color: 0xFBBF24,
            emissive: 0xFBBF24,
            emissiveIntensity: 2.0,
            roughness: 0.1
        });
        const starMesh = new THREE.InstancedMesh(starGeo, starMat, maxCounts.stars);
        starMesh.count = 0;
        starMesh.castShadow = true;

        this.instancedMeshes.set('grass', grassMesh);
        this.instancedMeshes.set('trees', treeMesh);
        this.instancedMeshes.set('flowers', flowerMesh);
        this.instancedMeshes.set('rocks', rockMesh);
        this.instancedMeshes.set('stars', starMesh);

        this.instanceData.set('grass', []);
        this.instanceData.set('trees', []);
        this.instanceData.set('flowers', []);
        this.instanceData.set('rocks', []);
        this.instanceData.set('stars', []);

        this.paintedGroup = new THREE.Group();
        this.paintedGroup.add(grassMesh);
        this.paintedGroup.add(treeMesh);
        this.paintedGroup.add(flowerMesh);
        this.paintedGroup.add(rockMesh);
        this.paintedGroup.add(starMesh);

        this.scene.add(this.paintedGroup);

        // Visual Brush Cursor Ring Indicator on Ground
        const cursorGeo = new THREE.RingGeometry(2.4, 2.5, 32);
        cursorGeo.rotateX(-Math.PI / 2);
        const cursorMat = new THREE.MeshBasicMaterial({
            color: 0xFFD700,
            side: THREE.DoubleSide,
            transparent: true,
            opacity: 0.75
        });
        this.brushCursorMesh = new THREE.Mesh(cursorGeo, cursorMat);
        this.brushCursorMesh.position.set(0, -1.98, 0);
        this.brushCursorMesh.visible = false;
        this.scene.add(this.brushCursorMesh);
    }

    togglePaintMode(enabled) {
        this.isPaintModeEnabled = enabled;
        if (this.brushCursorMesh) {
            this.brushCursorMesh.visible = enabled && !this.isRecordingVideo;
        }
        if (enabled) {
            if (this.selectionBox) {
                this.scene.remove(this.selectionBox);
                this.selectionBox = null;
            }
        }
    }

    setBrushType(type) {
        if (this.instancedMeshes.has(type)) {
            this.activeBrushType = type;
        }
    }

    setBrushTool(tool) {
        this.activeBrushTool = tool;
    }

    setBrushRadius(radius) {
        this.brushRadius = radius;
        if (this.brushCursorMesh) {
            const scale = radius / 2.5;
            this.brushCursorMesh.scale.set(scale, scale, scale);
        }
    }

    setBrushDensity(density) {
        this.brushDensity = density;
    }

    paintInstancesAt(centerX, centerZ, brushType, radius, density) {
        if (!this.instancedMeshes.has(brushType)) return;
        const imesh = this.instancedMeshes.get(brushType);
        const dataArr = this.instanceData.get(brushType);
        const maxCount = imesh.instanceMatrix.count;

        const dummy = new THREE.Object3D();
        const groundY = this.groundMesh ? this.groundMesh.position.y : -2.0;

        for (let i = 0; i < density; i++) {
            if (dataArr.length >= maxCount) break;

            const angle = Math.random() * Math.PI * 2;
            const r = Math.sqrt(Math.random()) * radius;
            const px = centerX + Math.cos(angle) * r;
            const pz = centerZ + Math.sin(angle) * r;
            const rotY = Math.random() * Math.PI * 2;
            const scaleVal = 0.65 + Math.random() * 0.7;

            dataArr.push({
                x: px,
                y: groundY,
                z: pz,
                rotY: rotY,
                scale: scaleVal
            });

            const index = dataArr.length - 1;
            dummy.position.set(px, groundY, pz);
            dummy.rotation.y = rotY;
            dummy.scale.set(scaleVal, scaleVal, scaleVal);
            dummy.updateMatrix();

            imesh.setMatrixAt(index, dummy.matrix);
        }

        imesh.count = dataArr.length;
        imesh.instanceMatrix.needsUpdate = true;

        this.notifyTotalPaintedCount();
    }

    eraseInstancesAt(centerX, centerZ, radius) {
        const dataArr = this.instanceData.get(this.activeBrushType);
        const imesh = this.instancedMeshes.get(this.activeBrushType);
        if (!dataArr || !imesh) return;

        const radiusSq = radius * radius;
        const newData = [];

        for (let i = 0; i < dataArr.length; i++) {
            const item = dataArr[i];
            const dx = item.x - centerX;
            const dz = item.z - centerZ;
            if (dx * dx + dz * dz > radiusSq) {
                newData.push(item);
            }
        }

        this.instanceData.set(this.activeBrushType, newData);

        const dummy = new THREE.Object3D();
        for (let i = 0; i < newData.length; i++) {
            const item = newData[i];
            dummy.position.set(item.x, item.y, item.z);
            dummy.rotation.y = item.rotY;
            dummy.scale.set(item.scale, item.scale, item.scale);
            dummy.updateMatrix();
            imesh.setMatrixAt(i, dummy.matrix);
        }

        imesh.count = newData.length;
        imesh.instanceMatrix.needsUpdate = true;

        this.notifyTotalPaintedCount();
    }

    scatterInstances(brushType, count) {
        if (!this.instancedMeshes.has(brushType)) return;
        const imesh = this.instancedMeshes.get(brushType);
        const dataArr = this.instanceData.get(brushType);
        const maxCount = imesh.instanceMatrix.count;
        const dummy = new THREE.Object3D();
        const groundY = this.groundMesh ? this.groundMesh.position.y : -2.0;

        for (let i = 0; i < count; i++) {
            if (dataArr.length >= maxCount) break;

            const px = (Math.random() - 0.5) * 44;
            const pz = (Math.random() - 0.5) * 44;
            const rotY = Math.random() * Math.PI * 2;
            const scaleVal = 0.6 + Math.random() * 0.8;

            dataArr.push({
                x: px,
                y: groundY,
                z: pz,
                rotY: rotY,
                scale: scaleVal
            });

            const index = dataArr.length - 1;
            dummy.position.set(px, groundY, pz);
            dummy.rotation.y = rotY;
            dummy.scale.set(scaleVal, scaleVal, scaleVal);
            dummy.updateMatrix();

            imesh.setMatrixAt(index, dummy.matrix);
        }

        imesh.count = dataArr.length;
        imesh.instanceMatrix.needsUpdate = true;

        this.notifyTotalPaintedCount();
    }

    clearAllPaintedInstances() {
        this.instancedMeshes.forEach((imesh, key) => {
            imesh.count = 0;
            imesh.instanceMatrix.needsUpdate = true;
            this.instanceData.set(key, []);
        });

        this.notifyTotalPaintedCount();
    }

    notifyTotalPaintedCount() {
        let total = 0;
        this.instanceData.forEach(arr => total += arr.length);
        this.notifyAndroid('onSurfacePaintUpdate', { totalCount: total });
    }

    setupTouchDragListeners() {
        const dom = this.renderer.domElement;

        let initialPinchDistance = 0;
        let isTwoTouchDepthDrag = false;

        dom.addEventListener('pointerdown', (e) => this.onPointerDown(e));
        dom.addEventListener('pointermove', (e) => this.onPointerMove(e));
        dom.addEventListener('pointerup', (e) => this.onPointerUp(e));
        dom.addEventListener('pointercancel', (e) => this.onPointerUp(e));

        // Touch gesture for multi-touch Z-depth positioning
        dom.addEventListener('touchstart', (e) => {
            if (e.touches.length === 2 && this.dragTargetGroup) {
                const dx = e.touches[0].clientX - e.touches[1].clientX;
                const dy = e.touches[0].clientY - e.touches[1].clientY;
                initialPinchDistance = Math.hypot(dx, dy);
                isTwoTouchDepthDrag = true;
            }
        });

        dom.addEventListener('touchmove', (e) => {
            if (isTwoTouchDepthDrag && e.touches.length === 2 && this.dragTargetGroup) {
                const dx = e.touches[0].clientX - e.touches[1].clientX;
                const dy = e.touches[0].clientY - e.touches[1].clientY;
                const currentDist = Math.hypot(dx, dy);
                const delta = (currentDist - initialPinchDistance) * 0.02;
                initialPinchDistance = currentDist;

                // Shift object's Z depth into / out of screen!
                this.dragTargetGroup.position.z += delta;
                this.dragTargetGroup.position.z = Math.max(-10, Math.min(10, this.dragTargetGroup.position.z));
                this.updateSelectionBox();

                this.notifyAndroid('onObjectDragged', {
                    id: this.dragTargetGroup.userData.id,
                    posX: this.dragTargetGroup.position.x,
                    posY: this.dragTargetGroup.position.y,
                    posZ: this.dragTargetGroup.position.z
                });
                if (e.cancelable) e.preventDefault();
            }
        }, { passive: false });

        dom.addEventListener('touchend', () => {
            isTwoTouchDepthDrag = false;
        });
    }

    onPointerDown(event) {
        this.isPointerDown = true;

        // Instant Emergency Stop when user touches the screen during preview
        if (this.cameraMotionState && this.cameraMotionState.active && !this.cameraMotionState.isRecording) {
            this.emergencyStop();
        }

        const rect = this.renderer.domElement.getBoundingClientRect();
        this.mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1;
        this.mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1;

        this.raycaster.setFromCamera(this.mouse, this.camera);

        if (this.isPaintModeEnabled && this.groundMesh) {
            this.controls.enabled = false;
            const groundIntersects = this.raycaster.intersectObject(this.groundMesh, false);
            if (groundIntersects.length > 0) {
                const pt = groundIntersects[0].point;
                if (this.brushCursorMesh) {
                    this.brushCursorMesh.position.set(pt.x, -1.98, pt.z);
                    this.brushCursorMesh.visible = !this.isRecordingVideo;
                }
                if (this.activeBrushTool === 'paint') {
                    this.paintInstancesAt(pt.x, pt.z, this.activeBrushType, this.brushRadius, this.brushDensity);
                } else if (this.activeBrushTool === 'erase') {
                    this.eraseInstancesAt(pt.x, pt.z, this.brushRadius);
                }
            }
            return;
        }

        const clickableMeshes = [];
        this.objects.forEach((group) => {
            group.traverse((child) => {
                if (child.isMesh) {
                    child.userData.parentGroupId = group.userData.id;
                    clickableMeshes.push(child);
                }
            });
        });

        const intersects = this.raycaster.intersectObjects(clickableMeshes, false);

        if (intersects.length > 0) {
            const hitMesh = intersects[0].object;
            const targetId = hitMesh.userData.parentGroupId;

            if (targetId && this.objects.has(targetId)) {
                const group = this.objects.get(targetId);
                this.selectedObjectId = targetId;
                this.dragTargetGroup = group;
                this.isDraggingObject = true;

                // Temporarily disable OrbitControls while dragging 3D object directly!
                this.controls.enabled = false;

                // Create virtual drag plane parallel to camera at object position
                const normal = this.camera.getWorldDirection(new THREE.Vector3()).negate();
                this.dragPlane.setFromNormalAndCoplanarPoint(normal, group.position);

                if (this.raycaster.ray.intersectPlane(this.dragPlane, this.planeIntersect)) {
                    this.dragOffset.copy(group.position).sub(this.planeIntersect);
                }

                this.updateSelectionBox();

                this.notifyAndroid('onObjectSelected', {
                    id: targetId,
                    posX: group.position.x,
                    posY: group.position.y,
                    posZ: group.position.z
                });
            }
        }
    }

    onPointerMove(event) {
        const rect = this.renderer.domElement.getBoundingClientRect();
        this.mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1;
        this.mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1;

        this.raycaster.setFromCamera(this.mouse, this.camera);

        if (this.isPaintModeEnabled && this.groundMesh) {
            const groundIntersects = this.raycaster.intersectObject(this.groundMesh, false);
            if (groundIntersects.length > 0) {
                const pt = groundIntersects[0].point;
                if (this.brushCursorMesh) {
                    this.brushCursorMesh.position.set(pt.x, -1.98, pt.z);
                    this.brushCursorMesh.visible = !this.isRecordingVideo;
                }
                if (this.isPointerDown) {
                    if (this.activeBrushTool === 'paint') {
                        this.paintInstancesAt(pt.x, pt.z, this.activeBrushType, this.brushRadius, this.brushDensity);
                    } else if (this.activeBrushTool === 'erase') {
                        this.eraseInstancesAt(pt.x, pt.z, this.brushRadius);
                    }
                }
            }
            return;
        }

        if (!this.isDraggingObject || !this.dragTargetGroup) return;

        if (this.raycaster.ray.intersectPlane(this.dragPlane, this.planeIntersect)) {
            const newPos = this.planeIntersect.clone().add(this.dragOffset);
            this.dragTargetGroup.position.x = newPos.x;
            this.dragTargetGroup.position.y = newPos.y;

            this.updateSelectionBox();

            this.notifyAndroid('onObjectDragged', {
                id: this.dragTargetGroup.userData.id,
                posX: this.dragTargetGroup.position.x,
                posY: this.dragTargetGroup.position.y,
                posZ: this.dragTargetGroup.position.z
            });
        }
    }

    onPointerUp(event) {
        this.isPointerDown = false;
        if (this.isPaintModeEnabled) {
            this.controls.enabled = true;
        }
        if (this.isDraggingObject) {
            this.isDraggingObject = false;
            this.controls.enabled = true; // Restore OrbitControls
        }
    }

    updateSelectionBox() {
        if (!this.selectedObjectId || !this.objects.has(this.selectedObjectId) || this.isRecordingVideo) {
            if (this.selectionBox) {
                this.scene.remove(this.selectionBox);
                this.selectionBox = null;
            }
            return;
        }

        const group = this.objects.get(this.selectedObjectId);
        if (!this.selectionBox) {
            this.selectionBox = new THREE.BoxHelper(group, 0xFFFFD700);
            this.scene.add(this.selectionBox);
        } else {
            this.selectionBox.setFromObject(group);
            this.selectionBox.visible = true;
        }
    }

    selectObject(id) {
        if (this.objects.has(id)) {
            this.selectedObjectId = id;
            this.updateSelectionBox();
        }
    }

    applyRenderQuality(quality) {
        this.renderQuality = quality;
        let dpr = 1.5;
        let shadowSize = 2048;

        switch (quality) {
            case 'economy': // Thermal & RAM Saver Mode
                dpr = 1.0;
                shadowSize = 1024;
                break;
            case 'balanced':
                dpr = Math.min(window.devicePixelRatio || 1, 1.5);
                shadowSize = 2048;
                break;
            case 'high':
                dpr = Math.min(window.devicePixelRatio || 1, 2.0);
                shadowSize = 2048;
                break;
            case 'ultra':
                dpr = Math.min(window.devicePixelRatio || 1, 3.0);
                shadowSize = 4096;
                break;
        }

        this.renderer.setPixelRatio(dpr);
        if (this.dirLight1) {
            this.dirLight1.shadow.mapSize.width = shadowSize;
            this.dirLight1.shadow.mapSize.height = shadowSize;
            this.dirLight1.shadow.map = null;
        }
    }

    setRenderQuality(quality) {
        this.applyRenderQuality(quality);
    }

    buildProceduralTerrainGeometry(preset, heightScale = 1.0, noiseFreq = 1.0) {
        const segs = 128;
        const geo = new THREE.PlaneGeometry(1200, 1200, segs, segs);
        const pos = geo.attributes.position;
        const count = pos.count;

        for (let i = 0; i < count; i++) {
            const x = pos.getX(i);
            const y = pos.getY(i);
            let z = 0;

            switch (preset) {
                case 'grass_meadow':
                    z = (Math.sin(x * 0.04 * noiseFreq) * Math.cos(y * 0.04 * noiseFreq) * 2.8 + Math.sin(x * 0.15 * noiseFreq + y * 0.1) * 0.8) * heightScale;
                    break;
                case 'desert_dunes':
                    z = (Math.sin(x * 0.03 * noiseFreq + y * 0.015) * 4.5 + Math.sin(y * 0.08 * noiseFreq) * 1.2) * heightScale;
                    break;
                case 'glacial_snow':
                    z = (Math.sin(x * 0.06 * noiseFreq) * Math.cos(y * 0.06 * noiseFreq) * 5.5 + Math.sin(x * 0.2) * 1.5) * heightScale;
                    break;
                case 'rocky_terrain':
                    z = (Math.sin(x * 0.05 * noiseFreq) * Math.cos(y * 0.05 * noiseFreq) * 6.2 + Math.cos(x * 0.15 + y * 0.15) * 2.0) * heightScale;
                    break;
                case 'forest_ground':
                    z = (Math.sin(x * 0.04 * noiseFreq) * Math.sin(y * 0.04 * noiseFreq) * 1.8 + Math.cos(x * 0.15) * 0.6) * heightScale;
                    break;
                case 'animated_water':
                    z = Math.sin(x * 0.08 * noiseFreq) * 0.5 * heightScale;
                    break;
                case 'concrete_platform':
                default:
                    z = 0;
                    break;
            }

            pos.setZ(i, z);
        }

        geo.computeVertexNormals();
        return geo;
    }

    autoScatterForTerrain(preset) {
        if (!this.instancedMeshes) return;
        this.clearAllPaintedInstances();

        switch (preset) {
            case 'grass_meadow':
                this.scatterInstances('grass', 1500);
                this.scatterInstances('flowers', 150);
                this.scatterInstances('rocks', 40);
                break;
            case 'desert_dunes':
                this.scatterInstances('rocks', 300);
                this.scatterInstances('stars', 60);
                break;
            case 'forest_ground':
                this.scatterInstances('trees', 160);
                this.scatterInstances('grass', 600);
                this.scatterInstances('rocks', 80);
                break;
            case 'rocky_terrain':
                this.scatterInstances('rocks', 400);
                this.scatterInstances('trees', 30);
                break;
            case 'glacial_snow':
                this.scatterInstances('stars', 120);
                this.scatterInstances('rocks', 60);
                break;
        }
    }

    setupSkyEnvironment() {
        if (this.starFieldGroup) this.scene.remove(this.starFieldGroup);
        if (this.moonMeshGroup) this.scene.remove(this.moonMeshGroup);
        if (this.sunMeshGroup) this.scene.remove(this.sunMeshGroup);
        if (this.cloudsMeshGroup) this.scene.remove(this.cloudsMeshGroup);

        this.twinkleStarsList = [];
        this.starFieldGroup = null;
        this.moonMeshGroup = null;
        this.sunMeshGroup = null;
        this.cloudsMeshGroup = null;

        if (this.stageTheme === 'transparent') {
            this.renderer.setClearColor(0x000000, 0);
            this.scene.background = null;
            this.scene.fog = null;
            return;
        }

        switch (this.skyPreset) {
            case 'deep_space':
                this.renderer.setClearColor(0x02040A, 1);
                this.scene.background = new THREE.Color(0x02040A);
                this.scene.fog = new THREE.FogExp2(0x02040A, 0.003);
                this.buildTwinklingStarField(this.starDensity);
                if (this.moonPhase !== 'none') {
                    this.buildMoonMesh(this.moonPhase);
                }
                break;

            case 'night_nebula':
                this.renderer.setClearColor(0x0A0618, 1);
                this.scene.background = new THREE.Color(0x0A0618);
                this.scene.fog = new THREE.FogExp2(0x0A0618, 0.003);
                this.buildTwinklingStarField(this.starDensity);
                if (this.moonPhase !== 'none') {
                    this.buildMoonMesh(this.moonPhase);
                }
                break;

            case 'blue_sky_sun':
                this.renderer.setClearColor(0x38BDF8, 1);
                this.scene.background = new THREE.Color(0x38BDF8);
                this.scene.fog = new THREE.FogExp2(0x38BDF8, 0.003);
                this.buildSunAndClouds('blue_sky_sun');
                break;

            case 'sunset_golden':
                this.renderer.setClearColor(0xEA580C, 1);
                this.scene.background = new THREE.Color(0xEA580C);
                this.scene.fog = new THREE.FogExp2(0xEA580C, 0.004);
                this.buildSunAndClouds('sunset_golden');
                break;

            case '3dsmax_white':
                this.renderer.setClearColor(0xF4F4F6, 1);
                this.scene.background = new THREE.Color(0xF4F4F6);
                this.scene.fog = new THREE.FogExp2(0xF4F4F6, 0.004);
                break;

            default:
                this.renderer.setClearColor(0x121214, 1);
                this.scene.background = new THREE.Color(0x121214);
                this.scene.fog = new THREE.FogExp2(0x121214, 0.004);
                break;
        }
    }

    buildTwinklingStarField(count = 1000) {
        this.starFieldGroup = new THREE.Group();
        this.twinkleStarsList = [];

        const starGeo = new THREE.OctahedronGeometry(0.18, 0);
        const colors = [0xFFFFFF, 0xBAE6FD, 0xFEF08A, 0xFDE68A, 0xE0E7FF];

        for (let i = 0; i < count; i++) {
            const mat = new THREE.MeshBasicMaterial({
                color: colors[Math.floor(Math.random() * colors.length)],
                transparent: true,
                opacity: 0.2 + Math.random() * 0.8
            });
            const starMesh = new THREE.Mesh(starGeo, mat);

            const radius = 35 + Math.random() * 35;
            const theta = Math.random() * Math.PI * 2;
            const phi = Math.acos((Math.random() * 2) - 1);

            starMesh.position.x = radius * Math.sin(phi) * Math.cos(theta);
            starMesh.position.y = Math.abs(radius * Math.sin(phi) * Math.sin(theta));
            starMesh.position.z = radius * Math.cos(phi);

            const baseScale = 0.4 + Math.random() * 0.9;
            starMesh.scale.set(baseScale, baseScale, baseScale);

            this.starFieldGroup.add(starMesh);

            this.twinkleStarsList.push({
                mesh: starMesh,
                phase: Math.random() * Math.PI * 2,
                speed: 0.02 + Math.random() * 0.05,
                baseScale: baseScale
            });
        }

        this.scene.add(this.starFieldGroup);
    }

    buildMoonMesh(phase) {
        this.moonMeshGroup = new THREE.Group();
        this.moonMeshGroup.position.set(-9.0, 15.0, -26.0);

        if (phase === 'full_moon') {
            const moonGeo = new THREE.SphereGeometry(3.2, 32, 32);

            const canvas = document.createElement('canvas');
            canvas.width = 512; canvas.height = 512;
            const ctx = canvas.getContext('2d');
            ctx.fillStyle = '#E2E8F0';
            ctx.fillRect(0, 0, 512, 512);

            for (let i = 0; i < 35; i++) {
                const cx = Math.random() * 512;
                const cy = Math.random() * 512;
                const cr = 8 + Math.random() * 35;
                ctx.fillStyle = '#94A3B8';
                ctx.beginPath();
                ctx.arc(cx, cy, cr, 0, Math.PI * 2);
                ctx.fill();

                ctx.fillStyle = '#CBD5E1';
                ctx.beginPath();
                ctx.arc(cx - 3, cy - 3, cr * 0.7, 0, Math.PI * 2);
                ctx.fill();
            }

            const moonTex = new THREE.CanvasTexture(canvas);
            const moonMat = new THREE.MeshStandardMaterial({
                map: moonTex,
                roughness: 0.8,
                metalness: 0.1,
                emissive: 0xF1F5F9,
                emissiveIntensity: 0.45
            });

            const moonMesh = new THREE.Mesh(moonGeo, moonMat);

            const haloGeo = new THREE.SphereGeometry(3.8, 24, 24);
            const haloMat = new THREE.MeshBasicMaterial({
                color: 0xE2E8F0,
                transparent: true,
                opacity: 0.22
            });
            const haloMesh = new THREE.Mesh(haloGeo, haloMat);

            this.moonMeshGroup.add(moonMesh);
            this.moonMeshGroup.add(haloMesh);

        } else if (phase === 'crescent') {
            const shape = new THREE.Shape();
            shape.absarc(0, 0, 3.2, -Math.PI * 0.4, Math.PI * 0.4, false);
            shape.absarc(1.2, 0, 2.9, Math.PI * 0.4, -Math.PI * 0.4, true);

            const extrudeSettings = {
                depth: 0.8,
                bevelEnabled: true,
                bevelThickness: 0.2,
                bevelSize: 0.15,
                bevelSegments: 4
            };

            const crescentGeo = new THREE.ExtrudeGeometry(shape, extrudeSettings);
            crescentGeo.center();

            const crescentMat = new THREE.MeshStandardMaterial({
                color: 0xFEF08A,
                emissive: 0xFDE047,
                emissiveIntensity: 0.8,
                roughness: 0.3,
                metalness: 0.2
            });

            const crescentMesh = new THREE.Mesh(crescentGeo, crescentMat);

            const haloGeo = new THREE.SphereGeometry(3.6, 24, 24);
            const haloMat = new THREE.MeshBasicMaterial({
                color: 0xFEF08A,
                transparent: true,
                opacity: 0.28
            });
            const haloMesh = new THREE.Mesh(haloGeo, haloMat);

            this.moonMeshGroup.add(crescentMesh);
            this.moonMeshGroup.add(haloMesh);
        }

        this.scene.add(this.moonMeshGroup);
    }

    buildSunAndClouds(preset) {
        this.sunMeshGroup = new THREE.Group();
        this.sunMeshGroup.position.set(12.0, 18.0, -26.0);

        const sunColor = preset === 'sunset_golden' ? 0xEA580C : 0xFBBF24;
        const sunGeo = new THREE.SphereGeometry(3.5, 32, 32);
        const sunMat = new THREE.MeshBasicMaterial({
            color: sunColor
        });
        const sunMesh = new THREE.Mesh(sunGeo, sunMat);

        const haloGeo = new THREE.SphereGeometry(5.2, 24, 24);
        const haloMat = new THREE.MeshBasicMaterial({
            color: sunColor,
            transparent: true,
            opacity: 0.35
        });
        const haloMesh = new THREE.Mesh(haloGeo, haloMat);

        this.sunMeshGroup.add(sunMesh);
        this.sunMeshGroup.add(haloMesh);
        this.scene.add(this.sunMeshGroup);

        if (this.dirLight1) {
            this.dirLight1.position.set(12, 18, 10);
            this.dirLight1.color.setHex(preset === 'sunset_golden' ? 0xF97316 : 0xFFFFFF);
        }

        this.cloudsMeshGroup = new THREE.Group();

        const cloudMat = new THREE.MeshStandardMaterial({
            color: preset === 'sunset_golden' ? 0xFDE68A : 0xFFFFFF,
            roughness: 0.9,
            metalness: 0.05,
            transparent: true,
            opacity: 0.9
        });

        for (let i = 0; i < 12; i++) {
            const cloudCluster = new THREE.Group();
            const puffCount = 6 + Math.floor(Math.random() * 5);

            for (let j = 0; j < puffCount; j++) {
                const r = 1.0 + Math.random() * 1.8;
                const puffGeo = new THREE.DodecahedronGeometry(r, 1);
                const puffMesh = new THREE.Mesh(puffGeo, cloudMat);

                puffMesh.position.set(
                    (j - puffCount / 2) * 1.6 + (Math.random() - 0.5) * 1.2,
                    (Math.random() - 0.5) * 1.0,
                    (Math.random() - 0.5) * 1.5
                );
                cloudCluster.add(puffMesh);
            }

            cloudCluster.position.set(
                (Math.random() - 0.5) * 70,
                11.0 + Math.random() * 8.0,
                -15.0 - Math.random() * 18.0
            );

            this.cloudsMeshGroup.add(cloudCluster);
        }

        this.scene.add(this.cloudsMeshGroup);
    }

    setupEnvironmentStage() {
        if (this.groundMesh) this.scene.remove(this.groundMesh);
        if (this.backdropMesh) this.scene.remove(this.backdropMesh);
        if (this.gridHelper) this.scene.remove(this.gridHelper);
        if (this.axesHelper) this.scene.remove(this.axesHelper);

        this.setupSkyEnvironment();

        // 1. HORIZONTAL 3D PROCEDURAL TERRAIN MESH
        const activePreset = this.terrainPreset || this.groundPreset || 'grass_meadow';
        const groundGeo = this.buildProceduralTerrainGeometry(activePreset, this.terrainHeight, this.terrainNoiseFreq);
        const groundMat = this.createGroundMaterial(activePreset);
        this.groundMesh = new THREE.Mesh(groundGeo, groundMat);
        this.groundMesh.rotation.x = -Math.PI / 2;
        this.groundMesh.position.y = -2.0;
        this.groundMesh.receiveShadow = true;
        this.scene.add(this.groundMesh);

        this.autoScatterForTerrain(activePreset);

        // 2. VERTICAL BACKDROP WALL (الخلفية العمودية)
        if (this.backdropPreset !== 'none' && this.stageTheme !== 'transparent') {
            const backdropGeo = new THREE.PlaneGeometry(80, 45);
            const backdropMat = this.createBackdropMaterial(this.backdropPreset);
            this.backdropMesh = new THREE.Mesh(backdropGeo, backdropMat);
            this.backdropMesh.position.set(0, 15.0, -20.0);
            this.scene.add(this.backdropMesh);
        }

        // 3D Coordinate Axes Triad (X/Y/Z) - Only visible when not recording video!
        this.axesHelper = new THREE.AxesHelper(3.5);
        this.axesHelper.position.set(0, -1.99, 0);
        if (!this.isRecordingCamera && !this.isRecordingVideo) {
            this.scene.add(this.axesHelper);
        }
    }

    setStageTheme(theme) {
        this.stageTheme = theme;
        this.setupEnvironmentStage();
    }

    // Custom Texture Loader for Images Imported from Phone Gallery
    loadCustomTexture(targetType, base64Url) {
        const loader = new THREE.TextureLoader();
        loader.load(base64Url, (texture) => {
            texture.wrapS = THREE.RepeatWrapping;
            texture.wrapT = THREE.RepeatWrapping;
            texture.anisotropy = 16;
            texture.needsUpdate = true;

            if (targetType === 'text') {
                this.textCustomTexture = texture;
                if (this.objects.has('text_main')) {
                    const group = this.objects.get('text_main');
                    group.children.forEach(mesh => {
                        if (mesh.isMesh) {
                            mesh.material.map = texture;
                            mesh.material.needsUpdate = true;
                        }
                    });
                }
            } else if (targetType === 'ground') {
                this.groundTexture = texture;
                texture.repeat.set(4, 4);
                if (this.groundMesh) {
                    this.groundMesh.material.map = texture;
                    this.groundMesh.material.needsUpdate = true;
                }
            } else if (targetType === 'backdrop') {
                this.backdropTexture = texture;
                if (this.backdropMesh) {
                    this.backdropMesh.material.map = texture;
                    this.backdropMesh.material.needsUpdate = true;
                }
            }
        });
    }

    toggleTextureAnimation(enabled) {
        this.isTextureAnimating = enabled;
    }

    createGroundMaterial(preset) {
        if (this.groundTexture && preset === 'custom_phone') {
            return new THREE.MeshStandardMaterial({ map: this.groundTexture, roughness: 0.5, metalness: 0.1 });
        }

        const canvas = document.createElement('canvas');
        canvas.width = 1024;
        canvas.height = 1024;
        const ctx = canvas.getContext('2d');

        switch (preset) {
            case 'grass':
            case 'grass_meadow':
                ctx.fillStyle = '#15803D';
                ctx.fillRect(0, 0, 1024, 1024);
                for (let i = 0; i < 40000; i++) {
                    const x = Math.random() * 1024;
                    const y = Math.random() * 1024;
                    ctx.fillStyle = Math.random() > 0.5 ? '#22C55E' : '#166534';
                    ctx.fillRect(x, y, 2, 8);
                }
                break;
            case 'sand':
            case 'desert_dunes':
                ctx.fillStyle = '#D97706';
                ctx.fillRect(0, 0, 1024, 1024);
                for (let i = 0; i < 30000; i++) {
                    ctx.fillStyle = Math.random() > 0.5 ? '#F59E0B' : '#B45309';
                    ctx.fillRect(Math.random() * 1024, Math.random() * 1024, 3, 3);
                }
                break;
            case 'glacial_snow':
                ctx.fillStyle = '#F8FAFC';
                ctx.fillRect(0, 0, 1024, 1024);
                for (let i = 0; i < 20000; i++) {
                    ctx.fillStyle = Math.random() > 0.5 ? '#E2E8F0' : '#BAE6FD';
                    ctx.fillRect(Math.random() * 1024, Math.random() * 1024, 4, 4);
                }
                break;
            case 'rocky_terrain':
                ctx.fillStyle = '#57534E';
                ctx.fillRect(0, 0, 1024, 1024);
                for (let i = 0; i < 25000; i++) {
                    ctx.fillStyle = Math.random() > 0.5 ? '#78716C' : '#292524';
                    ctx.fillRect(Math.random() * 1024, Math.random() * 1024, 4, 4);
                }
                break;
            case 'forest_ground':
                ctx.fillStyle = '#451A03';
                ctx.fillRect(0, 0, 1024, 1024);
                for (let i = 0; i < 25000; i++) {
                    ctx.fillStyle = Math.random() > 0.5 ? '#78350F' : '#166534';
                    ctx.fillRect(Math.random() * 1024, Math.random() * 1024, 3, 5);
                }
                break;
            case 'animated_water':
                ctx.fillStyle = '#0284C7';
                ctx.fillRect(0, 0, 1024, 1024);
                ctx.strokeStyle = '#38BDF8';
                ctx.lineWidth = 6;
                for (let i = 0; i < 20; i++) {
                    ctx.beginPath();
                    ctx.arc(Math.random() * 1024, Math.random() * 1024, 40 + Math.random() * 100, 0, Math.PI * 2);
                    ctx.stroke();
                }
                break;
            case 'wood_deck':
                ctx.fillStyle = '#78350F';
                ctx.fillRect(0, 0, 1024, 1024);
                ctx.fillStyle = '#451A03';
                for (let y = 0; y < 1024; y += 128) {
                    ctx.fillRect(0, y, 1024, 6);
                }
                break;
            case 'marble':
                ctx.fillStyle = '#F8FAFC';
                ctx.fillRect(0, 0, 1024, 1024);
                ctx.strokeStyle = '#CBD5E1';
                ctx.lineWidth = 4;
                for (let i = 0; i < 12; i++) {
                    ctx.beginPath();
                    ctx.moveTo(Math.random() * 1024, 0);
                    ctx.bezierCurveTo(Math.random() * 1024, 300, Math.random() * 1024, 700, Math.random() * 1024, 1024);
                    ctx.stroke();
                }
                break;
            case 'concrete_platform':
            default:
                ctx.fillStyle = '#F4F4F6';
                ctx.fillRect(0, 0, 1024, 1024);
                ctx.strokeStyle = '#CBD5E1';
                ctx.lineWidth = 8;
                for (let x = 0; x < 1024; x += 128) {
                    ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, 1024); ctx.stroke();
                    ctx.beginPath(); ctx.moveTo(0, x); ctx.lineTo(1024, x); ctx.stroke();
                }
        }

        const texture = new THREE.CanvasTexture(canvas);
        texture.wrapS = THREE.RepeatWrapping;
        texture.wrapT = THREE.RepeatWrapping;
        texture.repeat.set(6, 6);

        this.currentGroundTex = texture;

        return new THREE.MeshStandardMaterial({
            map: texture,
            roughness: preset === 'marble' ? 0.1 : 0.8,
            metalness: preset === 'marble' ? 0.2 : 0.05
        });
    }

    createBackdropMaterial(preset) {
        if (this.backdropTexture && preset === 'custom_phone') {
            return new THREE.MeshBasicMaterial({ map: this.backdropTexture });
        }

        const canvas = document.createElement('canvas');
        canvas.width = 1024;
        canvas.height = 512;
        const ctx = canvas.getContext('2d');

        switch (preset) {
            case 'mountains':
                const skyGrad = ctx.createLinearGradient(0, 0, 0, 512);
                skyGrad.addColorStop(0, '#0284C7');
                skyGrad.addColorStop(0.6, '#38BDF8');
                skyGrad.addColorStop(1, '#BAE6FD');
                ctx.fillStyle = skyGrad;
                ctx.fillRect(0, 0, 1024, 512);

                ctx.fillStyle = '#334155';
                ctx.beginPath();
                ctx.moveTo(0, 350);
                ctx.lineTo(200, 180); ctx.lineTo(350, 280);
                ctx.lineTo(550, 120); ctx.lineTo(750, 300);
                ctx.lineTo(900, 160); ctx.lineTo(1024, 350);
                ctx.lineTo(1024, 512); ctx.lineTo(0, 512);
                ctx.fill();

                ctx.fillStyle = '#F8FAFC';
                ctx.beginPath();
                ctx.moveTo(200, 180); ctx.lineTo(240, 210); ctx.lineTo(160, 210); ctx.fill();
                ctx.beginPath();
                ctx.moveTo(550, 120); ctx.lineTo(600, 170); ctx.lineTo(500, 170); ctx.fill();
                break;

            case 'sunset':
                const sunGrad = ctx.createLinearGradient(0, 0, 0, 512);
                sunGrad.addColorStop(0, '#7C2D12');
                sunGrad.addColorStop(0.4, '#EA580C');
                sunGrad.addColorStop(0.8, '#F59E0B');
                sunGrad.addColorStop(1, '#FEF08A');
                ctx.fillStyle = sunGrad;
                ctx.fillRect(0, 0, 1024, 512);
                break;

            default:
                ctx.fillStyle = '#E2E8F0';
                ctx.fillRect(0, 0, 1024, 512);
        }

        const texture = new THREE.CanvasTexture(canvas);
        this.currentBackdropTex = texture;
        return new THREE.MeshBasicMaterial({ map: texture });
    }

    setGroundTexture(preset) {
        this.groundPreset = preset;
        this.setupEnvironmentStage();
    }

    setBackdropTexture(preset) {
        this.backdropPreset = preset;
        this.setupEnvironmentStage();
    }

    // MULTI-SHOT CAMERA MOTION SEQUENCE ENGINE (سلاسة فائقة 60fps عبر requestAnimationFrame بدون أي تجميد أو تقطيع)
    calculateCameraMotion(sequence, elapsedSec) {
        if (!sequence || sequence.length === 0) return null;
        let accumulatedTime = 0;
        let currentStep = sequence[sequence.length - 1];
        let stepProgress = 1.0;

        for (let i = 0; i < sequence.length; i++) {
            const step = sequence[i];
            const stepDuration = Math.max(0.1, step.duration || step.durationSec || 5);
            if (elapsedSec < accumulatedTime + stepDuration) {
                currentStep = step;
                stepProgress = (elapsedSec - accumulatedTime) / stepDuration;
                break;
            }
            accumulatedTime += stepDuration;
        }
        stepProgress = Math.max(0.0, Math.min(1.0, stepProgress));

        const radius = 9.5;
        const angle = stepProgress * Math.PI * 2;
        const result = {
            pos: new THREE.Vector3(0, 1.8, 9.5),
            target: new THREE.Vector3(0, 0, 0)
        };

        switch (currentStep.preset) {
            case 'top':
                // Top High Angle Shot (كاميرا من الجهة العلوية)
                result.pos.set(0, 12.0 - (1 - stepProgress) * 2.0, 0.5);
                result.target.set(0, 0, 0);
                break;
            case 'bottom':
                // Bottom Low Angle Shot (كاميرا من الجهة السفلية)
                result.pos.set(0, -0.5 + stepProgress * 0.3, 6.5);
                result.target.set(0, 0.8, 0);
                break;
            case 'left_side':
                result.pos.set(-10.0 + stepProgress * 2.0, 2.0, 3.0);
                result.target.set(0, 0, 0);
                break;
            case 'right_side':
                result.pos.set(10.0 - stepProgress * 2.0, 2.0, 3.0);
                result.target.set(0, 0, 0);
                break;
            case 'close_up':
                result.pos.set(0, 0.5, 5.0 - stepProgress * 1.8);
                result.target.set(0, 0, 0);
                break;
            case 'hero_wide':
                result.pos.set(6.0 - stepProgress * 3.0, 4.0, 12.0);
                result.target.set(0, 0, 0);
                break;
            case 'orbit360':
                result.pos.set(radius * Math.sin(angle), 1.8, radius * Math.cos(angle));
                result.target.set(0, 0, 0);
                break;
            case 'descent':
                result.pos.set(0, 10.0 - stepProgress * 8.2, radius);
                result.target.set(0, 0, 0);
                break;
            case 'zoomOut':
                result.pos.set(0, 1.8, 6.0 + stepProgress * 16.0);
                result.target.set(0, 0, 0);
                break;
            case 'zoomIn':
                result.pos.set(0, 1.8, 22.0 - stepProgress * 16.0);
                result.target.set(0, 0, 0);
                break;
            case 'droneFlyby':
                result.pos.set(-15 + stepProgress * 30, 3.0 + Math.sin(stepProgress * Math.PI) * 4, 10.0 - Math.sin(stepProgress * Math.PI) * 4);
                result.target.set(0, 0, 0);
                break;
            case 'craneReveal':
                result.pos.set(0, 12.0 - stepProgress * 10.2, 3.0 + stepProgress * 6.5);
                result.target.set(0, 0, 0);
                break;
            case 'custom_waypoint':
                const sx = currentStep.startX !== undefined ? currentStep.startX : 0;
                const sy = currentStep.startY !== undefined ? currentStep.startY : 1.8;
                const sz = currentStep.startZ !== undefined ? currentStep.startZ : 9.5;
                const ex = currentStep.endX !== undefined ? currentStep.endX : 0;
                const ey = currentStep.endY !== undefined ? currentStep.endY : 0.5;
                const ez = currentStep.endZ !== undefined ? currentStep.endZ : 4.0;
                result.pos.set(sx + (ex - sx) * stepProgress, sy + (ey - sy) * stepProgress, sz + (ez - sz) * stepProgress);
                result.target.set(currentStep.targetX || 0, currentStep.targetY || 0, currentStep.targetZ || 0);
                break;
            default:
                result.pos.set(0, 1.8, 9.5);
                result.target.set(0, 0, 0);
        }
        return result;
    }

    playCameraSequence(sequenceJson, isRecording = false) {
        this.stopCameraSequence();

        let sequence = [];
        try {
            sequence = typeof sequenceJson === 'string' ? JSON.parse(sequenceJson) : sequenceJson;
        } catch (e) {
            console.error("Invalid sequence json", e);
            return;
        }

        if (!Array.isArray(sequence) || sequence.length === 0) return;

        const totalDuration = sequence.reduce((acc, step) => acc + (step.duration || step.durationSec || 5), 0);

        if (isRecording) {
            this.cameraKeyframes = [];
            this.isRecordingCamera = true;
            this.isRecordingVideo = true;
            if (this.axesHelper) this.axesHelper.visible = false;
            if (this.lightBulbMeshGroup) this.lightBulbMeshGroup.visible = false;
            if (this.cameraPathMeshGroup) this.cameraPathMeshGroup.visible = false;
            if (this.selectionBox) this.selectionBox.visible = false;
            if (this.brushCursorMesh) this.brushCursorMesh.visible = false;
            this.notifyAndroid('onCameraRecordingState', { status: 'recording' });
        } else {
            this.notifyAndroid('onCameraRecordingState', { status: 'previewing' });
        }

        this.cameraMotionState = {
            active: true,
            isRecording: isRecording,
            sequence: sequence,
            totalDuration: totalDuration,
            startTime: performance.now(),
            lastRecordedSec: -1
        };
    }

    emergencyStop() {
        if (this.sequenceInterval) {
            clearInterval(this.sequenceInterval);
            this.sequenceInterval = null;
        }

        if (this.videoExportState) {
            if (this.videoExportState.recorder) {
                try {
                    if (this.videoExportState.recorder.state !== 'inactive') {
                        this.videoExportState.recorder.stop();
                    }
                } catch (e) {}
            }
            this.videoExportState.active = false;
            this.videoExportState = null;
        }

        if (this.cameraMotionState) {
            this.cameraMotionState.active = false;
            this.cameraMotionState = null;
        }

        this.isPlaying = false;
        this.isRecordingVideo = false;
        this.isRecordingCamera = false;

        if (this.axesHelper) this.axesHelper.visible = true;
        if (this.lightBulbMeshGroup && this.showLightBulbMesh) this.lightBulbMeshGroup.visible = true;
        if (this.cameraPathMeshGroup && this.showCameraPath) this.cameraPathMeshGroup.visible = true;
        if (this.selectionBox && this.selectedObjectId) this.selectionBox.visible = true;
        if (this.brushCursorMesh) this.brushCursorMesh.visible = false;

        if (this.controls) {
            this.controls.enabled = true;
            this.controls.update();
        }

        this.notifyAndroid('onCameraRecordingState', { status: 'stopped', count: this.cameraKeyframes.length, immediate: true });
    }

    stopCameraSequence() {
        this.emergencyStop();
    }

    initOfflineFont() {
        if (window.OFFLINE_FONT_DATA && THREE.FontLoader) {
            try {
                const loader = new THREE.FontLoader();
                window.loadedFont = loader.parse(window.OFFLINE_FONT_DATA);
            } catch (e) {
                console.warn("Could not parse offline font bundle", e);
            }
        }
    }

    setupLighting() {
        this.ambientLight = new THREE.AmbientLight(0xffffff, 0.9);
        this.scene.add(this.ambientLight);

        this.dirLight1 = new THREE.DirectionalLight(0xFFFFFF, 2.2);
        this.dirLight1.position.set(8, 12, 10);
        this.dirLight1.castShadow = true;
        this.dirLight1.shadow.mapSize.width = 2048;
        this.dirLight1.shadow.mapSize.height = 2048;
        this.dirLight1.shadow.bias = -0.0001;
        this.scene.add(this.dirLight1);

        this.dirLight2 = new THREE.DirectionalLight(0xBAE6FD, 1.1);
        this.dirLight2.position.set(-8, -2, -6);
        this.scene.add(this.dirLight2);

        this.pointLight = new THREE.PointLight(0xFFD700, 3.5, 35);
        this.pointLight.position.set(0, 4.0, 3.5);
        this.pointLight.castShadow = true;
        this.pointLight.shadow.bias = -0.0002;
        this.scene.add(this.pointLight);

        this.setupLightBulbMesh();
    }

    setupLightBulbMesh() {
        if (this.lightBulbMeshGroup) this.scene.remove(this.lightBulbMeshGroup);

        this.lightBulbMeshGroup = new THREE.Group();

        // 1. Glowing Glass Bulb Sphere
        const bulbGeo = new THREE.SphereGeometry(0.4, 24, 24);
        const bulbMat = new THREE.MeshStandardMaterial({
            color: 0xFFD700,
            emissive: 0xFFD700,
            emissiveIntensity: 2.5,
            roughness: 0.1,
            metalness: 0.1,
            transparent: true,
            opacity: 0.92
        });
        const bulbMesh = new THREE.Mesh(bulbGeo, bulbMat);

        // 2. Metallic Socket Base
        const baseGeo = new THREE.CylinderGeometry(0.2, 0.15, 0.35, 16);
        const baseMat = new THREE.MeshStandardMaterial({
            color: 0x9CA3AF,
            metalness: 0.9,
            roughness: 0.2
        });
        const baseMesh = new THREE.Mesh(baseGeo, baseMat);
        baseMesh.position.y = 0.35;

        // 3. Glowing Light Halo Outer Ring
        const haloGeo = new THREE.SphereGeometry(0.65, 16, 16);
        const haloMat = new THREE.MeshBasicMaterial({
            color: 0xFFD700,
            transparent: true,
            opacity: 0.25
        });
        const haloMesh = new THREE.Mesh(haloGeo, haloMat);

        this.lightBulbMeshGroup.add(bulbMesh);
        this.lightBulbMeshGroup.add(baseMesh);
        this.lightBulbMeshGroup.add(haloMesh);

        this.lightBulbMeshGroup.position.copy(this.pointLight.position);
        this.lightBulbMeshGroup.userData = { id: 'light_bulb_mesh', isLightBulb: true };
        this.scene.add(this.lightBulbMeshGroup);

        this.showLightBulbMesh = true;
    }

    setLightPosition(x, y, z) {
        this.pointLight.position.set(x, y, z);
        if (this.lightBulbMeshGroup) {
            this.lightBulbMeshGroup.position.set(x, y, z);
        }
    }

    setLightColor(hexColor) {
        const cleanHex = hexColor.replace('#', '');
        const colorVal = parseInt(cleanHex, 16) || 0xFFD700;
        this.pointLight.color.setHex(colorVal);

        if (this.lightBulbMeshGroup && this.lightBulbMeshGroup.children[0]) {
            this.lightBulbMeshGroup.children[0].material.color.setHex(colorVal);
            this.lightBulbMeshGroup.children[0].material.emissive.setHex(colorVal);
        }
    }

    setLightIntensity(intensity) {
        this.pointLight.intensity = intensity;
    }

    toggleLightBulbMesh(visible) {
        this.showLightBulbMesh = visible;
        if (this.lightBulbMeshGroup) {
            this.lightBulbMeshGroup.visible = visible && !this.isRecordingVideo;
        }
    }

    // 3D Visual Camera Path Trajectory Engine
    updateCameraPathCurve(sequenceJson) {
        if (this.cameraPathMeshGroup) {
            this.scene.remove(this.cameraPathMeshGroup);
            this.cameraPathMeshGroup = null;
        }

        let sequence = [];
        try {
            sequence = typeof sequenceJson === 'string' ? JSON.parse(sequenceJson) : sequenceJson;
        } catch (e) { return; }

        if (!Array.isArray(sequence) || sequence.length < 2) return;

        this.cameraPathMeshGroup = new THREE.Group();

        const points = [];
        sequence.forEach((step) => {
            const pos = this.getCameraPresetPosition(step.preset);
            points.push(pos);
        });

        const curve = new THREE.CatmullRomCurve3(points);
        const curvePoints = curve.getPoints(60);
        const geometry = new THREE.BufferGeometry().setFromPoints(curvePoints);
        const material = new THREE.LineBasicMaterial({
            color: 0x38BDF8,
            linewidth: 4
        });
        const line = new THREE.Line(geometry, material);
        this.cameraPathMeshGroup.add(line);

        // Visual Start Node (Green Sphere) and End Node (Red Sphere)
        const nodeGeo = new THREE.SphereGeometry(0.25, 16, 16);
        const startMat = new THREE.MeshBasicMaterial({ color: 0x10B981 });
        const endMat = new THREE.MeshBasicMaterial({ color: 0xEF4444 });

        const startMesh = new THREE.Mesh(nodeGeo, startMat);
        startMesh.position.copy(points[0]);

        const endMesh = new THREE.Mesh(nodeGeo, endMat);
        endMesh.position.copy(points[points.length - 1]);

        this.cameraPathMeshGroup.add(startMesh);
        this.cameraPathMeshGroup.add(endMesh);

        this.cameraPathMeshGroup.visible = !this.isRecordingVideo;
        this.scene.add(this.cameraPathMeshGroup);
        this.showCameraPath = true;
    }

    getCameraPresetPosition(preset) {
        const radius = 9.5;
        switch (preset) {
            case 'top': return new THREE.Vector3(0, 12.0, 0.5);
            case 'bottom': return new THREE.Vector3(0, -0.5, 6.5);
            case 'left_side': return new THREE.Vector3(-10.0, 2.0, 3.0);
            case 'right_side': return new THREE.Vector3(10.0, 2.0, 3.0);
            case 'close_up': return new THREE.Vector3(0, 0.5, 3.5);
            case 'hero_wide': return new THREE.Vector3(6.0, 4.0, 12.0);
            case 'descent': return new THREE.Vector3(0, 10.0, radius);
            case 'zoomOut': return new THREE.Vector3(0, 1.8, 22.0);
            case 'zoomIn': return new THREE.Vector3(0, 1.8, 6.0);
            case 'orbit360':
            default: return new THREE.Vector3(0, 1.8, radius);
        }
    }

    createMaterial(preset = 'gold', customParams = {}) {
        let mat;
        switch (preset) {
            case 'gold':
                mat = new THREE.MeshStandardMaterial({
                    color: 0xD4AF37,
                    metalness: 0.95,
                    roughness: 0.12,
                    emissive: 0x221D00,
                    emissiveIntensity: 0.15,
                    side: THREE.DoubleSide
                });
                break;
            case 'silver':
                mat = new THREE.MeshStandardMaterial({
                    color: 0xE2E8F0,
                    metalness: 0.92,
                    roughness: 0.1,
                    side: THREE.DoubleSide
                });
                break;
            case 'chrome':
                mat = new THREE.MeshStandardMaterial({
                    color: 0xFFFFFF,
                    metalness: 1.0,
                    roughness: 0.02,
                    side: THREE.DoubleSide
                });
                break;
            case 'glass':
                mat = new THREE.MeshPhysicalMaterial({
                    color: 0xE0F2FE,
                    metalness: 0.05,
                    roughness: 0.05,
                    transmission: 0.92,
                    transparent: true,
                    opacity: 0.88,
                    side: THREE.DoubleSide
                });
                break;
            case 'neon':
                mat = new THREE.MeshStandardMaterial({
                    color: 0x10B981,
                    emissive: 0x10B981,
                    emissiveIntensity: 2.8,
                    roughness: 0.1,
                    side: THREE.DoubleSide
                });
                break;
            case 'wood':
                mat = new THREE.MeshStandardMaterial({
                    color: 0x78350F,
                    roughness: 0.5,
                    metalness: 0.05,
                    side: THREE.DoubleSide
                });
                break;
            case 'diamond':
                mat = new THREE.MeshPhysicalMaterial({
                    color: 0xF0F9FF,
                    metalness: 0.1,
                    roughness: 0.01,
                    transmission: 0.95,
                    ior: 2.4,
                    side: THREE.DoubleSide
                });
                break;
            default:
                mat = new THREE.MeshStandardMaterial({
                    color: customParams.color || 0xD4AF37,
                    metalness: customParams.metalness || 0.9,
                    roughness: customParams.roughness || 0.15,
                    side: THREE.DoubleSide
                });
        }

        if (this.textCustomTexture && preset === 'custom_phone') {
            mat.map = this.textCustomTexture;
        }

        return mat;
    }

    addTextObject(config) {
        const id = config.id || 'text_' + Date.now();
        const rawText = config.text || '3D TEXT';

        const group = new THREE.Group();
        const mat = this.createMaterial(config.materialPreset || 'gold', config.customMaterial);

        // Pass rawText directly so HTML5 2D Canvas performs native HarfBuzz Arabic cursive shaping!
        const textGeo = this.createExtruded3DTextGeometry(rawText, config);
        const mesh = new THREE.Mesh(textGeo, mat);
        mesh.castShadow = true;
        mesh.receiveShadow = false; // Prevents self-shadow acne and lines slicing across letters
        group.add(mesh);

        group.position.set(config.posX || 0, config.posY || 0, config.posZ || 0);
        const rx = (config.rotX || 0) * (Math.PI / 180);
        const ry = (config.rotY || 0) * (Math.PI / 180);
        const rz = (config.rotZ || 0) * (Math.PI / 180);
        group.rotation.set(rx, ry, rz);
        group.scale.set(config.scaleX || 1, config.scaleY || 1, config.scaleZ || 1);

        group.userData = {
            id: id,
            type: 'text',
            rawText: rawText,
            config: config
        };

        if (this.objects.has(id)) {
            this.scene.remove(this.objects.get(id));
        }

        this.scene.add(group);
        this.objects.set(id, group);
        this.selectedObjectId = id;

        this.notifyAndroid('onObjectAdded', { id: id, type: 'text', text: rawText });
        return id;
    }

    addImageObject(config) {
        const id = config.id || 'img_' + Date.now();
        const group = new THREE.Group();

        const w = config.width || 3.5;
        const h = config.height || 2.2;
        const geometry = new THREE.PlaneGeometry(w, h);
        let material;

        if (config.base64Url || config.imageUrl) {
            const texture = new THREE.TextureLoader().load(config.base64Url || config.imageUrl);
            texture.anisotropy = 16;
            material = new THREE.MeshStandardMaterial({
                map: texture,
                side: THREE.DoubleSide,
                transparent: true,
                alphaTest: 0.05,
                roughness: 0.3,
                metalness: 0.1
            });
        } else {
            const canvas = document.createElement('canvas');
            canvas.width = 512;
            canvas.height = 320;
            const ctx = canvas.getContext('2d');
            ctx.fillStyle = '#1E1E22';
            ctx.fillRect(0, 0, 512, 320);
            ctx.strokeStyle = '#FFD700';
            ctx.lineWidth = 8;
            ctx.strokeRect(12, 12, 488, 296);
            ctx.fillStyle = '#FFD700';
            ctx.font = 'bold 36px "Segoe UI", Roboto, sans-serif';
            ctx.textAlign = 'center';
            ctx.textBaseline = 'middle';
            ctx.fillText(config.label || '3D Image Frame', 256, 160);

            const texture = new THREE.CanvasTexture(canvas);
            material = new THREE.MeshStandardMaterial({
                map: texture,
                side: THREE.DoubleSide,
                roughness: 0.3
            });
        }

        const mesh = new THREE.Mesh(geometry, material);
        mesh.castShadow = true;
        mesh.receiveShadow = true;
        group.add(mesh);

        group.position.set(config.posX || 0, config.posY || 0, config.posZ || 0);
        const rx = (config.rotX || 0) * (Math.PI / 180);
        const ry = (config.rotY || 0) * (Math.PI / 180);
        const rz = (config.rotZ || 0) * (Math.PI / 180);
        group.rotation.set(rx, ry, rz);
        group.scale.set(config.scaleX || 1, config.scaleY || 1, config.scaleZ || 1);

        group.userData = { id: id, type: 'image', config: config };

        if (this.objects.has(id)) {
            this.scene.remove(this.objects.get(id));
        }

        this.scene.add(group);
        this.objects.set(id, group);
        this.selectedObjectId = id;

        this.notifyAndroid('onObjectAdded', { id: id, type: 'image' });
        return id;
    }

    addModelObject(config) {
        const id = config.id || 'model_' + Date.now();
        const modelType = config.modelType || 'cube';
        const group = new THREE.Group();

        let geometry;
        switch (modelType) {
            case 'sphere':
                geometry = new THREE.SphereGeometry(1.0, 32, 32);
                break;
            case 'cylinder':
                geometry = new THREE.CylinderGeometry(0.8, 0.8, 2.0, 32);
                break;
            case 'cone':
                geometry = new THREE.ConeGeometry(1.0, 2.0, 32);
                break;
            case 'torus':
                geometry = new THREE.TorusGeometry(1.0, 0.35, 16, 64);
                break;
            case 'star':
                geometry = new THREE.OctahedronGeometry(1.2, 0);
                break;
            case 'ring':
                geometry = new THREE.RingGeometry(0.5, 1.2, 32);
                break;
            case 'pyramid':
                geometry = new THREE.ConeGeometry(1.2, 1.8, 4);
                break;
            case 'cube':
            default:
                geometry = new THREE.BoxGeometry(1.5, 1.5, 1.5);
                break;
        }

        const mat = this.createMaterial(config.materialPreset || 'gold', config.customMaterial);
        const mesh = new THREE.Mesh(geometry, mat);
        mesh.castShadow = true;
        mesh.receiveShadow = true;
        group.add(mesh);

        group.position.set(config.posX || 0, config.posY || 0, config.posZ || 0);
        const rx = (config.rotX || 0) * (Math.PI / 180);
        const ry = (config.rotY || 0) * (Math.PI / 180);
        const rz = (config.rotZ || 0) * (Math.PI / 180);
        group.rotation.set(rx, ry, rz);
        group.scale.set(config.scaleX || 1, config.scaleY || 1, config.scaleZ || 1);

        group.userData = { id: id, type: 'model', config: config };

        if (this.objects.has(id)) {
            this.scene.remove(this.objects.get(id));
        }

        this.scene.add(group);
        this.objects.set(id, group);
        this.selectedObjectId = id;

        this.notifyAndroid('onObjectAdded', { id: id, type: 'model' });
        return id;
    }

    createPreset3DModel(presetName) {
        const root = new THREE.Group();
        switch (presetName) {
            case 'spaceship': {
                // Cockpit
                const bodyGeo = new THREE.ConeGeometry(0.7, 2.2, 8);
                bodyGeo.rotateX(Math.PI / 2);
                const bodyMat = new THREE.MeshStandardMaterial({ color: 0x38BDF8, metalness: 0.8, roughness: 0.2 });
                const body = new THREE.Mesh(bodyGeo, bodyMat);
                root.add(body);

                // Wings
                const wingGeo = new THREE.BoxGeometry(2.8, 0.08, 1.2);
                const wingMat = new THREE.MeshStandardMaterial({ color: 0x1E293B, metalness: 0.9, roughness: 0.3 });
                const wings = new THREE.Mesh(wingGeo, wingMat);
                wings.position.set(0, 0, -0.2);
                root.add(wings);

                // Thrusters
                const thrusterGeo = new THREE.CylinderGeometry(0.18, 0.22, 0.6, 16);
                thrusterGeo.rotateX(Math.PI / 2);
                const thrusterMat = new THREE.MeshStandardMaterial({ color: 0x06B6D4, emissive: 0x06B6D4, emissiveIntensity: 2.0 });
                const t1 = new THREE.Mesh(thrusterGeo, thrusterMat);
                t1.position.set(0.6, 0, -1.0);
                const t2 = new THREE.Mesh(thrusterGeo, thrusterMat);
                t2.position.set(-0.6, 0, -1.0);
                root.add(t1);
                root.add(t2);
                break;
            }
            case 'robot': {
                // Torso
                const torsoGeo = new THREE.BoxGeometry(1.0, 1.3, 0.7);
                const torsoMat = new THREE.MeshStandardMaterial({ color: 0x64748B, metalness: 0.7, roughness: 0.3 });
                const torso = new THREE.Mesh(torsoGeo, torsoMat);
                torso.position.y = 1.0;
                root.add(torso);

                // Head
                const headGeo = new THREE.BoxGeometry(0.7, 0.7, 0.7);
                const head = new THREE.Mesh(headGeo, torsoMat);
                head.position.y = 2.0;
                root.add(head);

                // Glowing Eye Visor
                const visorGeo = new THREE.BoxGeometry(0.5, 0.15, 0.1);
                const visorMat = new THREE.MeshStandardMaterial({ color: 0xEF4444, emissive: 0xEF4444, emissiveIntensity: 2.5 });
                const visor = new THREE.Mesh(visorGeo, visorMat);
                visor.position.set(0, 2.05, 0.36);
                root.add(visor);

                // Arms
                const armGeo = new THREE.CylinderGeometry(0.15, 0.15, 1.1, 12);
                const a1 = new THREE.Mesh(armGeo, torsoMat);
                a1.position.set(0.7, 0.9, 0);
                const a2 = new THREE.Mesh(armGeo, torsoMat);
                a2.position.set(-0.7, 0.9, 0);
                root.add(a1);
                root.add(a2);

                // Legs
                const legGeo = new THREE.CylinderGeometry(0.18, 0.18, 1.1, 12);
                const l1 = new THREE.Mesh(legGeo, torsoMat);
                l1.position.set(0.3, -0.1, 0);
                const l2 = new THREE.Mesh(legGeo, torsoMat);
                l2.position.set(-0.3, -0.1, 0);
                root.add(l1);
                root.add(l2);
                break;
            }
            case 'car': {
                // Main chassis
                const bodyGeo = new THREE.BoxGeometry(1.6, 0.55, 3.0);
                const bodyMat = new THREE.MeshStandardMaterial({ color: 0xDC2626, metalness: 0.8, roughness: 0.2 });
                const body = new THREE.Mesh(bodyGeo, bodyMat);
                body.position.y = 0.5;
                root.add(body);

                // Cabin / Cockpit
                const cabinGeo = new THREE.BoxGeometry(1.2, 0.45, 1.4);
                const cabinMat = new THREE.MeshStandardMaterial({ color: 0x0F172A, roughness: 0.1 });
                const cabin = new THREE.Mesh(cabinGeo, cabinMat);
                cabin.position.set(0, 0.95, -0.2);
                root.add(cabin);

                // 4 Wheels
                const wheelGeo = new THREE.CylinderGeometry(0.35, 0.35, 0.25, 16);
                wheelGeo.rotateZ(Math.PI / 2);
                const wheelMat = new THREE.MeshStandardMaterial({ color: 0x18181B, roughness: 0.8 });
                const wPositions = [
                    [0.85, 0.35, 0.9],
                    [-0.85, 0.35, 0.9],
                    [0.85, 0.35, -0.9],
                    [-0.85, 0.35, -0.9]
                ];
                wPositions.forEach(pos => {
                    const wheel = new THREE.Mesh(wheelGeo, wheelMat);
                    wheel.position.set(pos[0], pos[1], pos[2]);
                    root.add(wheel);
                });
                break;
            }
            case 'trophy':
            default: {
                // Cup
                const cupGeo = new THREE.CylinderGeometry(0.75, 0.3, 1.2, 24);
                const goldMat = new THREE.MeshStandardMaterial({ color: 0xFFD700, metalness: 0.9, roughness: 0.15 });
                const cup = new THREE.Mesh(cupGeo, goldMat);
                cup.position.y = 1.2;
                root.add(cup);

                // Stem & Base
                const stemGeo = new THREE.CylinderGeometry(0.12, 0.18, 0.7, 16);
                const stem = new THREE.Mesh(stemGeo, goldMat);
                stem.position.y = 0.4;
                root.add(stem);

                const baseGeo = new THREE.BoxGeometry(0.9, 0.3, 0.9);
                const baseMat = new THREE.MeshStandardMaterial({ color: 0x1E1E24, roughness: 0.4 });
                const base = new THREE.Mesh(baseGeo, baseMat);
                base.position.y = 0.05;
                root.add(base);

                // Handles
                const handleGeo = new THREE.TorusGeometry(0.35, 0.07, 12, 24);
                const h1 = new THREE.Mesh(handleGeo, goldMat);
                h1.position.set(0.65, 1.2, 0);
                const h2 = new THREE.Mesh(handleGeo, goldMat);
                h2.position.set(-0.65, 1.2, 0);
                root.add(h1);
                root.add(h2);
                break;
            }
        }
        return root;
    }

    loadImported3DModel(config) {
        const id = config.id || 'model_3d_' + Date.now();
        const format = (config.format || 'glb').toLowerCase();
        const data = config.data || '';
        const name = config.name || 'Model 3D';
        const asBrush = !!config.asBrush;
        const spawnInScene = config.spawnInScene !== undefined ? config.spawnInScene : true;

        const onParsed = (rootObj) => {
            if (!rootObj) return;

            // Compute bounds and normalize scale
            const box = new THREE.Box3().setFromObject(rootObj);
            const size = box.getSize(new THREE.Vector3());
            const center = box.getCenter(new THREE.Vector3());
            const maxDim = Math.max(size.x, size.y, size.z, 0.001);
            const targetDim = config.targetSize || 2.4;
            const normScale = targetDim / maxDim;

            rootObj.position.sub(center); // center around origin
            rootObj.traverse((child) => {
                if (child.isMesh) {
                    child.castShadow = true;
                    child.receiveShadow = true;
                    if (config.materialPreset) {
                        child.material = this.createMaterial(config.materialPreset);
                    }
                }
            });

            // If user requested spawning in scene
            if (spawnInScene) {
                const wrapper = new THREE.Group();
                wrapper.add(rootObj);
                wrapper.scale.set(normScale, normScale, normScale);
                wrapper.position.set(config.posX || 0, config.posY || 0, config.posZ || 0);

                wrapper.userData = { id: id, type: 'imported_model', format: format, name: name, config: config };
                if (this.objects.has(id)) {
                    this.scene.remove(this.objects.get(id));
                }
                this.scene.add(wrapper);
                this.objects.set(id, wrapper);
                this.selectedObjectId = id;
            }

            // Register as brush for 3D Surface Painting
            let primaryGeo = null;
            let primaryMat = null;
            rootObj.traverse((child) => {
                if (child.isMesh && !primaryGeo && child.geometry) {
                    try {
                        primaryGeo = child.geometry.clone();
                        primaryGeo.scale(normScale * 0.35, normScale * 0.35, normScale * 0.35);
                        primaryMat = child.material;
                    } catch(e) {}
                }
            });

            if (!primaryGeo) {
                primaryGeo = new THREE.ConeGeometry(0.5, 1.2, 8);
                primaryMat = new THREE.MeshStandardMaterial({ color: 0x38BDF8 });
            }

            const brushId = 'brush_' + id;
            const maxCount = 600;
            const customImesh = new THREE.InstancedMesh(primaryGeo, primaryMat, maxCount);
            customImesh.count = 0;
            customImesh.castShadow = true;
            customImesh.receiveShadow = true;

            this.instancedMeshes.set(brushId, customImesh);
            this.instanceData.set(brushId, []);
            this.paintedGroup.add(customImesh);

            if (asBrush) {
                this.activeBrushType = brushId;
                this.togglePaintMode(true);
            }

            this.notifyAndroid('onModelImportedSuccess', { id: id, name: name, format: format, brushId: brushId });
        };

        try {
            if (format === 'preset') {
                const presetObj = this.createPreset3DModel(config.presetName || 'spaceship');
                onParsed(presetObj);
            } else if (format === 'glb' || format === 'gltf') {
                if (typeof THREE.GLTFLoader === 'undefined') {
                    console.warn('GLTFLoader not ready, falling back to preset');
                    onParsed(this.createPreset3DModel('spaceship'));
                    return;
                }
                const gltfLoader = new THREE.GLTFLoader();
                let buffer;
                if (data.startsWith('data:') || !data.trim().startsWith('{')) {
                    const clean = data.includes(',') ? data.split(',')[1] : data;
                    const bin = atob(clean);
                    const bytes = new Uint8Array(bin.length);
                    for (let i = 0; i < bin.length; i++) {
                        bytes[i] = bin.charCodeAt(i);
                    }
                    buffer = bytes.buffer;
                } else {
                    buffer = data;
                }
                gltfLoader.parse(buffer, '', (gltf) => {
                    const scene = gltf.scene || gltf.scenes[0];
                    onParsed(scene);
                }, (err) => {
                    console.error('GLTF parse error', err);
                    onParsed(this.createPreset3DModel('spaceship'));
                });
            } else if (format === 'obj') {
                if (typeof THREE.OBJLoader === 'undefined') {
                    console.warn('OBJLoader not ready, falling back to preset');
                    onParsed(this.createPreset3DModel('trophy'));
                    return;
                }
                const objLoader = new THREE.OBJLoader();
                let text = data;
                if (data.includes('base64,') || (!data.includes('v ') && !data.includes('f '))) {
                    try {
                        const clean = data.includes(',') ? data.split(',')[1] : data;
                        text = atob(clean);
                    } catch(e) {
                        text = data;
                    }
                }
                const obj = objLoader.parse(text);
                onParsed(obj);
            } else {
                console.warn('Unknown format ' + format);
                onParsed(this.createPreset3DModel('trophy'));
            }
        } catch (err) {
            console.error('Error loading 3D model:', err);
            onParsed(this.createPreset3DModel('spaceship'));
        }
    }

    setModelAsPaintBrush(id) {
        const brushId = id.startsWith('brush_') ? id : 'brush_' + id;
        if (this.instancedMeshes.has(brushId)) {
            this.activeBrushType = brushId;
            this.togglePaintMode(true);
        }
    }

    spawnModelInScene(id) {
        if (this.objects.has(id)) {
            const obj = this.objects.get(id);
            obj.position.set(0, 0, 0);
            this.selectedObjectId = id;
        }
    }

    updateObjectTransform(id, transform) {
        if (!this.objects.has(id)) return;
        const group = this.objects.get(id);

        if (transform.posX !== undefined) group.position.x = transform.posX;
        if (transform.posY !== undefined) group.position.y = transform.posY;
        if (transform.posZ !== undefined) group.position.z = transform.posZ;

        if (transform.rotX !== undefined) group.rotation.x = transform.rotX * (Math.PI / 180);
        if (transform.rotY !== undefined) group.rotation.y = transform.rotY * (Math.PI / 180);
        if (transform.rotZ !== undefined) group.rotation.z = transform.rotZ * (Math.PI / 180);

        if (transform.scaleX !== undefined) group.scale.x = transform.scaleX;
        if (transform.scaleY !== undefined) group.scale.y = transform.scaleY;
        if (transform.scaleZ !== undefined) group.scale.z = transform.scaleZ;
    }

    createExtruded3DTextGeometry(text, config) {
        const tempCanvas = document.createElement('canvas');
        const tempCtx = tempCanvas.getContext('2d');
        const fontSize = 280;
        tempCtx.font = `bold ${fontSize}px "Segoe UI", Roboto, "Noto Sans Arabic", "Amiri", Tahoma, Arial, sans-serif`;
        const metrics = tempCtx.measureText(text);
        const measuredWidth = Math.max(metrics.width || 600, 300);

        const canvasWidth = Math.max(Math.ceil(measuredWidth) + 300, 1600);
        const canvasHeight = 500;

        const canvas = document.createElement('canvas');
        canvas.width = canvasWidth;
        canvas.height = canvasHeight;
        const ctx = canvas.getContext('2d');

        ctx.clearRect(0, 0, canvas.width, canvas.height);
        ctx.font = `bold ${fontSize}px "Segoe UI", Roboto, "Noto Sans Arabic", "Amiri", Tahoma, Arial, sans-serif`;
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.lineJoin = 'round';
        ctx.lineCap = 'round';

        // Draw crisp solid letters without miter spikes that cause tearing needles
        ctx.fillStyle = '#FFFFFF';
        ctx.strokeStyle = '#FFFFFF';
        ctx.lineWidth = 4;
        ctx.strokeText(text, canvas.width / 2, canvas.height / 2);
        ctx.fillText(text, canvas.width / 2, canvas.height / 2);

        const imgData = ctx.getImageData(0, 0, canvas.width, canvas.height);
        const data = imgData.data;

        // Trace 2D contours directly into upright CCW THREE.Shape objects
        const shapes = this.traceCanvasContoursToShapes(data, canvas.width, canvas.height);

        if (!shapes || shapes.length === 0) {
            return new THREE.BoxGeometry((config.size || 1.4) * 3, (config.size || 1.4), 0.4);
        }

        // Extrude with safe, non-self-intersecting bevel settings
        const depth = Math.max(3.0, (config.depth || 0.4) * 35);
        const bevelParam = Math.max(0.01, config.bevel !== undefined ? config.bevel : 0.05);
        const extrudeSettings = {
            depth: depth,
            bevelEnabled: bevelParam > 0.01,
            bevelThickness: Math.min(bevelParam * 8, 1.2),
            bevelSize: Math.min(bevelParam * 4, 0.5),
            bevelOffset: 0,
            bevelSegments: 2,
            curveSegments: 8
        };

        const geometry = new THREE.ExtrudeGeometry(shapes, extrudeSettings);
        geometry.center();

        // Safe positive scaling (Y is already oriented upright)
        const scale = (config.size || 1.4) * 0.0085;
        geometry.scale(scale, scale, scale);

        geometry.computeVertexNormals();
        return geometry;
    }

    cleanContourPoints(points) {
        if (!points || points.length < 3) return points || [];
        const filtered = [];
        const minDistanceSq = 3.0; // Avoid duplicate / zero-area degenerate vertices

        for (let i = 0; i < points.length; i++) {
            const p = points[i];
            if (filtered.length === 0) {
                filtered.push(p);
            } else {
                const prev = filtered[filtered.length - 1];
                const distSq = (p.x - prev.x) * (p.x - prev.x) + (p.y - prev.y) * (p.y - prev.y);
                if (distSq >= minDistanceSq) {
                    filtered.push(p);
                }
            }
        }

        if (filtered.length < 3) return filtered;

        // Apply 1 pass of sub-pixel smoothing for continuous organic curves
        const smoothed = [];
        const len = filtered.length;
        for (let i = 0; i < len; i++) {
            const p0 = filtered[i];
            const p1 = filtered[(i + 1) % len];
            smoothed.push(new THREE.Vector2(0.75 * p0.x + 0.25 * p1.x, 0.75 * p0.y + 0.25 * p1.y));
            smoothed.push(new THREE.Vector2(0.25 * p0.x + 0.75 * p1.x, 0.25 * p0.y + 0.75 * p1.y));
        }

        return smoothed;
    }

    traceCanvasContoursToShapes(data, w, h) {
        const labels = new Int32Array(w * h);
        let currentLabel = 0;
        const islands = [];

        function isPixelSolid(x, y) {
            if (x < 0 || x >= w || y < 0 || y >= h) return false;
            return data[(y * w + x) * 4 + 3] > 30;
        }

        // 1. BFS to label all solid pixel islands (letters, ligatures, diacritic dots)
        for (let y = 1; y < h - 1; y++) {
            for (let x = 1; x < w - 1; x++) {
                const idx = y * w + x;
                if (!labels[idx] && isPixelSolid(x, y)) {
                    currentLabel++;
                    let minX = x, maxX = x, minY = y, maxY = y;
                    const queue = [x, y];
                    labels[idx] = currentLabel;

                    let head = 0;
                    while (head < queue.length && head < 80000) {
                        const px = queue[head++];
                        const py = queue[head++];

                        if (px < minX) minX = px;
                        if (px > maxX) maxX = px;
                        if (py < minY) minY = py;
                        if (py > maxY) maxY = py;

                        const neighbors = [
                            [px + 1, py], [px - 1, py], [px, py + 1], [px, py - 1]
                        ];

                        for (let i = 0; i < 4; i++) {
                            const nx = neighbors[i][0];
                            const ny = neighbors[i][1];
                            if (nx >= 0 && nx < w && ny >= 0 && ny < h) {
                                const nidx = ny * w + nx;
                                if (!labels[nidx] && isPixelSolid(nx, ny)) {
                                    labels[nidx] = currentLabel;
                                    queue.push(nx, ny);
                                }
                            }
                        }
                    }

                    islands.push({ label: currentLabel, minX, minY, maxX, maxY, pixelCount: queue.length / 2 });
                }
            }
        }

        const shapes = [];

        // 2. Trace boundaries and enforce strict Counter-Clockwise (CCW) winding for outer boundary
        for (let k = 0; k < islands.length; k++) {
            const island = islands[k];
            if (island.pixelCount < 6) continue;

            let startX = -1, startY = -1;
            for (let y = island.minY; y <= island.maxY; y++) {
                for (let x = island.minX; x <= island.maxX; x++) {
                    if (labels[y * w + x] === island.label) {
                        startX = x;
                        startY = y;
                        break;
                    }
                }
                if (startX !== -1) break;
            }

            if (startX === -1) continue;

            const rawOuterPoints = [];
            let currX = startX;
            let currY = startY;
            let dirX = 1;
            let dirY = 0;
            let steps = 0;
            const maxSteps = 4000;

            while (steps < maxSteps) {
                // Invert Y so 3D coordinates are right-side-up naturally
                rawOuterPoints.push(new THREE.Vector2(currX, h - currY));

                const rightX = currX - dirY;
                const rightY = currY + dirX;
                const straightX = currX + dirX;
                const straightY = currY + dirY;

                if (labels[rightY * w + rightX] === island.label) {
                    const temp = dirX; dirX = -dirY; dirY = temp;
                    currX = rightX; currY = rightY;
                } else if (labels[straightY * w + straightX] === island.label) {
                    currX = straightX; currY = straightY;
                } else {
                    const temp = dirX; dirX = dirY; dirY = -temp;
                }

                steps++;
                if (Math.abs(currX - startX) <= 1 && Math.abs(currY - startY) <= 1 && steps > 6) {
                    break;
                }
            }

            const cleanOuter = this.cleanContourPoints(rawOuterPoints);
            if (cleanOuter.length < 3) continue;

            // Ensure outer contour is Counter-Clockwise (CCW)
            if (THREE.ShapeUtils && THREE.ShapeUtils.isClockWise(cleanOuter)) {
                cleanOuter.reverse();
            }

            let shape;
            try {
                shape = new THREE.Shape(cleanOuter);
            } catch (e) {
                continue;
            }

            // 3. Scan for inner holes (e.g. loops in A, O, P, D, e, o, م, ة, و, etc.)
            const holeVisited = new Uint8Array(w * h);

            for (let hy = island.minY + 4; hy <= island.maxY - 4; hy += 3) {
                for (let hx = island.minX + 4; hx <= island.maxX - 4; hx += 3) {
                    const hidx = hy * w + hx;
                    if (holeVisited[hidx] || isPixelSolid(hx, hy)) continue;

                    let hMinX = hx, hMaxX = hx, hMinY = hy, hMaxY = hy;
                    const hQueue = [hx, hy];
                    holeVisited[hidx] = 1;

                    let hHead = 0;
                    let touchesIslandEdge = false;

                    while (hHead < hQueue.length && hHead < 20000) {
                        const px = hQueue[hHead++];
                        const py = hQueue[hHead++];

                        if (px <= island.minX || px >= island.maxX || py <= island.minY || py >= island.maxY) {
                            touchesIslandEdge = true;
                        }

                        const neighbors = [[px + 1, py], [px - 1, py], [px, py + 1], [px, py - 1]];
                        for (let i = 0; i < 4; i++) {
                            const nx = neighbors[i][0];
                            const ny = neighbors[i][1];
                            if (nx >= 0 && nx < w && ny >= 0 && ny < h) {
                                const nidx = ny * w + nx;
                                if (!holeVisited[nidx] && !isPixelSolid(nx, ny)) {
                                    holeVisited[nidx] = 1;
                                    hQueue.push(nx, ny);
                                }
                            }
                        }
                    }

                    if (!touchesIslandEdge && hQueue.length >= 10) {
                        let hStartX = -1, hStartY = -1;
                        for (let y = hMinY; y <= hMaxY; y++) {
                            for (let x = hMinX; x <= hMaxX; x++) {
                                if (holeVisited[y * w + x]) {
                                    hStartX = x;
                                    hStartY = y;
                                    break;
                                }
                            }
                            if (hStartX !== -1) break;
                        }

                        if (hStartX !== -1) {
                            const rawHolePoints = [];
                            let hCurrX = hStartX;
                            let hCurrY = hStartY;
                            let hDirX = 1;
                            let hDirY = 0;
                            let hSteps = 0;

                            while (hSteps < 2000) {
                                rawHolePoints.push(new THREE.Vector2(hCurrX, h - hCurrY));

                                const rightX = hCurrX - hDirY;
                                const rightY = hCurrY + hDirX;
                                const straightX = hCurrX + hDirX;
                                const straightY = hCurrY + hDirY;

                                if (holeVisited[rightY * w + rightX]) {
                                    const temp = hDirX; hDirX = -hDirY; hDirY = temp;
                                    hCurrX = rightX; hCurrY = rightY;
                                } else if (holeVisited[straightY * w + straightX]) {
                                    hCurrX = straightX; hCurrY = straightY;
                                } else {
                                    const temp = hDirX; hDirX = hDirY; hDirY = -temp;
                                }

                                hSteps++;
                                if (Math.abs(hCurrX - hStartX) <= 1 && Math.abs(hCurrY - hStartY) <= 1 && hSteps > 6) {
                                    break;
                                }
                            }

                            const cleanHole = this.cleanContourPoints(rawHolePoints);
                            if (cleanHole.length >= 3) {
                                // In Three.js holes must be Clockwise (CW)
                                if (THREE.ShapeUtils && !THREE.ShapeUtils.isClockWise(cleanHole)) {
                                    cleanHole.reverse();
                                }
                                try {
                                    const holePath = new THREE.Path(cleanHole);
                                    shape.holes.push(holePath);
                                } catch (e) {
                                    console.warn("Hole creation failed", e);
                                }
                            }
                        }
                    }
                }
            }

            shapes.push(shape);
        }

        return shapes;
    }

    updateTextObject(id, newConfig) {
        if (!this.objects.has(id)) return;
        const group = this.objects.get(id);
        const currentConfig = group.userData.config;
        const mergedConfig = Object.assign({}, currentConfig, newConfig, { id: id });

        this.addTextObject(mergedConfig);
    }

    setMaterialPreset(id, presetName) {
        if (!this.objects.has(id)) return;
        const group = this.objects.get(id);
        group.userData.config.materialPreset = presetName;
        this.addTextObject(group.userData.config);
    }

    setCustomMaterialColor(id, hexColor, metalness = 0.9, roughness = 0.15, emissiveIntensity = 0.2) {
        if (!this.objects.has(id)) return;
        const group = this.objects.get(id);
        const cleanHex = hexColor.replace('#', '');
        const colorVal = parseInt(cleanHex, 16) || 0xFFD700;

        const customMat = new THREE.MeshStandardMaterial({
            color: colorVal,
            metalness: parseFloat(metalness),
            roughness: parseFloat(roughness),
            emissive: colorVal,
            emissiveIntensity: parseFloat(emissiveIntensity)
        });

        group.userData.config.materialPreset = 'custom';
        group.userData.config.customMaterial = { color: colorVal, metalness, roughness, emissiveIntensity };

        group.children.forEach(mesh => {
            if (mesh.isMesh) {
                mesh.material = customMat;
                mesh.material.needsUpdate = true;
            }
        });
    }

    recordCameraFrame() {
        if (!this.isRecordingCamera) return;
        this.cameraKeyframes.push({
            time: this.currentTime,
            position: this.camera.position.clone(),
            target: this.controls.target.clone()
        });
    }

    startCameraRecording() {
        this.cameraKeyframes = [];
        this.isRecordingCamera = true;
        this.isRecordingVideo = true;
        if (this.axesHelper) this.axesHelper.visible = false; // HIDE AXES DURING VIDEO RECORDING!
        this.notifyAndroid('onCameraRecordingState', { status: 'recording' });
    }

    stopCameraRecording() {
        this.isRecordingCamera = false;
        this.isRecordingVideo = false;
        if (this.axesHelper) this.axesHelper.visible = true; // RESTORE AXES
        this.notifyAndroid('onCameraRecordingState', { status: 'stopped', count: this.cameraKeyframes.length });
    }

    replayCameraPath() {
        if (this.cameraKeyframes.length === 0) return;
        let index = 0;
        const interval = setInterval(() => {
            if (index >= this.cameraKeyframes.length) {
                clearInterval(interval);
                return;
            }
            const kf = this.cameraKeyframes[index];
            this.camera.position.copy(kf.position);
            this.controls.target.copy(kf.target);
            this.controls.update();
            index++;
        }, 1000 / this.fps);
    }

    applyCameraPreset(preset) {
        switch (preset) {
            case 'orbit360':
                this.animateCameraOrbit();
                break;
            case 'zoomReveal':
                this.camera.position.set(0, 1.8, 18);
                this.controls.target.set(0, 0, 0);
                this.controls.update();
                break;
            case 'craneTilt':
                this.camera.position.set(0, 10, 4);
                this.controls.target.set(0, 0, 0);
                this.controls.update();
                break;
            default:
                this.camera.position.set(0, 1.8, 9.5);
                this.controls.target.set(0, 0, 0);
                this.controls.update();
        }
    }

    animateCameraOrbit() {
        const radius = 9.5;
        let angle = 0;
        const orbitInterval = setInterval(() => {
            if (!this.isPlaying) {
                clearInterval(orbitInterval);
                return;
            }
            angle += 0.02;
            this.camera.position.x = radius * Math.sin(angle);
            this.camera.position.z = radius * Math.cos(angle);
            this.controls.update();
        }, 30);
    }

    // ========================================================
    // Hollywood Green Screen 3D Composer Subsystem
    // ========================================================
    initGreenScreenComposer() {
        this.greenScreenActive = false;
        this.isSampleActorActive = false;
        this.sampleActorType = 'action_hero';
        this.greenScreenPlaying = true;
        this.actorScale = 1.0;
        this.actorBillboard = false;
        this.lastGreenScreenStateReport = 0;

        // 1. Create HTML5 Video Element for VideoTexture
        this.videoElement = document.createElement('video');
        this.videoElement.crossOrigin = 'anonymous';
        this.videoElement.playsInline = true;
        this.videoElement.loop = true;
        this.videoElement.muted = true;
        this.videoElement.autoplay = true;

        this.videoElement.addEventListener('loadedmetadata', () => {
            if (this.videoElement.videoWidth && this.videoElement.videoHeight && this.actorMesh) {
                const aspect = this.videoElement.videoWidth / this.videoElement.videoHeight;
                this.actorMesh.geometry.dispose();
                const newGeo = new THREE.PlaneGeometry(1.6 * aspect, 2.4);
                newGeo.translate(0, 1.2, 0); // pivot at feet
                this.actorMesh.geometry = newGeo;
            }
            this.notifyAndroid('onGreenScreenVideoState', {
                currentTime: this.videoElement.currentTime,
                duration: this.videoElement.duration || 10,
                isPlaying: !this.videoElement.paused
            });
        });

        // 2. Procedural Sample Green Screen Actor Canvas (for instant realistic testing)
        this.sampleActorCanvas = document.createElement('canvas');
        this.sampleActorCanvas.width = 512;
        this.sampleActorCanvas.height = 768;
        this.sampleActorCtx = this.sampleActorCanvas.getContext('2d');
        this.sampleActorTexture = new THREE.CanvasTexture(this.sampleActorCanvas);
        this.sampleActorTexture.minFilter = THREE.LinearFilter;
        this.sampleActorTexture.magFilter = THREE.LinearFilter;

        this.videoTexture = new THREE.VideoTexture(this.videoElement);
        this.videoTexture.minFilter = THREE.LinearFilter;
        this.videoTexture.magFilter = THREE.LinearFilter;
        this.videoTexture.format = THREE.RGBAFormat;

        // 3. Hollywood Broadcast-Quality GLSL Chroma Key ShaderMaterial
        const vertexShader = `
            varying vec2 vUv;
            void main() {
                vUv = uv;
                gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
            }
        `;

        const fragmentShader = `
            uniform sampler2D map;
            uniform vec3 keyColor;
            uniform float similarity;
            uniform float smoothness;
            uniform float spill;
            uniform float opacity;
            uniform vec3 envTint;
            uniform float envTintIntensity;
            varying vec2 vUv;

            vec3 rgb2yuv(vec3 rgb) {
                float y = 0.299 * rgb.r + 0.587 * rgb.g + 0.114 * rgb.b;
                float u = -0.14713 * rgb.r - 0.28886 * rgb.g + 0.436 * rgb.b;
                float v = 0.615 * rgb.r - 0.51499 * rgb.g - 0.10001 * rgb.b;
                return vec3(y, u, v);
            }

            void main() {
                vec4 tex = texture2D(map, vUv);
                vec3 rgb = tex.rgb;

                vec3 yuv = rgb2yuv(rgb);
                vec3 keyYUV = rgb2yuv(keyColor);
                float dist = distance(yuv.yz, keyYUV.yz);

                float edge0 = similarity;
                float edge1 = similarity + max(smoothness, 0.001);
                float alpha = smoothstep(edge0, edge1, dist);

                // Spill Suppression: removes green/blue fringing from hair, edges, and clothes
                if (keyColor.g > keyColor.r && keyColor.g > keyColor.b) {
                    float maxRB = max(rgb.r, rgb.b);
                    if (rgb.g > maxRB) {
                        rgb.g = mix(rgb.g, maxRB, spill);
                    }
                } else if (keyColor.b > keyColor.r && keyColor.b > keyColor.g) {
                    float maxRG = max(rgb.r, rgb.b);
                    if (rgb.b > maxRG) {
                        rgb.b = mix(rgb.b, maxRG, spill);
                    }
                }

                // Environment Light Integration (Color Bleed)
                rgb = mix(rgb, rgb * envTint, envTintIntensity * 0.45);

                alpha *= opacity;
                if (alpha < 0.02) discard;

                gl_FragColor = vec4(rgb, alpha);
            }
        `;

        this.chromaUniforms = {
            map: { value: this.sampleActorTexture },
            keyColor: { value: new THREE.Color(0x00FF00) },
            similarity: { value: 0.42 },
            smoothness: { value: 0.08 },
            spill: { value: 0.50 },
            opacity: { value: 1.0 },
            envTint: { value: new THREE.Color(0xFFE4B5) },
            envTintIntensity: { value: 0.35 }
        };

        this.chromaMaterial = new THREE.ShaderMaterial({
            uniforms: this.chromaUniforms,
            vertexShader: vertexShader,
            fragmentShader: fragmentShader,
            transparent: true,
            side: THREE.DoubleSide,
            depthWrite: false
        });

        // 4. Actor 3D Plane Mesh (Pivot at feet!)
        const actorPlaneGeo = new THREE.PlaneGeometry(1.6, 2.4);
        actorPlaneGeo.translate(0, 1.2, 0);
        this.actorMesh = new THREE.Mesh(actorPlaneGeo, this.chromaMaterial);
        this.actorMesh.position.set(0, -1.98, 0);
        this.actorMesh.visible = false;
        this.scene.add(this.actorMesh);

        // 5. Realistic Ground Contact Shadow
        const shadowCanvas = document.createElement('canvas');
        shadowCanvas.width = 128;
        shadowCanvas.height = 128;
        const sctx = shadowCanvas.getContext('2d');
        const sgrad = sctx.createRadialGradient(64, 64, 0, 64, 64, 60);
        sgrad.addColorStop(0, 'rgba(0, 0, 0, 0.75)');
        sgrad.addColorStop(0.5, 'rgba(0, 0, 0, 0.35)');
        sgrad.addColorStop(1, 'rgba(0, 0, 0, 0)');
        sctx.fillStyle = sgrad;
        sctx.fillRect(0, 0, 128, 128);

        const shadowTex = new THREE.CanvasTexture(shadowCanvas);
        const shadowMat = new THREE.MeshBasicMaterial({
            map: shadowTex,
            transparent: true,
            opacity: 0.75,
            depthWrite: false
        });
        const shadowGeo = new THREE.PlaneGeometry(1.6, 0.85);
        shadowGeo.rotateX(-Math.PI / 2);
        this.actorContactShadow = new THREE.Mesh(shadowGeo, shadowMat);
        this.actorContactShadow.position.set(0, -1.97, 0);
        this.actorContactShadow.visible = false;
        this.scene.add(this.actorContactShadow);
    }

    loadGreenScreenVideo(url, name) {
        this.greenScreenActive = true;
        this.isSampleActorActive = false;
        if (this.actorMesh) this.actorMesh.visible = true;
        if (this.actorContactShadow) this.actorContactShadow.visible = true;

        if (this.chromaUniforms) {
            this.chromaUniforms.map.value = this.videoTexture;
        }

        try {
            this.videoElement.src = url;
            this.videoElement.currentTime = 0;
            const playPromise = this.videoElement.play();
            if (playPromise !== undefined) {
                playPromise.catch(err => {
                    console.warn('Autoplay prevented, will play on user interaction:', err);
                });
            }
        } catch (e) {
            console.error('Error loading video src', e);
        }
    }

    loadSampleGreenScreenActor(sampleType) {
        this.greenScreenActive = true;
        this.isSampleActorActive = true;
        this.sampleActorType = sampleType || 'action_hero';
        this.greenScreenPlaying = true;
        if (this.actorMesh) this.actorMesh.visible = true;
        if (this.actorContactShadow) this.actorContactShadow.visible = true;

        if (this.chromaUniforms) {
            this.chromaUniforms.map.value = this.sampleActorTexture;
        }
    }

    setGreenScreenPlayState(isPlaying) {
        this.greenScreenPlaying = isPlaying;
        if (this.videoElement) {
            if (isPlaying) {
                this.videoElement.play().catch(() => {});
            } else {
                this.videoElement.pause();
            }
        }
    }

    seekGreenScreenVideo(timeSec) {
        if (this.videoElement && !isNaN(timeSec)) {
            this.videoElement.currentTime = timeSec;
        }
    }

    setGreenScreenPlaybackSpeed(speed) {
        if (this.videoElement) {
            this.videoElement.playbackRate = speed;
        }
    }

    setGreenScreenLoop(isLoop) {
        if (this.videoElement) {
            this.videoElement.loop = isLoop;
        }
    }

    updateChromaKeyShader(params) {
        if (!this.chromaUniforms) return;
        if (params.keyColor) {
            this.chromaUniforms.keyColor.value.set(params.keyColor);
        }
        if (params.similarity !== undefined) {
            this.chromaUniforms.similarity.value = params.similarity;
        }
        if (params.smoothness !== undefined) {
            this.chromaUniforms.smoothness.value = params.smoothness;
        }
        if (params.spill !== undefined) {
            this.chromaUniforms.spill.value = params.spill;
        }
        if (params.opacity !== undefined) {
            this.chromaUniforms.opacity.value = params.opacity;
        }
        if (params.envTintIntensity !== undefined) {
            this.chromaUniforms.envTintIntensity.value = params.envTintIntensity;
        }
    }

    updateActorTransform(params) {
        if (!this.actorMesh) return;
        if (params.posX !== undefined) this.actorMesh.position.x = params.posX;
        if (params.posZ !== undefined) this.actorMesh.position.z = params.posZ;
        if (params.groundLock !== undefined) {
            this.actorMesh.position.y = params.groundLock ? -1.98 : (params.posY !== undefined ? params.posY : this.actorMesh.position.y);
        } else if (params.posY !== undefined) {
            this.actorMesh.position.y = params.posY;
        }

        if (params.scale !== undefined) {
            this.actorScale = params.scale;
            this.actorMesh.scale.set(params.scale, params.scale, params.scale);
        }

        if (params.rotY !== undefined) {
            this.actorMesh.rotation.y = params.rotY * (Math.PI / 180);
        }

        if (params.billboard !== undefined) {
            this.actorBillboard = params.billboard;
        }

        if (params.contactShadow !== undefined && this.actorContactShadow) {
            this.actorContactShadow.visible = params.contactShadow;
        }
    }

    updateSampleActorFrame(now) {
        if (!this.sampleActorCtx) return;
        const ctx = this.sampleActorCtx;
        const w = this.sampleActorCanvas.width;
        const h = this.sampleActorCanvas.height;

        // 1. Fill entire background with pure Chroma Green #00FF00
        ctx.fillStyle = '#00FF00';
        ctx.fillRect(0, 0, w, h);

        const time = (now / 1000.0) * (this.greenScreenPlaying ? 1.0 : 0.0);
        const breath = Math.sin(time * 2.5) * 4;

        ctx.save();
        ctx.translate(w / 2, h - 90);

        // Legs
        const legLeft = Math.sin(time * 3.5) * 16;
        const legRight = -legLeft;

        // Left Leg
        ctx.save();
        ctx.rotate(legLeft * Math.PI / 180);
        ctx.fillStyle = '#1E293B';
        ctx.fillRect(-35, -280, 30, 280);
        // Left Shoe
        ctx.fillStyle = '#0F172A';
        ctx.fillRect(-45, -15, 45, 20);
        ctx.restore();

        // Right Leg
        ctx.save();
        ctx.rotate(legRight * Math.PI / 180);
        ctx.fillStyle = '#334155';
        ctx.fillRect(5, -280, 30, 280);
        // Right Shoe
        ctx.fillStyle = '#0F172A';
        ctx.fillRect(5, -15, 45, 20);
        ctx.restore();

        // Torso / Jacket
        ctx.fillStyle = '#18181B';
        ctx.fillRect(-55, -480 + breath, 110, 210);

        // Shirt highlight
        ctx.fillStyle = '#0284C7';
        ctx.beginPath();
        ctx.moveTo(-18, -480 + breath);
        ctx.lineTo(18, -480 + breath);
        ctx.lineTo(0, -410 + breath);
        ctx.closePath();
        ctx.fill();

        // Left Arm
        const armLeft = Math.cos(time * 3.5) * 20;
        ctx.save();
        ctx.translate(-55, -470 + breath);
        ctx.rotate(armLeft * Math.PI / 180);
        ctx.fillStyle = '#27272A';
        ctx.fillRect(-15, 0, 28, 190);
        ctx.fillStyle = '#E0AC69';
        ctx.beginPath();
        ctx.arc(0, 200, 14, 0, Math.PI * 2);
        ctx.fill();
        ctx.restore();

        // Right Arm
        const armRight = -armLeft;
        ctx.save();
        ctx.translate(55, -470 + breath);
        ctx.rotate(armRight * Math.PI / 180);
        ctx.fillStyle = '#27272A';
        ctx.fillRect(-13, 0, 28, 190);
        ctx.fillStyle = '#E0AC69';
        ctx.beginPath();
        ctx.arc(0, 200, 14, 0, Math.PI * 2);
        ctx.fill();
        ctx.restore();

        // Head and Face
        const headY = -545 + breath;
        ctx.fillStyle = '#F3D2B3';
        ctx.beginPath();
        ctx.arc(0, headY, 46, 0, Math.PI * 2);
        ctx.fill();

        // Hair
        ctx.fillStyle = '#291E16';
        ctx.beginPath();
        ctx.arc(0, headY - 14, 48, Math.PI * 0.8, Math.PI * 2.2);
        ctx.fill();

        // Hollywood Sunglasses
        ctx.fillStyle = '#09090B';
        ctx.fillRect(-26, headY - 10, 52, 16);
        ctx.strokeStyle = '#F59E0B';
        ctx.lineWidth = 2;
        ctx.strokeRect(-26, headY - 10, 52, 16);

        ctx.restore();
    }

    // High Bitrate MP4 Video Export Engine with Camera Movement Choreography (RAF-driven, zero freezing)
    exportCameraSequenceVideo(sequenceJson, exportFps = 30, format = 'mp4') {
        this.stopCameraSequence();
        if (this.videoExportState && this.videoExportState.active) {
            try { this.videoExportState.recorder.stop(); } catch (e) {}
            this.videoExportState.active = false;
        }

        let sequence = [];
        try {
            sequence = typeof sequenceJson === 'string' ? JSON.parse(sequenceJson) : sequenceJson;
        } catch (e) {
            console.error("Invalid sequence JSON for export", e);
            sequence = [];
        }

        if (!Array.isArray(sequence) || sequence.length === 0) {
            sequence = [{ preset: 'orbit360', duration: 5 }];
        }

        const totalDurationSec = sequence.reduce((acc, step) => acc + (step.duration || step.durationSec || 5), 0);
        const fps = Math.min(exportFps || 30, 30);

        this.isRecordingVideo = true;
        if (this.axesHelper) this.axesHelper.visible = false;
        if (this.lightBulbMeshGroup) this.lightBulbMeshGroup.visible = false;
        if (this.cameraPathMeshGroup) this.cameraPathMeshGroup.visible = false;
        if (this.selectionBox) this.selectionBox.visible = false;
        if (this.brushCursorMesh) this.brushCursorMesh.visible = false;

        const stream = this.renderer.domElement.captureStream(fps);
        let recorder;
        try {
            recorder = new MediaRecorder(stream, { mimeType: 'video/mp4;codecs=avc1', videoBitsPerSecond: 8000000 });
        } catch (e) {
            try {
                recorder = new MediaRecorder(stream, { mimeType: 'video/mp4' });
            } catch (e2) {
                recorder = new MediaRecorder(stream, { mimeType: 'video/webm' });
            }
        }

        const chunks = [];
        recorder.ondataavailable = (e) => {
            if (e.data.size > 0) chunks.push(e.data);
        };

        recorder.onstop = () => {
            this.isRecordingVideo = false;
            if (this.axesHelper) this.axesHelper.visible = true;
            if (this.lightBulbMeshGroup && this.showLightBulbMesh) this.lightBulbMeshGroup.visible = true;
            if (this.cameraPathMeshGroup && this.showCameraPath) this.cameraPathMeshGroup.visible = true;
            if (this.selectionBox && this.selectedObjectId) this.selectionBox.visible = true;

            const mime = recorder.mimeType || 'video/mp4';
            const blob = new Blob(chunks, { type: mime });
            const reader = new FileReader();
            reader.readAsDataURL(blob);
            reader.onloadend = () => {
                this.notifyAndroid('onExportComplete', {
                    format: format,
                    data: reader.result
                });
            };
        };

        recorder.start();

        // Drive export synchronously inside animate() for rock-solid stability without UI lockup
        this.videoExportState = {
            active: true,
            recorder: recorder,
            sequence: sequence,
            totalDuration: totalDurationSec,
            startTime: performance.now(),
            lastProgressPct: -1
        };
    }

    // High Bitrate MP4 Video Export Engine (Hide visual helpers during recording!)
    exportVideo(durationSec = 5, exportFps = 30, format = 'mp4') {
        this.isRecordingVideo = true;
        if (this.axesHelper) this.axesHelper.visible = false;
        if (this.lightBulbMeshGroup) this.lightBulbMeshGroup.visible = false;
        if (this.cameraPathMeshGroup) this.cameraPathMeshGroup.visible = false;
        if (this.selectionBox) this.selectionBox.visible = false;

        const stream = this.renderer.domElement.captureStream(exportFps);
        let recorder;
        try {
            recorder = new MediaRecorder(stream, { mimeType: 'video/mp4;codecs=avc1', videoBitsPerSecond: 8000000 });
        } catch (e) {
            try {
                recorder = new MediaRecorder(stream, { mimeType: 'video/mp4' });
            } catch (e2) {
                recorder = new MediaRecorder(stream, { mimeType: 'video/webm' });
            }
        }

        const chunks = [];
        recorder.ondataavailable = (e) => {
            if (e.data.size > 0) chunks.push(e.data);
        };

        recorder.onstop = () => {
            this.isRecordingVideo = false;
            if (this.axesHelper) this.axesHelper.visible = true;
            if (this.lightBulbMeshGroup && this.showLightBulbMesh) this.lightBulbMeshGroup.visible = true;
            if (this.cameraPathMeshGroup && this.showCameraPath) this.cameraPathMeshGroup.visible = true;
            if (this.selectionBox && this.selectedObjectId) this.selectionBox.visible = true;

            const mime = recorder.mimeType || 'video/mp4';
            const blob = new Blob(chunks, { type: mime });
            const reader = new FileReader();
            reader.readAsDataURL(blob);
            reader.onloadend = () => {
                this.notifyAndroid('onExportComplete', {
                    format: format,
                    data: reader.result
                });
            };
        };

        recorder.start();
        this.isPlaying = true;

        let elapsed = 0;
        const progressInterval = setInterval(() => {
            elapsed += 0.5;
            const pct = Math.min(Math.round((elapsed / durationSec) * 100), 100);
            this.notifyAndroid('onExportProgress', { progress: pct });

            if (elapsed >= durationSec) {
                clearInterval(progressInterval);
                recorder.stop();
                this.isPlaying = false;
            }
        }, 500);
    }

    onWindowResize() {
        this.camera.aspect = window.innerWidth / window.innerHeight;
        this.camera.updateProjectionMatrix();
        this.renderer.setSize(window.innerWidth, window.innerHeight);
    }

    animate() {
        requestAnimationFrame(() => this.animate());

        const now = performance.now();

        // 1. Process Video Export Motion (Synchronous with frame rendering, zero freeze)
        if (this.videoExportState && this.videoExportState.active) {
            const elapsedSec = (now - this.videoExportState.startTime) / 1000.0;
            const total = this.videoExportState.totalDuration;

            if (elapsedSec >= total) {
                this.videoExportState.active = false;
                const rec = this.videoExportState.recorder;
                this.videoExportState = null;
                this.notifyAndroid('onExportProgress', { progress: 100 });
                setTimeout(() => {
                    if (rec && rec.state !== 'inactive') {
                        try { rec.stop(); } catch (e) {}
                    }
                }, 200);
            } else {
                const motion = this.calculateCameraMotion(this.videoExportState.sequence, elapsedSec);
                if (motion) {
                    this.camera.position.copy(motion.pos);
                    this.controls.target.copy(motion.target);
                }
                const pct = Math.min(Math.round((elapsedSec / total) * 100), 99);
                if (pct !== this.videoExportState.lastProgressPct) {
                    this.videoExportState.lastProgressPct = pct;
                    this.notifyAndroid('onExportProgress', { progress: pct });
                }
            }
        }
        // 2. Process Camera Motion Sequence (Preview or Recording)
        else if (this.cameraMotionState && this.cameraMotionState.active) {
            const elapsedSec = (now - this.cameraMotionState.startTime) / 1000.0;
            const total = this.cameraMotionState.totalDuration;

            if (elapsedSec >= total) {
                this.stopCameraSequence();
            } else {
                const motion = this.calculateCameraMotion(this.cameraMotionState.sequence, elapsedSec);
                if (motion) {
                    this.camera.position.copy(motion.pos);
                    this.controls.target.copy(motion.target);
                }
                if (this.cameraMotionState.isRecording) {
                    if (now - this.cameraMotionState.lastRecordedSec > 33) {
                        this.cameraMotionState.lastRecordedSec = now;
                        this.recordCameraFrame();
                    }
                }
            }
        }

        this.controls.update();

        // 3. Texture Panning Animation
        if (this.isTextureAnimating) {
            if (this.currentGroundTex) {
                this.currentGroundTex.offset.x += this.textureScrollSpeedX;
            }
            if (this.currentBackdropTex) {
                this.currentBackdropTex.offset.x += this.textureScrollSpeedX;
            }
            if (this.textCustomTexture) {
                this.textCustomTexture.offset.x += this.textureScrollSpeedX;
            }
        }

        // 4. Twinkling Stars Animation Engine - BATCHED for smooth 60fps
        if (this.twinkleStarsList && this.twinkleStarsList.length > 0) {
            if (!this.starBatchIndex) this.starBatchIndex = 0;
            const batchSize = Math.min(60, this.twinkleStarsList.length);
            const len = this.twinkleStarsList.length;
            for (let b = 0; b < batchSize; b++) {
                const idx = (this.starBatchIndex + b) % len;
                const star = this.twinkleStarsList[idx];
                star.phase += star.speed * this.starTwinkleSpeed * 0.2;
                const opacity = 0.25 + 0.75 * (0.5 + 0.5 * Math.sin(star.phase));
                const scale = star.baseScale * (0.85 + 0.3 * Math.sin(star.phase));
                star.mesh.scale.set(scale, scale, scale);
                star.mesh.material.opacity = opacity;
            }
            this.starBatchIndex = (this.starBatchIndex + batchSize) % len;
        }

        // 5. 3D Volumetric Floating Clouds Drift
        if (this.cloudsMeshGroup && this.cloudsMeshGroup.children.length > 0) {
            const clouds = this.cloudsMeshGroup.children;
            for (let i = 0; i < clouds.length; i++) {
                const cloud = clouds[i];
                cloud.position.x += this.cloudSpeed;
                if (cloud.position.x > 38.0) {
                    cloud.position.x = -38.0;
                }
            }
        }

        // 6. Moon Slow Rotation
        if (this.moonMeshGroup) {
            this.moonMeshGroup.rotation.y += 0.0015;
        }

        // 7. Water simulation - THROTTLED to avoid CPU stall on Android
        if (this.terrainPreset === 'animated_water' && this.groundMesh) {
            if (!this.waterFrameCount) this.waterFrameCount = 0;
            this.waterFrameCount++;
            if (this.waterFrameCount % 2 === 0) {
                this.waterTime += 0.06 * this.waterSpeed;
                const pos = this.groundMesh.geometry.attributes.position;
                const count = pos.count;
                for (let i = 0; i < count; i += 2) {
                    const x = pos.getX(i);
                    const y = pos.getY(i);
                    const z = Math.sin(x * 0.25 * this.terrainNoiseFreq + this.waterTime) * 0.4 * this.terrainHeight +
                              Math.cos(y * 0.25 * this.terrainNoiseFreq + this.waterTime * 1.2) * 0.3 * this.terrainHeight;
                    pos.setZ(i, z);
                    if (i + 1 < count) pos.setZ(i + 1, z);
                }
                pos.needsUpdate = true;
                if (this.waterFrameCount % 6 === 0) {
                    this.groundMesh.geometry.computeVertexNormals();
                }
            }
        }

        // 8. Timeline animation
        if (this.isPlaying) {
            this.currentTime += 1 / this.fps;
            if (this.currentTime > this.duration) {
                this.currentTime = 0;
            }

            if (this.isText3DAnimating) {
                this.objects.forEach((group) => {
                    group.rotation.y += 0.015;
                });
            }

            this.notifyAndroid('onTimelineUpdate', { currentTime: this.currentTime });
        }

        // 8.5 Process Hollywood Green Screen Chroma Key & Actor Composite
        if (this.greenScreenActive) {
            if (this.isSampleActorActive && this.sampleActorCanvas) {
                this.updateSampleActorFrame(now);
                if (this.sampleActorTexture) {
                    this.sampleActorTexture.needsUpdate = true;
                }
            } else if (this.videoTexture) {
                this.videoTexture.needsUpdate = true;
            }

            if (this.actorBillboard && this.actorMesh) {
                this.actorMesh.rotation.y = Math.atan2(
                    this.camera.position.x - this.actorMesh.position.x,
                    this.camera.position.z - this.actorMesh.position.z
                );
            }

            if (this.actorContactShadow && this.actorMesh) {
                this.actorContactShadow.position.x = this.actorMesh.position.x;
                this.actorContactShadow.position.z = this.actorMesh.position.z;
                const scaleVal = this.actorScale || 1.0;
                this.actorContactShadow.scale.set(scaleVal, scaleVal, scaleVal);
            }

            // Report video playback timing periodically to Android UI
            if (now - this.lastGreenScreenStateReport > 400) {
                this.lastGreenScreenStateReport = now;
                const ct = this.videoElement && !isNaN(this.videoElement.currentTime) ? this.videoElement.currentTime : ((now / 1000) % 10);
                const dur = this.videoElement && !isNaN(this.videoElement.duration) && this.videoElement.duration > 0 ? this.videoElement.duration : 10;
                const isPl = this.videoElement ? !this.videoElement.paused : this.greenScreenPlaying;
                this.notifyAndroid('onGreenScreenVideoState', { currentTime: ct, duration: dur, isPlaying: isPl });
            }
        }

        // 9. Real-Time FPS Performance Tracking
        if (!this.fpsFrameCounter) {
            this.fpsFrameCounter = 0;
            this.fpsLastTime = now;
            this.lastReportedFps = 60;
        }
        this.fpsFrameCounter++;
        const deltaFps = now - this.fpsLastTime;
        if (deltaFps >= 1000) {
            const calculatedFps = Math.max(1, Math.min(120, Math.round((this.fpsFrameCounter * 1000) / deltaFps)));
            this.fpsFrameCounter = 0;
            this.fpsLastTime = now;
            if (calculatedFps !== this.lastReportedFps) {
                this.lastReportedFps = calculatedFps;
                this.notifyAndroid('onFpsUpdate', { fps: calculatedFps });
            }
        }

        this.renderer.render(this.scene, this.camera);
    }

    applyThermalThrottling(level) {
        switch (level) {
            case 'light':
                if (this.dirLight1 && this.dirLight1.shadow) {
                    this.dirLight1.shadow.mapSize.set(1024, 1024);
                }
                this.starTwinkleSpeed = 0.5;
                break;
            case 'moderate':
                if (this.dirLight1 && this.dirLight1.shadow) {
                    this.dirLight1.shadow.mapSize.set(512, 512);
                }
                this.starTwinkleSpeed = 0.3;
                this.cloudSpeed = 0.02;
                break;
            case 'severe':
                this.renderer.setPixelRatio(1.0);
                this.renderer.shadowMap.enabled = false;
                this.starTwinkleSpeed = 0.2;
                this.cloudSpeed = 0.01;
                break;
            case 'critical':
                this.emergencyStop();
                this.applyRenderQuality('draft');
                this.renderer.shadowMap.enabled = false;
                break;
            case 'normal':
            default:
                this.applyRenderQuality('balanced');
                if (this.dirLight1 && this.dirLight1.shadow) {
                    this.dirLight1.shadow.mapSize.set(2048, 2048);
                }
                this.renderer.shadowMap.enabled = true;
                this.starTwinkleSpeed = 1.0;
                this.cloudSpeed = 0.05;
                break;
        }
    }

    setSkyPreset(preset) {
        this.skyPreset = preset;
        this.setupSkyEnvironment();
    }

    setMoonPhase(phase) {
        this.moonPhase = phase;
        this.setupSkyEnvironment();
    }

    setStarDensity(density) {
        this.starDensity = parseInt(density);
        this.setupSkyEnvironment();
    }

    setCloudSpeed(speed) {
        this.cloudSpeed = parseFloat(speed);
    }

    setStarTwinkleSpeed(speed) {
        this.starTwinkleSpeed = parseFloat(speed);
    }

    setTerrainPreset(preset) {
        this.terrainPreset = preset;
        this.setupEnvironmentStage();
    }

    setTerrainDisplacementHeight(height) {
        this.terrainHeight = parseFloat(height);
        this.setupEnvironmentStage();
    }

    setTerrainNoiseFrequency(freq) {
        this.terrainNoiseFreq = parseFloat(freq);
        this.setupEnvironmentStage();
    }

    setWaterWaveSpeed(speed) {
        this.waterSpeed = parseFloat(speed);
    }

    toggleText3DAnimation(enabled) {
        this.isText3DAnimating = enabled;
    }

    removeTextObject(id) {
        if (this.objects.has(id)) {
            const group = this.objects.get(id);
            this.scene.remove(group);
            this.objects.delete(id);
            if (this.selectedObjectId === id) {
                const keys = Array.from(this.objects.keys());
                this.selectedObjectId = keys.length > 0 ? keys[0] : null;
            }
        }
    }

    notifyAndroid(eventName, payload) {
        if (window.AndroidBridge && window.AndroidBridge[eventName]) {
            window.AndroidBridge[eventName](JSON.stringify(payload));
        }
    }
}

// Global window exposed functions for Android WebView bridge calls
window.setSkyPreset = (preset) => window.studioEngine && window.studioEngine.setSkyPreset(preset);
window.setMoonPhase = (phase) => window.studioEngine && window.studioEngine.setMoonPhase(phase);
window.setStarDensity = (density) => window.studioEngine && window.studioEngine.setStarDensity(density);
window.setCloudSpeed = (speed) => window.studioEngine && window.studioEngine.setCloudSpeed(speed);
window.setStarTwinkleSpeed = (s) => window.studioEngine && window.studioEngine.setStarTwinkleSpeed(s);

window.setTerrainPreset = (preset) => window.studioEngine && window.studioEngine.setTerrainPreset(preset);
window.setTerrainDisplacementHeight = (h) => window.studioEngine && window.studioEngine.setTerrainDisplacementHeight(h);
window.setTerrainNoiseFrequency = (f) => window.studioEngine && window.studioEngine.setTerrainNoiseFrequency(f);
window.setWaterWaveSpeed = (s) => window.studioEngine && window.studioEngine.setWaterWaveSpeed(s);
window.playCameraSequence = (seq, rec) => window.studioEngine && window.studioEngine.playCameraSequence(seq, rec);
window.stopCameraPreview = () => window.studioEngine && window.studioEngine.emergencyStop();
window.emergencyStop = () => window.studioEngine && window.studioEngine.emergencyStop();
window.applyThermalThrottling = (lvl) => window.studioEngine && window.studioEngine.applyThermalThrottling(lvl);
window.exportCameraSequenceVideo = (seq, fps, fmt) => window.studioEngine && window.studioEngine.exportCameraSequenceVideo(seq, fps, fmt);
window.loadImported3DModel = (cfg) => window.studioEngine && window.studioEngine.loadImported3DModel(cfg);
window.setModelAsPaintBrush = (id) => window.studioEngine && window.studioEngine.setModelAsPaintBrush(id);
window.spawnModelInScene = (id) => window.studioEngine && window.studioEngine.spawnModelInScene(id);

window.loadGreenScreenVideo = (url, name) => window.studioEngine && window.studioEngine.loadGreenScreenVideo(url, name);
window.loadSampleGreenScreenActor = (type) => window.studioEngine && window.studioEngine.loadSampleGreenScreenActor(type);
window.setGreenScreenPlayState = (play) => window.studioEngine && window.studioEngine.setGreenScreenPlayState(play);
window.seekGreenScreenVideo = (time) => window.studioEngine && window.studioEngine.seekGreenScreenVideo(time);
window.setGreenScreenPlaybackSpeed = (speed) => window.studioEngine && window.studioEngine.setGreenScreenPlaybackSpeed(speed);
window.setGreenScreenLoop = (loop) => window.studioEngine && window.studioEngine.setGreenScreenLoop(loop);
window.updateChromaKeyShader = (params) => window.studioEngine && window.studioEngine.updateChromaKeyShader(params);
window.updateActorTransform = (params) => window.studioEngine && window.studioEngine.updateActorTransform(params);

// Global engine initialization
window.addEventListener('DOMContentLoaded', () => {
    window.studioEngine = new StudioEngine();
});
