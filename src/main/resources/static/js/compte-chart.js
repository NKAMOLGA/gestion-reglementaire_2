(function () {
	'use strict';

	function readJson(id) {
		var el = document.getElementById(id);
		if (!el || !el.textContent.trim()) {
			return null;
		}
		try {
			return JSON.parse(el.textContent);
		} catch (e) {
			console.error('JSON parse error for #' + id, e);
			return null;
		}
	}

	function chartLabels() {
		var el = document.getElementById('chart-i18n');
		if (!el) {
			return { amount: 'Montant', date: 'Date' };
		}
		return {
			amount: el.getAttribute('data-amount') || 'Montant',
			date: el.getAttribute('data-date') || 'Date'
		};
	}

	function themeColors() {
		var isDark = document.documentElement.getAttribute('data-theme') === 'dark';
		return {
			text: isDark ? '#e2e8f0' : '#334155',
			grid: isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.08)'
		};
	}

	function formatDate(isoDate) {
		if (!isoDate) {
			return '';
		}
		var parts = isoDate.split('-');
		if (parts.length !== 3) {
			return isoDate;
		}
		return parts[2] + '/' + parts[1] + '/' + parts[0];
	}

	function toNumber(value) {
		if (value == null) {
			return 0;
		}
		if (typeof value === 'number') {
			return value;
		}
		var n = parseFloat(value);
		return isNaN(n) ? 0 : n;
	}

	function initAccountTrendChart() {
		if (typeof Chart === 'undefined') {
			return;
		}

		var canvas = document.getElementById('accountTrendChart');
		var trendData = readJson('trend-data');
		if (!canvas || !trendData || !trendData.length) {
			return;
		}

		var labels = chartLabels();
		var colors = themeColors();
		var xLabels = trendData.map(function (p) { return formatDate(p.businessDate); });
		var amountData = trendData.map(function (p) { return toNumber(p.montant); });

		new Chart(canvas, {
			type: 'line',
			data: {
				labels: xLabels,
				datasets: [{
					label: labels.amount,
					data: amountData,
					borderColor: '#92BC48',
					backgroundColor: 'rgba(146, 188, 72, 0.15)',
					fill: true,
					tension: 0.3,
					pointRadius: 4,
					pointHoverRadius: 6
				}]
			},
			options: {
				responsive: true,
				maintainAspectRatio: false,
				plugins: {
					legend: { labels: { color: colors.text } }
				},
				scales: {
					x: {
						title: { display: true, text: labels.date, color: colors.text },
						ticks: { color: colors.text, maxRotation: 45, minRotation: 20 },
						grid: { color: colors.grid }
					},
					y: {
						title: { display: true, text: labels.amount, color: colors.text },
						ticks: { color: colors.text },
						grid: { color: colors.grid }
					}
				}
			}
		});
	}

	if (document.readyState === 'loading') {
		document.addEventListener('DOMContentLoaded', initAccountTrendChart);
	} else {
		initAccountTrendChart();
	}
})();
