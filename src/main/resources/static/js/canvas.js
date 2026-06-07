/**
 * Premium Rich Text Editor, Custom Table, & Interactive Shape Builder Engine
 * Designed for Theory Exam Student & Evaluator Portals
 * Author: Antigravity AI
 */

window.canvasStates = window.canvasStates || {};
(function () {
    // Immediate Theme Sync to avoid styling flashes (skipped for student pages)
    const savedTheme = localStorage.getItem('theme') || 'light';
    const isStudentPage = window.location.pathname.startsWith('/student') || 
                          window.location.pathname === '/' || 
                          window.location.pathname === '/login';
    if (savedTheme === 'dark' && !isStudentPage) {
        document.documentElement.classList.add('dark-theme');
        if (document.body) {
            document.body.classList.add('dark-theme');
        } else {
            document.addEventListener('DOMContentLoaded', () => {
                document.body.classList.add('dark-theme');
            });
        }
    }

    let currentEditingEditor = null;
    let currentEditingShape = null;

    /**
     * Scan the document for all question textareas and bootstrap TinyMCE Editors.
     */
    function initializeRichEditors() {
        document.querySelectorAll('textarea.answer-area').forEach(textarea => {
            if (textarea.getAttribute('data-rich-initialized') === 'true') return;
            setupRichEditor(textarea);
        });

        // Trigger drawing whiteboard tools initialization
        if (typeof window.initializeDrawingTool === 'function') {
            const attemptId = document.getElementById('attemptId')?.value;
            const submissionId = document.getElementById('submissionId')?.value;
            const type = document.getElementById('type')?.value;
            
            let finalAttemptId = attemptId;
            let finalSubmissionId = submissionId;
            if (type === 'paper' && attemptId) {
                finalSubmissionId = attemptId;
                finalAttemptId = null;
            }
            window.initializeDrawingTool(finalAttemptId, finalSubmissionId);
        }
    }

    function insertShapeIntoEditor(editor, type) {
        // Use TinyMCE dialog API instead of prompt() — browser prompt() is unreliable
        // when triggered from inside a TinyMCE iframe (focus/blocking issues)
        editor.windowManager.open({
            title: 'Insert Shape',
            body: {
                type: 'panel',
                items: [{
                    type: 'input',
                    name: 'shapeLabel',
                    label: 'Label inside shape (optional)',
                    placeholder: 'e.g. Start, Process, A...'
                }]
            },
            buttons: [
                { type: 'cancel', text: 'Cancel' },
                { type: 'submit', text: 'Insert', primary: true }
            ],
            onSubmit: function(api) {
                const text = api.getData().shapeLabel || '';
                api.close();

                let width = 150, height = 80;
                if (type === 'square' || type === 'circle') { width = 100; height = 100; }
                else if (type === 'oval') { width = 150; height = 90; }
                else if (type === 'triangle' || type === 'diamond' || type === 'pentagon') { width = 120; height = 100; }
                else if (type === 'hexagon') { width = 140; height = 100; }
                else if (type === 'line') { width = 150; height = 30; }
                else if (type === 'arrow' || type === 'arrow-double-horizontal') { width = 150; height = 50; }

                const fill = '#ffffff';
                const border = '#000000';
                const thickness = 2;
                const style = 'solid';
                const textSize = '14px';
                const textColor = '#000000';

                const actualType = type === 'square' ? 'rect' : (type === 'oval' ? 'circle' : type);
                const shapeSvg = generateShapeSvgParams(actualType, text, fill, border, thickness, style, width, height, textSize, textColor);
                const wrappedHtml = `
                    <span class="shape-wrapper" contenteditable="false" style="display:inline-block;position:relative;vertical-align:middle;margin:5px;">
                        ${shapeSvg}
                        <span class="shape-resize-handle nw" data-handle="nw"></span>
                        <span class="shape-resize-handle n" data-handle="n"></span>
                        <span class="shape-resize-handle ne" data-handle="ne"></span>
                        <span class="shape-resize-handle e" data-handle="e"></span>
                        <span class="shape-resize-handle se" data-handle="se"></span>
                        <span class="shape-resize-handle s" data-handle="s"></span>
                        <span class="shape-resize-handle sw" data-handle="sw"></span>
                        <span class="shape-resize-handle w" data-handle="w"></span>
                        <span class="shape-delete-btn" title="Delete shape">&times;</span>
                    </span>
                `;
                editor.insertContent(wrappedHtml);
                editor.fire('change');
            }
        });
    }

    /**
     * Bootstraps a single question answer box into a premium TinyMCE instance.
     */
    function setupRichEditor(originalTextarea) {
        originalTextarea.setAttribute('data-rich-initialized', 'true');
        
        const qid = originalTextarea.getAttribute('data-qid') || 'q_' + Math.random().toString(36).substr(2, 9);
        originalTextarea.id = `textarea-${qid}`;

        // Initialize TinyMCE
        tinymce.init({
            selector: `#textarea-${qid}`,
            height: 380,
            menubar: false,
            license_key: 'gpl',
            promotion: false,
            branding: false,
            statusbar: false,
            elementpath: false,
            // Skin & content CSS must point to the same CDN tinymce.min.js was loaded from.
            // Without this TinyMCE hangs on the loading spinner because it can't find its skin files.
            skin_url: 'https://cdn.jsdelivr.net/npm/tinymce@6/skins/ui/oxide',
            content_css: 'https://cdn.jsdelivr.net/npm/tinymce@6/skins/content/default/content.min.css',
            plugins: 'lists table lineheight charmap',
            toolbar: 'undo redo | fontfamily fontsize | bold italic underline forecolor backcolor | table shapesdropdown mathsymbols charmap | alignleft aligncenter alignright | bullist numlist lineheight | superscript subscript',
            toolbar_mode: 'wrap',
            // ── SVG shape preservation ──────────────────────────────────────────
            // Without these, TinyMCE silently strips every SVG element on insert/re-render
            extended_valid_elements: [
                'svg[*]', 'defs[*]', 'rect[*]', 'ellipse[*]', 'circle[*]',
                'line[*]', 'path[*]', 'polygon[*]', 'polyline[*]', 'text[*]',
                'tspan[*]', 'g[*]', 'linearGradient[*]', 'radialGradient[*]',
                'stop[*]', 'filter[*]', 'feDropShadow[*]', 'feGaussianBlur[*]',
                'feOffset[*]', 'feMerge[*]', 'feMergeNode[*]', 'marker[*]'
            ].join(','),
            valid_children: '+body[svg],+p[svg],+span[svg],+div[svg]',
            verify_html: false,
            // ────────────────────────────────────────────────────────────────────
            init_instance_callback: function (editor) {
                const theme = localStorage.getItem('theme') || 'light';
                const isStudentPage = window.location.pathname.startsWith('/student') || 
                                      window.location.pathname === '/' || 
                                      window.location.pathname === '/login';
                const body = editor.getBody();
                if (body) {
                    if (theme === 'dark' && !isStudentPage) {
                        body.style.backgroundColor = '#1e293b';
                        body.style.color = '#f8fafc';
                    } else {
                        body.style.backgroundColor = '#ffffff';
                        body.style.color = '#2d3748';
                    }
                }
                if (typeof startReadonly !== 'undefined' && startReadonly) { editor.setMode('readonly'); }

                // ── SHAPE DRAG & RESIZE ENGINE ────────────────────────────────
                (function attachShapeInteractions() {
                    const doc = editor.getDoc();
                    let selectedWrapper = null;
                    let dragState   = null;
                    let resizeState = null;

                    function wrapShape(svg) {
                        if (svg.parentNode && svg.parentNode.classList && svg.parentNode.classList.contains('shape-wrapper')) return svg.parentNode;
                        const w = doc.createElement('span');
                        w.className = 'shape-wrapper';
                        w.setAttribute('contenteditable', 'false');
                        w.style.cssText = 'display:inline-block;position:relative;vertical-align:middle;margin:5px;';
                        svg.parentNode.insertBefore(w, svg);
                        w.appendChild(svg);
                        ['nw','n','ne','e','se','s','sw','w'].forEach(function(pos) {
                            const h = doc.createElement('span');
                            h.className = 'shape-resize-handle ' + pos;
                            h.setAttribute('data-handle', pos);
                            w.appendChild(h);
                        });
                        const del = doc.createElement('span');
                        del.className = 'shape-delete-btn';
                        del.innerHTML = '&times;';
                        del.title = 'Delete shape';
                        del.addEventListener('mousedown', function(e) {
                            e.preventDefault(); e.stopPropagation();
                            w.parentNode && w.parentNode.removeChild(w);
                            selectedWrapper = null;
                            editor.fire('change');
                        });
                        w.appendChild(del);
                        return w;
                    }

                    function selectWrapper(wrapper) {
                        if (selectedWrapper && selectedWrapper !== wrapper) selectedWrapper.classList.remove('shape-selected');
                        selectedWrapper = wrapper;
                        if (wrapper) wrapper.classList.add('shape-selected');
                    }

                    function wrapAllShapes() {
                        Array.from(doc.querySelectorAll('.inserted-shape')).forEach(function(svg) { wrapShape(svg); });
                    }

                    wrapAllShapes();
                    editor.on('change SetContent', function() { setTimeout(wrapAllShapes, 30); });

                    doc.addEventListener('mousedown', function(e) {
                        const handle = e.target.closest && e.target.closest('.shape-resize-handle');
                        const wrapper = e.target.closest && e.target.closest('.shape-wrapper');
                        const delBtn  = e.target.closest && e.target.closest('.shape-delete-btn');
                        if (delBtn) {
                            e.preventDefault(); e.stopPropagation();
                            const wrap = delBtn.closest('.shape-wrapper');
                            if (wrap) {
                                wrap.parentNode && wrap.parentNode.removeChild(wrap);
                                selectedWrapper = null;
                                editor.fire('change');
                            }
                            return;
                        }
                        if (handle && wrapper) {
                            e.preventDefault(); e.stopPropagation();
                            const svg = wrapper.querySelector('.inserted-shape');
                            resizeState = {
                                wrapper: wrapper,
                                handle: handle.getAttribute('data-handle'),
                                startX: e.clientX, startY: e.clientY,
                                origW: parseInt(svg.style.width)  || svg.getBoundingClientRect().width,
                                origH: parseInt(svg.style.height) || svg.getBoundingClientRect().height,
                                svg: svg
                            };
                            selectWrapper(wrapper);
                            return;
                        }
                        if (wrapper) {
                            const svg = wrapper.querySelector('.inserted-shape');
                            if (svg && (e.target === svg || svg.contains(e.target))) {
                                e.preventDefault();
                                dragState = {
                                    wrapper: wrapper,
                                    startX: e.clientX, startY: e.clientY,
                                    origML: parseInt(wrapper.style.marginLeft) || 0,
                                    origMT: parseInt(wrapper.style.marginTop)  || 0
                                };
                                selectWrapper(wrapper);
                                return;
                            }
                        }
                        selectWrapper(null);
                    });

                    doc.addEventListener('mousemove', function(e) {
                        if (resizeState) {
                            e.preventDefault();
                            const dx = e.clientX - resizeState.startX;
                            const dy = e.clientY - resizeState.startY;
                            const h = resizeState.handle;
                            let newW = resizeState.origW;
                            let newH = resizeState.origH;
                            if (h.includes('e')) newW = Math.max(40, resizeState.origW + dx);
                            if (h.includes('s')) newH = Math.max(30, resizeState.origH + dy);
                            if (h.includes('w')) newW = Math.max(40, resizeState.origW - dx);
                            if (h.includes('n')) newH = Math.max(30, resizeState.origH - dy);
                            resizeState.svg.style.width  = newW + 'px';
                            resizeState.svg.style.height = newH + 'px';
                            resizeState.svg.setAttribute('data-shape-width',  Math.round(newW));
                            resizeState.svg.setAttribute('data-shape-height', Math.round(newH));
                            return;
                        }
                        if (dragState) {
                            e.preventDefault();
                            const dx = e.clientX - dragState.startX;
                            const dy = e.clientY - dragState.startY;
                            dragState.wrapper.style.marginLeft = (dragState.origML + dx) + 'px';
                            dragState.wrapper.style.marginTop  = (dragState.origMT + dy) + 'px';
                        }
                    });

                    doc.addEventListener('mouseup', function() {
                        if (resizeState || dragState) editor.fire('change');
                        resizeState = null;
                        dragState   = null;
                    });

                    doc.addEventListener('keydown', function(e) {
                        if ((e.key === 'Delete' || e.key === 'Backspace') && selectedWrapper) {
                            const active = doc.activeElement;
                            if (active && (active.tagName === 'INPUT' || active.tagName === 'TEXTAREA' || active.isContentEditable)) return;
                            e.preventDefault();
                            selectedWrapper.parentNode && selectedWrapper.parentNode.removeChild(selectedWrapper);
                            selectedWrapper = null;
                            editor.fire('change');
                        }
                    });
                })();
                // ── END SHAPE DRAG & RESIZE ENGINE ───────────────────────────
            },
            content_style: `
                body {
                    font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
                    font-size: 16px;
                    line-height: 1.6;
                    color: #2d3748;
                    padding: 15px;
                }
                .inserted-shape {
                    cursor: move;
                    display: inline-block;
                    position: relative;
                    vertical-align: middle;
                    margin: 5px;
                    user-select: none;
                }
                .inserted-shape.shape-selected {
                    outline: 2px solid #6f42c1;
                    outline-offset: 2px;
                }
                .shape-wrapper {
                    display: inline-block;
                    position: relative;
                    vertical-align: middle;
                    margin: 5px;
                    user-select: none;
                }
                .shape-wrapper.shape-selected .inserted-shape {
                    outline: 2.5px solid #6f42c1;
                    outline-offset: 2px;
                }
                .shape-resize-handle {
                    position: absolute;
                    width: 10px;
                    height: 10px;
                    background: #6f42c1;
                    border: 2px solid #fff;
                    border-radius: 50%;
                    z-index: 9999;
                    cursor: nwse-resize;
                    box-shadow: 0 0 0 1px #6f42c1;
                }
                .shape-resize-handle.nw { top:-5px; left:-5px; cursor:nwse-resize; }
                .shape-resize-handle.ne { top:-5px; right:-5px; cursor:nesw-resize; }
                .shape-resize-handle.sw { bottom:-5px; left:-5px; cursor:nesw-resize; }
                .shape-resize-handle.se { bottom:-5px; right:-5px; cursor:nwse-resize; }
                .shape-resize-handle.n  { top:-5px; left:calc(50% - 5px); cursor:ns-resize; }
                .shape-resize-handle.s  { bottom:-5px; left:calc(50% - 5px); cursor:ns-resize; }
                .shape-resize-handle.e  { right:-5px; top:calc(50% - 5px); cursor:ew-resize; }
                .shape-resize-handle.w  { left:-5px; top:calc(50% - 5px); cursor:ew-resize; }
                .shape-delete-btn {
                    position: absolute;
                    top: -14px;
                    right: -14px;
                    width: 22px;
                    height: 22px;
                    background: #ef4444;
                    color: #fff;
                    border: 2px solid #fff;
                    border-radius: 50%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-size: 13px;
                    font-weight: 900;
                    cursor: pointer;
                    z-index: 9999;
                    line-height: 1;
                }
                table {
                    border-collapse: collapse;
                    width: 100%;
                    margin: 15px 0;
                }
                table th, table td {
                    border: 1px solid #cbd5e1;
                    padding: 10px 14px;
                    min-width: 50px;
                }
                table th {
                    background-color: #f8fafc;
                    font-weight: 700;
                }
            `,
            setup: function (editor) {
                // Register Custom Shapes Dropdown Button
                editor.ui.registry.addMenuButton('shapesdropdown', {
                    text: 'Shapes ▼',
                    tooltip: 'Insert Shape',
                    fetch: function (callback) {
                        var items = [
                            { type: 'menuitem', text: 'Rectangle', onAction: function () { insertShapeIntoEditor(editor, 'rect'); } },
                            { type: 'menuitem', text: 'Square', onAction: function () { insertShapeIntoEditor(editor, 'square'); } },
                            { type: 'menuitem', text: 'Circle', onAction: function () { insertShapeIntoEditor(editor, 'circle'); } },
                            { type: 'menuitem', text: 'Oval', onAction: function () { insertShapeIntoEditor(editor, 'oval'); } },
                            { type: 'menuitem', text: 'Triangle', onAction: function () { insertShapeIntoEditor(editor, 'triangle'); } },
                            { type: 'menuitem', text: 'Diamond', onAction: function () { insertShapeIntoEditor(editor, 'diamond'); } },
                            { type: 'menuitem', text: 'Pentagon', onAction: function () { insertShapeIntoEditor(editor, 'pentagon'); } },
                            { type: 'menuitem', text: 'Hexagon', onAction: function () { insertShapeIntoEditor(editor, 'hexagon'); } },
                            { type: 'menuitem', text: 'Line', onAction: function () { insertShapeIntoEditor(editor, 'line'); } },
                            { type: 'menuitem', text: 'Arrow', onAction: function () { insertShapeIntoEditor(editor, 'arrow'); } },
                            { type: 'menuitem', text: 'Double Arrow', onAction: function () { insertShapeIntoEditor(editor, 'arrow-double-horizontal'); } },
                            { type: 'menuitem', text: 'Flowchart Process', onAction: function () { insertShapeIntoEditor(editor, 'rect'); } },
                            { type: 'menuitem', text: 'Flowchart Decision', onAction: function () { insertShapeIntoEditor(editor, 'diamond'); } },
                            { type: 'menuitem', text: 'Flowchart Start/End', onAction: function () { insertShapeIntoEditor(editor, 'capsule'); } },
                            { type: 'menuitem', text: 'Text Box', onAction: function () { insertShapeIntoEditor(editor, 'rect'); } }
                        ];
                        callback(items);
                    }
                });

                // Double-click to edit shape text inline
                editor.on('dblclick', function (e) {
                    const target = e.target.closest('.inserted-shape');
                    if (target) {
                        e.preventDefault();
                        const oldText = target.getAttribute('data-shape-text') || '';
                        const newText = prompt("Edit shape text:", oldText);
                        if (newText !== null) {
                            target.setAttribute('data-shape-text', newText);
                            const textEl = target.querySelector('text');
                            if (textEl) {
                                textEl.textContent = newText;
                            }
                            editor.fire('change');
                        }
                    }
                });

                // Register Custom Math Symbols Dropdown
                editor.ui.registry.addMenuButton('mathsymbols', {
                    text: '∑ Math',
                    tooltip: 'Insert Math Symbols',
                    fetch: function (callback) {
                        var items = [
                            {
                                type: 'nestedmenuitem',
                                text: 'Basic Arithmetic (×, ÷, √, ...)',
                                getSubmenuItems: function () {
                                    return [
                                        { type: 'menuitem', text: '× Multiplication (×)', onAction: function () { editor.insertContent('&times;'); } },
                                        { type: 'menuitem', text: '÷ Division (÷)', onAction: function () { editor.insertContent('&divide;'); } },
                                        { type: 'menuitem', text: '± Plus-Minus (±)', onAction: function () { editor.insertContent('&plusmn;'); } },
                                        { type: 'menuitem', text: '≠ Not Equal (≠)', onAction: function () { editor.insertContent('&ne;'); } },
                                        { type: 'menuitem', text: '≈ Approximately (≈)', onAction: function () { editor.insertContent('&asymp;'); } },
                                        { type: 'menuitem', text: '≤ Less Than or Equal (≤)', onAction: function () { editor.insertContent('&le;'); } },
                                        { type: 'menuitem', text: '≥ Greater Than or Equal (≥)', onAction: function () { editor.insertContent('&ge;'); } },
                                        { type: 'menuitem', text: '√ Square Root (√)', onAction: function () { editor.insertContent('&radic;'); } },
                                        { type: 'menuitem', text: '∞ Infinity (∞)', onAction: function () { editor.insertContent('&infin;'); } },
                                        { type: 'menuitem', text: 'π Pi (π)', onAction: function () { editor.insertContent('&pi;'); } },
                                        { type: 'menuitem', text: '° Degree (°)', onAction: function () { editor.insertContent('&deg;'); } }
                                    ];
                                }
                            },
                            {
                                type: 'nestedmenuitem',
                                text: 'Algebra & Calculus (∫, ∑, ...)',
                                getSubmenuItems: function () {
                                    return [
                                        { type: 'menuitem', text: '∫ Integral (∫)', onAction: function () { editor.insertContent('&#8747;'); } },
                                        { type: 'menuitem', text: '∬ Double Integral (∬)', onAction: function () { editor.insertContent('&#8748;'); } },
                                        { type: 'menuitem', text: '∂ Partial Derivative (∂)', onAction: function () { editor.insertContent('&#8706;'); } },
                                        { type: 'menuitem', text: '∑ Summation (∑)', onAction: function () { editor.insertContent('&sum;'); } },
                                        { type: 'menuitem', text: '∏ Product (∏)', onAction: function () { editor.insertContent('&prod;'); } },
                                        { type: 'menuitem', text: 'lim Limit (lim)', onAction: function () { editor.insertContent('lim '); } },
                                        { type: 'menuitem', text: 'Δ Delta / Change (Δ)', onAction: function () { editor.insertContent('&Delta;'); } },
                                        { type: 'menuitem', text: '∇ Nabla / Gradient (∇)', onAction: function () { editor.insertContent('&nabla;'); } },
                                        { type: 'menuitem', text: 'log Logarithm (log)', onAction: function () { editor.insertContent('log'); } },
                                        { type: 'menuitem', text: 'ln Natural Log (ln)', onAction: function () { editor.insertContent('ln'); } }
                                    ];
                                }
                            },
                            {
                                type: 'nestedmenuitem',
                                text: 'Greek Letters (α, β, θ, ...)',
                                getSubmenuItems: function () {
                                    return [
                                        { type: 'menuitem', text: 'α Alpha (α)', onAction: function () { editor.insertContent('&alpha;'); } },
                                        { type: 'menuitem', text: 'β Beta (β)', onAction: function () { editor.insertContent('&beta;'); } },
                                        { type: 'menuitem', text: 'γ Gamma (γ)', onAction: function () { editor.insertContent('&gamma;'); } },
                                        { type: 'menuitem', text: 'δ Delta (δ)', onAction: function () { editor.insertContent('&delta;'); } },
                                        { type: 'menuitem', text: 'ε Epsilon (ε)', onAction: function () { editor.insertContent('&epsilon;'); } },
                                        { type: 'menuitem', text: 'θ Theta (θ)', onAction: function () { editor.insertContent('&theta;'); } },
                                        { type: 'menuitem', text: 'λ Lambda (λ)', onAction: function () { editor.insertContent('&lambda;'); } },
                                        { type: 'menuitem', text: 'μ Mu (μ)', onAction: function () { editor.insertContent('&mu;'); } },
                                        { type: 'menuitem', text: 'σ Sigma (σ)', onAction: function () { editor.insertContent('&sigma;'); } },
                                        { type: 'menuitem', text: 'ω Omega (ω)', onAction: function () { editor.insertContent('&omega;'); } }
                                    ];
                                }
                            },
                            {
                                type: 'nestedmenuitem',
                                text: 'Relations & Set Logic (→, ⇒, ∈, ...)',
                                getSubmenuItems: function () {
                                    return [
                                        { type: 'menuitem', text: '→ Right Arrow (→)', onAction: function () { editor.insertContent('&rarr;'); } },
                                        { type: 'menuitem', text: '⇒ Implies (⇒)', onAction: function () { editor.insertContent('&rArr;'); } },
                                        { type: 'menuitem', text: '⇔ Equivalent / Iff (⇔)', onAction: function () { editor.insertContent('&hArr;'); } },
                                        { type: 'menuitem', text: '∈ Element of (∈)', onAction: function () { editor.insertContent('&isin;'); } },
                                        { type: 'menuitem', text: '∉ Not element of (∉)', onAction: function () { editor.insertContent('&notin;'); } },
                                        { type: 'menuitem', text: '⊂ Subset of (⊂)', onAction: function () { editor.insertContent('&sub;'); } },
                                        { type: 'menuitem', text: '∀ For All (∀)', onAction: function () { editor.insertContent('&forall;'); } },
                                        { type: 'menuitem', text: '∃ There Exists (∃)', onAction: function () { editor.insertContent('&exist;'); } },
                                        { type: 'menuitem', text: '∴ Therefore (∴)', onAction: function () { editor.insertContent('&there4;'); } },
                                        { type: 'menuitem', text: '∵ Because (∵)', onAction: function () { editor.insertContent('&#8757;'); } }
                                    ];
                                }
                            }
                        ];
                        callback(items);
                    }
                });
                // Auto-sync back to the underlying textarea on changes
                editor.on('change keyup undo redo input nodechange', function () {
                    const rawContent = editor.getContent();
                    
                    // Secure sanitization with DOMPurify while preserving tables and shapes SVGs
                    const sanitized = DOMPurify.sanitize(rawContent, {
                        ADD_TAGS: ['svg', 'rect', 'ellipse', 'line', 'path', 'text', 'defs', 'marker', 'linearGradient', 'stop', 'filter', 'feDropShadow', 'feGaussianBlur', 'feOffset', 'feMerge', 'feMergeNode'],
                        ADD_ATTR: [
                            'class', 'style', 'viewbox', 'xmlns', 'cx', 'cy', 'rx', 'ry', 
                            'x', 'y', 'width', 'height', 'stroke', 'stroke-width', 'stroke-dasharray',
                            'fill', 'dominant-baseline', 'text-anchor', 'font-size', 'font-family', 
                            'font-weight', 'x1', 'y1', 'x2', 'y2', 'marker-end', 'id', 'refx', 'refy',
                            'markerwidth', 'markerheight', 'orient', 'data-shape-type', 'data-shape-text', 
                            'data-shape-fill', 'data-shape-border', 'data-shape-border-thickness', 
                            'data-shape-border-style', 'data-shape-width', 'data-shape-height', 
                            'data-shape-text-size', 'data-shape-text-color',
                            'offset', 'stop-color', 'stop-opacity', 'stdDeviation', 'dx', 'dy',
                            'flood-opacity', 'flood-color', 'in', 'result'
                        ]
                    });

                    originalTextarea.value = sanitized;

                    // Trigger original input events to run debounced saves
                    originalTextarea.dispatchEvent(new Event('input', { bubbles: true }));
                });

                // ANTI-CHEAT: Disable copy/paste/cut within editor
                editor.on('copy', function (e) {
                    const selectedText = editor.selection.getContent({ format: 'text' });
                    if (selectedText) {
                        sessionStorage.setItem("examClipboard", selectedText);
                        sessionStorage.setItem("examClipboardSource", "INTERNAL_EXAM");
                        const clipboardEvent = e.clipboardData || (e.domEvent && e.domEvent.clipboardData);
                        if (clipboardEvent) {
                            clipboardEvent.setData('text/plain', selectedText);
                        } else {
                            navigator.clipboard.writeText(selectedText).catch(err => console.error(err));
                        }
                        e.preventDefault();
                    }
                });

                editor.on('cut', function (e) {
                    const selectedText = editor.selection.getContent({ format: 'text' });
                    if (selectedText) {
                        sessionStorage.setItem("examClipboard", selectedText);
                        sessionStorage.setItem("examClipboardSource", "INTERNAL_EXAM");
                        const clipboardEvent = e.clipboardData || (e.domEvent && e.domEvent.clipboardData);
                        if (clipboardEvent) {
                            clipboardEvent.setData('text/plain', selectedText);
                        } else {
                            navigator.clipboard.writeText(selectedText).catch(err => console.error(err));
                        }
                        editor.execCommand('Delete');
                        e.preventDefault();
                    }
                });

                editor.on('paste', function (e) {
                    const clipboardEvent = e.clipboardData || (e.domEvent && e.domEvent.clipboardData);
                    const pastedText = clipboardEvent ? clipboardEvent.getData('text/plain') : '';
                    const examClipboard = sessionStorage.getItem("examClipboard");
                    const examClipboardSource = sessionStorage.getItem("examClipboardSource");
                    const qid = editor.id.replace('textarea-', '');

                    const showWarning = window.showWarningToast || (window.parent && window.parent.showWarningToast);
                    const logAttempt = window.logPasteAttempt || (window.parent && window.parent.logPasteAttempt);

                    if (examClipboardSource === "INTERNAL_EXAM" && examClipboard && pastedText === examClipboard) {
                        // Allow default paste, log it
                        if (typeof logAttempt === "function") {
                            logAttempt(qid, "Internal Paste Allowed", "INTERNAL_EXAM");
                        }
                    } else {
                        e.preventDefault();
                        if (typeof showWarning === "function") {
                            showWarning("External content cannot be pasted during the examination.");
                        }
                        if (typeof logAttempt === "function") {
                            logAttempt(qid, "External Paste Attempt blocked", "EXTERNAL");
                        }
                    }
                });
            }
        });
    }

    // --- Shape Builder & Editor Functionality ---

    function syncVisualShapePicker() {
        const shapeVal = document.getElementById('shapeType') ? document.getElementById('shapeType').value : 'rect';
        document.querySelectorAll('.shape-item-btn').forEach(btn => {
            if (btn.getAttribute('data-shape-value') === shapeVal) {
                btn.classList.add('selected-shape');
            } else {
                btn.classList.remove('selected-shape');
            }
        });
    }

    function buildVisualShapePicker(container) {
        const categories = [
            {
                name: "Rectangles & Terminals",
                shapes: [
                    { value: "rect", name: "Rectangle", svg: `<rect x="3" y="5" width="18" height="14" rx="2" stroke-width="1.8" stroke="currentColor" fill="none" />` },
                    { value: "capsule", name: "Capsule / Pill Terminal", svg: `<rect x="3" y="7" width="18" height="10" rx="5" stroke-width="1.8" stroke="currentColor" fill="none" />` }
                ]
            },
            {
                name: "Flowchart & Core Shapes",
                shapes: [
                    { value: "circle", name: "Oval / Circle", svg: `<ellipse cx="12" cy="12" rx="9" ry="9" stroke-width="1.8" stroke="currentColor" fill="none" />` },
                    { value: "diamond", name: "Diamond / Rhombus", svg: `<polygon points="12,3 21,12 12,21 3,12" stroke-width="1.8" stroke="currentColor" fill="none" />` },
                    { value: "database", name: "Database / Cylinder", svg: `<path d="M 5,6 L 5,18 C 5,21, 19,21, 19,18 L 19,6 M 5,6 C 5,9, 19,9, 19,6 C 19,3, 5,3, 5,6" stroke-width="1.8" stroke="currentColor" fill="none" />` },
                    { value: "document", name: "Document / Page", svg: `<path d="M 5,3 L 14,3 L 19,8 L 19,21 L 5,21 Z M 14,3 L 14,8 L 19,8" stroke-width="1.8" stroke="currentColor" fill="none" />` },
                    { value: "cloud", name: "Cloud Storage", svg: `<path d="M 6.5,16 A 3.5,3.5 0 0,1 6.5,10 A 4,4 0 0,1 12.5,7.5 A 4.5,4.5 0 0,1 19.5,10.5 A 3.5,3.5 0 0,1 17.5,16 Z" stroke-width="1.8" stroke="currentColor" fill="none" />` },
                    { value: "hexagon", name: "Hexagon", svg: `<polygon points="7,4 17,4 21,12 17,20 7,20 3,12" stroke-width="1.8" stroke="currentColor" fill="none" />` }
                ]
            },
            {
                name: "Basic Shapes",
                shapes: [
                    { value: "triangle", name: "Isosceles Triangle", svg: `<polygon points="12,3 21,20 3,20" stroke-width="1.8" stroke="currentColor" fill="none" />` },
                    { value: "parallelogram", name: "Parallelogram", svg: `<polygon points="7,5 21,5 17,19 3,19" stroke-width="1.8" stroke="currentColor" fill="none" />` },
                    { value: "trapezoid", name: "Trapezoid", svg: `<polygon points="6,5 18,5 21,19 3,19" stroke-width="1.8" stroke="currentColor" fill="none" />` }
                ]
            },
            {
                name: "Lines",
                shapes: [
                    { value: "line", name: "Line", svg: `<line x1="3" y1="12" x2="21" y2="12" stroke-width="2" stroke="currentColor" stroke-linecap="round" />` }
                ]
            },
            {
                name: "Block Arrows",
                shapes: [
                    { value: "arrow-right", name: "Right Arrow", svg: `<polygon points="12,5 20,12 12,19 12,15 4,15 4,9 12,9" stroke-width="1.8" stroke="currentColor" fill="none" />` },
                    { value: "arrow-left", name: "Left Arrow", svg: `<polygon points="12,5 4,12 12,19 12,15 20,15 20,9 12,9" stroke-width="1.8" stroke="currentColor" fill="none" />` },
                    { value: "arrow-up", name: "Up Arrow", svg: `<polygon points="12,4 19,11 15,11 15,19 9,19 9,11 5,11" stroke-width="1.8" stroke="currentColor" fill="none" />` },
                    { value: "arrow-down", name: "Down Arrow", svg: `<polygon points="12,20 5,13 9,13 9,5 15,5 15,13 19,13" stroke-width="1.8" stroke="currentColor" fill="none" />` },
                    { value: "arrow-double-horizontal", name: "Left-Right Arrow", svg: `<polygon points="4,12 8,8 8,10 16,10 16,8 20,12 16,16 16,14 8,14 8,16" stroke-width="1.8" stroke="currentColor" fill="none" />` },
                    { value: "arrow-double-vertical", name: "Up-Down Arrow", svg: `<polygon points="12,4 8,8 10,8 10,16 8,16 12,20 16,16 14,16 14,8 16,8" stroke-width="1.8" stroke="currentColor" fill="none" />` }
                ]
            },
            {
                name: "Equation Shapes",
                shapes: [
                    { value: "plus", name: "Plus Sign", svg: `<path d="M 9 3 L 15 3 L 15 9 L 21 9 L 21 15 L 15 15 L 15 21 L 9 21 L 9 15 L 3 15 L 3 9 L 9 9 Z" stroke-width="1.8" stroke="currentColor" fill="none" />` },
                    { value: "minus", name: "Minus Sign", svg: `<rect x="3" y="10" width="18" height="4" rx="1" stroke-width="1.8" stroke="currentColor" fill="none" />` },
                    { value: "multiply", name: "Multiply Sign", svg: `<path d="M 6 3 L 12 9 L 18 3 L 21 6 L 15 12 L 21 18 L 18 21 L 12 15 L 6 21 L 3 18 L 9 12 L 3 6 Z" stroke-width="1.8" stroke="currentColor" fill="none" />` },
                    { value: "divide", name: "Divide Sign", svg: `<rect x="3" y="10" width="18" height="4" rx="1" stroke-width="1.8" stroke="currentColor" fill="none" /><circle cx="12" cy="5" r="1.5" fill="none" stroke="currentColor" stroke-width="1.8" /><circle cx="12" cy="19" r="1.5" fill="none" stroke="currentColor" stroke-width="1.8" />` }
                ]
            },
            {
                name: "Stars & Banners",
                shapes: [
                    { value: "star", name: "5-Point Star", svg: `<polygon points="12,2 15,9 22,9 17,14 19,21 12,17 5,21 7,14 2,9 9,9" stroke-width="1.8" stroke="currentColor" fill="none" />` }
                ]
            }
        ];

        let html = '<div class="shape-picker-container">';
        categories.forEach(cat => {
            html += `
                <div class="shape-picker-category">
                    <div class="shape-picker-category-title">${cat.name}</div>
                    <div class="shape-picker-grid">
            `;
            cat.shapes.forEach(shape => {
                html += `
                    <div class="shape-item-btn" data-shape-value="${shape.value}" data-shape-name="${shape.name}">
                        <svg viewBox="0 0 24 24">${shape.svg}</svg>
                    </div>
                `;
            });
            html += `
                    </div>
                </div>
            `;
        });
        html += '</div>';

        container.innerHTML = html;

        // Bind click events
        container.querySelectorAll('.shape-item-btn').forEach(btn => {
            btn.addEventListener('click', function () {
                const shapeVal = this.getAttribute('data-shape-value');
                document.getElementById('shapeType').value = shapeVal;
                syncVisualShapePicker();
                
                // Trigger input event to update live preview
                document.getElementById('shapeType').dispatchEvent(new Event('input', { bubbles: true }));
            });
        });
    }

    function resetShapeModal() {
        document.getElementById('shapeBuilderForm').reset();
        document.getElementById('shapeBuilderModalLabel').innerHTML = '<i class="fas fa-shapes me-2"></i> Insert Shape';
        document.getElementById('btnInsertShapeSubmit').innerText = 'Insert Shape';
        
        // Defaults
        document.getElementById('shapeType').value = 'rect';
        syncVisualShapePicker();
        document.getElementById('shapeText').value = '';
        document.getElementById('shapeWidth').value = '150';
        document.getElementById('shapeHeight').value = '80';
        
        updateLivePreview();
    }

    function loadShapeToModal(shapeSvg) {
        document.getElementById('shapeBuilderModalLabel').innerHTML = '<i class="fas fa-edit me-2"></i> Edit Shape';
        document.getElementById('btnInsertShapeSubmit').innerText = 'Update Shape';

        const type = shapeSvg.getAttribute('data-shape-type') || 'rect';
        const text = shapeSvg.getAttribute('data-shape-text') || '';
        const fill = shapeSvg.getAttribute('data-shape-fill') || '#ffffff';
        const border = shapeSvg.getAttribute('data-shape-border') || '#2d3748';
        const thickness = shapeSvg.getAttribute('data-shape-border-thickness') || '3';
        const style = shapeSvg.getAttribute('data-shape-border-style') || 'solid';
        const width = shapeSvg.getAttribute('data-shape-width') || '150';
        const height = shapeSvg.getAttribute('data-shape-height') || '80';
        const textSize = shapeSvg.getAttribute('data-shape-text-size') || '16px';
        const textColor = shapeSvg.getAttribute('data-shape-text-color') || '#2d3748';

        document.getElementById('shapeType').value = type;
        syncVisualShapePicker();
        document.getElementById('shapeText').value = text;
        document.getElementById('shapeFillColor').value = fill;
        document.getElementById('shapeBorderColor').value = border;
        document.getElementById('shapeBorderThickness').value = thickness;
        document.getElementById('shapeBorderStyle').value = style;
        document.getElementById('shapeWidth').value = width;
        document.getElementById('shapeHeight').value = height;
        document.getElementById('shapeFontSize').value = textSize;
        document.getElementById('shapeTextColor').value = textColor;

        updateLivePreview();
    }

    function generateShapeSvgParams(type, text, fill, border, thickness, style, width, height, textSize, textColor) {
        const dashArray = style === 'dashed' ? '6,4' : (style === 'dotted' ? '2,3' : '');
        let svgContent = '';

        // Unique IDs for SVG definitions
        const gradId = 'grad-' + Math.random().toString(36).substr(2, 9);
        const shadowId = 'shadow-' + Math.random().toString(36).substr(2, 9);

        // Core Gradient Palette mapping
        const gradientMap = {
            '#ebf8ff': { start: '#e0f2fe', end: '#bae6fd' }, // Sky Blue
            '#e6fffa': { start: '#f0fdfa', end: '#ccfbf1' }, // Teal
            '#f0fff4': { start: '#f0fdf4', end: '#dcfce7' }, // Emerald/Green
            '#fffaf0': { start: '#fff7ed', end: '#ffedd5' }, // Amber/Orange
            '#fff5f5': { start: '#fef2f2', end: '#fee2e2' }, // Rose/Red
            '#faf5ff': { start: '#faf5ff', end: '#f3e8ff' }, // Lavender/Purple
            '#ffffff': { start: '#ffffff', end: '#f1f5f9' }, // Cool White
            'none': { start: 'transparent', end: 'transparent' }
        };

        const hasFill = fill !== 'none';
        const startColor = gradientMap[fill] ? gradientMap[fill].start : fill;
        const endColor = gradientMap[fill] ? gradientMap[fill].end : fill;

        const defsHtml = `
            <defs>
                <filter id="${shadowId}" x="-20%" y="-20%" width="140%" height="140%">
                    <feDropShadow dx="2" dy="4" stdDeviation="4" flood-color="#334155" flood-opacity="0.12" />
                </filter>
                ${hasFill ? `
                <linearGradient id="${gradId}" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" stop-color="${startColor}" />
                    <stop offset="100%" stop-color="${endColor}" />
                </linearGradient>
                ` : ''}
            </defs>
        `;

        const fillUrl = hasFill ? `url(#${gradId})` : 'transparent';
        const filterAttr = `filter="url(#${shadowId})"`;

        if (type === 'rect') {
            svgContent = `
                <rect x="${thickness/2}" y="${thickness/2}" width="${width - thickness}" height="${height - thickness}" rx="8" ry="8" fill="${fillUrl}" stroke="${border}" stroke-width="${thickness}" stroke-dasharray="${dashArray}" ${filterAttr} />
                <text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" font-size="${textSize}" fill="${textColor}" font-family="'Inter', sans-serif" font-weight="600">${text}</text>
            `;
        } else if (type === 'capsule') {
            const rx = (height - thickness) / 2;
            svgContent = `
                <rect x="${thickness/2}" y="${thickness/2}" width="${width - thickness}" height="${height - thickness}" rx="${rx}" ry="${rx}" fill="${fillUrl}" stroke="${border}" stroke-width="${thickness}" stroke-dasharray="${dashArray}" ${filterAttr} />
                <text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" font-size="${textSize}" fill="${textColor}" font-family="'Inter', sans-serif" font-weight="600">${text}</text>
            `;
        } else if (type === 'circle') {
            const rx = (width - thickness) / 2;
            const ry = (height - thickness) / 2;
            const cx = width / 2;
            const cy = height / 2;
            svgContent = `
                <ellipse cx="${cx}" cy="${cy}" rx="${rx}" ry="${ry}" fill="${fillUrl}" stroke="${border}" stroke-width="${thickness}" stroke-dasharray="${dashArray}" ${filterAttr} />
                <text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" font-size="${textSize}" fill="${textColor}" font-family="'Inter', sans-serif" font-weight="600">${text}</text>
            `;
        } else if (type === 'triangle') {
            svgContent = `
                <polygon points="${width/2},${thickness/2} ${width - thickness/2},${height - thickness/2} ${thickness/2},${height - thickness/2}" fill="${fillUrl}" stroke="${border}" stroke-width="${thickness}" stroke-dasharray="${dashArray}" ${filterAttr} />
                <text x="50%" y="${height * 0.62}" dominant-baseline="middle" text-anchor="middle" font-size="${textSize}" fill="${textColor}" font-family="'Inter', sans-serif" font-weight="600">${text}</text>
            `;
        } else if (type === 'diamond') {
            svgContent = `
                <polygon points="${width/2},${thickness/2} ${width - thickness/2},${height/2} ${width/2},${height - thickness/2} ${thickness/2},${height/2}" fill="${fillUrl}" stroke="${border}" stroke-width="${thickness}" stroke-dasharray="${dashArray}" ${filterAttr} />
                <text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" font-size="${textSize}" fill="${textColor}" font-family="'Inter', sans-serif" font-weight="600">${text}</text>
            `;
        } else if (type === 'pentagon') {
            svgContent = `
                <polygon points="${width/2},${thickness/2} ${width - thickness/2},${height*0.38} ${width*0.81},${height - thickness/2} ${width*0.19},${height - thickness/2} ${thickness/2},${height*0.38}" fill="${fillUrl}" stroke="${border}" stroke-width="${thickness}" stroke-dasharray="${dashArray}" ${filterAttr} />
                <text x="50%" y="55%" dominant-baseline="middle" text-anchor="middle" font-size="${textSize}" fill="${textColor}" font-family="'Inter', sans-serif" font-weight="600">${text}</text>
            `;
        } else if (type === 'database') {
            const ry = height * 0.15;
            svgContent = `
                <path d="M ${thickness/2} ${ry} L ${thickness/2} ${height - ry} C ${thickness/2} ${height}, ${width - thickness/2} ${height}, ${width - thickness/2} ${height - ry} L ${width - thickness/2} ${ry} Z" fill="${fillUrl}" stroke="${border}" stroke-width="${thickness}" ${filterAttr} />
                <ellipse cx="${width/2}" cy="${ry}" rx="${width/2 - thickness/2}" ry="${ry - thickness/2}" fill="${fillUrl}" stroke="${border}" stroke-width="${thickness}" />
                <path d="M ${thickness/2} ${height * 0.4} C ${thickness/2} ${height * 0.4 + ry}, ${width - thickness/2} ${height * 0.4 + ry}, ${width - thickness/2} ${height * 0.4}" fill="none" stroke="${border}" stroke-width="${thickness}" stroke-dasharray="${dashArray}" />
                <path d="M ${thickness/2} ${height * 0.65} C ${thickness/2} ${height * 0.65 + ry}, ${width - thickness/2} ${height * 0.65 + ry}, ${width - thickness/2} ${height * 0.65}" fill="none" stroke="${border}" stroke-width="${thickness}" stroke-dasharray="${dashArray}" />
                <text x="50%" y="${height * 0.55}" dominant-baseline="middle" text-anchor="middle" font-size="${textSize}" fill="${textColor}" font-family="'Inter', sans-serif" font-weight="600">${text}</text>
            `;
        } else if (type === 'document') {
            const fold = Math.min(20, Math.min(width, height) * 0.25);
            svgContent = `
                <path d="M ${thickness/2} ${thickness/2} L ${width - fold} ${thickness/2} L ${width - thickness/2} ${fold} L ${width - thickness/2} ${height - thickness/2} L ${thickness/2} ${height - thickness/2} Z" fill="${fillUrl}" stroke="${border}" stroke-width="${thickness}" ${filterAttr} />
                <path d="M ${width - fold} ${thickness/2} L ${width - fold} ${fold} L ${width - thickness/2} ${fold} Z" fill="${hasFill ? endColor : '#e2e8f0'}" stroke="${border}" stroke-width="${thickness}" />
                <text x="50%" y="55%" dominant-baseline="middle" text-anchor="middle" font-size="${textSize}" fill="${textColor}" font-family="'Inter', sans-serif" font-weight="600">${text}</text>
            `;
        } else if (type === 'cloud') {
            svgContent = `
                <path d="M ${width * 0.25} ${height * 0.8} 
                         A ${width * 0.16} ${height * 0.22} 0 0 1 ${width * 0.15} ${height * 0.5} 
                         A ${width * 0.2} ${height * 0.25} 0 0 1 ${width * 0.45} ${height * 0.25} 
                         A ${width * 0.22} ${height * 0.28} 0 0 1 ${width * 0.8} ${height * 0.4} 
                         A ${width * 0.16} ${height * 0.22} 0 0 1 ${width * 0.75} ${height * 0.8} 
                         Z" fill="${fillUrl}" stroke="${border}" stroke-width="${thickness}" stroke-dasharray="${dashArray}" ${filterAttr} />
                <text x="50%" y="55%" dominant-baseline="middle" text-anchor="middle" font-size="${textSize}" fill="${textColor}" font-family="'Inter', sans-serif" font-weight="600">${text}</text>
            `;
        } else if (type === 'parallelogram') {
            svgContent = `
                <polygon points="${width * 0.2},${thickness/2} ${width - thickness/2},${thickness/2} ${width * 0.8},${height - thickness/2} ${thickness/2},${height - thickness/2}" fill="${fillUrl}" stroke="${border}" stroke-width="${thickness}" stroke-dasharray="${dashArray}" ${filterAttr} />
                <text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" font-size="${textSize}" fill="${textColor}" font-family="'Inter', sans-serif" font-weight="600">${text}</text>
            `;
        } else if (type === 'trapezoid') {
            svgContent = `
                <polygon points="${width * 0.2},${thickness/2} ${width * 0.8},${thickness/2} ${width - thickness/2},${height - thickness/2} ${thickness/2},${height - thickness/2}" fill="${fillUrl}" stroke="${border}" stroke-width="${thickness}" stroke-dasharray="${dashArray}" ${filterAttr} />
                <text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" font-size="${textSize}" fill="${textColor}" font-family="'Inter', sans-serif" font-weight="600">${text}</text>
            `;
        } else if (type === 'hexagon') {
            svgContent = `
                <polygon points="${width * 0.25},${thickness/2} ${width * 0.75},${thickness/2} ${width - thickness/2},${height/2} ${width * 0.75},${height - thickness/2} ${width * 0.25},${height - thickness/2} ${thickness/2},${height/2}" fill="${fillUrl}" stroke="${border}" stroke-width="${thickness}" stroke-dasharray="${dashArray}" ${filterAttr} />
                <text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" font-size="${textSize}" fill="${textColor}" font-family="'Inter', sans-serif" font-weight="600">${text}</text>
            `;
        } else if (type === 'star') {
            svgContent = `
                <polygon points="${width*0.5},0 ${width*0.62},${height*0.35} ${width*0.98},${height*0.35} ${width*0.69},${height*0.57} ${width*0.80},${height*0.91} ${width*0.5},${height*0.72} ${width*0.20},${height*0.91} ${width*0.31},${height*0.57} ${width*0.02},${height*0.35} ${width*0.38},${height*0.35}" fill="${fillUrl}" stroke="${border}" stroke-width="${thickness}" stroke-dasharray="${dashArray}" ${filterAttr} />
                <text x="50%" y="52%" dominant-baseline="middle" text-anchor="middle" font-size="${textSize}" fill="${textColor}" font-family="'Inter', sans-serif" font-weight="600">${text}</text>
            `;
        } else if (type === 'arrow-right' || type === 'arrow') {
            svgContent = `
                <polygon points="${width-thickness/2},${height/2} ${width*0.6},${thickness/2} ${width*0.6},${height*0.25} ${thickness/2},${height*0.25} ${thickness/2},${height*0.75} ${width*0.6},${height*0.75} ${width*0.6},${height-thickness/2}" fill="${fillUrl}" stroke="${border}" stroke-width="${thickness}" stroke-dasharray="${dashArray}" ${filterAttr} />
                <text x="45%" y="50%" dominant-baseline="middle" text-anchor="middle" font-size="${textSize}" fill="${textColor}" font-family="'Inter', sans-serif" font-weight="600">${text}</text>
            `;
        } else if (type === 'arrow-left') {
            svgContent = `
                <polygon points="${thickness/2},${height/2} ${width*0.4},${thickness/2} ${width*0.4},${height*0.25} ${width-thickness/2},${height*0.25} ${width-thickness/2},${height*0.75} ${width*0.4},${height*0.75} ${width*0.4},${height-thickness/2}" fill="${fillUrl}" stroke="${border}" stroke-width="${thickness}" stroke-dasharray="${dashArray}" ${filterAttr} />
                <text x="55%" y="50%" dominant-baseline="middle" text-anchor="middle" font-size="${textSize}" fill="${textColor}" font-family="'Inter', sans-serif" font-weight="600">${text}</text>
            `;
        } else if (type === 'arrow-up') {
            svgContent = `
                <polygon points="${width/2},${thickness/2} ${width-thickness/2},${height*0.4} ${width*0.75},${height*0.4} ${width*0.75},${height-thickness/2} ${width*0.25},${height-thickness/2} ${width*0.25},${height*0.4} ${thickness/2},${height*0.4}" fill="${fillUrl}" stroke="${border}" stroke-width="${thickness}" stroke-dasharray="${dashArray}" ${filterAttr} />
                <text x="50%" y="60%" dominant-baseline="middle" text-anchor="middle" font-size="${textSize}" fill="${textColor}" font-family="'Inter', sans-serif" font-weight="600">${text}</text>
            `;
        } else if (type === 'arrow-down') {
            svgContent = `
                <polygon points="${width/2},${height-thickness/2} ${width-thickness/2},${height*0.6} ${width*0.75},${height*0.6} ${width*0.75},${thickness/2} ${width*0.25},${thickness/2} ${width*0.25},${height*0.6} ${thickness/2},${height*0.6}" fill="${fillUrl}" stroke="${border}" stroke-width="${thickness}" stroke-dasharray="${dashArray}" ${filterAttr} />
                <text x="50%" y="40%" dominant-baseline="middle" text-anchor="middle" font-size="${textSize}" fill="${textColor}" font-family="'Inter', sans-serif" font-weight="600">${text}</text>
            `;
        } else if (type === 'arrow-double-horizontal') {
            svgContent = `
                <polygon points="${thickness/2},${height/2} ${width*0.25},${thickness/2} ${width*0.25},${height*0.25} ${width*0.75},${height*0.25} ${width*0.75},${thickness/2} ${width-thickness/2},${height/2} ${width*0.75},${height-thickness/2} ${width*0.75},${height*0.75} ${width*0.25},${height*0.75} ${width*0.25},${height-thickness/2}" fill="${fillUrl}" stroke="${border}" stroke-width="${thickness}" stroke-dasharray="${dashArray}" ${filterAttr} />
                <text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" font-size="${textSize}" fill="${textColor}" font-family="'Inter', sans-serif" font-weight="600">${text}</text>
            `;
        } else if (type === 'arrow-double-vertical') {
            svgContent = `
                <polygon points="${width/2},${thickness/2} ${width-thickness/2},${height*0.25} ${width*0.75},${height*0.25} ${width*0.75},${height*0.75} ${width-thickness/2},${height*0.75} ${width/2},${height-thickness/2} ${thickness/2},${height*0.75} ${width*0.25},${height*0.75} ${width*0.25},${height*0.25} ${thickness/2},${height*0.25}" fill="${fillUrl}" stroke="${border}" stroke-width="${thickness}" stroke-dasharray="${dashArray}" ${filterAttr} />
                <text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" font-size="${textSize}" fill="${textColor}" font-family="'Inter', sans-serif" font-weight="600">${text}</text>
            `;
        } else if (type === 'plus') {
            svgContent = `
                <path d="M ${width*0.38} ${thickness/2} L ${width*0.62} ${thickness/2} L ${width*0.62} ${height*0.35} L ${width-thickness/2} ${height*0.35} L ${width-thickness/2} ${height*0.65} L ${width*0.62} ${height*0.65} L ${width*0.62} ${height-thickness/2} L ${width*0.38} ${height-thickness/2} L ${width*0.38} ${height*0.65} L ${thickness/2} ${height*0.65} L ${thickness/2} ${height*0.35} L ${width*0.38} ${height*0.35} Z" fill="${fillUrl}" stroke="${border}" stroke-width="${thickness}" stroke-dasharray="${dashArray}" ${filterAttr} />
                <text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" font-size="${textSize}" fill="${textColor}" font-family="'Inter', sans-serif" font-weight="600">${text}</text>
            `;
        } else if (type === 'minus') {
            svgContent = `
                <rect x="${thickness/2}" y="${height*0.35}" width="${width - thickness}" height="${height*0.3}" rx="4" fill="${fillUrl}" stroke="${border}" stroke-width="${thickness}" stroke-dasharray="${dashArray}" ${filterAttr} />
                <text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" font-size="${textSize}" fill="${textColor}" font-family="'Inter', sans-serif" font-weight="600">${text}</text>
            `;
        } else if (type === 'multiply') {
            svgContent = `
                <path d="M ${width*0.2} ${thickness/2} L ${width*0.5} ${height*0.35} L ${width*0.8} ${thickness/2} L ${width-thickness/2} ${height*0.2} L ${width*0.65} ${height*0.5} L ${width-thickness/2} ${height*0.8} L ${width*0.8} ${height-thickness/2} L ${width*0.5} ${height*0.65} L ${width*0.2} ${height-thickness/2} L ${thickness/2} ${height*0.8} L ${width*0.35} ${height*0.5} L ${thickness/2} ${height*0.2} Z" fill="${fillUrl}" stroke="${border}" stroke-width="${thickness}" stroke-dasharray="${dashArray}" ${filterAttr} />
                <text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" font-size="${textSize}" fill="${textColor}" font-family="'Inter', sans-serif" font-weight="600">${text}</text>
            `;
        } else if (type === 'divide') {
            svgContent = `
                <rect x="${thickness/2}" y="${height*0.4}" width="${width - thickness}" height="${height*0.2}" rx="3" fill="${fillUrl}" stroke="${border}" stroke-width="${thickness}" stroke-dasharray="${dashArray}" ${filterAttr} />
                <circle cx="${width/2}" cy="${height*0.2}" r="${height*0.08}" fill="${border}" />
                <circle cx="${width/2}" cy="${height*0.8}" r="${height*0.08}" fill="${border}" />
                <text x="25%" y="50%" dominant-baseline="middle" text-anchor="middle" font-size="${textSize}" fill="${textColor}" font-family="'Inter', sans-serif" font-weight="600">${text}</text>
            `;
        } else if (type === 'line') {
            svgContent = `
                <line x1="5" y1="${height/2}" x2="${width - 5}" y2="${height/2}" stroke="${border}" stroke-width="${thickness}" stroke-dasharray="${dashArray}" ${filterAttr} />
                <text x="50%" y="${height/2 - 10}" dominant-baseline="auto" text-anchor="middle" font-size="${textSize}" fill="${textColor}" font-family="'Inter', sans-serif" font-weight="600">${text}</text>
            `;
        }

        return `<svg class="inserted-shape" data-shape-type="${type}" data-shape-text="${text}" data-shape-fill="${fill}" data-shape-border="${border}" data-shape-border-thickness="${thickness}" data-shape-border-style="${style}" data-shape-width="${width}" data-shape-height="${height}" data-shape-text-size="${textSize}" data-shape-text-color="${textColor}" style="width: ${width}px; height: ${height}px; vertical-align: middle; margin: 5px;" viewBox="0 0 ${width} ${height}" xmlns="http://www.w3.org/2000/svg">${defsHtml}${svgContent}</svg>`;
    }

    function generateShapeSvg() {
        const type = document.getElementById('shapeType')?.value || 'rect';
        const text = document.getElementById('shapeText')?.value || '';
        const fill = document.getElementById('shapeFillColor')?.value || '#ffffff';
        const border = document.getElementById('shapeBorderColor')?.value || '#000000';
        const thickness = parseInt(document.getElementById('shapeBorderThickness')?.value) || 2;
        const style = document.getElementById('shapeBorderStyle')?.value || 'solid';
        const width = parseInt(document.getElementById('shapeWidth')?.value) || 150;
        const height = parseInt(document.getElementById('shapeHeight')?.value) || 80;
        const textSize = document.getElementById('shapeFontSize')?.value || '14px';
        const textColor = document.getElementById('shapeTextColor')?.value || '#000000';

        return generateShapeSvgParams(type, text, fill, border, thickness, style, width, height, textSize, textColor);
    }

    function updateLivePreview() {
        const previewContainer = document.getElementById('shapeLivePreviewContainer');
        if (previewContainer) {
            previewContainer.innerHTML = generateShapeSvg();
        }
    }

    // --- Bootstrapping Shape Modal Events ---
    document.addEventListener("DOMContentLoaded", function () {
        const formElements = ['shapeType', 'shapeText', 'shapeFillColor', 'shapeBorderColor', 'shapeBorderThickness', 'shapeBorderStyle', 'shapeFontSize', 'shapeTextColor', 'shapeWidth', 'shapeHeight'];
        formElements.forEach(id => {
            const el = document.getElementById(id);
            if (el) el.addEventListener('input', updateLivePreview);
        });

        // Insert / Update Shape Submit Click
        const btnSubmit = document.getElementById('btnInsertShapeSubmit');
        if (btnSubmit) {
            btnSubmit.addEventListener('click', function () {
                const newSvg = generateShapeSvg();
                if (currentEditingEditor) {
                    if (currentEditingShape) {
                        // Edit Mode: Replace old element
                        currentEditingShape.outerHTML = newSvg;
                    } else {
                        // Insert Mode
                        currentEditingEditor.insertContent(newSvg);
                    }
                    // Trigger editor change to sync to textarea
                    currentEditingEditor.fire('change');
                }

                // Close Modal
                const modalEl = document.getElementById('shapeBuilderModal');
                const modal = bootstrap.Modal.getInstance(modalEl);
                if (modal) modal.hide();
            });
        }
    });

    // --- Backwards Compatibility for Old Teacher JSON evaluation submissions ---

    function renderTeacherSubmissions() {
        document.querySelectorAll('.student-answer-box').forEach(box => {
            if (box.getAttribute('data-rich-rendered') === 'true') return;
            box.setAttribute('data-rich-rendered', 'true');

            const content = box.textContent.trim();
            if (!content.startsWith('{"type":"rich"')) return; // Fallback to raw legacy HTML/plain text answers

            try {
                const richData = JSON.parse(content);
                box.innerHTML = ''; // Clear raw JSON text
                box.className += ' canvas-eval-preview';

                // 1. Text Answer Panel
                const textSection = document.createElement('div');
                textSection.className = 'eval-text-answer-section';
                textSection.innerHTML = `
                    <div class="eval-text-answer-title"><i class="fas fa-file-alt"></i> Text Answer Response</div>
                    <div class="eval-text-answer-content">${richData.text || '<span class="text-muted italic">No written text response submitted.</span>'}</div>
                `;
                box.appendChild(textSection);

                // 2. Canvas Drawings preview
                if (richData.shapes && richData.shapes.length > 0) {
                    const canvasSection = document.createElement('div');
                    canvasSection.className = 'eval-canvas-section';
                    
                    let canvasHtml = `
                        <div class="eval-canvas-title"><i class="fas fa-paint-brush"></i> Drawing Canvas Drawings</div>
                        <div class="eval-canvas-container">
                            <svg style="position: absolute; width: 0; height: 0;">
                                <defs>
                                    <marker id="arrow-head" viewBox="0 0 10 10" refX="6" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
                                        <path d="M 0 1.5 L 10 5 L 0 8.5 z" fill="currentColor" />
                                    </marker>
                                </defs>
                            </svg>
                            <div class="canvas-elements-container" style="position: absolute; width:100%; height:100%;">
                    `;

                    richData.shapes.forEach(shape => {
                        const shapeDiv = renderReadOnlyShape(shape);
                        canvasHtml += shapeDiv.outerHTML;
                    });

                    canvasHtml += `
                            </div>
                        </div>
                    `;
                    canvasSection.innerHTML = canvasHtml;
                    box.appendChild(canvasSection);
                }

                // 3. Comparison Table preview
                if (richData.table && richData.table.rows && richData.table.rows.length > 0) {
                    const tableSection = document.createElement('div');
                    tableSection.className = 'eval-table-section';
                    
                    let tableHtml = `
                        <div class="eval-table-title"><i class="fas fa-balance-scale"></i> Comparison Table</div>
                        <table class="eval-comparison-table">
                            <thead>
                                <tr>
                                    <th>Differences Between ${richData.table.concept1 || 'Concept A'}</th>
                                    <th>And ${richData.table.concept2 || 'Concept B'}</th>
                                </tr>
                            </thead>
                            <tbody>
                    `;

                    richData.table.rows.forEach(row => {
                        tableHtml += `
                            <tr>
                                <td>${row.col1 || ''}</td>
                                <td>${row.col2 || ''}</td>
                            </tr>
                        `;
                    });

                    tableHtml += `
                            </tbody>
                        </table>
                    `;
                    tableSection.innerHTML = tableHtml;
                    box.appendChild(tableSection);
                }

            } catch (e) {
                console.error("Evaluation Parse failed", e);
            }
        });
    }

    function renderReadOnlyShape(shape) {
        const div = document.createElement('div');
        div.className = 'canvas-element';
        div.style.left = shape.x + 'px';
        div.style.top = shape.y + 'px';
        div.style.width = shape.width + 'px';
        div.style.height = shape.height + 'px';
        div.style.transform = `rotate(${shape.rotation}deg)`;
        div.style.zIndex = shape.style.zIndex || 10;
        div.style.cursor = 'default';
        
        const thicknessMap = { 'thin': '1px', 'medium': '3px', 'thick': '6px', 'none': '0px' };
        const thicknessVal = thicknessMap[shape.style.strokeWidth || 'thin'];
        const styleVal = shape.style.strokeStyle === 'none' ? 'none' : (shape.style.strokeStyle || 'solid');
        const dashMap = { 'solid': 'none', 'dashed': '6,4', 'dotted': '2,3' };
        const strokeDash = dashMap[shape.style.strokeStyle || 'solid'];
        
        const color = shape.style.strokeColor || '#0f172a';
        const fill = shape.style.fillColor || '#ffffff';
        const opacity = (shape.style.opacity !== undefined ? shape.style.opacity : 100) / 100;
        
        div.style.filter = shape.style.shadow ? 'drop-shadow(2px 3px 4px rgba(0,0,0,0.1))' : 'none';
        
        if (shape.type === 'rect') {
            div.style.border = `${thicknessVal} ${styleVal} ${color}`;
            div.style.backgroundColor = fill;
            div.style.opacity = opacity;
            div.style.borderRadius = '4px';
        } else if (shape.type === 'circle') {
            div.style.border = `${thicknessVal} ${styleVal} ${color}`;
            div.style.backgroundColor = fill;
            div.style.opacity = opacity;
            div.style.borderRadius = '50%';
        } else if (shape.type === 'text') {
            div.style.border = 'none';
            div.style.backgroundColor = 'transparent';
            div.style.opacity = opacity;
        } else {
            const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
            svg.setAttribute('width', '100%');
            svg.setAttribute('height', '100%');
            svg.style.opacity = opacity;
            svg.style.color = color;
            
            const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
            path.setAttribute('stroke', color);
            path.setAttribute('stroke-width', thicknessVal === '0px' ? '0' : parseFloat(thicknessVal));
            if (strokeDash !== 'none') path.setAttribute('stroke-dasharray', strokeDash);
            
            const isOutline = shape.type.startsWith('line') || shape.type.startsWith('arrow') || shape.type === 'pencil';
            path.setAttribute('fill', isOutline ? 'none' : fill);
            
            const w = shape.width;
            const h = shape.height;
            let d = '';
            
            if (shape.type === 'triangle') {
                d = `M ${w / 2} 0 L ${w} ${h} L 0 ${h} Z`;
            } else if (shape.type === 'diamond') {
                d = `M ${w / 2} 0 L ${w} ${h / 2} L ${w / 2} ${h} L 0 ${h / 2} Z`;
            } else if (shape.type === 'hexagon') {
                d = `M ${w * 0.25} 0 L ${w * 0.75} 0 L ${w} ${h / 2} L ${w * 0.75} ${h} L ${w * 0.25} ${h} L 0 ${h / 2} Z`;
            } else if (shape.type === 'star') {
                const pts = [
                    { x: w * 0.5, y: 0 },
                    { x: w * 0.61, y: h * 0.35 },
                    { x: w * 0.98, y: h * 0.35 },
                    { x: w * 0.68, y: h * 0.57 },
                    { x: w * 0.79, y: h * 0.91 },
                    { x: w * 0.5, y: h * 0.70 },
                    { x: w * 0.21, y: h * 0.91 },
                    { x: w * 0.32, y: h * 0.57 },
                    { x: w * 0.02, y: h * 0.35 },
                    { x: w * 0.39, y: h * 0.35 }
                ];
                d = `M ${pts[0].x} ${pts[0].y} ` + pts.slice(1).map(p => `L ${p.x} ${p.y}`).join(' ') + ' Z';
            } else if (shape.type === 'line') {
                const x1 = shape.flipX ? w : 0;
                const y1 = shape.flipY ? h : 0;
                const x2 = shape.flipX ? 0 : w;
                const y2 = shape.flipY ? 0 : h;
                d = `M ${x1} ${y1} L ${x2} ${y2}`;
            } else if (shape.type === 'arrow-single') {
                const x1 = shape.flipX ? w : 0;
                const y1 = shape.flipY ? h : 0;
                const x2 = shape.flipX ? 0 : w;
                const y2 = shape.flipY ? 0 : h;
                d = `M ${x1} ${y1} L ${x2} ${y2}`;
                path.setAttribute('marker-end', 'url(#arrow-head)');
            } else if (shape.type === 'arrow-double') {
                const x1 = shape.flipX ? w : 0;
                const y1 = shape.flipY ? h : 0;
                const x2 = shape.flipX ? 0 : w;
                const y2 = shape.flipY ? 0 : h;
                d = `M ${x1} ${y1} L ${x2} ${y2}`;
                path.setAttribute('marker-start', 'url(#arrow-head)');
                path.setAttribute('marker-end', 'url(#arrow-head)');
            } else if (shape.type === 'pencil') {
                if (shape.points && shape.points.length > 0) {
                    d = `M ${shape.points[0].x} ${shape.points[0].y} ` + shape.points.slice(1).map(p => `L ${p.x} ${p.y}`).join(' ');
                }
            }
            
            path.setAttribute('d', d);
            svg.appendChild(path);
            div.appendChild(svg);
        }
        
        const textContainer = document.createElement('div');
        textContainer.className = 'canvas-element-text';
        textContainer.textContent = shape.text || "";
        
        const fontSizeMap = { 'small': '12px', 'medium': '16px', 'large': '24px' };
        textContainer.style.fontSize = fontSizeMap[shape.fontStyle.size || 'medium'];
        textContainer.style.fontWeight = shape.fontStyle.bold ? 'bold' : 'normal';
        textContainer.style.fontStyle = shape.fontStyle.italic ? 'italic' : 'normal';
        textContainer.style.textDecoration = shape.fontStyle.underline ? 'underline' : 'none';
        textContainer.style.color = shape.fontStyle.color || '#0f172a';
        textContainer.style.textAlign = shape.fontStyle.align || 'center';
        textContainer.style.justifyContent = shape.fontStyle.align === 'left' ? 'flex-start' : (shape.fontStyle.align === 'right' ? 'flex-end' : 'center');
        
        div.appendChild(textContainer);
        return div;
    }

    // Expose functions globally for execution
    window.initializeRichEditors = initializeRichEditors;
    window.renderTeacherSubmissions = renderTeacherSubmissions;
 
    // Bootstrap editor and teacher preview
    document.addEventListener("DOMContentLoaded", function () {
        // Dynamic MS Word-Equivalent Shapes Selector Upgrade
        const shapeSelect = document.getElementById('shapeType');
        if (shapeSelect && shapeSelect.tagName === 'SELECT') {
            // Create a hidden input to keep state
            const hiddenInput = document.createElement('input');
            hiddenInput.type = 'hidden';
            hiddenInput.id = 'shapeType';
            hiddenInput.name = 'shapeType';
            hiddenInput.value = shapeSelect.value || 'rect';

            // Find parent and replace the select with the hidden input
            const parent = shapeSelect.parentNode;
            parent.replaceChild(hiddenInput, shapeSelect);

            // Change the col-sm-6 div to col-12 for full width
            const colDiv = parent.closest('.col-sm-6');
            if (colDiv) {
                colDiv.className = 'col-12 mb-3';
                
                // Change the label text to something premium
                const label = colDiv.querySelector('label');
                if (label) {
                    label.innerHTML = '<i class="fas fa-shapes me-1 text-primary"></i> Shape Type';
                }
            }

            // Create a container for the visual shape picker
            const pickerWrapper = document.createElement('div');
            pickerWrapper.id = 'visualShapePickerWrapper';
            pickerWrapper.className = 'mt-2';
            parent.appendChild(pickerWrapper);

            // Build the visual picker
            buildVisualShapePicker(pickerWrapper);

            // Synchronize the active state
            syncVisualShapePicker();

            // Initial render delay to build preview
            setTimeout(() => {
                if (typeof updateLivePreview === 'function') {
                    updateLivePreview();
                }
            }, 100);
        }

        initializeRichEditors();
        renderTeacherSubmissions();

        // Trigger drawing whiteboard tools initialization
        if (typeof window.initializeDrawingTool === 'function') {
            const attemptId = document.getElementById('attemptId')?.value;
            const submissionId = document.getElementById('submissionId')?.value;
            const type = document.getElementById('type')?.value;
            
            let finalAttemptId = attemptId;
            let finalSubmissionId = submissionId;
            if (type === 'paper' && attemptId) {
                finalSubmissionId = attemptId;
                finalAttemptId = null;
            }
            window.initializeDrawingTool(finalAttemptId, finalSubmissionId);
        }

        // Anti-Cheat: Disable right-click globally on all active student portal pages
        const studentPaths = ['/dashboard', '/rules', '/start', '/paper-rules', '/confirm-paper', '/start-paper', '/section', '/feedback', '/result-summary', '/terminated'];
        const currentPath = window.location.pathname;
        const isStudentPage = studentPaths.some(path => currentPath === path || currentPath.startsWith(path + '/'));

        if (isStudentPage && !currentPath.startsWith('/admin')) {
            document.addEventListener('contextmenu', event => {
                if (event.target.closest("textarea.answer-area")) {
                    return; // Allow context menu on answer fields for Copy/Paste
                }
                event.preventDefault();
                alert("Right-click context menu is disabled during the exam.");
            });
        }
    });
})();
