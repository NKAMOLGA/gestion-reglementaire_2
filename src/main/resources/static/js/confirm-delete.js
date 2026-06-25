(function () {
	var overlay = document.getElementById('deleteConfirmOverlay');
	if (!overlay) return;

	var messageEl = document.getElementById('deleteConfirmMessage');
	var linkEl = document.getElementById('deleteConfirmLink');
	var cancelBtn = document.getElementById('deleteConfirmCancel');

	function closeModal() {
		overlay.hidden = true;
		document.body.classList.remove('confirm-open');
	}

	window.openDeleteConfirm = function (url, message) {
		linkEl.href = url;
		messageEl.textContent = message || 'Voulez-vous vraiment supprimer cet élément ?';
		overlay.hidden = false;
		document.body.classList.add('confirm-open');
	};

	cancelBtn.addEventListener('click', closeModal);
	overlay.addEventListener('click', function (e) {
		if (e.target === overlay) closeModal();
	});
	document.addEventListener('keydown', function (e) {
		if (e.key === 'Escape' && !overlay.hidden) closeModal();
	});

	document.querySelectorAll('[data-delete-url]').forEach(function (btn) {
		btn.addEventListener('click', function (e) {
			e.preventDefault();
			openDeleteConfirm(btn.getAttribute('data-delete-url'), btn.getAttribute('data-delete-message'));
		});
	});
})();
