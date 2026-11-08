// Modules page: toolbar (search, category chips, enabled counter), a scrolling 3-column grid of module cards,
// and a settings sheet that slides in from the right when a card's gear is pressed.
// Cost model: the cards are built once per page build. Search and category changes only toggle a 'hide' class on
// the existing cards (no HTML is rebuilt per keystroke). A toggle patches that card's own nodes. The sheet keeps the
// settings of the open module and re-renders only the row that changed, through that row's own wrapper element.
(function () {
	window.pages = window.pages || {};

	var CATS = ['All', 'HUD', 'Render', 'Util'];
	// Util has no category of its own: it groups everything that is not HUD or Render.
	var UTIL = { 'Misc': 1, 'Performance': 1 };
	var PALETTE = ['#7289da', '#6b7fe0', '#43b581', '#faa61a', '#f04747', '#ffffff',
		'#99aab5', '#eb459e', '#00b0f4', '#f1c40f', '#2ecc71', '#9b59b6'];
	var DEFAULT_ICON = 'f085';
	// Font Awesome solid glyph per module, keyed by the lowercased alphanumeric name.
	var ICONS = { fpscounter: 'f625', pingcounter: 'f012', coordinates: 'f3c5', directionhud: 'f14e',
		speedometer: 'f625', armorhud: 'f132', keystrokes: 'f11c', fullbright: 'f185', performancemods: 'f2db',
		clock: 'f017', daycounter: 'f186', serverdisplay: 'f233', memoryusage: 'f538', lowdurability: 'f071',
		walkdistance: 'f554', mainhandhud: 'f256', timerhud: 'f2f2', deathcoordinates: 'f54c', zoom: 'f00e',
		nohurtcam: 'f06e', particlelimiter: 'f0d0', menufpslimit: 'f0e4', glacierplayers: 'f007',
		elements: 'f0c9', editors: 'f044', cosmetics: 'f553',
		reachdisplay: 'f545', lightlevel: 'f0eb', falldistance: 'f063', blockinfo: 'f1b2', playersonline: 'f0c0',
		entitycount: 'f1b0', weather: 'f0c2', hitcombo: 'f0e7', verticalspeed: 'f062', airsupply: 'f043',
		attackcooldown: 'f05b', health: 'f004', fpsstats: 'f080', gamemode: 'f11b', potiontimers: 'f0c3',
		autorespawn: 'f021' };
	// Sheet sections, in pack order. Modules with fewer settings than GROUP_MIN show one flat list.
	var GROUP_ORDER = ['General', 'Layout', 'Opacity', 'Elements', 'Colors'];
	var GROUP_MIN = 6;
	var KEY = { ESC: 256, ENTER: 257, BACKSPACE: 259, DELETE: 261 };
	var CHEVRON = '<i class="fa sel-chev">&#xf078;</i>';

	// cat/q: filter; cards: one entry per module card; open: module id in the sheet; list: its settings (JS objects,
	// values kept current); entries: key -> setting; sel: key of the expanded choice row; cap: capturing a keybind.
	var st = {
		root: null, cat: 'All', q: '', typing: false,
		mods: [], cards: [], byId: {},
		searchEl: null, qEl: null, noneEl: null, countEl: null,
		open: null, sel: null, cap: false, list: [], entries: {}, sp: null
	};

	function esc(s) {
		return String(s).split('&').join('&amp;').split('<').join('&lt;')
			.split('>').join('&gt;').split('"').join('&quot;');
	}

	function glyph(m) {
		var slug = String(m.name).toLowerCase().replace(/[^a-z0-9]/g, '');
		return '&#x' + (ICONS[slug] || DEFAULT_ICON) + ';';
	}

	function fmt(v) {
		var n = Number(v);
		if (isNaN(n)) return '';
		return String(Math.round(n * 100) / 100);
	}

	function actOf(t) {
		while (t && !(t.dataset && t.dataset.act)) t = t.parentElement;
		return t;
	}

	function inCat(m) {
		if (st.cat === 'All') return true;
		if (st.cat === 'Util') return !!UTIL[m.category];
		return m.category === st.cat;
	}

	/* ------------------------------------------------------------- grid */

	// Seven nodes per card: the dot and gear are absolutely placed, so there is no header row wrapper.
	function cardHtml(m) {
		var on = m.enabled;
		var id = esc(m.id);
		return '<div class="mc' + (on ? ' on' : '') + '" id="m-' + id + '" data-act="card" data-id="' + id + '">'
			+ '<span class="dot' + (on ? ' on' : '') + '" id="md-' + id + '"></span>'
			+ '<button class="mc-gear" data-act="gear" data-id="' + id + '"><i class="fa">&#xf013;</i></button>'
			+ '<i class="fa mc-ico">' + glyph(m) + '</i>'
			+ '<div class="mc-name">' + esc(m.name) + '</div>'
			+ '<div class="mc-state" id="ms-' + id + '">' + (on ? 'ENABLED' : 'DISABLED') + '</div>'
			+ '</div>';
	}

	// Shows the cards that match the category and search text, by toggling their 'hide' class.
	function applyFilter() {
		var q = st.q.toLowerCase();
		var any = false;
		for (var i = 0; i < st.cards.length; i++) {
			var c = st.cards[i];
			var show = inCat(c.mod) && (!q || c.lower.indexOf(q) >= 0);
			if (show) any = true;
			if (show === c.shown) continue;
			c.shown = show;
			if (show) c.el.classList.remove('hide'); else c.el.classList.add('hide');
		}
		if (st.noneEl) {
			if (any) st.noneEl.classList.add('hide'); else st.noneEl.classList.remove('hide');
		}
	}

	function renderSearch() {
		if (!st.searchEl || !st.qEl) return;
		if (st.typing) st.searchEl.classList.add('focus'); else st.searchEl.classList.remove('focus');
		if (st.q) st.qEl.classList.remove('ph'); else st.qEl.classList.add('ph');
		st.qEl.textContent = st.q ? st.q + (st.typing ? '|' : '') : (st.typing ? '|' : 'Search modules...');
	}

	function recount() {
		var n = 0;
		for (var i = 0; i < st.cards.length; i++) if (st.cards[i].mod.enabled) n++;
		if (st.countEl) st.countEl.textContent = n + ' enabled';
	}

	function shell() {
		var chips = '';
		for (var i = 0; i < CATS.length; i++) {
			chips += '<button class="mchip' + (CATS[i] === st.cat ? ' on' : '') + '" data-act="cat" data-val="'
				+ CATS[i] + '">' + CATS[i] + '</button>';
		}
		var cards = '';
		for (var j = 0; j < st.mods.length; j++) cards += cardHtml(st.mods[j]);
		return '<div class="mp">'
			+ '<div class="mtool"><div class="msearch" id="msearch" data-act="search">' + '<i class="fa">&#xf002;</i>'
			+ '<span class="mq ph" id="mq">Search modules...</span></div>'
			+ '<div class="mchips">' + chips + '</div><span class="grow"></span>'
			+ '<span class="mcount" id="mcount"></span></div>'
			+ '<div class="mscroll"><div class="mgrid" id="mgrid">' + cards + '</div>'
			+ '<div class="empty mc-none hide" id="mnone">No modules match.</div></div>'
			+ '</div>';
	}

	// Patches one card in place: class, dot, state text and the enabled counter.
	function setCard(id, on) {
		var c = st.byId[id];
		if (!c) return;
		c.mod.enabled = on;
		if (on) {
			c.el.classList.add('on');
			c.dot.classList.add('on');
			c.state.textContent = 'ENABLED';
		} else {
			c.el.classList.remove('on');
			c.dot.classList.remove('on');
			c.state.textContent = 'DISABLED';
		}
		recount();
	}

	/* ------------------------------------------------------------ sheet */

	function switchHtml(act, key, on) {
		return '<button class="switch' + (on ? ' on' : '') + '" data-act="' + act + '" data-key="' + esc(key)
			+ '"><span class="knob"></span></button>';
	}

	function rowHtml(s) {
		var key = esc(s.key);
		if (s.type === 'toggle') {
			return '<div class="sr sr-inline"><span class="sr-label">' + esc(s.label) + '</span><span class="grow"></span>'
				+ switchHtml('switch', s.key, s.value) + '</div>';
		}
		if (s.type === 'slider') {
			return '<div class="sr"><div class="sr-line"><span class="sr-label">' + esc(s.label) + '</span>'
				+ '<span class="grow"></span><span class="sr-val" id="sv-' + key + '">' + fmt(s.value) + '</span></div>'
				+ '<input class="srange" type="range" min="' + fmt(s.min) + '" max="' + fmt(s.max) + '" step="'
				+ fmt(s.step) + '" value="' + fmt(s.value) + '" data-act="slider" data-key="' + key + '"></div>';
		}
		if (s.type === 'choice') {
			var h = '<div class="sr"><span class="sr-label">' + esc(s.label) + '</span>'
				+ '<div class="sel' + (st.sel === s.key ? ' open' : '') + '" data-act="sel" data-key="' + key + '">'
				+ '<span class="sel-val">' + esc(s.value) + '</span><span class="grow"></span>' + CHEVRON + '</div>';
			if (st.sel === s.key) {
				h += '<div class="sel-list">';
				for (var i = 0; i < s.options.length; i++) {
					var opt = String(s.options[i]);
					h += '<div class="sel-opt' + (opt === String(s.value) ? ' on' : '') + '" data-act="opt" data-key="'
						+ key + '" data-val="' + esc(opt) + '">' + esc(opt) + '</div>';
				}
				h += '</div>';
			}
			return h + '</div>';
		}
		if (s.type === 'color') {
			var cur = String(s.value || '').slice(0, 7).toLowerCase();
			var sw = '';
			for (var j = 0; j < PALETTE.length; j++) {
				var c = PALETTE[j];
				sw += '<button class="swatch' + (c === cur ? ' on' : '') + '" style="background: ' + c
					+ '" data-act="color" data-key="' + key + '" data-val="' + c + '"></button>';
			}
			return '<div class="sr"><span class="sr-label">' + esc(s.label) + '</span><div class="swatches">' + sw + '</div></div>';
		}
		return '';
	}

	// Every row sits in a wrapper with a stable id, so one row can be re-rendered without touching the others.
	function wrapHtml(s) {
		return '<div class="sr-wrap" id="srw-' + esc(s.key) + '">' + rowHtml(s) + '</div>';
	}

	function groupOf(s) {
		var l = String(s.label).toLowerCase();
		if (s.type === 'color') return 'Colors';
		if (s.type === 'slider' && /opacity|alpha|transparen/.test(l)) return 'Opacity';
		if (s.type === 'choice' && /position|orientation|style|layout|align|anchor/.test(l)) return 'Layout';
		if (s.type === 'toggle') return 'Elements';
		return 'General';
	}

	// Builds the body from st.list and indexes the settings by key. Sections only for modules with many settings.
	function bodyHtml() {
		var list = st.list;
		var h = '';
		st.entries = {};
		if (!list.length) return '<div class="empty">This module has no settings.</div>';
		for (var i = 0; i < list.length; i++) st.entries[list[i].key] = list[i];
		if (list.length < GROUP_MIN) {
			for (var a = 0; a < list.length; a++) h += wrapHtml(list[a]);
			return h;
		}
		var groups = {};
		for (var b = 0; b < list.length; b++) {
			var g = groupOf(list[b]);
			groups[g] = (groups[g] || '') + wrapHtml(list[b]);
		}
		for (var k = 0; k < GROUP_ORDER.length; k++) {
			var name = GROUP_ORDER[k];
			if (groups[name]) h += '<div class="sr-group">' + name + '</div>' + groups[name];
		}
		return h;
	}

	function renderBody() {
		var b = document.getElementById('sp-body');
		if (b) b.innerHTML = bodyHtml();
	}

	// Re-renders one setting's row in place.
	function renderRow(key) {
		var w = document.getElementById('srw-' + key);
		var s = st.entries[key];
		if (w && s) w.innerHTML = rowHtml(s);
	}

	function renderKey() {
		var k = document.getElementById('kchip');
		if (!k) return;
		k.textContent = keyLabel();
		if (st.cap) k.classList.add('cap'); else k.classList.remove('cap');
	}

	function keyLabel() {
		return st.cap ? 'Press a key...' : glacier.keyName(st.open);
	}

	function sheetHtml(m) {
		return '<div class="sp-head"><div class="sp-ico">' + '<i class="fa">' + glyph(m) + '</i></div>'
			+ '<span class="sp-title">' + esc(m.name) + '</span><span class="grow"></span>'
			+ switchHtml('mod', '', m.enabled) + '</div>'
			+ '<div class="sp-body" id="sp-body">' + bodyHtml() + '</div>'
			+ '<div class="sp-foot"><span class="sr-label">Keybind</span><span class="grow"></span>'
			+ '<button class="kchip" id="kchip" data-act="key">' + esc(keyLabel()) + '</button>'
			+ '<button class="sp-reset" data-act="reset">Reset</button></div>';
	}

	function openSheet(id) {
		var c = st.byId[id];
		if (!c) return;
		st.open = id;
		st.sel = null;
		st.cap = false;
		st.list = glacier.settings(id);
		var sp = window.menu.openSheet(function () {
			st.open = null;
			st.cap = false;
			st.list = [];
			st.entries = {};
		});
		st.sp = sp;
		sp.onclick = onSheetClick;
		sp.oninput = onInput;
		sp.onchange = onChange;
		sp.innerHTML = sheetHtml(c.mod);
	}

	function onSheetClick(e) {
		var t = actOf(e.target);
		if (!t || !st.open) return;
		var act = t.dataset.act;
		var key = t.dataset.key;
		if (act !== 'key' && st.cap) { st.cap = false; renderKey(); }
		if (act === 'mod') {
			var on = glacier.toggle(st.open);
			if (on) t.classList.add('on'); else t.classList.remove('on');
			setCard(st.open, on);
		} else if (act === 'switch') {
			var v = !t.classList.contains('on');
			glacier.set(st.open, key, v);
			if (st.entries[key]) st.entries[key].value = v;
			if (v) t.classList.add('on'); else t.classList.remove('on');
		} else if (act === 'sel') {
			var prev = st.sel;
			st.sel = st.sel === key ? null : key;
			if (prev && prev !== key) renderRow(prev);
			renderRow(key);
		} else if (act === 'opt' || act === 'color') {
			glacier.set(st.open, key, t.dataset.val);
			if (st.entries[key]) st.entries[key].value = t.dataset.val;
			st.sel = null;
			renderRow(key);
		} else if (act === 'key') {
			st.cap = !st.cap;
			renderKey();
		} else if (act === 'reset') {
			glacier.reset(st.open);
			st.list = glacier.settings(st.open);
			st.sel = null;
			renderBody();
		}
	}

	function onInput(e) {
		var t = e.target;
		if (!t || !t.dataset || t.dataset.act !== 'slider') return;
		var lab = document.getElementById('sv-' + t.dataset.key);
		if (lab) lab.textContent = fmt(e.value !== undefined ? e.value : t.value);
	}

	function onChange(e) {
		var t = e.target;
		if (!t || !t.dataset || t.dataset.act !== 'slider' || !st.open) return;
		var v = Number(e.value !== undefined ? e.value : t.value);
		glacier.set(st.open, t.dataset.key, v);
		if (st.entries[t.dataset.key]) st.entries[t.dataset.key].value = v;
	}

	/* ------------------------------------------------------------- input */

	// Returns true when the key was used. code = GLFW key for keydown, ch = typed text for keypress.
	function onKey(code, ch) {
		if (st.cap && st.open) {
			if (ch !== null) return true;
			if (code === KEY.BACKSPACE || code === KEY.DELETE) glacier.clearKey(st.open);
			else if (code !== KEY.ESC) glacier.setKey(st.open, code);
			st.cap = false;
			renderKey();
			return true;
		}
		if (!st.typing || st.open) return false;
		if (ch !== null) {
			st.q += ch;
		} else if (code === KEY.BACKSPACE) {
			st.q = st.q.slice(0, -1);
		} else if (code === KEY.ESC || code === KEY.ENTER) {
			st.typing = false;
		} else {
			return false;
		}
		renderSearch();
		applyFilter();
		return true;
	}

	function onClick(e) {
		var t = actOf(e.target);
		var act = t ? t.dataset.act : null;
		if (act !== 'search' && st.typing) { st.typing = false; renderSearch(); }
		if (!t) return;
		var id = t.dataset.id;
		if (act === 'card') {
			setCard(id, glacier.toggle(id));
		} else if (act === 'gear') {
			openSheet(id);
		} else if (act === 'search') {
			st.typing = true;
			renderSearch();
		} else if (act === 'cat') {
			st.cat = t.dataset.val;
			var chips = st.root.querySelectorAll('.mchip');
			for (var i = 0; i < chips.length; i++) {
				if (chips[i].dataset.val === st.cat) chips[i].classList.add('on'); else chips[i].classList.remove('on');
			}
			applyFilter();
		}
	}

	/* ------------------------------------------------------------ page */

	// Builds the page once per tab build: the shell, one card per module, and the element handles the patches use.
	window.pages.modules = function (el) {
		st.root = el;
		st.typing = false;
		var arr = glacier.modules();
		st.mods = [];
		for (var i = 0; i < arr.length; i++) st.mods.push(arr[i]);

		el.innerHTML = shell();
		el.onclick = onClick;
		el.oninput = null;
		el.onchange = null;

		st.searchEl = document.getElementById('msearch');
		st.qEl = document.getElementById('mq');
		st.noneEl = document.getElementById('mnone');
		st.countEl = document.getElementById('mcount');
		st.cards = [];
		st.byId = {};
		for (var j = 0; j < st.mods.length; j++) {
			var m = st.mods[j];
			var id = m.id;
			var c = {
				mod: m,
				lower: String(m.name).toLowerCase(),
				shown: true,
				el: document.getElementById('m-' + id),
				dot: document.getElementById('md-' + id),
				state: document.getElementById('ms-' + id)
			};
			st.cards.push(c);
			st.byId[id] = c;
		}
		applyFilter();
		renderSearch();
		recount();
	};

	// Key claiming lives in enter(), because the menu drops page key hooks whenever another tab is shown.
	window.pages.modules.enter = function () {
		window.menu.onKey(onKey);
	};
})();
