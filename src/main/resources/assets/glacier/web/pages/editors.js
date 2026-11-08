// Editors page. The live preview follows the selected tab, as on the Editors board. Settings are read from
// glacier.editorSettings() and written with glacier.set('editors', key, value); the page re-renders after a change.
(function () {
	window.pages = window.pages || {};

	var MOD = 'editors';
	var PX = 3; // preview pixels per GUI pixel

	var TABS = [
		{ id: 'crosshairs', label: 'Crosshairs', icon: 'f05b' },
		{ id: 'hotbar', label: 'Hotbar', icon: 'f00a' },
		{ id: 'expbar', label: 'EXP Bar', icon: 'f080' },
		{ id: 'display', label: 'Display', icon: 'f26c' }
	];
	var BIOMES = [
		{ name: 'Overworld', bg: '#3b5a3a' },
		{ name: 'Nether', bg: '#4a1f1a' },
		{ name: 'Mangrove', bg: '#2e3d2a' },
		{ name: 'End City', bg: '#2a2438' }
	];
	// Names and colours mirror EditorsModule.
	var CROSS_COLOURS = [
		['Default', '#ffffff'], ['White', '#ffffff'], ['Red', '#f04747'], ['Orange', '#faa61a'], ['Yellow', '#fee75c'],
		['Green', '#43b581'], ['Aqua', '#4ad0e0'], ['Blue', '#7289da'], ['Purple', '#9b59b6'], ['Pink', '#eb459e']
	];
	var EXP_COLOURS = [
		['Default', '#80ff20'], ['White', '#ffffff'], ['Red', '#f04747'], ['Orange', '#faa61a'], ['Yellow', '#fee75c'],
		['Blue', '#4f7ce0'], ['Purple', '#9b59b6'], ['Pink', '#eb459e'], ['Chroma', '#ff6ad5']
	];
	var DISPLAY_COLOURS = [
		['Bright', '#ffffff'], ['Red', '#f04747'], ['Orange', '#faa61a'], ['Yellow', '#fee75c'], ['Green', '#43b581'],
		['Aqua', '#4ad0e0'], ['Blue', '#4f7ce0'], ['Purple', '#9b59b6']
	];
	// Hotbar looks from the board: background, corner radius, slot gap.
	var HOTBAR_STYLES = [
		{ id: 'Vanilla', bg: 'rgba(0,0,0,0.35)', radius: 2, gap: 2 },
		{ id: 'Default', bg: 'rgba(0,0,0,0.55)', radius: 4, gap: 2 },
		{ id: 'Connected', bg: 'rgba(0,0,0,0.55)', radius: 0, gap: 0 },
		{ id: 'Glacier', bg: 'rgba(44,47,51,0.9)', radius: 8, gap: 3 },
		{ id: 'Chroma', bg: 'rgba(114,137,218,0.45)', radius: 4, gap: 2 }
	];
	// {arm, half gap, thickness} for presets 1..4, as in EditorsModule.
	var PRESETS = [[4, 2, 1], [6, 3, 1], [5, 0, 2], [3, 1, 1]];

	var st = { tab: 'crosshairs', biome: 'Overworld' };

	function esc(s) {
		return String(s === undefined || s === null ? '' : s).split('&').join('&amp;').split('<').join('&lt;')
			.split('>').join('&gt;').split('"').join('&quot;');
	}

	function fa(code) {
		return window.menu ? window.menu.fa(code) : '';
	}

	function clamp(v, lo, hi) {
		return Math.max(lo, Math.min(hi, v));
	}

	function named(pairs, name, dflt) {
		for (var i = 0; i < pairs.length; i++) {
			if (pairs[i][0] === name) return pairs[i][1];
		}
		return dflt;
	}

	function findStyle(name) {
		for (var i = 0; i < HOTBAR_STYLES.length; i++) {
			if (HOTBAR_STYLES[i].id === name) return HOTBAR_STYLES[i];
		}
		return HOTBAR_STYLES[0];
	}

	function rgba(hex, a) {
		var n = parseInt(hex.substr(1), 16);
		return 'rgba(' + ((n >> 16) & 255) + ',' + ((n >> 8) & 255) + ',' + (n & 255) + ',' + a.toFixed(2) + ')';
	}

	function byKey(list) {
		var m = {};
		for (var i = 0; i < list.length; i++) m[list[i].key] = list[i];
		return m;
	}

	function val(m, key, dflt) {
		var s = m[key];
		return s && s.value !== undefined && s.value !== null ? s.value : dflt;
	}

	function readSettings() {
		try {
			return byKey(glacier.editorSettings());
		} catch (e) {
			return {};
		}
	}

	function setOpt(key, value) {
		try {
			glacier.set(MOD, key, value);
		} catch (e) { /* bridge not ready: keep the old value */ }
		window.menu.render();
	}

	/* ---------- crosshair ---------- */

	function bar(x, y, w, h, bg, px) {
		return '<i style="left:' + x * px + 'px;top:' + y * px + 'px;width:' + w * px + 'px;height:' + h * px
			+ 'px;background:' + bg + '"></i>';
	}

	// Same geometry as EditorsModule.plus(): four arms of `arm`, a `gap` around the centre, `thick` wide,
	// and an optional 1 px dark outline drawn first.
	function plusHtml(arm, gap, thick, colour, outline, px) {
		var cc = arm + gap;
		var lo = Math.floor(thick / 2);
		var size = (2 * cc + 1) * px;
		var out = '';
		var pads = outline ? [1, 0] : [0];
		for (var p = 0; p < pads.length; p++) {
			var pad = pads[p];
			var bg = pad ? 'rgba(0,0,0,0.9)' : colour;
			out += bar(cc - lo - pad, -pad, thick + 2 * pad, arm + 2 * pad, bg, px)
				+ bar(cc - lo - pad, cc + gap + 1 - pad, thick + 2 * pad, arm + 2 * pad, bg, px)
				+ bar(-pad, cc - lo - pad, arm + 2 * pad, thick + 2 * pad, bg, px)
				+ bar(cc + gap + 1 - pad, cc - lo - pad, arm + 2 * pad, thick + 2 * pad, bg, px);
		}
		return '<div class="ed-plus" style="width:' + size + 'px;height:' + size + 'px">' + out + '</div>';
	}

	function crossSpec(m) {
		if (val(m, 'crosshairstyle', 'Vanilla') !== 'Preset') {
			return { arm: 6, gap: 1, thick: 1, outline: false, colour: '#ffffff' };
		}
		var p = PRESETS[clamp(Math.round(Number(val(m, 'crosshairpreset', 1))) - 1, 0, PRESETS.length - 1)];
		var s = Number(val(m, 'crosshairsize', 1));
		return {
			arm: Math.max(1, Math.round(p[0] * s)),
			gap: Math.round(p[1] * s),
			thick: Math.max(1, Math.round(p[2] * s)),
			outline: !!val(m, 'crosshairoutlines', true),
			colour: named(CROSS_COLOURS, val(m, 'crosshaircolour', 'Default'), '#ffffff')
		};
	}

	/* ---------- preview card ---------- */

	function previewHtml(m) {
		var tab = st.tab;
		var biome = BIOMES[0];
		for (var b = 0; b < BIOMES.length; b++) {
			if (BIOMES[b].name === st.biome) biome = BIOMES[b];
		}
		var biomes = '';
		for (b = 0; b < BIOMES.length; b++) {
			var bon = BIOMES[b].name === st.biome;
			biomes += '<button class="ed-biome' + (bon ? ' on' : '') + '" role="tab" aria-selected="' + (bon ? 'true' : 'false')
				+ '" data-ed="biome" data-val="' + esc(BIOMES[b].name) + '">' + esc(BIOMES[b].name) + '</button>';
		}

		var inner = '';
		if (tab === 'display') {
			var tint = named(DISPLAY_COLOURS, val(m, 'displaycolour', 'Bright'), '#ffffff');
			var alpha = clamp(Number(val(m, 'displayintensity', 3)), 0, 10) * 0.03;
			inner += '<div class="ed-tint" style="background:' + rgba(tint, alpha) + '"></div>';
		}
		if (tab === 'crosshairs') {
			var xs = crossSpec(m);
			var span = (2 * (xs.arm + xs.gap) + 1) * PX;
			inner += '<div class="ed-xh" style="margin-left:' + (-span / 2) + 'px;margin-top:' + (-span / 2) + 'px">'
				+ plusHtml(xs.arm, xs.gap, xs.thick, xs.colour, xs.outline, PX) + '</div>';
		}

		var slots = '';
		var hideCount = !!val(m, 'hideitemcount', false);
		var showSlots = !!val(m, 'showslotnumbers', false);
		for (var i = 0; i < 9; i++) {
			var edge = i === 0 ? '#ffffff' : 'rgba(255,255,255,0.15)';
			var num = tab === 'hotbar' && showSlots ? String(i + 1) : '';
			var cnt = !hideCount && i % 3 === 0 ? '64' : '';
			slots += '<span class="ed-slot" style="border-color:' + edge + '"><span>' + num + '</span><span>' + cnt + '</span></span>';
		}
		var hb = findStyle(val(m, 'hotbarstyle', 'Vanilla'));
		var xpColour = named(EXP_COLOURS, val(m, 'expcolour', 'Default'), '#80ff20');
		var xpLabel = tab === 'expbar' && !!val(m, 'showxppercentage', false)
			? '<span class="ed-xppct" style="color:' + xpColour + '">64%</span>' : '';

		return '<section class="card ed-preview">'
			+ '<div class="ed-head"><h2 class="card-h ed-grow">Editor Preview</h2><span class="ed-live">LIVE</span>'
			+ '<button class="btn primary" data-ed="hud">' + fa('f0b2') + '<span>HUD Editor</span></button></div>'
			+ '<div class="ed-biomes" role="tablist" aria-label="Preview biome">' + biomes + '</div>'
			+ '<div class="ed-screen" style="background:' + biome.bg + '">'
			+ '<span class="ed-cap">' + esc(biome.name) + ' preview</span>' + inner
			+ '<div class="ed-hud">' + xpLabel
			+ '<div class="ed-xp"><i style="width:64%;background:' + xpColour + '"></i></div>'
			+ '<div class="ed-hotbar" style="background:' + hb.bg + ';border-radius:' + hb.radius + 'px;gap:' + hb.gap + 'px">'
			+ slots + '</div></div></div></section>';
	}

	/* ---------- controls ---------- */

	function tabsHtml() {
		var h = '<div class="ed-tabs" role="tablist" aria-label="Editor">';
		for (var i = 0; i < TABS.length; i++) {
			var t = TABS[i];
			var on = t.id === st.tab;
			h += '<button class="ed-tab' + (on ? ' on' : '') + '" role="tab" aria-selected="' + (on ? 'true' : 'false')
				+ '" data-ed="tab" data-val="' + t.id + '">' + fa(t.icon) + '<span>' + t.label + '</span></button>';
		}
		return h + '</div>';
	}

	function switchRow(title, sub, key, on, first) {
		return '<div class="row' + (first ? ' first' : '') + '"><div class="row-text"><span class="row-label">' + esc(title)
			+ '</span><span class="row-sub">' + esc(sub) + '</span></div>'
			+ '<button class="switch' + (on ? ' on' : '') + '" role="switch" aria-checked="' + (on ? 'true' : 'false')
			+ '" aria-label="' + esc(title) + '" data-ed="toggle" data-key="' + key + '"><span class="knob"></span></button></div>';
	}

	function seg(key, options, current) {
		var h = '<div class="seg" role="radiogroup">';
		for (var i = 0; i < options.length; i++) {
			var on = options[i] === current;
			h += '<button class="' + (on ? 'on' : '') + '" role="radio" aria-checked="' + (on ? 'true' : 'false')
				+ '" data-ed="set" data-key="' + key + '" data-val="' + esc(options[i]) + '">' + esc(options[i]) + '</button>';
		}
		return h + '</div>';
	}

	// Grid of named options; `pairs` is [name, colour] where colour may be '' for a text-only option.
	function pickGrid(key, pairs, current) {
		var h = '<div class="ed-colors">';
		for (var i = 0; i < pairs.length; i++) {
			var name = pairs[i][0];
			var colour = pairs[i][1];
			var on = name === current;
			h += '<button class="ed-pick' + (on ? ' on' : '') + '" data-ed="set" data-key="' + key + '" data-val="' + esc(name) + '">'
				+ (colour ? '<i class="ed-sw" style="background:' + colour + '"></i>' : '')
				+ '<span>' + esc(name) + '</span></button>';
		}
		return h + '</div>';
	}

	function crosshairsHtml(m) {
		var style = val(m, 'crosshairstyle', 'Vanilla');
		var size = Number(val(m, 'crosshairsize', 1));
		var h = '<section class="card">'
			+ '<div class="row first"><div class="row-text"><span class="row-label">Crosshair Style</span>'
			+ '<span class="row-sub">Vanilla, or a Glacier preset drawn over it.</span></div>'
			+ seg('crosshairstyle', ['Vanilla', 'Preset'], style) + '</div>'
			+ switchRow('Crosshair Outlines', 'Draws a dark outline around the crosshair.', 'crosshairoutlines',
				!!val(m, 'crosshairoutlines', true), false);
		if (style === 'Preset') {
			var preset = Math.round(Number(val(m, 'crosshairpreset', 1)));
			var colour = val(m, 'crosshaircolour', 'Default');
			var pr = '';
			for (var i = 0; i < PRESETS.length; i++) {
				var p = PRESETS[i];
				var on = preset === i + 1;
				pr += '<button class="ed-preset' + (on ? ' on' : '') + '" aria-label="Preset ' + (i + 1)
					+ '" data-ed="set" data-type="n" data-key="crosshairpreset" data-val="' + (i + 1) + '">'
					+ plusHtml(p[0], p[1], p[2], '#ffffff', false, 1.6) + '</button>';
			}
			h += '<div class="row row-col"><div class="row-text"><span class="row-label">Preset</span>'
				+ '<span class="row-sub">Four Glacier crosshair shapes.</span></div><div class="ed-presets">' + pr + '</div></div>'
				+ '<div class="row row-col"><div class="ed-split"><span class="row-label">Crosshair Size</span>'
				+ '<span class="ed-val" id="ed-range-out">' + size.toFixed(1) + '</span></div>'
				+ '<input class="ed-range" type="range" min="0.5" max="3" step="0.1" value="' + size
				+ '" data-ed="range" data-key="crosshairsize" aria-label="Crosshair Size"></div>'
				+ '<div class="row row-col"><span class="row-label">Crosshair Colour</span>'
				+ pickGrid('crosshaircolour', CROSS_COLOURS, colour) + '</div>';
		}
		return h + '</section>';
	}

	function hotbarHtml(m) {
		var style = val(m, 'hotbarstyle', 'Vanilla');
		var styles = [];
		for (var i = 0; i < HOTBAR_STYLES.length; i++) styles.push([HOTBAR_STYLES[i].id, '']);
		return '<section class="card"><span class="row-label ed-sub-h">Style</span>'
			+ pickGrid('hotbarstyle', styles, style) + '</section>'
			+ '<section class="card">'
			+ switchRow('Hide Item Count', 'Hides the stack count on hotbar items.', 'hideitemcount',
				!!val(m, 'hideitemcount', false), true)
			+ switchRow('Show Slot Numbers', 'Shows the slot number on each hotbar slot.', 'showslotnumbers',
				!!val(m, 'showslotnumbers', false), false)
			+ '</section>';
	}

	function expHtml(m) {
		return '<section class="card"><span class="row-label ed-sub-h">Colour</span>'
			+ pickGrid('expcolour', EXP_COLOURS, val(m, 'expcolour', 'Default')) + '</section>'
			+ '<section class="card">'
			+ switchRow('Show XP Percentage', 'Shows your experience progress as a percentage.', 'showxppercentage',
				!!val(m, 'showxppercentage', false), true)
			+ '</section>';
	}

	function displayHtml(m) {
		var intensity = Math.round(clamp(Number(val(m, 'displayintensity', 3)), 0, 10));
		return '<section class="card"><span class="row-label ed-sub-h">Colour</span>'
			+ pickGrid('displaycolour', DISPLAY_COLOURS, val(m, 'displaycolour', 'Bright')) + '</section>'
			+ '<section class="card"><div class="row first"><div class="row-text"><span class="row-label">Intensity</span>'
			+ '<span class="row-sub">Strength of the colour tint, 0 to 10.</span></div>'
			+ '<div class="ed-stepper">'
			+ '<button class="ed-step" data-ed="step" data-key="displayintensity" data-delta="-1" data-min="0" data-max="10" aria-label="Less intense">'
			+ fa('f068') + '</button>'
			+ '<span class="ed-val">' + intensity + '</span>'
			+ '<button class="ed-step" data-ed="step" data-key="displayintensity" data-delta="1" data-min="0" data-max="10" aria-label="More intense">'
			+ fa('f067') + '</button></div></div></section>';
	}

	/* ---------- events ---------- */

	// Walks up from the event target to the nearest element with data-ed, stopping at the page root.
	function actOf(t, rootId) {
		while (t) {
			if (t.dataset && t.dataset.ed) return t;
			if (t.id === rootId) return null;
			t = t.parentElement;
		}
		return null;
	}

	function handleClick(e, root) {
		var el = actOf(e.target, 'ed-root');
		if (!el) return;
		var d = el.dataset;
		var m;
		if (d.ed === 'tab') {
			st.tab = d.val;
			window.menu.render();
		} else if (d.ed === 'biome') {
			st.biome = d.val;
			window.menu.render();
		} else if (d.ed === 'set') {
			setOpt(d.key, d.type === 'n' ? Number(d.val) : d.val);
		} else if (d.ed === 'toggle') {
			m = readSettings();
			setOpt(d.key, !val(m, d.key, false));
		} else if (d.ed === 'step') {
			m = readSettings();
			setOpt(d.key, clamp(Number(val(m, d.key, 0)) + Number(d.delta), Number(d.min), Number(d.max)));
		} else if (d.ed === 'hud') {
			try {
				glacier.openHudEditor();
			} catch (x) { /* bridge not ready */ }
		}
	}

	window.pages.editors = function (el) {
		var m = readSettings();
		var body;
		if (st.tab === 'hotbar') body = hotbarHtml(m);
		else if (st.tab === 'expbar') body = expHtml(m);
		else if (st.tab === 'display') body = displayHtml(m);
		else body = crosshairsHtml(m);

		el.innerHTML = '<div class="pg ed-root" id="ed-root">' + tabsHtml() + previewHtml(m) + body + '</div>';
		var root = document.getElementById('ed-root');
		if (!root) return;

		root.onclick = function (e) {
			handleClick(e, root);
		};
		// Slider: the readout follows the drag, the value is saved on release.
		root.oninput = function (e) {
			var t = e.target;
			if (t && t.dataset && t.dataset.ed === 'range') {
				var out = document.getElementById('ed-range-out');
				if (out) out.textContent = Number(t.value).toFixed(1);
			}
		};
		root.onchange = function (e) {
			var t = e.target;
			if (t && t.dataset && t.dataset.ed === 'range') setOpt(t.dataset.key, Number(t.value));
		};
	};
})();
