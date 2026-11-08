// Elements page: switches and sliders that hide or restyle vanilla HUD elements.
// Rows come from glacier.elementsSettings(), grouped the way the Elements board lists them.
(function () {
	window.pages = window.pages || {};

	var MODULE = 'elements';

	var GROUPS = [
		{ title: 'HUD Factories Elements', labels: ['Hide Item Name Text', 'Hide Jukebox Text', 'Hide Tip Text', 'Hide Actionbar Text'] },
		{ title: 'Bossbar Elements', labels: ['Hide Boss Bar', 'Hide Boss Name', 'Show Boss Percentage', 'Boss Bar Vertical Padding'] },
		{ title: 'Scoreboard', labels: ['Hide Scoreboard', 'Hide Scores'] },
		{ title: 'HUD Elements', labels: ['Hide Vignette', 'Hide Effects Icon', 'Use Base BG', 'Show Sound Subtitles'] }
	];

	var SUB = {
		'Hide Item Name Text': 'Name of the held item above your hotbar.',
		'Hide Jukebox Text': 'Now-playing text from jukeboxes.',
		'Hide Tip Text': 'Tips and hints shown on the HUD.',
		'Hide Actionbar Text': 'Action bar messages above the hotbar.',
		'Hide Boss Bar': 'The boss health bar.',
		'Hide Boss Name': 'The boss name above the bar.',
		'Show Boss Percentage': 'Health percentage on the boss bar.',
		'Boss Bar Vertical Padding': 'Space between stacked boss bars.',
		'Hide Scoreboard': 'The sidebar scoreboard.',
		'Hide Scores': 'The red score numbers on the scoreboard.',
		'Hide Vignette': 'The dark edge overlay effect.',
		'Hide Effects Icon': 'Status effect icons at the top right.',
		'Use Base BG': 'Backgrounds behind HUD modules.',
		'Show Sound Subtitles': 'Sound subtitles at the bottom right.'
	};

	function esc(s) {
		return String(s).split('&').join('&amp;').split('<').join('&lt;')
			.split('>').join('&gt;').split('"').join('&quot;');
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

	function rowHtml(s) {
		var sub = SUB[s.label] ? '<div class="row-sub">' + esc(SUB[s.label]) + '</div>' : '';
		if (s.type === 'slider') {
			return '<div class="row"><div class="row-text"><div class="row-label">' + esc(s.label) + '</div>' + sub + '</div>'
				+ '<input class="range" type="range" min="' + fmt(s.min) + '" max="' + fmt(s.max) + '" step="' + fmt(s.step)
				+ '" value="' + fmt(s.value) + '" data-act="slider" data-id="' + MODULE + '" data-key="' + esc(s.key) + '">'
				+ '<span class="slider-val" id="sv-' + MODULE + '-' + esc(s.key) + '">' + fmt(s.value) + ' px</span></div>';
		}
		return '<div class="row"><div class="row-text"><div class="row-label">' + esc(s.label) + '</div>' + sub + '</div>'
			+ '<button class="switch' + (s.value ? ' on' : '') + '" role="switch" aria-checked="' + (s.value ? 'true' : 'false')
			+ '" aria-label="' + esc(s.label) + '" data-act="switch" data-id="' + MODULE + '" data-key="' + esc(s.key)
			+ '"><span class="knob"></span></button></div>';
	}

	function sectionsHtml(list) {
		var byLabel = {};
		var used = {};
		for (var i = 0; i < list.length; i++) byLabel[list[i].label] = list[i];

		var h = '';
		for (var g = 0; g < GROUPS.length; g++) {
			var rows = '';
			for (var j = 0; j < GROUPS[g].labels.length; j++) {
				var s = byLabel[GROUPS[g].labels[j]];
				if (!s) continue;
				used[s.label] = true;
				rows += rowHtml(s);
			}
			if (rows) h += '<section class="card"><h2 class="card-h">' + esc(GROUPS[g].title) + '</h2>' + rows + '</section>';
		}
		var other = '';
		for (var k = 0; k < list.length; k++) {
			if (!used[list[k].label]) other += rowHtml(list[k]);
		}
		if (other) h += '<section class="card"><h2 class="card-h">Other</h2>' + other + '</section>';
		return h;
	}

	function shell() {
		var list = glacier.elementsSettings();
		var items = [];
		for (var i = 0; i < list.length; i++) items.push(list[i]);
		return '<div class="pg"><div class="grid2">' + sectionsHtml(items) + '</div></div>';
	}

	function onClick(e) {
		var t = actOf(e.target);
		if (!t || t.dataset.act !== 'switch') return;
		var on = !t.classList.contains('on');
		glacier.set(t.dataset.id, t.dataset.key, on);
		if (on) t.classList.add('on'); else t.classList.remove('on');
		t.setAttribute('aria-checked', on ? 'true' : 'false');
	}

	function onInput(e) {
		var t = e.target;
		if (!t || !t.dataset || t.dataset.act !== 'slider') return;
		var lab = document.getElementById('sv-' + t.dataset.id + '-' + t.dataset.key);
		if (lab) lab.textContent = fmt(e.value !== undefined ? e.value : t.value) + ' px';
	}

	function onChange(e) {
		var t = e.target;
		if (!t || !t.dataset || t.dataset.act !== 'slider') return;
		var v = e.value !== undefined ? Number(e.value) : Number(t.value);
		glacier.set(t.dataset.id, t.dataset.key, v);
	}

	window.pages.elements = function (el) {
		el.innerHTML = shell();
		el.onclick = onClick;
		el.oninput = onInput;
		el.onchange = onChange;
	};
})();
