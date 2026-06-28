(function () {
	'use strict';

	var STORAGE_KEY = 'bcpme-lang';
	var COOKIE_NAME = 'bcpme-lang';

	function getStoredLang() {
		var lang = localStorage.getItem(STORAGE_KEY);
		return lang === 'en' ? 'en' : 'fr';
	}

	function setCookie(lang) {
		document.cookie = COOKIE_NAME + '=' + lang + ';path=/;max-age=31536000;SameSite=Lax';
	}

	function applyLangToggleUi(lang) {
		document.querySelectorAll('.lang-toggle').forEach(function (btn) {
			var label = btn.querySelector('.lang-toggle__label');
			if (label) {
				label.textContent = lang === 'en' ? 'FR' : 'EN';
			}
			btn.setAttribute('title', lang === 'en' ? 'Switch to French' : 'Passer en anglais');
			btn.setAttribute('aria-label', lang === 'en' ? 'Switch to French' : 'Passer en anglais');
		});
	}

	function applyLang(lang, reloadIfNeeded) {
		document.documentElement.lang = lang;
		localStorage.setItem(STORAGE_KEY, lang);
		setCookie(lang);
		applyLangToggleUi(lang);

		if (reloadIfNeeded) {
			window.location.reload();
		}
	}

	function syncCookieFromStorage() {
		var lang = getStoredLang();
		document.documentElement.lang = lang;
		var match = document.cookie.match(new RegExp('(?:^|; )' + COOKIE_NAME + '=([^;]*)'));
		var cookieLang = match ? decodeURIComponent(match[1]) : null;
		if (cookieLang !== lang) {
			setCookie(lang);
			// Recharger seulement si le cookie existait déjà avec une autre valeur
			if (cookieLang !== null && cookieLang !== lang) {
				window.location.reload();
			}
		}
	}

	window.BcpmeLocale = {
		getLang: getStoredLang,
		applyLang: applyLang
	};

	syncCookieFromStorage();

	document.addEventListener('DOMContentLoaded', function () {
		applyLangToggleUi(getStoredLang());

		document.querySelectorAll('.lang-toggle').forEach(function (btn) {
			btn.addEventListener('click', function () {
				var next = getStoredLang() === 'fr' ? 'en' : 'fr';
				applyLang(next, true);
			});
		});
	});
})();
