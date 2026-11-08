/* Glacier mod menu: tab router.
 *
 * Page renderers are registered by the page scripts loaded before this file:
 *   window.pages.modules = function (host) { host.innerHTML = '...'; ... };
 * Each tab owns a host element (#pg-<tab>). A renderer runs once, the first time its tab is opened, and then
 * stays in the DOM: switching tabs only shows or hides hosts. A renderer may also define:
 *   renderer.enter(host)  called every time its tab is shown (claim keys, start timers)
 *   renderer.leave(host)  called every time another tab is shown (stop timers)
 * window.menu.render() rebuilds the current tab from its renderer (keeps the host's scroll position).
 *
 * Helpers for renderers:
 *   window.menu.cols(n)     -> grid column count: n, or 2 on a narrow viewport
 *   window.menu.fa(code)    -> '<i class="fa">glyph</i>' for a hex codepoint like 'f1b2'
 *   window.menu.esc(text)   -> HTML-escaped text
 *   window.menu.show(id)    -> switch tab (modules, elements, editors, music, cosmetics, performance)
 *   window.menu.render()    -> rebuild the current page
 *   window.menu.tab()       -> current tab id
 */
(function () {
	var TABS = [
		{ id: 'modules', label: 'Modules', icon: 'f009' },
		{ id: 'elements', label: 'Elements', icon: 'f0c9' },
		{ id: 'editors', label: 'Editors', icon: 'f044' },
		{ id: 'music', label: 'Music', icon: 'f001' },
		{ id: 'cosmetics', label: 'Cosmetics', icon: 'f553' },
		{ id: 'performance', label: 'Performance', icon: 'f3fd' }
	];
	var DEFAULT_TAB = 'modules';
	var STORE_KEY = 'glacier.menu.tab';

	var tabsEl = document.getElementById('tabs');
	var pageEl = document.getElementById('page');
	var closeEl = document.getElementById('close');
	var current = null;
	/* tab id -> true once its renderer has run (lazy: a tab that is never opened is never rendered) */
	var built = {};

	function fa(code) {
		return '<i class="fa">' + String.fromCharCode(parseInt(code, 16)) + '</i>';
	}

	function esc(text) {
		return String(text === undefined || text === null ? '' : text)
			.split('&').join('&amp;')
			.split('<').join('&lt;')
			.split('>').join('&gt;')
			.split('"').join('&quot;')
			.split("'").join('&#39;');
	}

	function findTab(id) {
		for (var i = 0; i < TABS.length; i++) {
			if (TABS[i].id === id) return TABS[i];
		}
		return null;
	}

	/* Last tab survives reopening only where localStorage exists; the engine may not provide it. */
	function remembered() {
		try {
			if (typeof localStorage !== 'undefined' && localStorage) {
				return localStorage.getItem(STORE_KEY);
			}
		} catch (e) { /* unavailable */ }
		return null;
	}

	function remember(id) {
		try {
			if (typeof localStorage !== 'undefined' && localStorage) {
				localStorage.setItem(STORE_KEY, id);
			}
		} catch (e) { /* unavailable */ }
	}

	/* One click handler on the tab strip (event delegation); the buttons are built once. */
	function buildTabs() {
		var html = '';
		for (var i = 0; i < TABS.length; i++) {
			var t = TABS[i];
			html += '<button class="tab" data-tab="' + t.id + '">' + fa(t.icon) + '<span>' + t.label + '</span></button>';
		}
		tabsEl.innerHTML = html;
		tabsEl.onclick = function (e) {
			var t = e.target;
			while (t && !(t.dataset && t.dataset.tab)) t = t.parentElement;
			if (t) show(t.dataset.tab);
		};
	}

	function markTabs() {
		var buttons = tabsEl.querySelectorAll('.tab');
		for (var i = 0; i < buttons.length; i++) {
			if (buttons[i].dataset.tab === current) {
				buttons[i].classList.add('active');
			} else {
				buttons[i].classList.remove('active');
			}
		}
	}

	/* One host per tab, all hidden until shown. Built in one innerHTML call. */
	function buildHosts() {
		var h = '';
		for (var i = 0; i < TABS.length; i++) {
			h += '<div class="pg-host hide" id="pg-' + TABS[i].id + '"></div>';
		}
		pageEl.innerHTML = h;
	}

	function hostOf(id) {
		return id ? document.getElementById('pg-' + id) : null;
	}

	function placeholder(message) {
		return '<div class="empty">' + fa('f110') + '<div class="empty-title">' + esc(message) + '</div></div>';
	}

	function report(id, what, e) {
		if (typeof console !== 'undefined' && console) console.error('[menu] ' + id + ' ' + what + ' failed: ' + e);
	}

	/* Runs a page hook (enter or leave) when the page defines it. */
	function hook(id, name) {
		var r = window.pages ? window.pages[id] : null;
		var host = hostOf(id);
		if (typeof r !== 'function' || typeof r[name] !== 'function' || !host) return;
		try {
			r[name](host);
		} catch (e) {
			report(id, name, e);
		}
	}

	/* Rebuilds one tab's host from its renderer. The scroll position survives a rebuild. */
	function renderInto(id) {
		var host = hostOf(id);
		if (!host) return;
		var keep = 0;
		try { keep = host.scrollTop || 0; } catch (e) { keep = 0; }

		var renderer = window.pages ? window.pages[id] : null;
		host.innerHTML = '';
		if (typeof renderer !== 'function') {
			host.innerHTML = placeholder(findTab(id).label + ' is not loaded');
		} else {
			try {
				renderer(host);
			} catch (e) {
				host.innerHTML = '<div class="error">' + esc(findTab(id).label + ' failed: ' + e) + '</div>';
			}
		}

		try { host.scrollTop = keep; } catch (e) { /* engine without scrollTop */ }
	}

	function render() {
		if (current) renderInto(current);
	}

	function show(id) {
		var tab = findTab(id) ? id : DEFAULT_TAB;
		if (current && sheetEl) closeSheet();
		if (tab === current) return;

		var prev = current;
		if (prev) {
			hook(prev, 'leave');
			var prevHost = hostOf(prev);
			if (prevHost) prevHost.classList.add('hide');
		}
		keyHook = null;
		current = tab;
		remember(tab);
		markTabs();

		var host = hostOf(tab);
		if (host) host.classList.remove('hide');
		if (!built[tab]) {
			built[tab] = true;
			renderInto(tab);
		}
		hook(tab, 'enter');
	}

	/* Narrow-viewport classes. The logical viewport comes from glacier.viewport() (0 before the first frame),
	 * so it is polled twice a second; the page only re-renders when the grid column count changes. */
	var appEl = document.getElementById('app');
	var panelEl = appEl.querySelector('.panel');
	var size = '';
	var lastW = 0, lastH = 0;
	function checkViewport() {
		var w = 0, h = 0;
		try { var v = glacier.viewport(); w = v.w; h = v.h; } catch (e) { w = 0; }
		if (!w || (w === lastW && h === lastH)) return;
		lastW = w;
		lastH = h;
		panelEl.style.width = Math.min(960, w) + 'px';
		panelEl.style.height = Math.min(600, h) + 'px';
		var next = w < 420 ? 'tiny' : (w < 760 ? 'compact' : '');
		if (next === size) return;
		var wasNarrow = size !== '';
		size = next;
		if (next) appEl.classList.add('compact'); else appEl.classList.remove('compact');
		if (next === 'tiny') appEl.classList.add('tiny'); else appEl.classList.remove('tiny');
		if (wasNarrow !== (next !== '') && current) render();
	}

	closeEl.addEventListener('click', function () {
		glacier.close();
	});

	/* Settings sheet over the whole panel: a dimmed scrim (click closes) and a right panel the page fills. */
	var sheetEl = document.getElementById('sheet');
	var spEl = document.getElementById('sp');
	var sheetClose = null;

	function openSheet(onClose) {
		sheetClose = onClose || null;
		sheetEl.style.display = 'block';
		return spEl;
	}

	function closeSheet() {
		if (sheetEl.style.display === 'none') return;
		sheetEl.style.display = 'none';
		spEl.innerHTML = '';
		var cb = sheetClose;
		sheetClose = null;
		if (cb) cb();
	}

	sheetEl.addEventListener('click', function (e) {
		if (e.target && e.target.dataset && e.target.dataset.act === 'close-sheet') closeSheet();
	});

	/* Keys: WebScreen sends keydown (detail key = GLFW code) and keypress (detail char) here. A page can claim
	 * keys with menu.onKey(fn); fn returns true when it used the key. Esc closes an open sheet before the screen. */
	var KEY_ESC = 256;
	var keyHook = null;
	function claim(e) {
		e.preventDefault();
		glacier.keyHandled();
	}
	var docEl = document.documentElement || appEl;
	docEl.addEventListener('keydown', function (e) {
		if (keyHook && keyHook(e.key, null)) { claim(e); return; }
		if (e.key === KEY_ESC && sheetEl.style.display !== 'none') { closeSheet(); claim(e); }
	});
	docEl.addEventListener('keypress', function (e) {
		if (keyHook && keyHook(0, e['char'])) claim(e);
	});

	window.menu = {
		openSheet: openSheet,
		closeSheet: closeSheet,
		onKey: function (fn) { keyHook = fn; },
		fa: fa,
		esc: esc,
		cols: function (wide) { return size ? 2 : wide; },
		show: show,
		render: render,
		tab: function () { return current; }
	};

	buildTabs();
	buildHosts();
	checkViewport();
	show(remembered() || DEFAULT_TAB);
	/* 500 ms is plenty for a viewport change; the call is one JS-to-Java read and returns early when nothing moved. */
	setInterval(checkViewport, 500);
})();
