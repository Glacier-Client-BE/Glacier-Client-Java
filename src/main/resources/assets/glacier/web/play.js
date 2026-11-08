/* Glacier v7 play screen. Actions go through glacier.screen (ScreenApi). Keys typed while the page is open fill the
   search box; Backspace deletes. The world list is the vanilla world browser; servers come from the saved list. */
(function () {
	var S = glacier.screen;
	var KEY_BACKSPACE = 259;
	var query = '';
	var servers = [];

	function $(id) { return document.getElementById(id); }
	function on(id, fn) { $(id).addEventListener('click', fn); }

	function esc(s) {
		return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
	}

	/* tabs */
	var tabs = document.querySelectorAll('.pl-tab');
	function selectTab(name) {
		for (var i = 0; i < tabs.length; i++) {
			tabs[i].classList.toggle('pl-tab-on', tabs[i].getAttribute('data-tab') === name);
		}
		$('pl-worlds').style.display = name === 'worlds' ? 'flex' : 'none';
		$('pl-friends').style.display = name === 'friends' ? 'flex' : 'none';
		$('pl-servers').style.display = name === 'servers' ? 'flex' : 'none';
	}
	function bindTab(el) {
		el.addEventListener('click', function () { selectTab(el.getAttribute('data-tab')); });
	}
	for (var t = 0; t < tabs.length; t++) bindTab(tabs[t]);

	/* server list: rebuilt only when the list or the search text changes */
	function renderServers() {
		var q = query.toLowerCase();
		var html = '';
		var shown = 0;
		for (var i = 0; i < servers.length; i++) {
			var s = servers[i];
			if (q && s.name.toLowerCase().indexOf(q) < 0 && s.ip.toLowerCase().indexOf(q) < 0) continue;
			html += '<button class="pl-item" data-i="' + i + '"><span class="pl-iname">' + esc(s.name) +
				'</span><span class="pl-iip">' + esc(s.ip) + '</span></button>';
			shown++;
		}
		$('pl-list').innerHTML = html;
		$('pl-empty').style.display = shown === 0 ? 'block' : 'none';
	}

	/* world list: loaded async by Java, polled until it arrives */
	var worlds = null;
	function ago(ms) {
		if (!ms) return '';
		var d = Math.floor((Date.now() - ms) / 60000);
		if (d < 1) return 'just now';
		if (d < 60) return d + ' min ago';
		if (d < 1440) return Math.floor(d / 60) + ' h ago';
		return Math.floor(d / 1440) + ' d ago';
	}
	function renderWorlds() {
		if (worlds === null) return;
		var q = query.toLowerCase();
		var html = '';
		var shown = 0;
		for (var i = 0; i < worlds.length; i++) {
			var w = worlds[i];
			if (q && w.name.toLowerCase().indexOf(q) < 0) continue;
			html += '<button class="pl-item" data-i="' + i + '"' + (w.playable ? '' : ' disabled') + '><span class="pl-iname">' +
				esc(w.name) + '</span><span class="pl-iip">' + esc(w.mode) + (w.hardcore ? ' · Hardcore' : '') +
				' · ' + esc(ago(w.last)) + '</span></button>';
			shown++;
		}
		$('pl-wlist').innerHTML = html;
		$('pl-wempty').textContent = 'No worlds found.';
		$('pl-wempty').style.display = shown === 0 ? 'block' : 'none';
	}
	$('pl-wlist').addEventListener('click', function (e) {
		var el = e.target;
		while (el && el !== this && !el.classList.contains('pl-item')) el = el.parentElement;
		if (!el || el === this) return;
		S.playWorld(parseInt(el.getAttribute('data-i'), 10));
	});
	var pollTicks = 0;
	var poll = setInterval(function () {
		var w = S.worlds();
		if (w) { worlds = w; clearInterval(poll); renderWorlds(); return; }
		if (++pollTicks < 100) return; // 100 ticks of 100 ms = 10 s
		clearInterval(poll);
		$('pl-wlist').innerHTML = '';
		$('pl-wempty').textContent = 'Could not load worlds';
		$('pl-wempty').style.display = 'block';
	}, 100);

	$('pl-list').addEventListener('click', function (e) {
		var el = e.target;
		while (el && el !== this && !el.classList.contains('pl-item')) el = el.parentElement;
		if (!el || el === this) return;
		S.joinServer(parseInt(el.getAttribute('data-i'), 10));
	});

	function setQuery(v) {
		query = v;
		var q = $('pl-query');
		q.textContent = v === '' ? 'Search Worlds...' : v;
		q.classList.toggle('pl-query-on', v !== '');
		renderServers();
		renderWorlds();
	}

	/* buttons */
	on('pl-back', function () { S.close(); });
	on('pl-settings', function () { S.mods(); });
	on('pl-create', function () { S.createWorld(); });
	on('pl-browse', function () { S.singleplayer(); });
	on('pl-addfriend', function () { S.multiplayer(); });
	on('pl-code', function () { S.multiplayer(); });
	on('pl-addserver', function () { S.addServer(); });

	/* keys: keypress carries the typed character, keydown carries the key code */
	var root = document.querySelector('.pl-root');
	root.addEventListener('keypress', function (e) {
		if (e.char === undefined) return;
		setQuery(query + e.char);
	});
	/* Esc is left to the Java side, which closes the screen when the page does not consume it */
	root.addEventListener('keydown', function (e) {
		if (e.key === KEY_BACKSPACE && query.length > 0) setQuery(query.substring(0, query.length - 1));
	});

	servers = S.servers();
	renderServers();
})();
