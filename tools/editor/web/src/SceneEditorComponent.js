/**
 * SceneEditorComponent.js - Web Components implementation of the scene editor
 *
 * This component provides a professional web-based 3D scene editor using:
 * - HTML5 Canvas for rendering
 * - WebGL/WebGPU for graphics
 * - Modern Web Components API
 * - Shadow DOM for encapsulation
 * - Real-time collaboration support
 */

class SceneEditorComponent extends HTMLElement {
    constructor() {
        super();

        // Create shadow DOM for encapsulation
        this.attachShadow({ mode: 'open' });

        // Component state
        this.state = {
            project: null,
            isLoading: false,
            error: null,
            selectedObjects: [],
            activeTool: 'SELECT',
            transformMode: 'GLOBAL',
            snapToGrid: false,
            cameraTransform: {
                position: { x: 5, y: 5, z: 5 },
                target: { x: 0, y: 0, z: 0 },
                fov: 75
            },
            renderStats: {
                fps: 60,
                frameTime: 16.67,
                triangles: 0,
                drawCalls: 0
            }
        };

        // Event listeners
        this.listeners = new Map();

        // WebGL/WebGPU context
        this.renderer = null;
        this.scene = null;
        this.camera = null;
        this.controls = null;

        // Animation frame
        this.animationId = null;

        // Collaboration
        this.websocket = null;
        this.sessionId = null;

        // Bind methods
        this.handleResize = this.handleResize.bind(this);
        this.handleKeyDown = this.handleKeyDown.bind(this);
        this.animate = this.animate.bind(this);
    }

    // Web Components lifecycle
    connectedCallback() {
        this.render();
        this.setupEventListeners();
        this.initializeRenderer();
        this.startAnimation();

        // Auto-resize handling
        window.addEventListener('resize', this.handleResize);
        document.addEventListener('keydown', this.handleKeyDown);
    }

    disconnectedCallback() {
        this.dispose();
        window.removeEventListener('resize', this.handleResize);
        document.removeEventListener('keydown', this.handleKeyDown);
    }

    // Observed attributes for reactive updates
    static get observedAttributes() {
        return ['project-id', 'width', 'height', 'theme', 'readonly'];
    }

    attributeChangedCallback(name, oldValue, newValue) {
        if (oldValue === newValue) return;

        switch (name) {
            case 'project-id':
                this.loadProject(newValue);
                break;
            case 'width':
            case 'height':
                this.handleResize();
                break;
            case 'theme':
                this.updateTheme(newValue);
                break;
            case 'readonly':
                this.updateReadonlyMode(newValue !== null);
                break;
        }
    }

