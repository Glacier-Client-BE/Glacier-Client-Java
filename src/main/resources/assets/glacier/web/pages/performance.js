// Performance page: live frame stats and a 12 second frame-rate chart (sampled every 250 ms),
// the performance boosts as switches, and the bundled mods with the Sodium status.
(function () {
	window.pages = window.pages || {};

	var MODULE = 'performancemods';
	var SAMPLE_MS = 250;
	var BAR_COUNT = 48;          // 48 samples at 250 ms = 12 seconds
	var HISTORY_MAX = 240;       // 60 seconds for the 1% low
	var CHART_H = 180;           // px; the .bars box is 200px including padding
	var FPS_MAX = 160;
	var MODS_EVERY = 8;          // refresh the enabled-module count every 2 seconds

	var BOOSTS = {
		'Low-end laptop preset': { desc: 'Fast graphics, render distance 6, 60 FPS cap. Undone when switched off', impact: 'HIGH' },
		'No Entity Shadows': { desc: 'Skip the shadow pass under mobs and items', impact: 'HIGH' },
		'Minimal Particles': { desc: 'Cut particle count by a large share', impact: 'HIGH' },
		'No Clouds': { desc: 'Don’t build cloud geometry', impact: 'MED' },
		'No View Bobbing': { desc: 'Keep the camera still while walking', impact: 'LOW' }
	};

	var BUNDLED = [
		{ name: 'Sodium', desc: 'Modern rendering engine with far higher FPS.' },
		{ name: 'Lithium', desc: 'Game logic and server-side work optimizations.' },
		{ name: 'FerriteCore', desc: 'Lower memory use for block states and models.' },
		{ name: 'ImmediatelyFast', desc: 'Faster immediate-mode drawing for HUD and UI.' }	];

	var hist = [];
	var ticks = 0;
	var modsOn = 0;
	var modsTotal = 0;
	var timer = null;

	function esc(s) {
		return String(s).split('&').join('&amp;').split('<').join('&lt;')
			.split('>').join('&gt;').split('"').join('&quot;');
	}

	function actOf(t) {
		while (t && !(t.dataset && t.dataset.act)) t = t.parentElement;
		return t;
	}

	function fpsColor(f) {
		return f >= 60 ? '#43b581' : f >= 30 ? '#faa61a' : '#f04747';
	}

	function barColor(f) {
		return f >= 60 ? '#43b581' : f >= 30 ? '#faa61a' : '#f04747';
	}

	function switchHtml(key, on, label) {
		return '<button class="switch' + (on ? ' on' : '') + '" role="switch" aria-checked="' + (on ? 'true' : 'false')
			+ '" aria-label="' + esc(label) + '" data-act="switch" data-key="' + esc(key) + '"><span class="knob"></span></button>';
	}

	function boostsHtml() {
		var list = glacier.performanceSettings();
		var h = '';
		for (var i = 0; i < list.length; i++) {
			var s = list[i];
			if (s.type !== 'toggle') continue;
			var info = BOOSTS[s.label] || { desc: '', impact: 'LOW' };
			var pill = info.impact === 'HIGH' ? 'pill-high' : info.impact === 'MED' ? 'pill-med' : 'pill-low';
			h += '<div class="row"><div class="row-text"><div class="row-label">' + esc(s.label) + '</div>'
				+ '<div class="row-sub">' + esc(info.desc) + '</div></div>'
				+ '<span class="pill ' + pill + '">' + info.impact + '</span>'
				+ switchHtml(s.key, s.value, s.label) + '</div>';
		}
		return h;
	}

	function barsHtml() {
		var pad = BAR_COUNT - hist.length;
		var h = '';
		for (var i = 0; i < pad; i++) h += '<div class="bar-col" style="height: 0px"></div>';
		for (var j = 0; j < hist.length; j++) {
			var f = hist[j];
			var px = Math.max(0, Math.min(CHART_H, Math.round((f / FPS_MAX) * CHART_H)));
			h += '<div class="bar-col" style="height: ' + px + 'px; background: ' + barColor(f) + '"></div>';
		}
		return h;
	}

	function bundledHtml() {
		var h = '';
		for (var i = 0; i < BUNDLED.length; i++) {
			h += '<div class="mod-item"><span class="mod-name">' + esc(BUNDLED[i].name) + '</span>'
				+ '<span class="mod-desc">' + esc(BUNDLED[i].desc) + '</span></div>';
		}
		return h;
	}

	/** Launcher steps and the JVM argument line, read-only, with a copy button. */
	function jvmHtml() {
		return '<div class="card-soft">'
			+ '<div class="card-head"><span class="eyebrow" style="flex-grow: 1">JVM ARGUMENTS</span>'
			+ '<button class="btn small" data-act="copy-jvm">Copy JVM args</button></div>'
			+ '<span class="small muted">Launcher: Installations, then Edit, then More options, then the JVM Arguments field. '
			+ 'Paste this line and pick a Java 21 runtime.</span>'
			+ '<textarea id="perf-jvm-text" readonly spellcheck="false" rows="3" style="width: 100%; box-sizing: border-box; '
			+ 'resize: none; font-family: Consolas, monospace; font-size: 11px; color: #ffffff; '
			+ 'background: rgba(0,0,0,0.25); border: 1px solid rgba(255,255,255,0.10); border-radius: 9px; padding: 8px;">'
			+ esc(glacier.jvmArgs()) + '</textarea></div>';
	}

	function stat(id, label, value, sub, color) {
		return '<div class="stat"><span class="eyebrow">' + label + '</span>'
			+ '<span class="stat-value" id="' + id + '" style="color: ' + color + '">' + value + '</span>'
			+ '<span class="stat-sub" id="' + id + '-sub">' + sub + '</span></div>';
	}

	function shell() {
		return '<div class="pg">'
			+ '<div class="grid4">'
			+ stat('pf-fps', 'FRAMES PER SECOND', '--', '1% low --', '#ffffff')
			+ stat('pf-ft', 'FRAME TIME (MS)', '--', 'worst --', '#ffffff')
			+ stat('pf-mods', 'MODULES ON', '--', 'of --', '#ffffff')
			+ stat('pf-session', 'SESSION', '00:00', 'this menu session', '#ffffff')
			+ '</div>'
			+ '<div class="split">'
			+ '<div class="card-soft" style="flex-grow: 1">'
			+ '<div class="card-head"><span class="eyebrow" style="flex-grow: 1">FRAME RATE — LAST 12 SECONDS</span>'
			+ '<span class="small muted">60 fps line</span></div>'
			+ '<div class="bars" id="perf-bars"><div class="hline" style="bottom: ' + Math.round(60 / FPS_MAX * CHART_H + 10)
			+ 'px"></div>' + barsHtml() + '</div>'
			+ '<div class="legend">'
			+ '<span class="legend-item"><span class="legend-dot" style="background: #43b581"></span>60+ fps</span>'
			+ '<span class="legend-item"><span class="legend-dot" style="background: #faa61a"></span>30–60</span>'
			+ '<span class="legend-item"><span class="legend-dot" style="background: #f04747"></span>under 30</span>'
			+ '</div></div>'
			+ '<div class="card-soft side">'
			+ '<span class="eyebrow">BOOSTS</span>'
			+ '<div id="perf-boosts">' + boostsHtml() + '</div>'
			+ '</div></div>'
			+ '<div class="card-soft">'
			+ '<span class="eyebrow">BUNDLED MODS</span>'
			+ '<div class="grid4">' + bundledHtml() + '</div>'
			+ '</div>'
			+ jvmHtml()
			+ '</div>';
	}

	function percentileLow() {
		if (!hist.length) return 0;
		var sorted = hist.slice().sort(function (a, b) { return a - b; });
		return sorted[Math.floor(sorted.length * 0.01)];
	}

	function setText(id, text) {
		var el = document.getElementById(id);
		if (el) el.textContent = text;
	}

	function setStyleColor(id, color) {
		var el = document.getElementById(id);
		if (el) el.style.color = color;
	}

	function formatClock(seconds) {
		var s = Math.floor(seconds);
		var m = Math.floor(s / 60);
		var r = s % 60;
		return (m < 10 ? '0' : '') + m + ':' + (r < 10 ? '0' : '') + r;
	}

	function tick() {
		var bars = document.getElementById('perf-bars');
		if (!bars) {
			clearInterval(timer);
			timer = null;
			return;
		}
		ticks++;
		var fps = Math.round(glacier.fps());
		hist.push(fps);
		if (hist.length > HISTORY_MAX) hist.shift();

		var ft = fps > 0 ? 1000 / fps : 0;
		var low = Math.round(percentileLow());
		var minFps = hist.length ? Math.min.apply(null, hist) : 0;
		var worst = minFps > 0 ? 1000 / minFps : 0;

		setText('pf-fps', String(fps));
		setStyleColor('pf-fps', fpsColor(fps));
		setText('pf-fps-sub', '1% low ' + low);
		setText('pf-ft', ft.toFixed(1));
		setText('pf-ft-sub', 'worst ' + worst.toFixed(1));
		setText('pf-session', formatClock(ticks * SAMPLE_MS / 1000));

		if (ticks % MODS_EVERY === 1) {
			var arr = glacier.modules();
			modsTotal = arr.length;
			modsOn = 0;
			for (var i = 0; i < arr.length; i++) if (arr[i].enabled) modsOn++;
			setText('pf-mods', String(modsOn));
			setText('pf-mods-sub', 'of ' + modsTotal);
		}

		bars.innerHTML = '<div class="hline" style="bottom: ' + Math.round(60 / FPS_MAX * CHART_H + 10)
			+ 'px"></div>' + barsHtml();
	}

	function flipSwitch(t) {
		var on = !t.classList.contains('on');
		// the toggle follows what the game accepted, not what was asked for
		if (!glacier.set(MODULE, t.dataset.key, on)) return;
		if (on) t.classList.add('on'); else t.classList.remove('on');
		t.setAttribute('aria-checked', on ? 'true' : 'false');
	}

	function copyJvm(btn) {
		var ok = glacier.copyJvmArgs();
		btn.textContent = ok ? 'Copied' : 'Copy failed';
		setTimeout(function () { btn.textContent = 'Copy JVM args'; }, 1500);
	}

	function onClick(e) {
		var t = actOf(e.target);
		if (!t) return;
		if (t.dataset.act === 'copy-jvm') { copyJvm(t); return; }
		if (t.dataset.act !== 'switch') return;
		flipSwitch(t);
	}

	// The sampler runs only while this tab is shown: enter starts it, leave stops it.
	function stop() {
		if (timer) clearInterval(timer);
		timer = null;
	}

	window.pages.performance = function (el) {
		stop();
		// a freshly opened page starts a new session: samples and the session clock restart
		hist = [];
		ticks = 0;
		el.innerHTML = shell();
		el.onclick = onClick;
		el.onchange = null;
		el.oninput = null;
	};

	window.pages.performance.enter = function () {
		stop();
		tick();
		timer = setInterval(tick, SAMPLE_MS);
	};

	window.pages.performance.leave = function () {
		stop();
	};
})();
