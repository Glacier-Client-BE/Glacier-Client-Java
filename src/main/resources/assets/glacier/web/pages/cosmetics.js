// Cosmetics page: a preview stage on the left (the equipped cape and accessories, drawn from their texture sheets)
// and, on the right, five tabs: Capes, Hats & Head, Wings, Hands & Face, Equipped. Data comes from
// glacier.cosmetics() (name, file, base, accent, selected) and glacier.accessories() (type, designs[{name, slug,
// file}], selected). Picks call glacier.selectCape(name) or glacier.setAccessory(type, name), then menu.render().
// Textures are sized with inline pixel dimensions because the engine has no object-fit.
(function () {
	window.pages = window.pages || {};

	var TEX = 'glacier:textures/cosmetics/';
	var CAPE_SIZE = [64, 32];
	var DEFAULT_ACCENT = '#6b7fe0';
	var NONE = 'None';

	/* Sheet size of each accessory type (matches the UV size in AccessoryModels) and its accent colour. The API carries
	 * no colours for accessories, so the accent per type lives here. */
	var TYPES = {
		Bandana: { size: [36, 11], accent: '#e5484d' },
		Glasses: { size: [20, 10], accent: '#f5b83d' },
		Wings: { size: [26, 17], accent: '#9d8cff' },
		Bracelet: { size: [20, 7], accent: '#e0b15a' },
		Halo: { size: [40, 11], accent: '#7fd6ff' },
		Ears: { size: [6, 4], accent: '#ff8fb1' }
	};

	var TABS = [
		{ id: 'capes', label: 'Capes' },
		{ id: 'head', label: 'Hats & Head', types: ['Bandana', 'Halo', 'Ears'] },
		{ id: 'wings', label: 'Wings', types: ['Wings'] },
		{ id: 'hands', label: 'Hands & Face', types: ['Bracelet', 'Glasses'] },
		{ id: 'equipped', label: 'Equipped' }
	];

	/* Remembered across re-renders, so a pick keeps the tab the user is on. */
	var currentTab = 'capes';

	function esc(s) {
		return String(s === undefined || s === null ? '' : s).split('&').join('&amp;').split('<').join('&lt;')
			.split('>').join('&gt;').split('"').join('&quot;');
	}

	function fa(code) {
		return window.menu ? window.menu.fa(code) : '';
	}

	function capes() {
		try {
			return glacier.cosmetics() || [];
		} catch (e) {
			return [];
		}
	}

	function accessories() {
		try {
			return glacier.accessories() || [];
		} catch (e) {
			return [];
		}
	}

	/* "#rrggbb" or "#rrggbbaa" to an rgba() string with the given alpha. */
	function rgba(hex, a) {
		var h = String(hex || '').replace('#', '');
		if (h.length < 6) h = DEFAULT_ACCENT.replace('#', '');
		return 'rgba(' + parseInt(h.substr(0, 2), 16) + ',' + parseInt(h.substr(2, 2), 16) + ','
			+ parseInt(h.substr(4, 2), 16) + ',' + a + ')';
	}

	/* Largest integer size of a sheet that fits in maxW x maxH, scaled by whole-ish steps up to maxScale. */
	function fit(size, maxW, maxH, maxScale) {
		var s = Math.min(maxW / size[0], maxH / size[1], maxScale);
		return { w: Math.floor(size[0] * s), h: Math.floor(size[1] * s) };
	}

	function sheetImg(file, size, maxW, maxH, maxScale) {
		var d = fit(size, maxW, maxH, maxScale);
		return '<img src="' + TEX + esc(file) + '" style="width:' + d.w + 'px;height:' + d.h + 'px" alt="">';
	}

	function capeFile(c) {
		return c.file || String(c.name).toLowerCase() + '.png';
	}

	function hasCape(c) {
		return !!c.base;
	}

	function selectedCape(list) {
		for (var i = 0; i < list.length; i++) {
			if (list[i].selected) return list[i];
		}
		return null;
	}

	function typeOf(acc) {
		return TYPES[acc.type] || { size: [16, 16], accent: DEFAULT_ACCENT };
	}

	function designFile(acc, name) {
		var designs = acc.designs || [];
		for (var i = 0; i < designs.length; i++) {
			if (designs[i].name === name) return designs[i].file;
		}
		return '';
	}

	function tabForType(typeName) {
		for (var i = 0; i < TABS.length; i++) {
			var types = TABS[i].types || [];
			for (var j = 0; j < types.length; j++) {
				if (types[j] === typeName) return TABS[i].id;
			}
		}
		return 'capes';
	}

	/* ---------- preview stage ---------- */

	function stageHtml(caps, accs) {
		var cape = selectedCape(caps);
		var worn = '';
		for (var i = 0; i < accs.length; i++) {
			var a = accs[i];
			if (!a.selected || a.selected === NONE) continue;
			var meta = typeOf(a);
			worn += rowHtml(a.type, a.selected, sheetImg(designFile(a, a.selected), meta.size, 56, 30, 3),
				'tab', tabForType(a.type), '');
		}
		var art = cape && hasCape(cape)
			? '<img src="' + TEX + esc(capeFile(cape)) + '" style="width:192px;height:96px" alt="">'
			: '<span class="cos-none-ico">' + fa('f05e') + '</span>';
		var bg = cape && hasCape(cape)
			? 'background:linear-gradient(135deg,' + cape.base + ',' + cape.accent + ')'
			: 'background:#1f2127';
		return '<aside class="card cos-stage"><span class="eyebrow accent">PREVIEW</span>'
			+ '<div class="cos-stage-art" style="' + bg + '">' + art + '</div>'
			+ '<div class="cos-meta"><span class="cos-title">' + esc(cape ? cape.name : NONE) + '</span>'
			+ '<span class="cos-sub">' + (cape && hasCape(cape) ? 'Cape equipped' : 'No cape equipped') + '</span></div>'
			+ '<button class="cos-btn" data-act="randomize">Randomize outfit</button>'
			+ '<span class="eyebrow">WORN</span>'
			+ '<div class="cos-worn">' + (worn || '<span class="cos-sub">No accessories equipped</span>') + '</div>'
			+ '</aside>';
	}

	/* ---------- shared row (equipped list and stage) ---------- */

	function rowHtml(label, name, art, act, val, clearAttrs) {
		return '<div class="cos-row" data-act="' + act + '" data-val="' + esc(val) + '" role="button">'
			+ '<div class="cos-row-art">' + art + '</div>'
			+ '<div class="cos-meta"><span class="eyebrow">' + esc(label) + '</span>'
			+ '<span class="cos-name">' + esc(name) + '</span></div>'
			+ (clearAttrs ? '<button class="cos-btn" ' + clearAttrs + '>Remove</button>' : '')
			+ '</div>';
	}

	/* ---------- Capes tab ---------- */

	function capeTile(c) {
		var on = !!c.selected;
		var none = !hasCape(c);
		var acc = none ? DEFAULT_ACCENT : c.accent;
		var art = none
			? '<div class="cos-art cos-art-none">' + fa('f05e') + '</div>'
			: '<div class="cos-art" style="background:linear-gradient(135deg,' + c.base + ',' + c.accent + ')">'
				+ '<img src="' + TEX + esc(capeFile(c)) + '" style="width:120px;height:60px" alt=""></div>';
		return tileHtml(c.name, none ? 'No cape' : 'Cape', acc, on, art, 'cape', c.name, '');
	}

	function tileHtml(name, sub, acc, on, art, act, val, extra) {
		var border = on ? acc : rgba(acc, 0.45);
		var glow = on ? 'box-shadow:0 0 18px ' + rgba(acc, 0.45) + ';' : '';
		return '<div class="cos-tile' + (on ? ' on' : '') + '" role="button" aria-pressed="' + (on ? 'true' : 'false')
			+ '" data-act="' + act + '" data-val="' + esc(val) + '" ' + (extra || '')
			+ ' style="border-color:' + border + ';' + glow + '">'
			+ art
			+ (on ? '<span class="cos-check" style="background:' + acc + '">' + fa('f00c') + '</span>' : '')
			+ '<div class="cos-info"><span class="cos-name">' + esc(name) + '</span>'
			+ '<span class="cos-sub">' + esc(sub) + '</span></div></div>';
	}

	function capesGrid(list) {
		var h = '';
		for (var i = 0; i < list.length; i++) h += capeTile(list[i]);
		return '<div class="cos-grid">' + h + '</div>';
	}

	/* ---------- accessory groups ---------- */

	function accTile(acc, name, file, on) {
		var meta = typeOf(acc);
		var colour = meta.accent;
		var art = !file
			? '<div class="cos-art cos-art-none">' + fa('f05e') + '</div>'
			: '<div class="cos-art" style="background:linear-gradient(135deg,' + rgba(colour, 0.24) + ','
				+ rgba(colour, 0.03) + ')">' + sheetImg(file, meta.size, 120, 56, 4) + '</div>';
		return tileHtml(name, file ? acc.type : 'Nothing', colour, on, art, 'acc', name, 'data-type="' + esc(acc.type) + '"');
	}

	function accGroup(acc) {
		var sel = acc.selected || NONE;
		var tiles = accTile(acc, NONE, '', sel === NONE);
		var designs = acc.designs || [];
		for (var i = 0; i < designs.length; i++) {
			tiles += accTile(acc, designs[i].name, designs[i].file, sel === designs[i].name);
		}
		return '<section class="card cos-group"><div class="cos-group-head"><span class="cos-group-h">'
			+ esc(acc.type) + '</span><span class="cos-sub">' + esc(sel === NONE ? 'Nothing equipped' : sel)
			+ '</span></div><div class="cos-grid">' + tiles + '</div></section>';
	}

	function groupsFor(accs, types) {
		var h = '';
		for (var i = 0; i < accs.length; i++) {
			for (var j = 0; j < types.length; j++) {
				if (accs[i].type === types[j]) h += accGroup(accs[i]);
			}
		}
		return '<div class="cos-stack">' + h + '</div>';
	}

	/* ---------- Equipped tab ---------- */

	function equippedHtml(caps, accs) {
		var cape = selectedCape(caps);
		var rows = '';
		var capeArt = cape && hasCape(cape)
			? '<img src="' + TEX + esc(capeFile(cape)) + '" style="width:56px;height:28px" alt="">'
			: '<span class="cos-none-ico">' + fa('f05e') + '</span>';
		rows += rowHtml('Cape', cape ? cape.name : NONE, capeArt, 'tab', 'capes',
			cape && hasCape(cape) ? 'data-act="clear-cape"' : '');
		for (var i = 0; i < accs.length; i++) {
			var a = accs[i];
			var on = a.selected && a.selected !== NONE;
			var meta = typeOf(a);
			var art = on
				? sheetImg(designFile(a, a.selected), meta.size, 56, 30, 3)
				: '<span class="cos-none-ico">' + fa('f05e') + '</span>';
			rows += rowHtml(a.type, on ? a.selected : NONE, art, 'tab', tabForType(a.type),
				on ? 'data-act="clear-acc" data-type="' + esc(a.type) + '"' : '');
		}
		return '<section class="card cos-group"><div class="cos-group-head"><span class="cos-group-h">Equipped</span>'
			+ '<button class="cos-btn" data-act="clear-all">Unequip all</button></div>'
			+ '<div class="cos-stack">' + rows + '</div></section>';
	}

	/* ---------- page ---------- */

	/* Number of equipped items shown in a tab's label, so you can see where your outfit lives. */
	function tabCount(tab, caps, accs) {
		var n = 0;
		if (tab.id === 'capes') return selectedCape(caps) && hasCape(selectedCape(caps)) ? 1 : 0;
		if (tab.id === 'equipped') {
			n = selectedCape(caps) && hasCape(selectedCape(caps)) ? 1 : 0;
			for (var i = 0; i < accs.length; i++) if (accs[i].selected && accs[i].selected !== NONE) n++;
			return n;
		}
		for (var a = 0; a < accs.length; a++) {
			if ((tab.types || []).indexOf(accs[a].type) >= 0 && accs[a].selected && accs[a].selected !== NONE) n++;
		}
		return n;
	}

	function tabsHtml(caps, accs) {
		var h = '<div class="cos-tabs" role="tablist" aria-label="Cosmetics">';
		for (var i = 0; i < TABS.length; i++) {
			var on = TABS[i].id === currentTab;
			h += '<button class="cos-tab' + (on ? ' on' : '') + '" role="tab" aria-selected="' + (on ? 'true' : 'false')
				+ '" data-act="tab" data-val="' + TABS[i].id + '">' + esc(TABS[i].label)
				+ (tabCount(TABS[i], caps, accs) ? ' · ' + tabCount(TABS[i], caps, accs) : '') + '</button>';
		}
		return h + '</div>';
	}

	function bodyHtml(caps, accs) {
		if (currentTab === 'capes') return capesGrid(caps);
		if (currentTab === 'equipped') return equippedHtml(caps, accs);
		for (var i = 0; i < TABS.length; i++) {
			if (TABS[i].id === currentTab) return groupsFor(accs, TABS[i].types || []);
		}
		return capesGrid(caps);
	}

	function actOf(t, root) {
		while (t && t !== root) {
			if (t.dataset && t.dataset.act) return t;
			t = t.parentElement;
		}
		return null;
	}

	window.pages.cosmetics = function (el) {
		var caps = capes();
		var accs = accessories();
		if (!caps.length && !accs.length) {
			el.innerHTML = '<div class="empty">No cosmetics are available.</div>';
			return;
		}
		el.innerHTML = '<div class="pg cos-root" id="cos-root"><div class="cos-layout">' + stageHtml(caps, accs)
			+ '<div class="cos-main">' + tabsHtml(caps, accs) + bodyHtml(caps, accs) + '</div>'
			+ '</div></div>';
		var root = document.getElementById('cos-root');
		if (!root) return;

		root.onclick = function (e) {
			var a = actOf(e.target, root);
			if (!a) return;
			var act = a.dataset.act;
			var val = a.dataset.val;
			if (act === 'tab') {
				currentTab = val;
				window.menu.render();
				return;
			}
			try {
				if (act === 'cape') glacier.selectCape(val);
				else if (act === 'clear-cape') glacier.selectCape(NONE);
				else if (act === 'acc') glacier.setAccessory(a.dataset.type, val);
				else if (act === 'clear-acc') glacier.setAccessory(a.dataset.type, NONE);
				else if (act === 'randomize') {
					var pool = capes().filter(hasCape);
					if (pool.length) glacier.selectCape(pool[Math.floor(Math.random() * pool.length)].name);
					var all = accessories();
					for (var r = 0; r < all.length; r++) {
						var ds = all[r].designs || [];
						var pick = Math.floor(Math.random() * (ds.length + 1));
						glacier.setAccessory(all[r].type, pick === 0 ? NONE : ds[pick - 1].name);
					}
				} else if (act === 'clear-all') {
					glacier.selectCape(NONE);
					var list = accessories();
					for (var i = 0; i < list.length; i++) glacier.setAccessory(list[i].type, NONE);
				}
			} catch (x) { /* bridge not ready */ }
			window.menu.render();
		};
	};
})();
