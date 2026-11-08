/* Glacier v7 start screen. Actions go through glacier.start (StartApi). */
(function () {
	var S = glacier.start;
	var KEY_ESC = 256;
	function $(id) { return document.getElementById(id); }

	var overlay = $('st-overlay');
	var chooser = $('st-chooser');
	var infowin = $('st-infowin');
	var infoBtn = $('st-info');
	var ddList = $('st-ddlist');
	var ddCatch = $('st-ddcatch');

	$('st-name').textContent = S.playerName();
	$('st-ver').textContent = S.mcVersion();

	function on(id, fn) { $(id).addEventListener('click', fn); }

	function show(card) {
		chooser.style.display = card === chooser ? 'flex' : 'none';
		infowin.style.display = card === infowin ? 'flex' : 'none';
		overlay.style.display = card ? 'flex' : 'none';
		infoBtn.classList.toggle('st-info-on', card === infowin);
	}

	function dropdown(open) {
		ddList.style.display = open ? 'flex' : 'none';
		ddCatch.style.display = open ? 'block' : 'none';
	}

	/* centre card */
	on('st-play', function () { show(null); S.play(); });
	on('st-options', function () { S.options(); });
	on('st-market', function () { S.modMenu(); });
	on('st-dress', function () { S.cosmetics(); });
	on('st-info', function () { show(infowin.style.display === 'flex' ? null : infowin); });
	on('st-sp', function () { show(null); S.singleplayer(); });
	on('st-mp', function () { show(null); S.multiplayer(); });
	on('st-realms', function () { show(null); S.realms(); });

	/* corners */
	on('st-skins', function () { S.cosmetics(); });
	on('st-ach', function () { S.modMenu(); });
	on('st-inbox', function () { show(infowin); });
	on('st-exit', function () { S.quit(); });
	on('st-discord', function () { S.copyDiscord(); });
	on('st-dd', function () { dropdown(ddList.style.display !== 'flex'); });
	on('st-ddcatch', function () { dropdown(false); });
	var rows = document.querySelectorAll('.st-row');
	for (var r = 0; r < rows.length; r++) rows[r].addEventListener('click', function () { dropdown(false); });

	['.st-close', '.st-close-btn'].forEach(function (sel) {
		var closers = document.querySelectorAll(sel);
		for (var i = 0; i < closers.length; i++) closers[i].addEventListener('click', function () { show(null); });
	});

	/* information tabs */
	var tabs = document.querySelectorAll('.st-tab');
	function selectTab(name) {
		for (var t = 0; t < tabs.length; t++) {
			var n = tabs[t].dataset.tab;
			tabs[t].classList.toggle('st-tab-on', n === name);
			$('st-pane-' + n).style.display = n === name ? 'flex' : 'none';
		}
	}
	function bindTab(el) {
		el.addEventListener('click', function () { selectTab(el.dataset.tab); });
	}
	for (var k = 0; k < tabs.length; k++) bindTab(tabs[k]);

	document.querySelector('.st-root').addEventListener('keydown', function (e) {
		if (e.key !== KEY_ESC) return;
		if (ddList.style.display === 'flex') dropdown(false);
		else show(null);
	});

	/* news flipbook: 6 frames, 5 s each */
	var news = $('st-news');
	var frame = 0;
	setInterval(function () {
		frame = (frame + 1) % 6;
		news.setAttribute('src', 'glacier:textures/start/news_' + frame + '.png');
	}, 5000);

	/* The panorama is drawn natively by StartScreen (cover-fit, 120 s pan); nothing to animate here. */

	/* one-time intro: logo pops in and flies off, UI waits off-screen 1.75 s then slides up (out_expo 0.45 s) */
	var ui = $('st-ui');
	var card = $('st-card');
	var logo = $('st-intro');
	function outExpo(x) { return x >= 1 ? 1 : 1 - Math.pow(2, -10 * x); }
	function inExpo(x) { return x <= 0 ? 0 : Math.pow(2, 10 * x - 10); }
	function outBack(x) { var c = 1.70158; return 1 + (c + 1) * Math.pow(x - 1, 3) + c * Math.pow(x - 1, 2); }
	function sine(x) { return -(Math.cos(Math.PI * x) - 1) / 2; }
	function clamp(x) { return x < 0 ? 0 : x > 1 ? 1 : x; }

	ui.style.transform = 'translateY(100%)';
	var start = Date.now();
	var intro = setInterval(function () {
		var t = (Date.now() - start) / 1000;
		/* logo: fade 0-0.35, rise 0-0.6, grow 0.6-0.8, shrink 0.8-1.0, hold to 1.4, fly 1.4-1.85, fade 1.85-2.0 */
		var a = outExpo(clamp(t / 0.35));
		var y = 100 * (1 - outExpo(clamp(t / 0.6)));
		var size = 180;
		if (t > 0.6 && t <= 0.8) size = 180 + 16 * outBack(clamp((t - 0.6) / 0.2));
		else if (t > 0.8 && t <= 1.0) size = 196 - 16 * sine(clamp((t - 0.8) / 0.2));
		if (t > 1.4) y = -1400 * inExpo(clamp((t - 1.4) / 0.45));
		if (t > 1.85) a = 1 - inExpo(clamp((t - 1.85) / 0.15));
		logo.style.opacity = a.toFixed(3);
		logo.style.width = size.toFixed(1) + 'px';
		logo.style.height = size.toFixed(1) + 'px';
		logo.style.transform = 'translateY(' + y.toFixed(1) + 'px)';

		/* UI slide-up: panels wait off-screen 1.75 s, then slide up (out_expo 0.45 s) */
		var u = t < 1.75 ? 1 : 1 - outExpo(clamp((t - 1.75) / 0.45));
		ui.style.transform = 'translateY(' + (u * 100).toFixed(2) + '%)';

		/* centre card: starts after the panel slide (2.2 s), rises 12 px (out_cubic 0.36 s) and fades in (0.28 s) */
		var c = clamp((t - 2.2) / 0.36);
		var rise = 1 - (1 - c) * (1 - c) * (1 - c);
		card.style.transform = 'translateY(' + ((1 - rise) * 12).toFixed(2) + 'px)';
		card.style.opacity = clamp((t - 2.2) / 0.28).toFixed(3);

		if (t >= 2.6) {
			clearInterval(intro);
			$('st-introwrap').style.display = 'none';
			ui.style.transform = 'none';
			card.style.transform = 'none';
			card.style.opacity = '1';
		}
	}, 33);
})();
