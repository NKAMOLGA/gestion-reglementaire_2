(function () {
	'use strict';

	var STORAGE_KEY = 'bcpme-theme';

	function applyTheme(theme) {
		document.documentElement.setAttribute('data-theme', theme);
		localStorage.setItem(STORAGE_KEY, theme);
		document.querySelectorAll('.theme-toggle').forEach(function (btn) {
			var icon = btn.querySelector('i');
			if (icon) {
				icon.className = theme === 'dark' ? 'bi bi-sun-fill' : 'bi bi-moon-fill';
			}
			btn.setAttribute('title', theme === 'dark' ? 'Mode clair' : 'Mode sombre');
			btn.setAttribute('aria-label', theme === 'dark' ? 'Activer le mode clair' : 'Activer le mode sombre');
		});
	}

	function initDate() {
		var el = document.getElementById('topbar-date');
		if (el) {
			el.textContent = new Date().toLocaleDateString('fr-FR', {
				weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
			});
		}
	}

	document.addEventListener('DOMContentLoaded', function () {
		applyTheme(localStorage.getItem(STORAGE_KEY) || 'light');
		initDate();

		document.querySelectorAll('.theme-toggle').forEach(function (btn) {
			btn.addEventListener('click', function () {
				var current = document.documentElement.getAttribute('data-theme') || 'light';
				applyTheme(current === 'dark' ? 'light' : 'dark');
			});
		});

		document.querySelectorAll('.login-alert').forEach(function (alert) {
			setTimeout(function () { alert.style.display = 'none'; }, 5000);
		});
	});
})();
