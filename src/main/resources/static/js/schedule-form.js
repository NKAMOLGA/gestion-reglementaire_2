(function () {
	'use strict';

	var typeSelect = document.getElementById('typeRecurrence');
	var monthlySection = document.getElementById('monthlySection');
	var dailySection = document.getElementById('dailySection');
	var selectAllMonthsBtn = document.getElementById('selectAllMonths');
	var selectAllDaysBtn = document.getElementById('selectAllDays');
	var form = document.getElementById('scheduleForm');

	function toggleRecurrenceSections() {
		if (!typeSelect || !monthlySection || !dailySection) {
			return;
		}
		var isMonthly = typeSelect.value === 'MONTHLY';
		monthlySection.style.display = isMonthly ? '' : 'none';
		dailySection.style.display = isMonthly ? 'none' : '';
	}

	function toggleAll(name, checked) {
		document.querySelectorAll('input[name="' + name + '"]').forEach(function (input) {
			input.checked = checked;
		});
	}

	function allChecked(name) {
		var inputs = document.querySelectorAll('input[name="' + name + '"]');
		if (!inputs.length) {
			return false;
		}
		return Array.prototype.every.call(inputs, function (input) {
			return input.checked;
		});
	}

	if (typeSelect) {
		typeSelect.addEventListener('change', toggleRecurrenceSections);
		toggleRecurrenceSections();
	}

	if (selectAllMonthsBtn) {
		selectAllMonthsBtn.addEventListener('click', function () {
			toggleAll('selectedMonths', !allChecked('selectedMonths'));
		});
	}

	if (selectAllDaysBtn) {
		selectAllDaysBtn.addEventListener('click', function () {
			toggleAll('selectedDays', !allChecked('selectedDays'));
		});
	}

	if (form) {
		if (form.getAttribute('data-is-new') === 'true') {
			toggleAll('selectedMonths', false);
			toggleAll('selectedDays', false);
		}

		form.addEventListener('submit', function (event) {
			if (typeSelect && typeSelect.value === 'MONTHLY') {
				var daysChecked = document.querySelectorAll('input[name="selectedDays"]:checked').length;
				var monthsChecked = document.querySelectorAll('input[name="selectedMonths"]:checked').length;
				if (daysChecked === 0 || monthsChecked === 0) {
					event.preventDefault();
					alert('Sélectionnez au moins un mois et un jour du mois.');
				}
			}
		});
	}
})();
