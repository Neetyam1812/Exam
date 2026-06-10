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
                
                <!-- Group 2: Shapes Dropdown -->
                <div class="toolbar-group">
                    <div class="dropdown">
                        <button type="button" class="drawing-btn dropdown-toggle hide-toggle-arrow" id="btn-shapes-${qid}" data-bs-toggle="dropdown" aria-expanded="false" data-tooltip="Insert Shapes">
                            <i class="fa-solid fa-shapes"></i>
                        </button>
                        <div class="dropdown-menu shapes-dropdown-menu shadow-lg" aria-labelledby="btn-shapes-${qid}">
                            <div class="shapes-dropdown-scrollable">
                                <!-- Category: Recently Used Shapes -->
                                <div class="shapes-dropdown-section">
                                    <div class="shapes-dropdown-title">Recently Used Shapes</div>
                                    <div class="shapes-dropdown-grid">
                                        <button type="button" class="shape-dropdown-item" data-shape="textbox" data-tooltip="Text Box" onclick="window.insertShapeIntoCanvas('${qid}', 'textbox')">
                                            <svg viewBox="0 0 16 16"><rect x="1" y="1" width="14" height="14" rx="1" stroke-dasharray="2,2"/><text x="5" y="11" font-size="9" font-family="sans-serif" fill="currentColor">A</text></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="line" data-tooltip="Line" onclick="window.insertShapeIntoCanvas('${qid}', 'line')">
                                            <svg viewBox="0 0 16 16"><line x1="2" y1="2" x2="14" y2="14"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="arrow" data-tooltip="Arrow" onclick="window.insertShapeIntoCanvas('${qid}', 'arrow')">
                                            <svg viewBox="0 0 16 16"><line x1="2" y1="2" x2="14" y2="14"/><polygon points="14,10 14,14 10,14" fill="currentColor"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="rect" data-tooltip="Rectangle" onclick="window.insertShapeIntoCanvas('${qid}', 'rect')">
                                            <svg viewBox="0 0 16 16"><rect x="2" y="3" width="12" height="10"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="circle" data-tooltip="Oval" onclick="window.insertShapeIntoCanvas('${qid}', 'circle')">
                                            <svg viewBox="0 0 16 16"><circle cx="8" cy="8" r="6"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="rounded-rect" data-tooltip="Rounded Rectangle" onclick="window.insertShapeIntoCanvas('${qid}', 'rounded-rect')">
                                            <svg viewBox="0 0 16 16"><rect x="2" y="3" width="12" height="10" rx="2" ry="2"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="triangle" data-tooltip="Isosceles Triangle" onclick="window.insertShapeIntoCanvas('${qid}', 'triangle')">
                                            <svg viewBox="0 0 16 16"><polygon points="8,2 2,14 14,14"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="elbow-connector" data-tooltip="Elbow Connector" onclick="window.insertShapeIntoCanvas('${qid}', 'elbow-connector')">
                                            <svg viewBox="0 0 16 16"><path d="M2,2 L8,2 L8,14 L14,14"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="elbow-arrow" data-tooltip="Elbow Arrow Connector" onclick="window.insertShapeIntoCanvas('${qid}', 'elbow-arrow')">
                                            <svg viewBox="0 0 16 16"><path d="M2,2 L8,2 L8,14 L14,14"/><polygon points="12,12 14,14 12,16" fill="currentColor"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="arrow-right" data-tooltip="Right Arrow" onclick="window.insertShapeIntoCanvas('${qid}', 'arrow-right')">
                                            <svg viewBox="0 0 16 16"><path d="M2,6 L10,6 L10,3 L14,8 L10,13 L10,10 L2,10 Z"/></svg>
                                        </button>
                                    </div>
                                </div>
                                
                                <!-- Category: Lines -->
                                <div class="shapes-dropdown-section">
                                    <div class="shapes-dropdown-title">Lines</div>
                                    <div class="shapes-dropdown-grid">
                                        <button type="button" class="shape-dropdown-item" data-shape="line" data-tooltip="Line" onclick="window.insertShapeIntoCanvas('${qid}', 'line')">
                                            <svg viewBox="0 0 16 16"><line x1="2" y1="2" x2="14" y2="14"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="arrow" data-tooltip="Arrow" onclick="window.insertShapeIntoCanvas('${qid}', 'arrow')">
                                            <svg viewBox="0 0 16 16"><line x1="2" y1="2" x2="14" y2="14"/><polygon points="14,10 14,14 10,14" fill="currentColor"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="doublearrow" data-tooltip="Double Arrow" onclick="window.insertShapeIntoCanvas('${qid}', 'doublearrow')">
                                            <svg viewBox="0 0 16 16"><line x1="3" y1="13" x2="13" y2="3"/><polygon points="3,9 3,13 7,13" fill="currentColor"/><polygon points="9,3 13,3 13,7" fill="currentColor"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="elbow-connector" data-tooltip="Elbow Connector" onclick="window.insertShapeIntoCanvas('${qid}', 'elbow-connector')">
                                            <svg viewBox="0 0 16 16"><path d="M2,2 L8,2 L8,14 L14,14"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="elbow-arrow" data-tooltip="Elbow Arrow Connector" onclick="window.insertShapeIntoCanvas('${qid}', 'elbow-arrow')">
                                            <svg viewBox="0 0 16 16"><path d="M2,2 L8,2 L8,14 L14,14"/><polygon points="12,12 14,14 12,16" fill="currentColor"/></svg>
                                        </button>
                                    </div>
                                </div>

                                <!-- Category: Rectangles -->
                                <div class="shapes-dropdown-section">
                                    <div class="shapes-dropdown-title">Rectangles</div>
                                    <div class="shapes-dropdown-grid">
                                        <button type="button" class="shape-dropdown-item" data-shape="rect" data-tooltip="Rectangle" onclick="window.insertShapeIntoCanvas('${qid}', 'rect')">
                                            <svg viewBox="0 0 16 16"><rect x="2" y="3" width="12" height="10"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="rounded-rect" data-tooltip="Rounded Rectangle" onclick="window.insertShapeIntoCanvas('${qid}', 'rounded-rect')">
                                            <svg viewBox="0 0 16 16"><rect x="2" y="3" width="12" height="10" rx="2" ry="2"/></svg>
                                        </button>
                                    </div>
                                </div>

                                <!-- Category: Basic Shapes -->
                                <div class="shapes-dropdown-section">
                                    <div class="shapes-dropdown-title">Basic Shapes</div>
                                    <div class="shapes-dropdown-grid">
                                        <button type="button" class="shape-dropdown-item" data-shape="textbox" data-tooltip="Text Box" onclick="window.insertShapeIntoCanvas('${qid}', 'textbox')">
                                            <svg viewBox="0 0 16 16"><rect x="1" y="1" width="14" height="14" rx="1" stroke-dasharray="2,2"/><text x="5" y="11" font-size="9" font-family="sans-serif" fill="currentColor">A</text></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="circle" data-tooltip="Oval" onclick="window.insertShapeIntoCanvas('${qid}', 'circle')">
                                            <svg viewBox="0 0 16 16"><circle cx="8" cy="8" r="6"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="triangle" data-tooltip="Isosceles Triangle" onclick="window.insertShapeIntoCanvas('${qid}', 'triangle')">
                                            <svg viewBox="0 0 16 16"><polygon points="8,2 2,14 14,14"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="right-triangle" data-tooltip="Right Triangle" onclick="window.insertShapeIntoCanvas('${qid}', 'right-triangle')">
                                            <svg viewBox="0 0 16 16"><polygon points="2,2 2,14 14,14"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="parallelogram" data-tooltip="Parallelogram" onclick="window.insertShapeIntoCanvas('${qid}', 'parallelogram')">
                                            <svg viewBox="0 0 16 16"><polygon points="5,3 14,3 11,13 2,13"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="trapezoid" data-tooltip="Trapezoid" onclick="window.insertShapeIntoCanvas('${qid}', 'trapezoid')">
                                            <svg viewBox="0 0 16 16"><polygon points="5,3 11,3 14,13 2,13"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="diamond" data-tooltip="Diamond" onclick="window.insertShapeIntoCanvas('${qid}', 'diamond')">
                                            <svg viewBox="0 0 16 16"><polygon points="8,2 14,8 8,14 2,8"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="pentagon" data-tooltip="Pentagon" onclick="window.insertShapeIntoCanvas('${qid}', 'pentagon')">
                                            <svg viewBox="0 0 16 16"><polygon points="8,2 14,6 12,14 4,14 2,6"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="hexagon" data-tooltip="Hexagon" onclick="window.insertShapeIntoCanvas('${qid}', 'hexagon')">
                                            <svg viewBox="0 0 16 16"><polygon points="8,2 14,5 14,11 8,14 2,11 2,5"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="octagon" data-tooltip="Octagon" onclick="window.insertShapeIntoCanvas('${qid}', 'octagon')">
                                            <svg viewBox="0 0 16 16"><polygon points="5,2 11,2 14,5 14,11 11,14 5,14 2,11 2,5"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="l-shape" data-tooltip="L-Shape" onclick="window.insertShapeIntoCanvas('${qid}', 'l-shape')">
                                            <svg viewBox="0 0 16 16"><polygon points="2,2 6,2 6,10 14,10 14,14 2,14"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="cross" data-tooltip="Cross" onclick="window.insertShapeIntoCanvas('${qid}', 'cross')">
                                            <svg viewBox="0 0 16 16"><polygon points="6,2 10,2 10,6 14,6 14,10 10,10 10,14 6,14 6,10 2,10 2,6 6,6"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="cylinder" data-tooltip="Cylinder" onclick="window.insertShapeIntoCanvas('${qid}', 'cylinder')">
                                            <svg viewBox="0 0 16 16"><ellipse cx="8" cy="4" rx="5" ry="2"/><path d="M3,4 L3,12 A5,2 0 0,0 13,12 L13,4"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="cube" data-tooltip="Cube" onclick="window.insertShapeIntoCanvas('${qid}', 'cube')">
                                            <svg viewBox="0 0 16 16"><polygon points="8,1 15,4.5 15,11.5 8,15 1,11.5 1,4.5"/><line x1="8" y1="8" x2="8" y2="15"/><line x1="8" y1="8" x2="15" y2="4.5"/><line x1="8" y1="8" x2="1" y2="4.5"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="document" data-tooltip="Folder/Document" onclick="window.insertShapeIntoCanvas('${qid}', 'document')">
                                            <svg viewBox="0 0 16 16"><path d="M2,2 L10,2 L14,6 L14,14 L2,14 Z M10,2 L10,6 L14,6 Z"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="smiley" data-tooltip="Smiley Face" onclick="window.insertShapeIntoCanvas('${qid}', 'smiley')">
                                            <svg viewBox="0 0 16 16"><circle cx="8" cy="8" r="6"/><path d="M5,9.5 Q8,12.5 11,9.5"/><circle cx="6" cy="6" r="0.8" fill="currentColor"/><circle cx="10" cy="6" r="0.8" fill="currentColor"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="heart" data-tooltip="Heart" onclick="window.insertShapeIntoCanvas('${qid}', 'heart')">
                                            <svg viewBox="0 0 16 16"><path d="M8,14 C8,14 1,10 1,5.5 C1,3 3,1 5.5,1 C7,1 8,2.5 8,2.5 C8,2.5 9,1 10.5,1 C13,1 15,3 15,5.5 C15,10 8,14 8,14 Z" fill="none"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="lightning" data-tooltip="Lightning" onclick="window.insertShapeIntoCanvas('${qid}', 'lightning')">
                                            <svg viewBox="0 0 16 16"><polygon points="10,1 4,8 8,8 6,15 12,7 8,7"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="sun" data-tooltip="Sun" onclick="window.insertShapeIntoCanvas('${qid}', 'sun')">
                                            <svg viewBox="0 0 16 16"><circle cx="8" cy="8" r="3"/><line x1="8" y1="1" x2="8" y2="3"/><line x1="8" y1="13" x2="8" y2="15"/><line x1="1" y1="8" x2="3" y2="8"/><line x1="13" y1="8" x2="15" y2="8"/><line x1="3.1" y1="3.1" x2="4.5" y2="4.5"/><line x1="11.5" y1="11.5" x2="12.9" y2="12.9"/><line x1="3.1" y1="12.9" x2="4.5" y2="11.5"/><line x1="11.5" y1="4.5" x2="12.9" y2="3.1"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="moon" data-tooltip="Moon" onclick="window.insertShapeIntoCanvas('${qid}', 'moon')">
                                            <svg viewBox="0 0 16 16"><path d="M12,2 A6,6 0 1,0 14,11 A5,5 0 1,1 12,2 Z"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="cloud" data-tooltip="Cloud" onclick="window.insertShapeIntoCanvas('${qid}', 'cloud')">
                                            <svg viewBox="0 0 16 16"><path d="M4,12 C2,12 1,10.5 1,9 C1,7.5 2.5,6.5 4,6.5 C4.5,4.5 6.5,3.5 8.5,4 C11,4.5 12,6 12,8 C13.5,8 15,9.2 15,10.5 C15,12 13.5,12 12,12 Z"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="database" data-tooltip="Database" onclick="window.insertShapeIntoCanvas('${qid}', 'database')">
                                            <svg viewBox="0 0 16 16"><ellipse cx="8" cy="4" rx="6" ry="2.5"/><path d="M2,4 L2,9 A6,2.5 0 0,0 14,9 L14,4"/><path d="M2,9 L2,14 A6,2.5 0 0,0 14,14 L14,9"/><path d="M2,4 A6,2.5 0 0,0 14,4"/></svg>
                                        </button>
                                    </div>
                                </div>

                                <!-- Category: Block Arrows -->
                                <div class="shapes-dropdown-section">
                                    <div class="shapes-dropdown-title">Block Arrows</div>
                                    <div class="shapes-dropdown-grid">
                                        <button type="button" class="shape-dropdown-item" data-shape="arrow-right" data-tooltip="Right Arrow" onclick="window.insertShapeIntoCanvas('${qid}', 'arrow-right')">
                                            <svg viewBox="0 0 16 16"><path d="M2,6 L10,6 L10,3 L14,8 L10,13 L10,10 L2,10 Z"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="arrow-left" data-tooltip="Left Arrow" onclick="window.insertShapeIntoCanvas('${qid}', 'arrow-left')">
                                            <svg viewBox="0 0 16 16"><path d="M14,6 L6,6 L6,3 L2,8 L6,13 L6,10 L14,10 Z"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="arrow-up" data-tooltip="Up Arrow" onclick="window.insertShapeIntoCanvas('${qid}', 'arrow-up')">
                                            <svg viewBox="0 0 16 16"><path d="M6,14 L6,6 L3,6 L8,2 L13,6 L10,6 L10,14 Z"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="arrow-down" data-tooltip="Down Arrow" onclick="window.insertShapeIntoCanvas('${qid}', 'arrow-down')">
                                            <svg viewBox="0 0 16 16"><path d="M6,2 L6,10 L3,10 L8,14 L13,10 L10,10 L10,2 Z"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="arrow-left-right" data-tooltip="Left-Right Arrow" onclick="window.insertShapeIntoCanvas('${qid}', 'arrow-left-right')">
                                            <svg viewBox="0 0 16 16"><path d="M2,8 L5,5 L5,7 L11,7 L11,5 L14,8 L11,11 L11,9 L5,9 L5,11 Z"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="arrow-up-down" data-tooltip="Up-Down Arrow" onclick="window.insertShapeIntoCanvas('${qid}', 'arrow-up-down')">
                                            <svg viewBox="0 0 16 16"><path d="M8,2 L5,5 L7,5 L7,11 L5,11 L8,14 L11,11 L9,11 L9,5 L11,5 Z"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="arrow-quad" data-tooltip="4-Way Arrow" onclick="window.insertShapeIntoCanvas('${qid}', 'arrow-quad')">
                                            <svg viewBox="0 0 16 16"><path d="M8,1 L6,3.5 L7,3.5 L7,7 L3.5,7 L3.5,6 L1,8 L3.5,10 L3.5,9 L7,9 L7,12.5 L6,12.5 L8,15 L10,12.5 L9,12.5 L9,9 L12.5,9 L12.5,10 L15,8 L12.5,6 L12.5,7 L9,7 L9,3.5 L10,3.5 Z"/></svg>
                                        </button>
                                    </div>
                                </div>

                                <!-- Category: Equation Shapes -->
                                <div class="shapes-dropdown-section">
                                    <div class="shapes-dropdown-title">Equation Shapes</div>
                                    <div class="shapes-dropdown-grid">
                                        <button type="button" class="shape-dropdown-item" data-shape="equation-plus" data-tooltip="Plus" onclick="window.insertShapeIntoCanvas('${qid}', 'equation-plus')">
                                            <svg viewBox="0 0 16 16"><line x1="8" y1="2" x2="8" y2="14"/><line x1="2" y1="8" x2="14" y2="8"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="equation-minus" data-tooltip="Minus" onclick="window.insertShapeIntoCanvas('${qid}', 'equation-minus')">
                                            <svg viewBox="0 0 16 16"><line x1="2" y1="8" x2="14" y2="8"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="equation-multiply" data-tooltip="Multiply" onclick="window.insertShapeIntoCanvas('${qid}', 'equation-multiply')">
                                            <svg viewBox="0 0 16 16"><line x1="3" y1="3" x2="13" y2="13"/><line x1="13" y1="3" x2="3" y2="13"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="equation-divide" data-tooltip="Divide" onclick="window.insertShapeIntoCanvas('${qid}', 'equation-divide')">
                                            <svg viewBox="0 0 16 16"><line x1="2" y1="8" x2="14" y2="8"/><circle cx="8" cy="4" r="1.5" fill="currentColor"/><circle cx="8" cy="12" r="1.5" fill="currentColor"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="equation-equal" data-tooltip="Equals" onclick="window.insertShapeIntoCanvas('${qid}', 'equation-equal')">
                                            <svg viewBox="0 0 16 16"><line x1="2" y1="6" x2="14" y2="6"/><line x1="2" y1="10" x2="14" y2="10"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="equation-notequal" data-tooltip="Not Equal" onclick="window.insertShapeIntoCanvas('${qid}', 'equation-notequal')">
                                            <svg viewBox="0 0 16 16"><line x1="2" y1="6" x2="14" y2="6"/><line x1="2" y1="10" x2="14" y2="10"/><line x1="3" y1="13" x2="13" y2="3"/></svg>
                                        </button>
                                    </div>
                                </div>

                                <!-- Category: Flowchart -->
                                <div class="shapes-dropdown-section">
                                    <div class="shapes-dropdown-title">Flowchart</div>
                                    <div class="shapes-dropdown-grid">
                                        <button type="button" class="shape-dropdown-item" data-shape="flowprocess" data-tooltip="Process" onclick="window.insertShapeIntoCanvas('${qid}', 'flowprocess')">
                                            <svg viewBox="0 0 16 16"><rect x="2" y="3" width="12" height="10"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="flowdecision" data-tooltip="Decision" onclick="window.insertShapeIntoCanvas('${qid}', 'flowdecision')">
                                            <svg viewBox="0 0 16 16"><polygon points="8,2 14,8 8,14 2,8"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="flowstartend" data-tooltip="Terminator" onclick="window.insertShapeIntoCanvas('${qid}', 'flowstartend')">
                                            <svg viewBox="0 0 16 16"><rect x="2" y="4" width="12" height="8" rx="4" ry="4"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="document" data-tooltip="Document" onclick="window.insertShapeIntoCanvas('${qid}', 'document')">
                                            <svg viewBox="0 0 16 16"><path d="M2,2 L10,2 L14,6 L14,14 L2,14 Z M10,2 L10,6 L14,6 Z"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="parallelogram" data-tooltip="Data (Input/Output)" onclick="window.insertShapeIntoCanvas('${qid}', 'parallelogram')">
                                            <svg viewBox="0 0 16 16"><polygon points="5,3 14,3 11,13 2,13"/></svg>
                                        </button>
                                    </div>
                                </div>

                                <!-- Category: Stars and Banners -->
                                <div class="shapes-dropdown-section">
                                    <div class="shapes-dropdown-title">Stars and Banners</div>
                                    <div class="shapes-dropdown-grid">
                                        <button type="button" class="shape-dropdown-item" data-shape="star-4" data-tooltip="4-Point Star" onclick="window.insertShapeIntoCanvas('${qid}', 'star-4')">
                                            <svg viewBox="0 0 16 16"><polygon points="8,2 10,6.5 14,8 10,9.5 8,14 6,9.5 2,8 6,6.5"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="star-5" data-tooltip="5-Point Star" onclick="window.insertShapeIntoCanvas('${qid}', 'star-5')">
                                            <svg viewBox="0 0 16 16"><polygon points="8,1 10,6 15,6.5 11,10 12.5,15 8,12 3.5,15 5,10 1,6.5 6,6"/></svg>
                                        </button>
                                        <button type="button" class="shape-dropdown-item" data-shape="star-6" data-tooltip="6-Point Star" onclick="window.insertShapeIntoCanvas('${qid}', 'star-6')">
                                            <svg viewBox="0 0 16 16"><polygon points="8,1 11,4.5 14,4.5 12.5,8 14,11.5 11,11.5 8,15 5,11.5 2,11.5 3.5,8 2,4.5 5,4.5"/></svg>
                                        </button>
                                    </div>
                                </div>
                            </div>
                            <!-- Footer Reset Action -->
                            <div class="shapes-dropdown-footer">
                                <button type="button" class="shapes-dropdown-footer-btn" onclick="document.getElementById('btn-clear-${qid}').click()">
                                    <i class="fa-solid fa-square-plus"></i> New Drawing Canvas
                                </button>
                            </div>
                        </div>
                    </div>
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
            case 'rounded-rect':
                shape = new fabric.Rect({
                    ...defaultProps,
                    width: 120,
                    height: 80,
                    rx: 15,
                    ry: 15
                });
                break;
            case 'triangle':
                shape = new fabric.Triangle({
                    ...defaultProps,
                    width: 100,
                    height: 90
                });
                break;
            case 'right-triangle':
                shape = new fabric.Polygon([
                    { x: 0, y: 0 },
                    { x: 0, y: 80 },
                    { x: 100, y: 80 }
                ], defaultProps);
                break;
            case 'parallelogram':
                shape = new fabric.Polygon([
                    { x: 25, y: 0 },
                    { x: 125, y: 0 },
                    { x: 100, y: 80 },
                    { x: 0, y: 80 }
                ], defaultProps);
                break;
            case 'trapezoid':
                shape = new fabric.Polygon([
                    { x: 25, y: 0 },
                    { x: 95, y: 0 },
                    { x: 120, y: 80 },
                    { x: 0, y: 80 }
                ], defaultProps);
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
            case 'octagon':
                shape = new fabric.Polygon([
                    { x: 29, y: 0 },
                    { x: 71, y: 0 },
                    { x: 100, y: 29 },
                    { x: 100, y: 71 },
                    { x: 71, y: 100 },
                    { x: 29, y: 100 },
                    { x: 0, y: 71 },
                    { x: 0, y: 29 }
                ], defaultProps);
                break;
            case 'l-shape':
                shape = new fabric.Polygon([
                    { x: 0, y: 0 },
                    { x: 35, y: 0 },
                    { x: 35, y: 65 },
                    { x: 100, y: 65 },
                    { x: 100, y: 100 },
                    { x: 0, y: 100 }
                ], defaultProps);
                break;
            case 'cross':
                shape = new fabric.Polygon([
                    { x: 35, y: 0 },
                    { x: 65, y: 0 },
                    { x: 65, y: 35 },
                    { x: 100, y: 35 },
                    { x: 100, y: 65 },
                    { x: 65, y: 65 },
                    { x: 65, y: 100 },
                    { x: 35, y: 100 },
                    { x: 35, y: 65 },
                    { x: 0, y: 65 },
                    { x: 0, y: 35 },
                    { x: 35, y: 35 }
                ], defaultProps);
                break;
            case 'cylinder':
                shape = new fabric.Path('M 0 10 A 50 10 0 1 0 100 10 A 50 10 0 1 0 0 10 Z M 0 10 L 0 90 A 50 10 0 0 0 100 90 L 100 10', defaultProps);
                break;
            case 'cube':
                shape = new fabric.Path('M 50 0 L 100 25 L 100 75 L 50 100 L 0 75 L 0 25 Z M 50 0 L 50 50 M 50 50 L 100 25 M 50 50 L 0 25 M 50 50 L 50 100', defaultProps);
                break;
            case 'document':
                shape = new fabric.Path('M 0 0 L 75 0 L 100 25 L 100 100 L 0 100 Z M 75 0 L 75 25 L 100 25 Z', defaultProps);
                break;
            case 'smiley':
                shape = new fabric.Path('M 50 0 A 50 50 0 1 0 50 100 A 50 50 0 1 0 50 0 Z M 30 35 A 5 5 0 1 1 30 35.1 Z M 70 35 A 5 5 0 1 1 70 35.1 Z M 30 60 Q 50 85 70 60', defaultProps);
                break;
            case 'heart':
                shape = new fabric.Path('M 50 15 C 35 -5, 0 -5, 0 35 C 0 65, 30 85, 50 95 C 70 85, 100 65, 100 35 C 100 -5, 65 -5, 50 15 Z', defaultProps);
                break;
            case 'lightning':
                shape = new fabric.Path('M 60 0 L 20 55 L 50 55 L 35 100 L 80 40 L 50 40 Z', defaultProps);
                break;
            case 'sun':
                shape = new fabric.Path('M 50 30 A 20 20 0 1 0 50 70 A 20 20 0 1 0 50 30 Z M 50 5 L 50 15 M 50 85 L 50 95 M 5 50 L 15 50 M 85 50 L 95 50 M 18 18 L 25 25 M 75 75 L 82 82 M 18 82 L 25 75 M 75 18 L 82 25', defaultProps);
                break;
            case 'moon':
                shape = new fabric.Path('M 50 10 A 40 40 0 1 0 90 50 A 32 32 0 1 1 50 10 Z', defaultProps);
                break;
            case 'cloud':
                shape = new fabric.Path('M 25 80 C 10 80, 0 70, 0 55 C 0 40, 15 30, 30 30 C 35 15, 50 10, 65 10 C 85 10, 100 25, 100 45 C 100 65, 85 80, 65 80 Z', defaultProps);
                break;
            case 'database':
                shape = new fabric.Path('M 0 15 L 0 85 C 0 95, 100 95, 100 85 L 100 15 Z M 0 15 C 0 25, 100 25, 100 15 C 100 5, 0 5, 0 15 M 0 45 C 0 55, 100 55, 100 45 M 0 70 C 0 80, 100 80, 100 70', defaultProps);
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
            case 'elbow-connector':
                shape = new fabric.Path('M 0 0 L 50 0 L 50 50', {
                    ...defaultProps,
                    fill: 'transparent'
                });
                break;
            case 'elbow-arrow':
                shape = new fabric.Path('M 0 0 L 50 0 L 50 50 M 45 45 L 50 50 L 55 45', {
                    ...defaultProps,
                    fill: 'transparent'
                });
                break;
            case 'arrow-right':
                shape = new fabric.Path('M 0 35 L 60 35 L 60 15 L 100 50 L 60 85 L 60 65 L 0 65 Z', defaultProps);
                break;
            case 'arrow-left':
                shape = new fabric.Path('M 100 35 L 40 35 L 40 15 L 0 50 L 40 85 L 40 65 L 100 65 Z', defaultProps);
                break;
            case 'arrow-up':
                shape = new fabric.Path('M 35 100 L 35 40 L 15 40 L 50 0 L 85 40 L 65 40 L 65 100 Z', defaultProps);
                break;
            case 'arrow-down':
                shape = new fabric.Path('M 35 0 L 35 60 L 15 60 L 50 100 L 85 60 L 65 60 L 65 0 Z', defaultProps);
                break;
            case 'arrow-left-right':
                shape = new fabric.Path('M 40 15 L 40 35 L 60 35 L 60 15 L 100 50 L 60 85 L 60 65 L 40 65 L 40 85 L 0 50 Z', defaultProps);
                break;
            case 'arrow-up-down':
                shape = new fabric.Path('M 15 40 L 35 40 L 35 60 L 15 60 L 50 100 L 85 60 L 65 60 L 65 40 L 85 40 L 50 0 Z', defaultProps);
                break;
            case 'arrow-quad':
                shape = new fabric.Path('M 35 35 L 35 15 L 20 15 L 50 0 L 80 15 L 65 15 L 65 35 L 85 35 L 85 20 L 100 50 L 85 80 L 85 65 L 65 65 L 65 85 L 80 85 L 50 100 L 20 85 L 35 85 L 35 65 L 15 65 L 15 80 L 0 50 L 15 20 L 15 35 Z', defaultProps);
                break;
            case 'equation-plus':
                shape = new fabric.Polygon([
                    { x: 35, y: 0 },
                    { x: 65, y: 0 },
                    { x: 65, y: 35 },
                    { x: 100, y: 35 },
                    { x: 100, y: 65 },
                    { x: 65, y: 65 },
                    { x: 65, y: 100 },
                    { x: 35, y: 100 },
                    { x: 35, y: 65 },
                    { x: 0, y: 65 },
                    { x: 0, y: 35 },
                    { x: 35, y: 35 }
                ], defaultProps);
                break;
            case 'equation-minus':
                shape = new fabric.Rect({
                    ...defaultProps,
                    width: 100,
                    height: 30
                });
                break;
            case 'equation-multiply':
                shape = new fabric.Path('M 25 0 L 50 25 L 75 0 L 100 25 L 75 50 L 100 75 L 75 100 L 50 75 L 25 100 L 0 75 L 25 50 L 0 25 Z', defaultProps);
                break;
            case 'equation-divide':
                shape = new fabric.Path('M 0 40 L 100 40 L 100 60 L 0 60 Z M 50 10 A 10 10 0 1 1 50 30 A 10 10 0 1 1 50 10 Z M 50 70 A 10 10 0 1 1 50 90 A 10 10 0 1 1 50 70 Z', defaultProps);
                break;
            case 'equation-equal':
                shape = new fabric.Path('M 0 25 L 100 25 L 100 45 L 0 45 Z M 0 55 L 100 55 L 100 75 L 0 75 Z', defaultProps);
                break;
            case 'equation-notequal':
                shape = new fabric.Path('M 0 25 L 100 25 L 100 45 L 0 45 Z M 0 55 L 100 55 L 100 75 L 0 75 Z M 20 90 L 80 10 L 90 20 L 30 100 Z', defaultProps);
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
            case 'star-4':
                shape = new fabric.Path('M 50 0 L 62 38 L 100 50 L 62 62 L 50 100 L 38 62 L 0 50 L 38 38 Z', defaultProps);
                break;
            case 'star-5':
                shape = new fabric.Path('M 50 0 L 65 35 L 100 35 L 72 57 L 83 95 L 50 75 L 17 95 L 28 57 L 0 35 L 35 35 Z', defaultProps);
                break;
            case 'star-6':
                shape = new fabric.Path('M 50 0 L 65 25 L 95 25 L 80 50 L 95 75 L 65 75 L 50 100 L 35 75 L 5 75 L 20 50 L 5 25 L 35 25 Z', defaultProps);
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
