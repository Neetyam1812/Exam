/**
 * Premium Microsoft Word-Style Shape Drawing & Whiteboard Engine
 * Built for Spring Boot + Thymeleaf + PostgreSQL examination portals
 * Uses Fabric.js for high-fidelity interactive vector graphics
 * Author: Antigravity AI
 */

window.canvasStates = window.canvasStates || {};
(function () {
    // History states mapped by question ID
    const canvasStates = window.canvasStates;

    function bindShapeAndTextEvents(shape, textObj, canvas) {
        if (!shape || !textObj) return;
        shape.textObject = textObj;
        textObj.shapeObject = shape;
        
        // Listen to shape movements to align text
        shape.on('moving', () => {
            textObj.set({
                left: shape.left,
                top: shape.top
            });
            textObj.setCoords();
            canvas.renderAll();
        });
        
        shape.on('scaling', () => {
            textObj.set({
                left: shape.left,
                top: shape.top
            });
            textObj.setCoords();
            canvas.renderAll();
        });
        
        shape.on('rotating', () => {
            textObj.set({
                left: shape.left,
                top: shape.top,
                angle: shape.angle
            });
            textObj.setCoords();
            canvas.renderAll();
        });
        
        // Propagate removal
        shape.on('removed', () => {
            if (textObj && canvas.getObjects().indexOf(textObj) > -1) {
                canvas.remove(textObj);
            }
        });

        textObj.on('removed', () => {
            shape.textObject = null;
            shape.textLinkId = null;
        });
    }

    /**
     * Initializes whiteboard drawing canvases below descriptive question answer containers.
     * @param {Long} attemptId - Active student exam attempt ID
     * @param {Long} submissionId - Active student paper submission ID
     */
    function initializeDrawingTool(attemptId, submissionId) {
        const textareas = document.querySelectorAll('.answer-area');
        textareas.forEach(textarea => {
            let qid = textarea.getAttribute('data-qid');
            if (!qid) {
                if (textarea.id === 'answerTextarea') {
                    qid = 'paper-full';
                } else {
                    return;
                }
            }
            if (document.getElementById(`wb-container-${qid}`)) {
                return;
            }
            const html = createWhiteboardHTML(qid);
            const parent = textarea.parentElement;
            if (parent) {
                parent.appendChild(document.createRange().createContextualFragment(html));
            } else {
                textarea.insertAdjacentHTML('afterend', html);
            }
            setupFabricCanvas(qid, attemptId, submissionId);
        });
    }

    /**
     * Generates a modern Microsoft Word-style floating toolbar and drawing canvas template.
     */
    function createWhiteboardHTML(qid) {
        return `
        <div class="whiteboard-container shadow-sm" id="wb-container-${qid}">
            <div class="whiteboard-header">
                <div class="whiteboard-title">
                    <i class="fa-solid fa-compass-drafting fs-5"></i> Word-Style Drawing Canvas & Whitespace (Draw trees, flowcharts, or diagrams)
                </div>
                <span class="badge bg-secondary-subtle text-secondary px-3 py-1.5 rounded-pill small" id="wb-save-status-${qid}">
                    <i class="fa-solid fa-cloud-arrow-up me-1"></i> Saved
                </span>
            </div>
            
            <div class="drawing-toolbar">
                <!-- Group 1: Modes -->
                <div class="toolbar-group">
                    <button type="button" class="drawing-btn active" id="btn-select-${qid}" data-tooltip="Pointer Mode (Move/Resize)">
                        <i class="fa-solid fa-arrow-pointer"></i>
                    </button>
                    <button type="button" class="drawing-btn" id="btn-pencil-${qid}" data-tooltip="Freehand Pencil">
                        <i class="fa-solid fa-pencil"></i>
                    </button>
                </div>
                

                
                <!-- Group 3: Word Colors and Sliders -->
                <div class="toolbar-group">
                    <div class="tool-setting-wrapper">
                        <span>Fill:</span>
                        <input type="color" class="color-picker-input" id="fill-color-${qid}" value="#ffffff">
                    </div>
                    <div class="tool-setting-wrapper">
                        <span>Stroke:</span>
                        <input type="color" class="color-picker-input" id="stroke-color-${qid}" value="#000000">
                    </div>
                    <div class="tool-setting-wrapper">
                        <span>Size:</span>
                        <input type="range" class="brush-slider" id="stroke-width-${qid}" min="1" max="15" value="2">
                    </div>
                </div>

                <!-- Group 3.5: Text Style Controls -->
                <div class="toolbar-group">
                    <button type="button" class="drawing-btn" id="btn-bold-${qid}" data-tooltip="Bold Text">
                        <i class="fa-solid fa-bold"></i>
                    </button>
                    <button type="button" class="drawing-btn" id="btn-italic-${qid}" data-tooltip="Italic Text">
                        <i class="fa-solid fa-italic"></i>
                    </button>
                    <button type="button" class="drawing-btn" id="btn-underline-${qid}" data-tooltip="Underline Text">
                        <i class="fa-solid fa-underline"></i>
                    </button>
                    <div class="tool-setting-wrapper">
                        <span>Font:</span>
                        <select id="font-family-${qid}" class="drawing-select" style="width:130px;">
                            <option value='"Aptos (Body)", sans-serif' selected>Aptos (Body)</option>
                            <option value='"Inter", sans-serif'>Inter</option>
                            <option value='"Outfit", sans-serif'>Outfit</option>
                            <option value='"Arial", sans-serif'>Arial</option>
                            <option value='"Times New Roman", serif'>Times New Roman</option>
                            <option value='"Courier New", monospace'>Courier New</option>
                            <option value='"Georgia", serif'>Georgia</option>
                            <option value='"Verdana", sans-serif'>Verdana</option>
                        </select>
                    </div>
                    <div class="tool-setting-wrapper">
                        <span>Size:</span>
                        <input type="text" id="font-size-${qid}" class="drawing-select" list="font-sizes-list-${qid}" style="width:75px;" value="12pt">
                        <datalist id="font-sizes-list-${qid}">
                            <option value="10pt">10pt</option>
                            <option value="11pt">11pt</option>
                            <option value="12pt">12pt</option>
                            <option value="14pt">14pt</option>
                            <option value="16pt">16pt</option>
                            <option value="18pt">18pt</option>
                            <option value="20pt">20pt</option>
                            <option value="24pt">24pt</option>
                            <option value="28pt">28pt</option>
                            <option value="32pt">32pt</option>
                        </datalist>
                    </div>
                </div>
                
                <!-- Group 4: Ordering & Canvas Controls -->
                <div class="toolbar-group">
                    <button type="button" class="drawing-btn" id="btn-eraser-${qid}" data-tooltip="Eraser (Delete Selected)">
                        <i class="fa-solid fa-trash-can"></i>
                    </button>
                    <button type="button" class="drawing-btn" id="btn-duplicate-${qid}" data-tooltip="Duplicate Object">
                        <i class="fa-regular fa-copy"></i>
                    </button>
                    <button type="button" class="drawing-btn" id="btn-forward-${qid}" data-tooltip="Bring Forward">
                        <i class="fa-solid fa-layer-group"></i>
                    </button>
                    <button type="button" class="drawing-btn" id="btn-backward-${qid}" data-tooltip="Send Backward">
                        <i class="fa-solid fa-layer-group" style="transform: scaleY(-1);"></i>
                    </button>
                    <button type="button" class="drawing-btn" id="btn-grid-${qid}" data-tooltip="Toggle Grid Background">
                        <i class="fa-solid fa-border-all"></i>
                    </button>
                </div>
                
                <!-- Group 5: Whiteboard History -->
                <div class="toolbar-group">
                    <button type="button" class="drawing-btn" id="btn-undo-${qid}" data-tooltip="Undo Action">
                        <i class="fa-solid fa-rotate-left"></i>
                    </button>
                    <button type="button" class="drawing-btn" id="btn-redo-${qid}" data-tooltip="Redo Action">
                        <i class="fa-solid fa-rotate-right"></i>
                    </button>
                    <button type="button" class="drawing-btn text-danger border-danger-subtle" id="btn-clear-${qid}" data-tooltip="Clear Canvas">
                        <i class="fa-solid fa-circle-xmark"></i>
                    </button>
                </div>
            </div>
            
            <div class="canvas-wrapper grid-bg" id="canvas-wrapper-${qid}" style="touch-action: none; user-select: none;">
                <canvas id="canvas-${qid}" width="800" height="400"></canvas>
            </div>
        </div>
        `;
    }

    /**
     * Initializes the Fabric.js dynamic whiteboard instance, registers shape draw listeners, and coordinates history.
     */
    function setupFabricCanvas(qid, attemptId, submissionId) {
        if (canvasStates[qid]) {
            const oldState = canvasStates[qid];
            if (oldState.canvas && document.body.contains(oldState.canvas.getElement())) {
                console.log(`Canvas for qid ${qid} is already active in the DOM. Skipping duplicate initialization.`);
                return;
            } else {
                console.log(`Canvas for qid ${qid} element is detached. Disposing old instance...`);
                try {
                    oldState.canvas.dispose();
                } catch(e) {
                    console.error("Error disposing old canvas:", e);
                }
                delete canvasStates[qid];
            }
        }

        const wrapper = document.getElementById(`canvas-wrapper-${qid}`);
        let canvasWidth = 800;
        let canvasHeight = 400;
        if (wrapper) {
            canvasWidth = wrapper.clientWidth || 800;
            canvasHeight = wrapper.clientHeight || 400;
        }

        console.log(`Canvas Initialization details for qid: ${qid}. Width: ${canvasWidth}, Height: ${canvasHeight}`);

        const canvasEl = document.getElementById(`canvas-${qid}`);
        if (canvasEl) {
            canvasEl.width = canvasWidth;
            canvasEl.height = canvasHeight;
        }

        const fabricCanvas = new fabric.Canvas(`canvas-${qid}`, {
            isDrawingMode: false,
            selection: true,
            width: canvasWidth,
            height: canvasHeight
        });

        // Initialize state machine
        const state = {
            canvas: fabricCanvas,
            activeTool: 'select', // select, pencil, rect, circle, triangle, diamond, line, arrow, text
            fillColor: '#ffffff',
            strokeColor: '#000000',
            strokeWidth: 2,
            historyUndo: [],
            historyRedo: [],
            isDrawingShape: false,
            tempShape: null,
            startX: 0,
            startY: 0
        };

        canvasStates[qid] = state;

        // Color & Brush updates
        const fillEl = document.getElementById(`fill-color-${qid}`);
        const strokeEl = document.getElementById(`stroke-color-${qid}`);
        const sizeEl = document.getElementById(`stroke-width-${qid}`);

        if (fillEl) fillEl.addEventListener('input', e => {
            state.fillColor = e.target.value;
            updateSelectedObjectProperties(state);
        });
        if (strokeEl) strokeEl.addEventListener('input', e => {
            state.strokeColor = e.target.value;
            state.canvas.freeDrawingBrush.color = e.target.value;
            updateSelectedObjectProperties(state);
        });
        if (sizeEl) sizeEl.addEventListener('input', e => {
            state.strokeWidth = parseInt(e.target.value);
            state.canvas.freeDrawingBrush.width = parseInt(e.target.value);
            updateSelectedObjectProperties(state);
        });

        // Initialize pencil brush parameters
        state.canvas.freeDrawingBrush.color = state.strokeColor;
        state.canvas.freeDrawingBrush.width = state.strokeWidth;

        state.triggerChange = triggerChange;

        // Text styling controls binding
        const boldBtn = document.getElementById(`btn-bold-${qid}`);
        const italicBtn = document.getElementById(`btn-italic-${qid}`);
        const underlineBtn = document.getElementById(`btn-underline-${qid}`);
        const fontSizeSel = document.getElementById(`font-size-${qid}`);
        const fontFamilySel = document.getElementById(`font-family-${qid}`);

        if (boldBtn) boldBtn.addEventListener('click', () => {
            const activeObj = state.canvas.getActiveObject();
            if (activeObj && activeObj.type === 'textbox') {
                const isBold = activeObj.fontWeight === 'bold';
                activeObj.set('fontWeight', isBold ? 'normal' : 'bold');
                state.canvas.renderAll();
                triggerChange();
            }
        });

        if (italicBtn) italicBtn.addEventListener('click', () => {
            const activeObj = state.canvas.getActiveObject();
            if (activeObj && activeObj.type === 'textbox') {
                const isItalic = activeObj.fontStyle === 'italic';
                activeObj.set('fontStyle', isItalic ? 'normal' : 'italic');
                state.canvas.renderAll();
                triggerChange();
            }
        });

        if (underlineBtn) underlineBtn.addEventListener('click', () => {
            const activeObj = state.canvas.getActiveObject();
            if (activeObj && activeObj.type === 'textbox') {
                const isUnderline = activeObj.underline;
                activeObj.set('underline', !isUnderline);
                state.canvas.renderAll();
                triggerChange();
            }
        });

        if (fontSizeSel) {
            fontSizeSel.addEventListener('focus', e => {
                const val = e.target.value.replace('pt', '').trim();
                e.target.value = val;
            });
            fontSizeSel.addEventListener('blur', e => {
                const val = parseInt(e.target.value.replace(/[^0-9]/g, ''));
                if (!isNaN(val) && val > 0) {
                    e.target.value = val + 'pt';
                }
            });

            const handleFontSizeChange = e => {
                const activeObj = state.canvas.getActiveObject();
                if (activeObj && activeObj.type === 'textbox') {
                    const parsedSize = parseInt(e.target.value.replace(/[^0-9]/g, ''));
                    if (!isNaN(parsedSize) && parsedSize > 0) {
                        activeObj.set('fontSize', parsedSize);
                        state.canvas.renderAll();
                        triggerChange();
                    }
                }
            };
            fontSizeSel.addEventListener('change', handleFontSizeChange);
            fontSizeSel.addEventListener('input', handleFontSizeChange);
        }

        if (fontFamilySel) fontFamilySel.addEventListener('change', e => {
            const activeObj = state.canvas.getActiveObject();
            if (activeObj && activeObj.type === 'textbox') {
                activeObj.set('fontFamily', e.target.value);
                state.canvas.renderAll();
                triggerChange();
            }
        });

        // Synchronize selectors when clicking or modifying textboxes
        function syncToolbarToSelectedObject(obj) {
            if (obj && obj.type === 'textbox') {
                if (fontSizeSel && obj.fontSize) {
                    fontSizeSel.value = obj.fontSize.toString() + 'pt';
                }
                if (fontFamilySel && obj.fontFamily) {
                    const family = obj.fontFamily;
                    for (let i = 0; i < fontFamilySel.options.length; i++) {
                        const optVal = fontFamilySel.options[i].value;
                        if (optVal.includes(family) || family.includes(optVal)) {
                            fontFamilySel.value = optVal;
                            break;
                        }
                    }
                }
                if (boldBtn) {
                    if (obj.fontWeight === 'bold') boldBtn.classList.add('active');
                    else boldBtn.classList.remove('active');
                }
                if (italicBtn) {
                    if (obj.fontStyle === 'italic') italicBtn.classList.add('active');
                    else italicBtn.classList.remove('active');
                }
                if (underlineBtn) {
                    if (obj.underline) underlineBtn.classList.add('active');
                    else underlineBtn.classList.remove('active');
                }
            }
        }

        state.canvas.on('selection:created', e => {
            syncToolbarToSelectedObject(e.target);
        });
        state.canvas.on('selection:updated', e => {
            syncToolbarToSelectedObject(e.target);
        });

        // Button events binding
        const tools = ['select', 'pencil', 'rect', 'capsule', 'circle', 'triangle', 'diamond', 'database', 'cloud', 'document', 'line', 'arrow', 'text'];
        tools.forEach(tool => {
            const btn = document.getElementById(`btn-${tool}-${qid}`);
            if (btn) {
                btn.addEventListener('click', () => {
                    // Reset active states
                    tools.forEach(t => {
                        const el = document.getElementById(`btn-${t}-${qid}`);
                        if (el) el.classList.remove('active');
                    });
                    btn.classList.add('active');

                    // Set mode
                    state.activeTool = tool;
                    if (tool === 'pencil') {
                        state.canvas.isDrawingMode = true;
                    } else {
                        state.canvas.isDrawingMode = false;
                        
                        // Disable selection inside vector draw modes so mouse dragging creates shapes
                        if (tool === 'select') {
                            state.canvas.selection = true;
                            state.canvas.forEachObject(obj => obj.selectable = obj.evented = true);
                        } else {
                            state.canvas.selection = false;
                            state.canvas.forEachObject(obj => obj.selectable = obj.evented = false);
                            state.canvas.discardActiveObject().renderAll();
                        }
                    }
                });
            }
        });

        // Extra operations binding
        bindWhiteboardOperations(qid, state);

        // Vector Shape Drawing Mouse Triggers
        setupShapeDrawingEvents(state);

        // Push state on modifications for undo history & auto-saving
        let saveTimeout = null;
        function triggerChange() {
            saveHistoryState(state);
            
            // Show auto-saving indicator
            const statusEl = document.getElementById(`wb-save-status-${qid}`);
            if (statusEl) {
                statusEl.className = 'badge bg-warning-subtle text-warning px-3 py-1.5 rounded-pill small';
                statusEl.innerHTML = '<i class="fa-solid fa-spinner fa-spin me-1"></i> Saving...';
            }

            // Debounced autosave
            clearTimeout(saveTimeout);
            saveTimeout = setTimeout(() => {
                saveCanvasToBackend(qid, state, attemptId, submissionId);
            }, 3000);
        }

        // Periodic 10-seconds secure auto-save interval
        setInterval(() => {
            if (saveTimeout !== null) {
                clearTimeout(saveTimeout);
                saveTimeout = null;
                saveCanvasToBackend(qid, state, attemptId, submissionId);
            }
        }, 10000);

        state.canvas.on({
            'object:added': e => {
                if (!e.target.fromHistory) triggerChange();
            },
            'object:modified': () => triggerChange(),
            'object:removed': e => {
                if (!e.target.fromHistory) triggerChange();
            },
            'text:changed': () => triggerChange()
        });

        // Initial fetch from database
        loadCanvasFromBackend(qid, state, attemptId, submissionId);
    }

    /**
     * Binds layer commands, duplicates, grid toggles, histories, and canvas clears.
     */
    function bindWhiteboardOperations(qid, state) {
        // Eraser / Delete Shape
        const eraserBtn = document.getElementById(`btn-eraser-${qid}`);
        if (eraserBtn) eraserBtn.addEventListener('click', () => {
            const activeObj = state.canvas.getActiveObject();
            if (activeObj) {
                state.canvas.remove(activeObj);
                state.canvas.discardActiveObject().renderAll();
            }
        });

        // Duplicate Active Object
        const dupBtn = document.getElementById(`btn-duplicate-${qid}`);
        if (dupBtn) dupBtn.addEventListener('click', () => {
            const activeObj = state.canvas.getActiveObject();
            if (activeObj) {
                activeObj.clone(cloned => {
                    state.canvas.discardActiveObject();
                    cloned.set({
                        left: cloned.left + 15,
                        top: cloned.top + 15,
                        evented: true
                    });
                    if (cloned.type === 'activeSelection') {
                        cloned.canvas = state.canvas;
                        cloned.forEachObject(obj => state.canvas.add(obj));
                        cloned.setCoords();
                    } else {
                        state.canvas.add(cloned);
                    }
                    state.canvas.setActiveObject(cloned);
                    state.canvas.requestRenderAll();
                });
            }
        });

        // Layer Forward / Backward
        const fwdBtn = document.getElementById(`btn-forward-${qid}`);
        if (fwdBtn) fwdBtn.addEventListener('click', () => {
            const activeObj = state.canvas.getActiveObject();
            if (activeObj) {
                state.canvas.bringForward(activeObj);
            }
        });

        const bwdBtn = document.getElementById(`btn-backward-${qid}`);
        if (bwdBtn) bwdBtn.addEventListener('click', () => {
            const activeObj = state.canvas.getActiveObject();
            if (activeObj) {
                state.canvas.sendBackwards(activeObj);
            }
        });

        // Toggle Grid Background
        const gridBtn = document.getElementById(`btn-grid-${qid}`);
        if (gridBtn) gridBtn.addEventListener('click', () => {
            const wrapper = document.getElementById(`canvas-wrapper-${qid}`);
            if (wrapper) {
                wrapper.classList.toggle('grid-bg');
            }
        });

        // History Undo / Redo
        const undoBtn = document.getElementById(`btn-undo-${qid}`);
        if (undoBtn) undoBtn.addEventListener('click', () => {
            if (state.historyUndo.length > 0) {
                state.historyRedo.push(JSON.stringify(state.canvas.toJSON(['id', 'textLinkId', 'shapeLinkId'])));
                const previousState = state.historyUndo.pop();
                state.canvas.loadFromJSON(previousState, () => {
                    state.canvas.renderAll();
                    const objects = state.canvas.getObjects();
                    const textsMap = {};
                    objects.forEach(obj => {
                        obj.selectable = obj.evented = true;
                        obj.fromHistory = true;
                        if (obj.id && obj.shapeLinkId) {
                            textsMap[obj.id] = obj;
                        }
                    });
                    objects.forEach(obj => {
                        if (obj.textLinkId && textsMap[obj.textLinkId]) {
                            bindShapeAndTextEvents(obj, textsMap[obj.textLinkId], state.canvas);
                        }
                    });
                });
            }
        });

        const redoBtn = document.getElementById(`btn-redo-${qid}`);
        if (redoBtn) redoBtn.addEventListener('click', () => {
            if (state.historyRedo.length > 0) {
                state.historyUndo.push(JSON.stringify(state.canvas.toJSON(['id', 'textLinkId', 'shapeLinkId'])));
                const nextState = state.historyRedo.pop();
                state.canvas.loadFromJSON(nextState, () => {
                    state.canvas.renderAll();
                    const objects = state.canvas.getObjects();
                    const textsMap = {};
                    objects.forEach(obj => {
                        obj.selectable = obj.evented = true;
                        obj.fromHistory = true;
                        if (obj.id && obj.shapeLinkId) {
                            textsMap[obj.id] = obj;
                        }
                    });
                    objects.forEach(obj => {
                        if (obj.textLinkId && textsMap[obj.textLinkId]) {
                            bindShapeAndTextEvents(obj, textsMap[obj.textLinkId], state.canvas);
                        }
                    });
                });
            }
        });

        // Clear Canvas
        const clearBtn = document.getElementById(`btn-clear-${qid}`);
        if (clearBtn) clearBtn.addEventListener('click', () => {
            if (confirm("Are you sure you want to clear the entire whiteboard?")) {
                state.canvas.clear();
            }
        });

        // Bind standard Delete / Backspace keys to remove active object
        window.addEventListener('keydown', e => {
            if (e.key === 'Delete' || e.key === 'Backspace') {
                // Confirm user is not focused on an active text area or input field
                if (document.activeElement.tagName !== 'INPUT' && 
                    document.activeElement.tagName !== 'TEXTAREA' && 
                    !document.activeElement.classList.contains('tox-edit-area__iframe') &&
                    !state.canvas.getActiveObject()?.isEditing) {
                    
                    const activeObj = state.canvas.getActiveObject();
                    if (activeObj) {
                        state.canvas.remove(activeObj);
                        state.canvas.discardActiveObject().renderAll();
                    }
                }
            }
        });
    }

    /**
     * Creates a high-fidelity linear gradient for Fabric.js shapes matching the premium document editor color stops.
     */
    function getFabricGradient(fillColor, width, height) {
        if (!fillColor || fillColor === 'transparent' || fillColor === 'none') {
            return 'transparent';
        }
        // Core Gradient Palette mapping
        const gradientMap = {
            '#ebf8ff': { start: '#e0f2fe', end: '#bae6fd' }, // Sky Blue
            '#e6fffa': { start: '#f0fdfa', end: '#ccfbf1' }, // Teal
            '#f0fff4': { start: '#f0fdf4', end: '#dcfce7' }, // Emerald/Green
            '#fffaf0': { start: '#fff7ed', end: '#ffedd5' }, // Amber/Orange
            '#fff5f5': { start: '#fef2f2', end: '#fee2e2' }, // Rose/Red
            '#faf5ff': { start: '#faf5ff', end: '#f3e8ff' }, // Lavender/Purple
            '#ffffff': { start: '#ffffff', end: '#f1f5f9' }  // Cool White
        };

        const startColor = gradientMap[fillColor] ? gradientMap[fillColor].start : fillColor;
        const endColor = gradientMap[fillColor] ? gradientMap[fillColor].end : fillColor;

        return new fabric.Gradient({
            type: 'linear',
            coords: {
                x1: 0,
                y1: 0,
                x2: width || 100,
                y2: height || 100
            },
            colorStops: [
                { offset: 0, color: startColor },
                { offset: 1, color: endColor }
            ]
        });
    }

    /**
     * Changes border colors, brush stroke widths, or backgrounds of currently selected objects.
     */
    function updateSelectedObjectProperties(state) {
        const activeObj = state.canvas.getActiveObject();
        if (activeObj) {
            const updateProps = (obj) => {
                if (obj.stroke) obj.set('stroke', state.strokeColor);
                if (obj.strokeWidth) obj.set('strokeWidth', state.strokeWidth);
                if (obj.fill && obj.fill !== 'transparent' && obj.type !== 'textbox') {
                    const finalW = obj.width * (obj.scaleX || 1);
                    const finalH = obj.height * (obj.scaleY || 1);
                    const gradient = getFabricGradient(state.fillColor, finalW, finalH);
                    obj.set('fill', gradient);
                }
            };

            if (activeObj.type === 'activeSelection') {
                activeObj.forEachObject(updateProps);
            } else {
                updateProps(activeObj);
            }
            state.canvas.renderAll();
        }
    }

    /**
     * Captures undo checkpoints.
     */
    function saveHistoryState(state) {
        state.historyUndo.push(JSON.stringify(state.canvas.toJSON(['id', 'textLinkId', 'shapeLinkId'])));
        state.historyRedo = []; // Clear redo stack on manual actions
    }

    /**
     * Coordinate mouse events (Mouse Down, Mouse Move, Mouse Up) to dynamically construct shapes.
     */
    function setupShapeDrawingEvents(state) {
        state.canvas.on('mouse:down', o => {
            if (state.activeTool === 'select' || state.activeTool === 'pencil') return;

            state.isDrawingShape = true;
            const pointer = state.canvas.getPointer(o.e);
            state.startX = pointer.x;
            state.startY = pointer.y;

            // Soft drop shadow matching premium design guidelines
            const shadow = new fabric.Shadow({
                color: 'rgba(51, 65, 85, 0.12)',
                blur: 8,
                offsetX: 2,
                offsetY: 4
            });

            const baseOptions = {
                left: state.startX,
                top: state.startY,
                width: 0,
                height: 0,
                fill: state.fillColor,
                stroke: state.strokeColor,
                strokeWidth: state.strokeWidth,
                selectable: false,
                evented: false
            };

            if (state.activeTool === 'rect') {
                state.tempShape = new fabric.Rect({
                    ...baseOptions,
                    rx: 8,
                    ry: 8,
                    shadow: shadow
                });
            } else if (state.activeTool === 'capsule') {
                state.tempShape = new fabric.Rect({
                    ...baseOptions,
                    rx: 0,
                    ry: 0,
                    shadow: shadow
                });
            } else if (state.activeTool === 'circle') {
                state.tempShape = new fabric.Ellipse({
                    ...baseOptions,
                    rx: 0,
                    ry: 0,
                    shadow: shadow
                });
            } else if (state.activeTool === 'triangle') {
                state.tempShape = new fabric.Triangle({
                    ...baseOptions,
                    shadow: shadow
                });
            } else if (state.activeTool === 'diamond') {
                state.tempShape = new fabric.Polygon([
                    { x: 0, y: 0 },
                    { x: 0, y: 0 },
                    { x: 0, y: 0 },
                    { x: 0, y: 0 }
                ], {
                    ...baseOptions,
                    left: state.startX,
                    top: state.startY,
                    shadow: shadow
                });
            } else if (state.activeTool === 'database') {
                const pathStr = 'M 0 15 L 0 85 C 0 95, 100 95, 100 85 L 100 15 Z M 0 15 C 0 25, 100 25, 100 15 C 100 5, 0 5, 0 15 M 0 45 C 0 55, 100 55, 100 45 M 0 70 C 0 80, 100 80, 100 70';
                state.tempShape = new fabric.Path(pathStr, {
                    ...baseOptions,
                    width: 100,
                    height: 100,
                    shadow: shadow
                });
            } else if (state.activeTool === 'document') {
                const pathStr = 'M 0 0 L 75 0 L 100 25 L 100 100 L 0 100 Z M 75 0 L 75 25 L 100 25 Z';
                state.tempShape = new fabric.Path(pathStr, {
                    ...baseOptions,
                    width: 100,
                    height: 100,
                    shadow: shadow
                });
            } else if (state.activeTool === 'cloud') {
                const pathStr = 'M 25 80 C 10 80, 0 70, 0 55 C 0 40, 15 30, 30 30 C 35 15, 50 10, 65 10 C 85 10, 100 25, 100 45 C 100 65, 85 80, 65 80 Z';
                state.tempShape = new fabric.Path(pathStr, {
                    ...baseOptions,
                    width: 100,
                    height: 100,
                    shadow: shadow
                });
            } else if (state.activeTool === 'line') {
                state.tempShape = new fabric.Line([state.startX, state.startY, state.startX, state.startY], {
                    stroke: state.strokeColor,
                    strokeWidth: state.strokeWidth,
                    selectable: false,
                    evented: false
                });
            } else if (state.activeTool === 'arrow') {
                // Arrow is composed of a line and a triangle head
                state.tempShape = new fabric.Group([
                    new fabric.Line([0, 0, 0, 0], {
                        stroke: state.strokeColor,
                        strokeWidth: state.strokeWidth,
                        originX: 'center',
                        originY: 'center'
                    })
                ], {
                    left: state.startX,
                    top: state.startY,
                    selectable: false,
                    evented: false
                });
            } else if (state.activeTool === 'text') {
                state.isDrawingShape = false;
                const activeFontFamily = document.getElementById(`font-family-${qid}`)?.value || '"Aptos (Body)", sans-serif';
                const activeFontSize = parseInt(document.getElementById(`font-size-${qid}`)?.value || '12');
                const textObj = new fabric.Textbox('Double-click to type...', {
                    left: state.startX,
                    top: state.startY,
                    width: 150,
                    fontSize: activeFontSize,
                    fill: state.strokeColor,
                    fontFamily: activeFontFamily,
                    hasControls: true,
                    selectable: true,
                    evented: true
                });
                state.canvas.add(textObj);
                state.canvas.setActiveObject(textObj);
                textObj.enterEditing();
                
                // Return to select pointer
                document.getElementById(`btn-select-${getCanvasId(state)}`)?.click();
                return;
            }

            if (state.tempShape) {
                state.canvas.add(state.tempShape);
            }
        });

        state.canvas.on('mouse:move', o => {
            if (!state.isDrawingShape || !state.tempShape) return;

            const pointer = state.canvas.getPointer(o.e);
            const currX = pointer.x;
            const currY = pointer.y;

            const width = Math.abs(currX - state.startX);
            const height = Math.abs(currY - state.startY);
            const left = Math.min(state.startX, currX);
            const top = Math.min(state.startY, currY);

            if (state.activeTool === 'rect' || state.activeTool === 'triangle') {
                state.tempShape.set({
                    left: left,
                    top: top,
                    width: width,
                    height: height
                });
            } else if (state.activeTool === 'capsule') {
                state.tempShape.set({
                    left: left,
                    top: top,
                    width: width,
                    height: height,
                    rx: height / 2,
                    ry: height / 2
                });
            } else if (state.activeTool === 'circle') {
                state.tempShape.set({
                    left: left,
                    top: top,
                    rx: width / 2,
                    ry: height / 2
                });
            } else if (state.activeTool === 'diamond') {
                const centerLeft = width / 2;
                const centerTop = height / 2;
                state.tempShape.set({
                    left: left,
                    top: top,
                    points: [
                        { x: centerLeft, y: 0 },
                        { x: width, y: centerTop },
                        { x: centerLeft, y: height },
                        { x: 0, y: centerTop }
                    ]
                });
                state.tempShape.width = width;
                state.tempShape.height = height;
            } else if (state.activeTool === 'database' || state.activeTool === 'document' || state.activeTool === 'cloud') {
                state.tempShape.set({
                    left: left,
                    top: top,
                    scaleX: width / 100,
                    scaleY: height / 100
                });
            } else if (state.activeTool === 'line') {
                state.tempShape.set({
                    x2: currX,
                    y2: currY
                });
            } else if (state.activeTool === 'arrow') {
                state.canvas.remove(state.tempShape);

                // Reconstruct Arrow in real time
                const dx = currX - state.startX;
                const dy = currY - state.startY;
                const angle = Math.atan2(dy, dx) * 180 / Math.PI;
                const length = Math.sqrt(dx * dx + dy * dy);

                const lineObj = new fabric.Line([0, 0, length - 12, 0], {
                    stroke: state.strokeColor,
                    strokeWidth: state.strokeWidth,
                    originX: 'left',
                    originY: 'center',
                    top: 0,
                    left: 0
                });

                const headObj = new fabric.Triangle({
                    width: 12 + state.strokeWidth * 2,
                    height: 12 + state.strokeWidth * 2,
                    fill: state.strokeColor,
                    originX: 'center',
                    originY: 'center',
                    left: length - 6,
                    top: 0,
                    angle: 90
                });

                state.tempShape = new fabric.Group([lineObj, headObj], {
                    left: state.startX,
                    top: state.startY,
                    angle: angle,
                    originX: 'left',
                    originY: 'center',
                    selectable: false,
                    evented: false
                });

                state.canvas.add(state.tempShape);
            }

            state.canvas.renderAll();
        });

        state.canvas.on('mouse:up', () => {
            if (!state.isDrawingShape) return;

            state.isDrawingShape = false;

            if (state.tempShape) {
                const isLine = state.activeTool === 'line' || state.activeTool === 'arrow';
                const finalW = state.tempShape.width * (state.tempShape.scaleX || 1);
                const finalH = state.tempShape.height * (state.tempShape.scaleY || 1);

                if (!isLine && finalW < 5 && finalH < 5) {
                    state.canvas.remove(state.tempShape);
                } else {
                    // Apply premium linear gradient fill once sizing is finalized
                    if (!isLine) {
                        const gradient = getFabricGradient(state.fillColor, finalW, finalH);
                        state.tempShape.set('fill', gradient);
                    }

                    // Upgrade shape to selectable pointer objects
                    state.tempShape.set({
                        selectable: true,
                        evented: true
                    });
                    
                    // Return back to select pointer tool
                    const qid = getCanvasId(state);
                    document.getElementById(`btn-select-${qid}`)?.click();
                }
            }

            state.tempShape = null;
            state.canvas.renderAll();
        });

        // Supporting writing inside shapes: Double click a shape to spawn a Textbox overlaying it!
        state.canvas.on('mouse:dblclick', o => {
            console.log("Shape Double Clicked");
            const target = o.target;
            if (target && target.type !== 'textbox') {
                if (target.textObject) {
                    console.log("Opening existing linked textbox for editing");
                    state.canvas.setActiveObject(target.textObject);
                    target.textObject.enterEditing();
                    target.textObject.selectAll();
                    state.canvas.renderAll();
                    return;
                }
                
                console.log("Creating new linked textbox for shape");
                const center = target.getCenterPoint();
                const targetW = target.width * (target.scaleX || 1);
                const activeFontFamily = document.getElementById(`font-family-${qid}`)?.value || '"Aptos (Body)", sans-serif';
                const activeFontSize = parseInt(document.getElementById(`font-size-${qid}`)?.value || '12');
                
                const shapeId = 'shape-' + Math.random().toString(36).substr(2, 9);
                const textId = 'text-' + Math.random().toString(36).substr(2, 9);
                
                target.id = shapeId;
                target.textLinkId = textId;
                
                const textObj = new fabric.Textbox('Type text...', {
                    id: textId,
                    shapeLinkId: shapeId,
                    left: center.x,
                    top: center.y,
                    width: Math.max(80, Math.min(150, targetW - 20)),
                    fontSize: activeFontSize,
                    fill: state.strokeColor || '#000000',
                    originX: 'center',
                    originY: 'center',
                    fontFamily: activeFontFamily,
                    hasControls: true,
                    selectable: true,
                    evented: true,
                    textAlign: 'center'
                });
                
                state.canvas.add(textObj);
                bindShapeAndTextEvents(target, textObj, state.canvas);
                
                state.canvas.setActiveObject(textObj);
                textObj.enterEditing();
                textObj.selectAll();
                state.canvas.renderAll();
                
                // Switch tool mode to select to prevent drawing mode conflict
                const selectBtn = document.getElementById(`btn-select-${qid}`);
                if (selectBtn) {
                    const tools = ['select', 'pencil', 'rect', 'capsule', 'circle', 'triangle', 'diamond', 'database', 'cloud', 'document', 'line', 'arrow', 'text'];
                    tools.forEach(t => {
                        const btn = document.getElementById(`btn-${t}-${qid}`);
                        if (btn) btn.classList.remove('active');
                    });
                    selectBtn.classList.add('active');
                }
                state.activeTool = 'select';
            }
        });
    }

    /**
     * Extracts the canvas ID from the active state container.
     */
    function getCanvasId(state) {
        for (let key in canvasStates) {
            if (canvasStates[key] === state) return key;
        }
        return 'paper-full';
    }

    /**
     * REST auto-save to Java Spring Boot database.
     */
    function saveCanvasToBackendPromise(qid, state, attemptId, submissionId) {
        const json = JSON.stringify(state.canvas.toJSON(['id', 'textLinkId', 'shapeLinkId']));
        const image = state.canvas.toDataURL({
            format: 'png',
            quality: 1.0
        });

        const payload = {
            questionId: isNaN(qid) ? 999999 : parseInt(qid),
            canvasJson: json,
            canvasImage: image
        };

        if (attemptId && attemptId !== 'null') payload.attemptId = parseInt(attemptId);
        if (submissionId && submissionId !== 'null') payload.submissionId = parseInt(submissionId);

        console.log("saveCanvasToBackendPromise executing for qid:", qid);

        return fetch('/api/drawing/save', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        })
        .then(response => response.json())
        .then(data => {
            console.log("Save Canvas Response for qid:", qid, data);
            if (data.status === 'saved') {
                const statusEl = document.getElementById(`wb-save-status-${qid}`);
                if (statusEl) {
                    statusEl.className = 'badge bg-success-subtle text-success px-3 py-1.5 rounded-pill small';
                    statusEl.innerHTML = '<i class="fa-solid fa-cloud-arrow-up me-1"></i> Saved';
                }
            }
        })
        .catch(err => {
            console.error("Auto-save Whiteboard drawing failed for qid:", qid, err);
            const statusEl = document.getElementById(`wb-save-status-${qid}`);
            if (statusEl) {
                statusEl.className = 'badge bg-danger-subtle text-danger px-3 py-1.5 rounded-pill small';
                statusEl.innerHTML = '<i class="fa-solid fa-circle-exclamation me-1"></i> Offline';
            }
        });
    }

    function saveCanvasToBackend(qid, state, attemptId, submissionId) {
        saveCanvasToBackendPromise(qid, state, attemptId, submissionId);
    }

    function saveAllCanvasesImmediately() {
        console.log("saveAllCanvasesImmediately called");
        const promises = [];
        for (let qid in canvasStates) {
            const state = canvasStates[qid];
            if (state && state.canvas) {
                const subIdEl = document.getElementById('submissionId');
                const attIdEl = document.getElementById('attemptId');
                const typeEl = document.getElementById('type');
                
                let submissionId = subIdEl ? subIdEl.value : null;
                let attemptId = attIdEl ? attIdEl.value : null;
                const type = typeEl ? typeEl.value : null;

                if (type === 'paper' && attemptId) {
                    submissionId = attemptId;
                    attemptId = null;
                }
                promises.push(saveCanvasToBackendPromise(qid, state, attemptId, submissionId));
            }
        }
        return Promise.all(promises);
    }

    window.saveAllCanvasesImmediately = saveAllCanvasesImmediately;

    /**
     * Restores canvas elements on page reload.
     */
    function loadCanvasFromBackend(qid, state, attemptId, submissionId) {
        let url = `/api/drawing/get?questionId=${isNaN(qid) ? 999999 : qid}`;
        if (attemptId && attemptId !== 'null') url += `&attemptId=${attemptId}`;
        if (submissionId && submissionId !== 'null') url += `&submissionId=${submissionId}`;

        fetch(url)
        .then(response => response.json())
        .then(data => {
            if (data.canvasJson) {
                console.log("Restoring Canvas state from backend for qid:", qid);
                state.canvas.loadFromJSON(data.canvasJson, () => {
                    state.canvas.renderAll();
                    const objects = state.canvas.getObjects();
                    const textsMap = {};
                    objects.forEach(obj => {
                        obj.selectable = obj.evented = true;
                        obj.fromHistory = true;
                        if (obj.id && obj.shapeLinkId) {
                            textsMap[obj.id] = obj;
                        }
                    });
                    objects.forEach(obj => {
                        if (obj.textLinkId && textsMap[obj.textLinkId]) {
                            bindShapeAndTextEvents(obj, textsMap[obj.textLinkId], state.canvas);
                        }
                    });
                });
            }
        })
        .catch(err => console.error("Loading Whiteboard drawing failed:", err));
    }

    window.insertShapeIntoCanvas = function(qid, shapeType) {
        console.log("Shape Selected");
        console.log("window.insertShapeIntoCanvas called with qid:", qid, "shapeType:", shapeType);
        
        let state = canvasStates[qid];
        if (!state) {
            // Try to find state by string or numeric key lookup fallback
            for (let key in canvasStates) {
                if (String(key) === String(qid)) {
                    state = canvasStates[key];
                    console.log("Fallback match found canvasState for key:", key);
                    break;
                }
            }
        }

        if (!state || !state.canvas) {
            console.warn("Canvas state not found for qid:", qid, "Available states:", Object.keys(canvasStates));
            return;
        }

        const canvas = state.canvas;
        let shape;

        // Dynamically calculate horizontal and vertical center of canvas
        const canvasWidth = canvas.getWidth();
        const canvasHeight = canvas.getHeight();
        const centerX = canvasWidth / 2;
        const centerY = canvasHeight / 2;

        console.log(`Centering shape on canvas of size ${canvasWidth}x${canvasHeight} at position (${centerX}, ${centerY})`);

        // Default styling properties matching requirements:
        // Fill Color = White, Border Color = Black, Border Width = 2px, Visible = true, Selectable = true, Evented = true
        const defaultProps = {
            left: centerX,
            top: centerY,
            fill: '#ffffff',
            stroke: '#000000',
            strokeWidth: 2,
            visible: true,
            selectable: true,
            evented: true,
            hasControls: true,
            hasBorders: true,
            originX: 'center',
            originY: 'center'
        };

        switch (shapeType) {
            case 'rect':
                shape = new fabric.Rect({
                    ...defaultProps,
                    width: 120,
                    height: 80
                });
                break;
            case 'square':
                shape = new fabric.Rect({
                    ...defaultProps,
                    width: 80,
                    height: 80
                });
                break;
            case 'circle':
                shape = new fabric.Circle({
                    ...defaultProps,
                    radius: 40
                });
                break;
            case 'oval':
                shape = new fabric.Ellipse({
                    ...defaultProps,
                    rx: 60,
                    ry: 40
                });
                break;
            case 'triangle':
                shape = new fabric.Triangle({
                    ...defaultProps,
                    width: 100,
                    height: 90
                });
                break;
            case 'diamond':
                shape = new fabric.Polygon([
                    { x: 50, y: 0 },
                    { x: 100, y: 50 },
                    { x: 50, y: 100 },
                    { x: 0, y: 50 }
                ], defaultProps);
                break;
            case 'pentagon':
                shape = new fabric.Polygon([
                    {x: 50, y: 0},
                    {x: 100, y: 38},
                    {x: 81, y: 100},
                    {x: 19, y: 100},
                    {x: 0, y: 38}
                ], defaultProps);
                break;
            case 'hexagon':
                shape = new fabric.Polygon([
                    {x: 50, y: 0},
                    {x: 100, y: 25},
                    {x: 100, y: 75},
                    {x: 50, y: 100},
                    {x: 0, y: 75},
                    {x: 0, y: 25}
                ], defaultProps);
                break;
            case 'line':
                shape = new fabric.Line([10, 10, 110, 10], {
                    left: centerX,
                    top: centerY,
                    stroke: '#000000',
                    strokeWidth: 2,
                    visible: true,
                    selectable: true,
                    evented: true,
                    hasControls: true,
                    hasBorders: true,
                    originX: 'center',
                    originY: 'center'
                });
                break;
            case 'arrow':
                shape = new fabric.Path('M 0 5 L 80 5 M 80 5 L 70 0 M 80 5 L 70 10', {
                    left: centerX,
                    top: centerY,
                    fill: 'transparent',
                    stroke: '#000000',
                    strokeWidth: 2,
                    visible: true,
                    selectable: true,
                    evented: true,
                    hasControls: true,
                    hasBorders: true,
                    originX: 'center',
                    originY: 'center'
                });
                break;
            case 'doublearrow':
                shape = new fabric.Path('M 10 5 L 70 5 M 10 5 L 20 0 M 10 5 L 20 10 M 70 5 L 60 0 M 70 5 L 60 10', {
                    left: centerX,
                    top: centerY,
                    fill: 'transparent',
                    stroke: '#000000',
                    strokeWidth: 2,
                    visible: true,
                    selectable: true,
                    evented: true,
                    hasControls: true,
                    hasBorders: true,
                    originX: 'center',
                    originY: 'center'
                });
                break;
            case 'flowprocess':
                shape = new fabric.Rect({
                    ...defaultProps,
                    width: 120,
                    height: 80
                });
                break;
            case 'flowdecision':
                shape = new fabric.Polygon([
                    { x: 60, y: 0 },
                    { x: 120, y: 60 },
                    { x: 60, y: 120 },
                    { x: 0, y: 60 }
                ], defaultProps);
                break;
            case 'flowstartend':
                shape = new fabric.Rect({
                    ...defaultProps,
                    width: 120,
                    height: 60,
                    rx: 30,
                    ry: 30
                });
                break;
            case 'textbox':
                const activeFontFamily = document.getElementById(`font-family-${qid}`)?.value || '"Aptos (Body)", sans-serif';
                const activeFontSize = parseInt(document.getElementById(`font-size-${qid}`)?.value || '12');
                shape = new fabric.Textbox('Type here...', {
                    left: centerX,
                    top: centerY,
                    width: 150,
                    fontSize: activeFontSize,
                    fill: '#000000',
                    fontFamily: activeFontFamily,
                    visible: true,
                    selectable: true,
                    evented: true,
                    hasControls: true,
                    hasBorders: true,
                    originX: 'center',
                    originY: 'center'
                });
                break;
        }

        if (shape) {
            canvas.add(shape);
            shape.bringToFront();
            canvas.setActiveObject(shape);
            canvas.renderAll();
            
            console.log("Shape Added");
            console.log("Canvas Object Count:", canvas.getObjects().length);

            // Auto-switch back to select tool mode to make immediate dragging/resizing work
            state.activeTool = 'select';
            canvas.isDrawingMode = false;
            canvas.selection = true;
            canvas.forEachObject(obj => {
                obj.selectable = true;
                obj.evented = true;
            });

            // Update UI toolbar state
            const selectBtn = document.getElementById(`btn-select-${qid}`);
            if (selectBtn) {
                const tools = ['select', 'pencil', 'rect', 'capsule', 'circle', 'triangle', 'diamond', 'database', 'cloud', 'document', 'line', 'arrow', 'text'];
                tools.forEach(t => {
                    const btn = document.getElementById(`btn-${t}-${qid}`);
                    if (btn) btn.classList.remove('active');
                });
                selectBtn.classList.add('active');
            }

            // Trigger auto-save
            if (state.triggerChange) {
                state.triggerChange();
            } else {
                canvas.fire('object:added', { target: shape });
            }
        }
    };

    // Specific shape helper functions exposed globally to resolve Shape Toolbar Fix requirements
    function getActiveQid(qid) {
        if (qid) return qid;
        if (window.tinymce && window.tinymce.activeEditor) {
            return window.tinymce.activeEditor.id.replace('textarea-', '');
        }
        const keys = Object.keys(canvasStates);
        if (keys.length > 0) return keys[0];
        return 'paper-full';
    }

    window.addRectangle = function(qid) {
        window.insertShapeIntoCanvas(getActiveQid(qid), 'rect');
    };
    window.addCircle = function(qid) {
        window.insertShapeIntoCanvas(getActiveQid(qid), 'circle');
    };
    window.addTriangle = function(qid) {
        window.insertShapeIntoCanvas(getActiveQid(qid), 'triangle');
    };
    window.addDiamond = function(qid) {
        window.insertShapeIntoCanvas(getActiveQid(qid), 'diamond');
    };
    window.addPentagon = function(qid) {
        window.insertShapeIntoCanvas(getActiveQid(qid), 'pentagon');
    };
    window.addHexagon = function(qid) {
        window.insertShapeIntoCanvas(getActiveQid(qid), 'hexagon');
    };
    window.addArrow = function(qid) {
        window.insertShapeIntoCanvas(getActiveQid(qid), 'arrow');
    };
    window.addLine = function(qid) {
        window.insertShapeIntoCanvas(getActiveQid(qid), 'line');
    };
    window.addTextbox = function(qid) {
        window.insertShapeIntoCanvas(getActiveQid(qid), 'textbox');
    };

    // Expose drawing engine globally
    window.initializeDrawingTool = initializeDrawingTool;

    // Auto-hook triggers on window load
    function init() {
        // Fetch submission/attempt IDs dynamically from the DOM hidden fields
        const subIdEl = document.getElementById('submissionId');
        const attIdEl = document.getElementById('attemptId');
        const typeEl = document.getElementById('type');
        
        let submissionId = subIdEl ? subIdEl.value : null;
        let attemptId = attIdEl ? attIdEl.value : null;
        const type = typeEl ? typeEl.value : null;

        if (type === 'paper' && attemptId) {
            submissionId = attemptId;
            attemptId = null;
        }

        // Auto-run delay to ensure textareas have been loaded
        setTimeout(() => {
            initializeDrawingTool(attemptId, submissionId);
        }, 1200);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    window.addEventListener('unload', function() {
        const remainingSecondsEl = document.getElementById('remainingSeconds');
        const remainingSeconds = remainingSecondsEl ? parseInt(remainingSecondsEl.value) : 0;
        if (remainingSeconds > 0 && !window.isNavigating) {
            for (let qid in canvasStates) {
                const state = canvasStates[qid];
                if (state && state.canvas) {
                    const json = JSON.stringify(state.canvas.toJSON(['id', 'textLinkId', 'shapeLinkId']));
                    const image = state.canvas.toDataURL({
                        format: 'png',
                        quality: 1.0
                    });
                    const payload = {
                        questionId: isNaN(qid) ? 999999 : parseInt(qid),
                        canvasJson: json,
                        canvasImage: image
                    };
                    const subIdEl = document.getElementById('submissionId');
                    const attIdEl = document.getElementById('attemptId');
                    const typeEl = document.getElementById('type');
                    let submissionId = subIdEl ? subIdEl.value : null;
                    let attemptId = attIdEl ? attIdEl.value : null;
                    const type = typeEl ? typeEl.value : null;
                    if (type === 'paper' && attemptId) {
                        submissionId = attemptId;
                        attemptId = null;
                    }
                    if (attemptId && attemptId !== 'null') payload.attemptId = parseInt(attemptId);
                    if (submissionId && submissionId !== 'null') payload.submissionId = parseInt(submissionId);
                    const blob = new Blob([JSON.stringify(payload)], { type: 'application/json' });
                    navigator.sendBeacon('/api/drawing/save', blob);
                }
            }
        }
    });
})();
