/* Glacier v7 progress screen. Java pushes state with glacierSetProgress(percent, title, canClose); percent -1 means
   unknown. Exit calls glacier.screen.close(), which closes the vanilla screen only when it allows Esc (saving cannot be
   cancelled), so Exit is hidden when canClose is false. The engine ignores @keyframes, so the unknown state sweeps the
   bar from JS. */
var glacierProgressAnim = { pct: -1, title: '' };
var glacierSweepTimer = null;
var glacierSweepPos = 0;

function glacierSweep(fill, on) {
	if (on && glacierSweepTimer === null) {
		glacierSweepPos = 0;
		glacierSweepTimer = setInterval(function () {
			// the 30% segment travels from fully left of the track (-30%) to fully right of it (100%)
			glacierSweepPos = (glacierSweepPos + 2) % 130;
			fill.style.left = (glacierSweepPos - 30) + '%';
			fill.style.width = '30%';
		}, 33);
	} else if (!on && glacierSweepTimer !== null) {
		clearInterval(glacierSweepTimer);
		glacierSweepTimer = null;
		fill.style.left = '0%';
	}
}

function glacierSetProgress(pct, title, canClose) {
	var fill = document.getElementById('pg-fill');
	var pctEl = document.getElementById('pg-pct');
	var titleEl = document.getElementById('pg-title');
	var exitEl = document.getElementById('pg-exit');
	glacierProgressAnim.pct = pct;
	if (titleEl && title !== glacierProgressAnim.title) {
		glacierProgressAnim.title = title;
		titleEl.textContent = title;
	}
	if (exitEl) exitEl.style.display = canClose ? '' : 'none';
	if (!fill || !pctEl) return;
	if (pct < 0) {
		pctEl.textContent = '...';
		glacierSweep(fill, true);
	} else {
		glacierSweep(fill, false);
		var p = Math.max(0, Math.min(100, Math.round(pct)));
		pctEl.textContent = p + '%';
		fill.style.width = p + '%';
	}
}

(function () {
	var S = glacier.screen;
	var spin = document.getElementById('pg-spin');
	var tick = 0;

	document.getElementById('pg-exit').addEventListener('click', function () { S.close(); });

	/* spinner: the glyph pulses between full and 55% opacity, one full cycle every 0.9 s */
	setInterval(function () {
		tick = (tick + 1) % 18;
		var phase = tick / 18;
		var a = phase < 0.5 ? 1 - 0.9 * (phase * 2) : 0.1 + 0.9 * ((phase - 0.5) * 2);
		spin.style.opacity = 0.55 + 0.45 * a;
	}, 50);
})();