    // Component rendering
    render() {
        this.shadowRoot.innerHTML = `
            <style>
                ${this.getStyles()}
            </style>

            <div class="scene-editor">
                <!-- Toolbar -->
                <div class="toolbar">
                    <div class="tool-group">
                        <button class="tool-btn active" data-tool="SELECT" title="Select (V)">
                            <svg width="16" height="16" viewBox="0 0 16 16">
                                <path d="M2 2L6 6L4 8L0 4Z" fill="currentColor"/>
                            </svg>
                        </button>
                        <button class="tool-btn" data-tool="TRANSLATE" title="Move (G)">
                            <svg width="16" height="16" viewBox="0 0 16 16">
                                <path d="M8 0L8 16M0 8L16 8" stroke="currentColor" stroke-width="2"/>
                            </svg>
                        </button>
                        <button class="tool-btn" data-tool="ROTATE" title="Rotate (R)">
                            <svg width="16" height="16" viewBox="0 0 16 16">
                                <circle cx="8" cy="8" r="6" fill="none" stroke="currentColor" stroke-width="2"/>
                                <path d="M8 2L10 4L6 4Z" fill="currentColor"/>
                            </svg>
                        </button>
                        <button class="tool-btn" data-tool="SCALE" title="Scale (S)">
                            <svg width="16" height="16" viewBox="0 0 16 16">
                                <rect x="2" y="2" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2"/>
                                <rect x="4" y="4" width="8" height="8" fill="currentColor"/>
                            </svg>
                        </button>
                    </div>

                    <div class="tool-group">
                        <select class="transform-mode">
                            <option value="GLOBAL">Global</option>
                            <option value="LOCAL">Local</option>
                            <option value="VIEW">View</option>
                        </select>
                        <button class="tool-btn" data-toggle="snap" title="Snap to Grid (Ctrl+Shift+S)">
                            <svg width="16" height="16" viewBox="0 0 16 16">
                                <defs>
                                    <pattern id="grid" width="4" height="4" patternUnits="userSpaceOnUse">
                                        <circle cx="2" cy="2" r="0.5" fill="currentColor"/>
                                    </pattern>
                                </defs>
                                <rect width="16" height="16" fill="url(#grid)"/>
                            </svg>
                        </button>
                    </div>

                    <div class="tool-group">
                        <button class="tool-btn" data-action="undo" title="Undo (Ctrl+Z)">
                            <svg width="16" height="16" viewBox="0 0 16 16">
                                <path d="M8 2A6 6 0 0 0 2.5 4.5L1 3V7H5L3.5 5.5A4 4 0 1 1 4 10" stroke="currentColor" fill="none" stroke-width="2"/>
                            </svg>
                        </button>
                        <button class="tool-btn" data-action="redo" title="Redo (Ctrl+Y)">
                            <svg width="16" height="16" viewBox="0 0 16 16">
                                <path d="M8 2A6 6 0 0 1 13.5 4.5L15 3V7H11L12.5 5.5A4 4 0 1 0 12 10" stroke="currentColor" fill="none" stroke-width="2"/>
                            </svg>
                        </button>
                    </div>

                    <div class="tool-group">
                        <button class="tool-btn" data-action="play" title="Play Animation">
                            <svg width="16" height="16" viewBox="0 0 16 16">
                                <polygon points="3,2 13,8 3,14" fill="currentColor"/>
                            </svg>
                        </button>
                    </div>
                </div>

                <!-- Main content area -->
                <div class="main-content">
                    <!-- Left panel - Scene hierarchy -->
                    <div class="left-panel">
                        <div class="panel-header">
                            <h3>Scene</h3>
                            <button class="add-btn" data-action="add-object">+</button>
                        </div>
                        <div class="scene-hierarchy">
                            <!-- Scene tree will be populated here -->
                        </div>
                    </div>

                    <!-- Center - 3D Viewport -->
                    <div class="viewport-container">
                        <canvas class="viewport" width="800" height="600"></canvas>

                        <!-- Viewport overlay -->
                        <div class="viewport-overlay">
                            <!-- Gizmos and helpers will be rendered here -->
                        </div>

                        <!-- Loading indicator -->
                        <div class="loading-indicator" style="display: none;">
                            <div class="spinner"></div>
                            <span>Loading...</span>
                        </div>

                        <!-- Stats overlay -->
                        <div class="stats-overlay">
                            <div class="stats-item">FPS: <span class="fps-counter">60</span></div>
                            <div class="stats-item">Triangles: <span class="triangle-counter">0</span></div>
                            <div class="stats-item">Draw Calls: <span class="drawcall-counter">0</span></div>
                        </div>
                    </div>

                    <!-- Right panel - Properties -->
                    <div class="right-panel">
                        <div class="panel-header">
                            <h3>Properties</h3>
                        </div>
                        <div class="properties-content">
                            <div class="property-group">
                                <h4>Transform</h4>
                                <div class="property-row">
                                    <label>Position</label>
                                    <div class="vector3-input">
                                        <input type="number" class="x-input" placeholder="X" step="0.1">
                                        <input type="number" class="y-input" placeholder="Y" step="0.1">
                                        <input type="number" class="z-input" placeholder="Z" step="0.1">
                                    </div>
                                </div>
                                <div class="property-row">
                                    <label>Rotation</label>
                                    <div class="vector3-input">
                                        <input type="number" class="rx-input" placeholder="X" step="1">
                                        <input type="number" class="ry-input" placeholder="Y" step="1">
                                        <input type="number" class="rz-input" placeholder="Z" step="1">
                                    </div>
                                </div>
                                <div class="property-row">
                                    <label>Scale</label>
                                    <div class="vector3-input">
                                        <input type="number" class="sx-input" placeholder="X" step="0.1" value="1">
                                        <input type="number" class="sy-input" placeholder="Y" step="0.1" value="1">
                                        <input type="number" class="sz-input" placeholder="Z" step="0.1" value="1">
                                    </div>
                                </div>
                            </div>

                            <div class="property-group">
                                <h4>Material</h4>
                                <div class="property-row">
                                    <label>Material</label>
                                    <select class="material-select">
                                        <option value="">None</option>
                                    </select>
                                </div>
                                <div class="property-row">
                                    <label>Color</label>
                                    <input type="color" class="color-input" value="#ffffff">
                                </div>
                            </div>

                            <div class="property-group">
                                <h4>Visibility</h4>
                                <div class="property-row">
                                    <label>
                                        <input type="checkbox" class="visible-checkbox" checked>
                                        Visible
                                    </label>
                                </div>
                                <div class="property-row">
                                    <label>
                                        <input type="checkbox" class="shadows-checkbox" checked>
                                        Cast Shadows
                                    </label>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Status bar -->
                <div class="status-bar">
                    <div class="status-left">
                        <span class="project-name">Untitled Project</span>
                        <span class="save-status">●</span>
                    </div>
                    <div class="status-right">
                        <span class="camera-info">Camera: Perspective</span>
                        <span class="selection-info">Selected: 0</span>
                    </div>
                </div>
            </div>
        `;

        // Cache DOM references
        this.cacheElements();
    }

