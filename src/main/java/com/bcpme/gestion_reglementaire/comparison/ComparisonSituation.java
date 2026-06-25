package com.bcpme.gestion_reglementaire.comparison;

public enum ComparisonSituation {
	HAUSSE("Hausse", "Surveiller impact monétaire"),
	BAISSE("Baisse", "Surveiller contraction"),
	STABILITE("Stabilité", "Neutre / à contextualiser"),
	NOUVEAU_COMPTE("Nouveau compte", "Vérifier conformité"),
	COMPTE_DISPARU("Compte disparu", "Risque d'omission"),
	VARIATION_ANORMALE("Variation anormale", "Alerte / justification requise");

	private final String label;
	private final String signalBeac;

	ComparisonSituation(String label, String signalBeac) {
		this.label = label;
		this.signalBeac = signalBeac;
	}

	public String label() {
		return label;
	}

	public String signalBeac() {
		return signalBeac;
	}
}
