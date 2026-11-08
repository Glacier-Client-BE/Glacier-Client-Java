/* Glacier quick bar: back (close), MODS (full menu), grid toggle.
 * The alignment grid and HUD dragging are drawn natively by HudQuickScreen, which reads the
 * 'active' class of #qb-grid to know whether the grid is on.
 */
(function () {
	var gridBtn = document.getElementById('qb-grid');
	var shown = null;

	/* Applies glacier.gridOn() to the toggle button. Only touches the DOM on change. */
	function sync() {
		var on = false;
		try {
			on = !!glacier.gridOn();
		} catch (e) { /* bridge not ready */ }
		if (on === shown) return;
		shown = on;
		if (on) {
			gridBtn.classList.add('active');
		} else {
			gridBtn.classList.remove('active');
		}
	}

	document.getElementById('qb-back').addEventListener('click', function () {
		glacier.close();
	});

	document.getElementById('qb-mods').addEventListener('click', function () {
		glacier.openMenu();
	});

	gridBtn.addEventListener('click', function () {
		glacier.toggleGrid();
		sync();
	});

	sync();
})();