    cacheElements() {
        const root = this.shadowRoot;

        this.elements = {
            canvas: root.querySelector('.viewport'),
            toolbar: root.querySelector('.toolbar'),
            sceneHierarchy: root.querySelector('.scene-hierarchy'),
            propertiesContent: root.querySelector('.properties-content'),
            loadingIndicator: root.querySelector('.loading-indicator'),
            statsOverlay: root.querySelector('.stats-overlay'),
            statusBar: root.querySelector('.status-bar')
        };
    }

    getStyles() {
        return `
            :host {
                display: block;
                width: 100%;
                height: 100%;
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                --bg-primary: #1e1e1e;
                --bg-secondary: #2d2d2d;
                --bg-tertiary: #383838;
                --text-primary: #ffffff;
                --text-secondary: #cccccc;
                --border-color: #404040;
                --accent-color: #007acc;
                --success-color: #4caf50;
                --warning-color: #ff9800;
                --error-color: #f44336;
            }

            .scene-editor {
                display: flex;
                flex-direction: column;
                height: 100%;
                background: var(--bg-primary);
                color: var(--text-primary);
            }

            /* Toolbar Styles */
            .toolbar {
                display: flex;
                align-items: center;
                gap: 16px;
                padding: 8px 16px;
                background: var(--bg-secondary);
                border-bottom: 1px solid var(--border-color);
                flex-shrink: 0;
            }

            .tool-group {
                display: flex;
                gap: 4px;
                padding-right: 16px;
                border-right: 1px solid var(--border-color);
            }

            .tool-group:last-child {
                border-right: none;
            }

            .tool-btn {
                width: 32px;
                height: 32px;
                border: 1px solid var(--border-color);
                background: var(--bg-tertiary);
                color: var(--text-secondary);
                cursor: pointer;
                border-radius: 4px;
                display: flex;
                align-items: center;
                justify-content: center;
                transition: all 0.2s;
            }

            .tool-btn:hover {
                background: var(--bg-primary);
                color: var(--text-primary);
            }

            .tool-btn.active {
                background: var(--accent-color);
                color: white;
                border-color: var(--accent-color);
            }

            .transform-mode {
                background: var(--bg-tertiary);
                color: var(--text-primary);
                border: 1px solid var(--border-color);
                border-radius: 4px;
                padding: 6px 8px;
                font-size: 12px;
            }

            /* Main Content Layout */
            .main-content {
                display: flex;
                flex: 1;
                overflow: hidden;
            }

            .left-panel, .right-panel {
                width: 280px;
                background: var(--bg-secondary);
                border-right: 1px solid var(--border-color);
                display: flex;
                flex-direction: column;
                flex-shrink: 0;
            }

            .right-panel {
                border-right: none;
                border-left: 1px solid var(--border-color);
            }

            .panel-header {
                display: flex;
                align-items: center;
                justify-content: space-between;
                padding: 12px 16px;
                background: var(--bg-tertiary);
                border-bottom: 1px solid var(--border-color);
            }

            .panel-header h3 {
                margin: 0;
                font-size: 14px;
                font-weight: 600;
            }

            .add-btn {
                width: 24px;
                height: 24px;
                border: 1px solid var(--border-color);
                background: var(--bg-primary);
                color: var(--text-primary);
                border-radius: 3px;
                cursor: pointer;
                font-size: 16px;
                line-height: 1;
            }

            /* Viewport Styles */
            .viewport-container {
                flex: 1;
                position: relative;
                display: flex;
                align-items: center;
                justify-content: center;
                background: var(--bg-primary);
            }

            .viewport {
                width: 100%;
                height: 100%;
                border: none;
                outline: none;
                cursor: crosshair;
            }

            .viewport-overlay {
                position: absolute;
                top: 0;
                left: 0;
                right: 0;
                bottom: 0;
                pointer-events: none;
                z-index: 10;
            }

            .loading-indicator {
                position: absolute;
                top: 50%;
                left: 50%;
                transform: translate(-50%, -50%);
                display: flex;
                flex-direction: column;
                align-items: center;
                gap: 16px;
                color: var(--text-secondary);
                z-index: 100;
            }

            .spinner {
                width: 32px;
                height: 32px;
                border: 3px solid var(--border-color);
                border-top: 3px solid var(--accent-color);
                border-radius: 50%;
                animation: spin 1s linear infinite;
            }

            @keyframes spin {
                0% { transform: rotate(0deg); }
                100% { transform: rotate(360deg); }
            }

            .stats-overlay {
                position: absolute;
                top: 16px;
                right: 16px;
                background: rgba(0, 0, 0, 0.7);
                padding: 8px 12px;
                border-radius: 4px;
                font-size: 12px;
                font-family: monospace;
                z-index: 20;
            }

            .stats-item {
                display: flex;
                justify-content: space-between;
                gap: 8px;
                margin-bottom: 2px;
            }

            .stats-item:last-child {
                margin-bottom: 0;
            }

            /* Properties Panel */
            .properties-content {
                flex: 1;
                overflow-y: auto;
                padding: 0;
            }

            .property-group {
                border-bottom: 1px solid var(--border-color);
            }

            .property-group h4 {
                margin: 0;
                padding: 12px 16px;
                background: var(--bg-tertiary);
                font-size: 12px;
                font-weight: 600;
                text-transform: uppercase;
                letter-spacing: 0.5px;
            }

            .property-row {
                padding: 8px 16px;
                display: flex;
                align-items: center;
                justify-content: space-between;
                gap: 8px;
            }

            .property-row label {
                font-size: 12px;
                color: var(--text-secondary);
                min-width: 60px;
            }

            .vector3-input {
                display: flex;
                gap: 4px;
            }

            .vector3-input input {
                width: 50px;
                padding: 4px 6px;
                background: var(--bg-tertiary);
                border: 1px solid var(--border-color);
                color: var(--text-primary);
                border-radius: 3px;
                font-size: 11px;
            }

            .material-select, .color-input {
                padding: 4px 6px;
                background: var(--bg-tertiary);
                border: 1px solid var(--border-color);
                color: var(--text-primary);
                border-radius: 3px;
                font-size: 11px;
            }

            /* Status Bar */
            .status-bar {
                display: flex;
                align-items: center;
                justify-content: space-between;
                padding: 6px 16px;
                background: var(--bg-secondary);
                border-top: 1px solid var(--border-color);
                font-size: 12px;
                flex-shrink: 0;
            }

            .status-left, .status-right {
                display: flex;
                align-items: center;
                gap: 16px;
            }

            .save-status {
                color: var(--warning-color);
            }

            .save-status.saved {
                color: var(--success-color);
            }

            /* Responsive Design */
            @media (max-width: 1200px) {
                .left-panel, .right-panel {
                    width: 240px;
                }
            }

            @media (max-width: 900px) {
                .left-panel, .right-panel {
                    position: absolute;
                    top: 0;
                    bottom: 0;
                    z-index: 100;
                    transform: translateX(-100%);
                    transition: transform 0.3s ease;
                }

                .right-panel {
                    right: 0;
                    transform: translateX(100%);
                }

                .panel-visible .left-panel {
                    transform: translateX(0);
                }

                .panel-visible .right-panel {
                    transform: translateX(0);
                }
            }
        `;
    }

