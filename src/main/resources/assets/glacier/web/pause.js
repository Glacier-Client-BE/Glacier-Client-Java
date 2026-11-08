/* Glacier v7 pause screen. Actions go through glacier.screen (ScreenApi). Esc closes the confirm box first. */
(function () {
	var S = glacier.screen;
	var KEY_ESC = 256;
	function $(id) { return document.getElementById(id); }
	function on(id, fn) { $(id).addEventListener('click', fn); }

	var confirmEl = $('pz-confirm');
	var infoEl = $('pz-infotext');

	function showConfirm(v) { confirmEl.style.display = v ? 'flex' : 'none'; }

	on('pz-return', function () { S.close(); });
	on('pz-quit', function () { showConfirm(true); });
	on('pz-cancel', function () { showConfirm(false); });
	on('pz-leave', function () { S.disconnect(); });
	on('pz-adv', function () { S.achievements(); });
	on('pz-options', function () { S.options(); });
	on('pz-wardrobe', function () { S.cosmetics(); });
	on('pz-mods', function () { S.mods(); });
	on('pz-shot', function () { S.screenshot(); });
	on('pz-share', function () { S.shareToLan(); });
	on('pz-info', function () { infoEl.style.display = infoEl.style.display === 'none' ? 'block' : 'none'; });

	if (!S.canShareToLan()) $('pz-share').style.display = 'none';

	document.querySelector('.pz-root').addEventListener('keydown', function (e) {
		if (e.key !== KEY_ESC) return;
		if (confirmEl.style.display === 'flex') {
			showConfirm(false);
			S.escape();
		}
	});
})();
