/**
 * Sur la page de connexion : empêche le retour vers une page précédente
 * (ex. tableau de bord après déconnexion).
 */
(function () {
	if (document.body.classList.contains('login-body')) {
		history.pushState(null, '', location.href);
		window.addEventListener('popstate', function () {
			history.pushState(null, '', location.href);
		});
	}
})();