    setupEventListeners() {
        const root = this.shadowRoot;

        // Tool selection
        root.querySelectorAll('[data-tool]').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const tool = e.currentTarget.dataset.tool;
                this.setActiveTool(tool);
            });
        });

        // Actions
        root.querySelectorAll('[data-action]').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const action = e.currentTarget.dataset.action;
                this.handleAction(action);
            });
        });

        // Transform mode
        root.querySelector('.transform-mode').addEventListener('change', (e) => {
            this.setTransformMode(e.target.value);
        });

        // Property inputs
        this.setupPropertyInputs();

        // Canvas interactions
        this.setupCanvasEvents();
    }

    setupPropertyInputs() {
        const root = this.shadowRoot;

        // Vector3 inputs (position, rotation, scale)
        ['x', 'y', 'z'].forEach(axis => {
            const posInput = root.querySelector(`.${axis}-input`);
            const rotInput = root.querySelector(`.r${axis}-input`);
            const scaleInput = root.querySelector(`.s${axis}-input`);

            if (posInput) {
                posInput.addEventListener('change', () => this.updateObjectTransform());
            }
            if (rotInput) {
                rotInput.addEventListener('change', () => this.updateObjectTransform());
            }
            if (scaleInput) {
                scaleInput.addEventListener('change', () => this.updateObjectTransform());
            }
        });

        // Material and color
        root.querySelector('.material-select')?.addEventListener('change', (e) => {
            this.updateObjectMaterial(e.target.value);
        });

        root.querySelector('.color-input')?.addEventListener('change', (e) => {
            this.updateObjectColor(e.target.value);
        });

        // Visibility checkboxes
        root.querySelector('.visible-checkbox')?.addEventListener('change', (e) => {
            this.updateObjectVisibility(e.target.checked);
        });
    }

    setupCanvasEvents() {
        const canvas = this.elements.canvas;

        // Mouse events for interaction
        canvas.addEventListener('mousedown', this.handleMouseDown.bind(this));
        canvas.addEventListener('mousemove', this.handleMouseMove.bind(this));
        canvas.addEventListener('mouseup', this.handleMouseUp.bind(this));
        canvas.addEventListener('wheel', this.handleWheel.bind(this));

        // Context menu
        canvas.addEventListener('contextmenu', (e) => e.preventDefault());

        // Focus for keyboard events
        canvas.addEventListener('click', () => canvas.focus());
        canvas.tabIndex = 0;
    }

    // Core editor functionality
    async initializeRenderer() {
        try {
            this.setState({ isLoading: true });

            const canvas = this.elements.canvas;

            // Try WebGPU first, fallback to WebGL
            let context;
            if (navigator.gpu) {
                try {
                    const adapter = await navigator.gpu.requestAdapter();
                    const device = await adapter.requestDevice();
                    context = canvas.getContext('webgpu');
                    // Initialize WebGPU renderer
                    this.renderer = new WebGPURenderer(context, device);
                } catch (e) {
                    console.warn('WebGPU not available, falling back to WebGL:', e);
                }
            }

            if (!this.renderer) {
                context = canvas.getContext('webgl2') || canvas.getContext('webgl');
                if (!context) {
                    throw new Error('WebGL not supported');
                }
                this.renderer = new WebGLRenderer(context);
            }

            // Initialize scene, camera, and controls
            this.scene = new Scene();
            this.camera = new PerspectiveCamera(75, canvas.width / canvas.height, 0.1, 1000);
            this.controls = new OrbitControls(this.camera, canvas);

            // Set default camera position
            this.camera.position.set(5, 5, 5);
            this.camera.lookAt(0, 0, 0);

            // Add default lighting
            this.addDefaultLighting();

            // Create grid and helpers
            this.createGridAndHelpers();

            this.setState({ isLoading: false });
            this.dispatchEvent(new CustomEvent('renderer-initialized'));

        } catch (error) {
            this.setState({
                isLoading: false,
                error: `Failed to initialize renderer: ${error.message}`
            });
            console.error('Renderer initialization failed:', error);
        }
    }

    addDefaultLighting() {
        // Directional light (sun)
        const directionalLight = new DirectionalLight(0xffffff, 3);
        directionalLight.position.set(10, 10, 10);
        directionalLight.castShadow = true;
        this.scene.add(directionalLight);

        // Ambient light
        const ambientLight = new AmbientLight(0x404040, 0.3);
        this.scene.add(ambientLight);
    }

    createGridAndHelpers() {
        // Grid
        this.grid = new GridHelper(20, 20, 0x888888, 0x444444);
        this.scene.add(this.grid);

        // Axes helper
        this.axesHelper = new AxesHelper(5);
        this.scene.add(this.axesHelper);
    }

    startAnimation() {
        this.animate();
    }

    animate() {
        this.animationId = requestAnimationFrame(this.animate);

        if (this.renderer && this.scene && this.camera) {
            // Update controls
            this.controls?.update();

            // Update statistics
            this.updateRenderStats();

            // Render scene
            this.renderer.render(this.scene, this.camera);
        }
    }

    updateRenderStats() {
        const now = performance.now();
        if (this.lastFrameTime) {
            const frameTime = now - this.lastFrameTime;
            const fps = 1000 / frameTime;

            this.setState({
                renderStats: {
                    ...this.state.renderStats,
                    fps: Math.round(fps),
                    frameTime: Math.round(frameTime * 100) / 100
                }
            });

            // Update stats overlay
            const fpsCounter = this.shadowRoot.querySelector('.fps-counter');
            if (fpsCounter) {
                fpsCounter.textContent = Math.round(fps);
            }
        }
        this.lastFrameTime = now;
    }

    // Public API methods implementing SceneEditor interface
    async loadProject(projectId) {
        try {
            this.setState({ isLoading: true });

            // Make API call to load project
            const response = await fetch(`/api/projects/${projectId}`);
            if (!response.ok) {
                throw new Error(`Failed to load project: ${response.statusText}`);
            }

            const project = await response.json();
            this.setState({ project, isLoading: false });

            // Load scene content
            await this.loadSceneContent(project.scene);

            this.dispatchEvent(new CustomEvent('project-loaded', { detail: project }));

        } catch (error) {
            this.setState({
                isLoading: false,
                error: `Failed to load project: ${error.message}`
            });
        }
    }

    async createProject(name, template = 'BASIC') {
        try {
            const response = await fetch('/api/projects', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ name, template })
            });

            if (!response.ok) {
                throw new Error(`Failed to create project: ${response.statusText}`);
            }

            const project = await response.json();
            this.setState({ project });

            this.dispatchEvent(new CustomEvent('project-created', { detail: project }));
            return project;

        } catch (error) {
            this.setState({ error: error.message });
            throw error;
        }
    }

    async saveProject() {
        if (!this.state.project) return;

        try {
            const response = await fetch(`/api/projects/${this.state.project.id}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(this.state.project)
            });

            if (!response.ok) {
                throw new Error(`Failed to save project: ${response.statusText}`);
            }

            this.dispatchEvent(new CustomEvent('project-saved'));

        } catch (error) {
            this.setState({ error: error.message });
            throw error;
        }
    }

    setActiveTool(tool) {
        this.setState({ activeTool: tool });

        // Update UI
        this.shadowRoot.querySelectorAll('[data-tool]').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.tool === tool);
        });

        // Update cursor and interaction mode
        this.updateCursorForTool(tool);

        this.dispatchEvent(new CustomEvent('tool-changed', { detail: tool }));
    }

    setTransformMode(mode) {
        this.setState({ transformMode: mode });
        this.dispatchEvent(new CustomEvent('transform-mode-changed', { detail: mode }));
    }

    // Event handlers
    handleAction(action) {
        switch (action) {
            case 'undo':
                this.undo();
                break;
            case 'redo':
                this.redo();
                break;
            case 'play':
                this.toggleAnimation();
                break;
            case 'add-object':
                this.showAddObjectMenu();
                break;
        }
    }

    handleMouseDown(event) {
        // Implement mouse interaction based on active tool
    }

    handleMouseMove(event) {
        // Handle mouse movement for tool interaction
    }

    handleMouseUp(event) {
        // Complete mouse interaction
    }

    handleWheel(event) {
        // Zoom functionality (handled by controls)
        event.preventDefault();
    }

    handleResize() {
        const canvas = this.elements.canvas;
        const container = canvas.parentElement;

        canvas.width = container.clientWidth;
        canvas.height = container.clientHeight;

        if (this.camera) {
            this.camera.aspect = canvas.width / canvas.height;
            this.camera.updateProjectionMatrix();
        }

        if (this.renderer) {
            this.renderer.setSize(canvas.width, canvas.height);
        }
    }

    handleKeyDown(event) {
        // Keyboard shortcuts
        if (event.ctrlKey || event.metaKey) {
            switch (event.key) {
                case 'z':
                    event.preventDefault();
                    if (event.shiftKey) {
                        this.redo();
                    } else {
                        this.undo();
                    }
                    break;
                case 'y':
                    event.preventDefault();
                    this.redo();
                    break;
                case 's':
                    event.preventDefault();
                    this.saveProject();
                    break;
            }
        } else {
            switch (event.key) {
                case 'v':
                    this.setActiveTool('SELECT');
                    break;
                case 'g':
                    this.setActiveTool('TRANSLATE');
                    break;
                case 'r':
                    this.setActiveTool('ROTATE');
                    break;
                case 's':
                    this.setActiveTool('SCALE');
                    break;
                case 'Delete':
                    this.deleteSelectedObjects();
                    break;
            }
        }
    }

    // Utility methods
    setState(newState) {
        this.state = { ...this.state, ...newState };
        this.updateUI();
    }

    updateUI() {
        // Update UI elements based on state changes
        this.updateStatusBar();
        this.updateStatsOverlay();
        this.updateLoadingState();
    }

    updateStatusBar() {
        const statusBar = this.elements.statusBar;
        const projectName = statusBar.querySelector('.project-name');
        const selectionInfo = statusBar.querySelector('.selection-info');

        if (projectName && this.state.project) {
            projectName.textContent = this.state.project.name;
        }

        if (selectionInfo) {
            selectionInfo.textContent = `Selected: ${this.state.selectedObjects.length}`;
        }
    }

    updateStatsOverlay() {
        const stats = this.state.renderStats;
        const overlay = this.elements.statsOverlay;

        overlay.querySelector('.fps-counter').textContent = stats.fps;
        overlay.querySelector('.triangle-counter').textContent = stats.triangles;
        overlay.querySelector('.drawcall-counter').textContent = stats.drawCalls;
    }

    updateLoadingState() {
        const indicator = this.elements.loadingIndicator;
        indicator.style.display = this.state.isLoading ? 'flex' : 'none';
    }

    updateCursorForTool(tool) {
        const canvas = this.elements.canvas;
        const cursors = {
            'SELECT': 'default',
            'TRANSLATE': 'move',
            'ROTATE': 'grab',
            'SCALE': 'nw-resize'
        };
        canvas.style.cursor = cursors[tool] || 'default';
    }

    // Cleanup
    dispose() {
        if (this.animationId) {
            cancelAnimationFrame(this.animationId);
        }

        if (this.websocket) {
            this.websocket.close();
        }

        if (this.renderer) {
            this.renderer.dispose();
        }

        // Remove event listeners
        this.listeners.clear();
    }
}

// Register the custom element
customElements.define('scene-editor', SceneEditorComponent);

// Export for module usage
export { SceneEditorComponent };

// Placeholder renderer classes (would be implemented with actual WebGL/WebGPU)
class WebGLRenderer {
    constructor(context) {
        this.context = context;
    }

    render(scene, camera) {
        // WebGL rendering implementation
    }

    setSize(width, height) {
        // Update renderer size
    }

    dispose() {
        // Cleanup WebGL resources
    }
}

class WebGPURenderer {
    constructor(context, device) {
        this.context = context;
        this.device = device;
    }

    render(scene, camera) {
        // WebGPU rendering implementation
    }

    setSize(width, height) {
        // Update renderer size
    }

    dispose() {
        // Cleanup WebGPU resources
    }
}

// Placeholder 3D classes (would be implemented with actual 3D math)
class Scene {
    constructor() {
        this.children = [];
    }

    add(object) {
        this.children.push(object);
    }

    remove(object) {
        const index = this.children.indexOf(object);
        if (index > -1) {
            this.children.splice(index, 1);
        }
    }
}

class PerspectiveCamera {
    constructor(fov, aspect, near, far) {
        this.fov = fov;
        this.aspect = aspect;
        this.near = near;
        this.far = far;
        this.position = { x: 0, y: 0, z: 0 };
    }

    updateProjectionMatrix() {
        // Update camera projection
    }

    lookAt(x, y, z) {
        // Set camera target
    }
}

class OrbitControls {
    constructor(camera, domElement) {
        this.camera = camera;
        this.domElement = domElement;
    }

    update() {
        // Update camera controls
    }
}

class DirectionalLight {
    constructor(color, intensity) {
        this.color = color;
        this.intensity = intensity;
        this.position = { x: 0, y: 0, z: 0 };
        this.castShadow = false;
    }
}

class AmbientLight {
    constructor(color, intensity) {
        this.color = color;
        this.intensity = intensity;
    }
}

class GridHelper {
    constructor(size, divisions, colorCenterLine, colorGrid) {
        this.size = size;
        this.divisions = divisions;
        this.colorCenterLine = colorCenterLine;
        this.colorGrid = colorGrid;
    }
}

class AxesHelper {
    constructor(size) {
        this.size = size;
    }
}