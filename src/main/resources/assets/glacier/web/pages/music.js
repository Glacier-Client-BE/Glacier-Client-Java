// Music page: now-playing card, volume and the track list. Backed by glacier.music
// (state, play, stop, next, prev, volume). Progress ticks in place; the page only re-renders when the track changes.
// The engine has no pause call, so the main button stops playback and starts it again.
(function () {
	window.pages = window.pages || {};

	var DISCS = ['#43b581', '#f04747', '#faa61a', '#4aa3df', '#9b59b6', '#e3b341', '#3fa7a0', '#c1563a', '#f5f5f5', '#6b8fa8'];
	var TICK_MS = 500;
	var gen = 0;
	var last = { playing: false, index: -1 };

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

	function mmss(sec) {
		var s = Math.max(0, Math.floor(Number(sec) || 0));
		var m = Math.floor(s / 60);
		var r = s % 60;
		return m + ':' + (r < 10 ? '0' : '') + r;
	}

	function pct(p, len) {
		if (!len || len <= 0) return 0;
		return clamp(p / len * 100, 0, 100);
	}

	function state() {
		try {
			return glacier.music.state();
		} catch (e) {
			return null;
		}
	}

	function call(name, arg) {
		try {
			if (arg === undefined) glacier.music[name]();
			else glacier.music[name](arg);
		} catch (e) { /* bridge not ready */ }
		window.menu.render();
	}

	function actOf(t, rootId) {
		while (t) {
			if (t.dataset && t.dataset.mu) return t;
			if (t.id === rootId) return null;
			t = t.parentElement;
		}
		return null;
	}

	function nowHtml(s) {
		var has = !!s.playing;
		var idx = has ? Number(s.index) : 0;
		var title = has ? s.title : 'Nothing playing';
		var len = has ? Number(s.length) : 0;
		var prog = has ? Number(s.progress) : 0;
		var disc = DISCS[(idx < 0 ? 0 : idx) % DISCS.length];
		return '<section class="card mu-now" aria-label="Now playing">'
			+ '<div class="mu-art"><span class="mu-disc"><i style="background:' + disc + '"></i></span></div>'
			+ '<div class="mu-meta"><span class="eyebrow accent">NOW PLAYING</span>'
			+ '<span class="mu-title">' + esc(title) + '</span>'
			+ '<div class="mu-prog"><span class="mu-time" id="mu-time">' + mmss(prog) + '</span>'
			+ '<div class="mu-bar"><i id="mu-bar" style="width:' + pct(prog, len) + '%"></i></div>'
			+ '<span class="mu-time">' + mmss(len) + '</span></div></div>'
			+ '<div class="mu-ctrl">'
			+ '<button class="mu-btn" data-mu="prev" aria-label="Previous track">' + fa('f048') + '</button>'
			+ '<button class="mu-btn primary" data-mu="toggle" aria-label="' + (has ? 'Stop' : 'Play') + '">'
			+ fa(has ? 'f04d' : 'f04b') + '</button>'
			+ '<button class="mu-btn" data-mu="next" aria-label="Next track">' + fa('f051') + '</button>'
			+ '</div></section>';
	}

	function volHtml(vol) {
		var v = Math.round(clamp(vol, 0, 1) * 100);
		return '<section class="card mu-vol">' + fa('f028')
			+ '<span class="row-label">Volume</span>'
			+ '<input class="ed-range mu-range" type="range" min="0" max="100" step="1" value="' + v
			+ '" data-mu="vol" aria-label="Music volume">'
			+ '<span class="ed-val" id="mu-vol-out">' + v + '%</span></section>';
	}

	function listHtml(s) {
		var tracks = s.tracks || [];
		var h = '<section class="card mu-list"><h2 class="card-h">Tracks</h2><div class="mu-rows">';
		if (!tracks.length) h += '<div class="empty">No music tracks found.</div>';
		for (var i = 0; i < tracks.length; i++) {
			var t = tracks[i];
			var on = !!s.playing && Number(s.index) === i;
			var label = (on ? 'Stop ' : 'Play ') + t.title;
			h += '<div class="mu-row' + (on ? ' on' : '') + '">'
				+ '<span class="mu-disc-sm"><i style="background:' + DISCS[i % DISCS.length] + '"></i></span>'
				+ '<span class="mu-name">' + esc(t.title) + '</span>'
				+ '<span class="mu-time">' + mmss(t.seconds) + '</span>'
				+ '<button class="mu-btn sm' + (on ? ' primary' : '') + '" data-mu="track" data-idx="' + i
				+ '" data-on="' + (on ? '1' : '0') + '" aria-label="' + esc(label) + '">'
				+ fa(on ? 'f04d' : 'f04b') + '</button></div>';
		}
		return h + '</div></section>';
	}

	// Updates only the progress readout, so the page is not rebuilt every tick.
	function progress(s) {
		var prog = s.playing ? Number(s.progress) : 0;
		var len = s.playing ? Number(s.length) : 0;
		var t = document.getElementById('mu-time');
		if (t) t.textContent = mmss(prog);
		var b = document.getElementById('mu-bar');
		if (b) b.style.width = pct(prog, len) + '%';
	}

	// The progress loop runs only while this tab is shown: enter starts it, leave bumps gen so the pending tick exits.
	function schedule() {
		var my = ++gen;
		setTimeout(function () {
			if (my !== gen) return;
			if (!document.getElementById('mu-root')) return;
			var s = state();
			if (!s) return;
			if (!!s.playing !== last.playing || Number(s.index) !== last.index) {
				window.menu.render();
				schedule();
				return;
			}
			progress(s);
			schedule();
		}, TICK_MS);
	}

	function handleClick(e, root) {
		var el = actOf(e.target, 'mu-root');
		if (!el) return;
		var d = el.dataset;
		var s;
		if (d.mu === 'prev') {
			call('prev');
		} else if (d.mu === 'next') {
			call('next');
		} else if (d.mu === 'toggle') {
			s = state();
			if (s && s.playing) call('stop');
			else call('play', s && Number(s.index) >= 0 ? Number(s.index) : 0);
		} else if (d.mu === 'track') {
			if (d.on === '1') call('stop');
			else call('play', Number(d.idx));
		}
	}

	window.pages.music = function (el) {
		var s = state();
		if (!s) {
			el.innerHTML = '<div class="empty">Music is not available.</div>';
			return;
		}
		el.innerHTML = '<div class="pg mu-root" id="mu-root">' + nowHtml(s) + volHtml(Number(s.volume)) + listHtml(s) + '</div>';
		var root = document.getElementById('mu-root');
		if (!root) return;

		last.playing = !!s.playing;
		last.index = Number(s.index);

		root.onclick = function (e) {
			handleClick(e, root);
		};
		root.oninput = function (e) {
			var t = e.target;
			if (t && t.dataset && t.dataset.mu === 'vol') {
				var out = document.getElementById('mu-vol-out');
				if (out) out.textContent = Math.round(Number(t.value)) + '%';
			}
		};
		root.onchange = function (e) {
			var t = e.target;
			if (t && t.dataset && t.dataset.mu === 'vol') {
				try {
					glacier.music.volume(Number(t.value) / 100);
				} catch (x) { /* bridge not ready */ }
			}
		};
	};

	window.pages.music.enter = function () {
		var s = state();
		if (s && (!!s.playing !== last.playing || Number(s.index) !== last.index)) {
			window.menu.render();
		}
		schedule();
	};

	window.pages.music.leave = function () {
		gen++;
	};
})();
