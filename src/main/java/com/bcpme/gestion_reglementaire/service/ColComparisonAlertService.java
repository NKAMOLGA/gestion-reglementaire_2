package com.bcpme.gestion_reglementaire.service;

import com.bcpme.gestion_reglementaire.comparison.ChangeType;
import com.bcpme.gestion_reglementaire.comparison.ColComparisonResult;
import com.bcpme.gestion_reglementaire.comparison.ColLineDiff;
import com.bcpme.gestion_reglementaire.comparison.ComparisonSituation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ColComparisonAlertService {

	private final MailService mailService;
	private final BigDecimal abnormalPercentThreshold;
	private final BigDecimal abnormalAbsoluteThreshold;

	public ColComparisonAlertService(
			MailService mailService,
			@Value("${app.comparison.abnormal-variation-percent:25}") BigDecimal abnormalPercentThreshold,
			@Value("${app.comparison.abnormal-variation-absolute:1000000}") BigDecimal abnormalAbsoluteThreshold) {
		this.mailService = mailService;
		this.abnormalPercentThreshold = abnormalPercentThreshold;
		this.abnormalAbsoluteThreshold = abnormalAbsoluteThreshold;
	}

	public void notifyComparison(ColComparisonResult result,
								 String mode,
								 String utilisateur,
								 Long comparisonId) {
		if (result.addedCount() + result.removedCount() + result.modifiedCount() == 0) {
			return;
		}

		try {
			String subject = buildSubject(result, mode);
			String body = buildBody(result, mode, utilisateur, comparisonId);
			mailService.envoyerAlerteReglementaire(subject, body);
		} catch (Exception e) {
			System.err.println("Erreur envoi mail comparaison COL : " + e.getMessage());
		}
	}

	private String buildSubject(ColComparisonResult result, String mode) {
		Map<ComparisonSituation, List<ColLineDiff>> grouped = groupBySituation(result);
		int alertCount = grouped.values().stream().mapToInt(List::size).sum();
		int abnormalCount = countAbnormalVariations(result);

		String prefix = abnormalCount > 0 ? "[ALERTE BEAC] " : "[Comparaison COL] ";
		return prefix + mode + " — " + alertCount + " variation(s)"
				+ (abnormalCount > 0 ? " dont " + abnormalCount + " anormale(s)" : "");
	}

	private String buildBody(ColComparisonResult result,
							 String mode,
							 String utilisateur,
							 Long comparisonId) {
		StringBuilder body = new StringBuilder();
		body.append("BC-PME SA — Alerte comparaison COL (Réserves obligatoires)\n");
		body.append("============================================================\n\n");
		body.append("Mode              : ").append(mode).append('\n');
		body.append("Utilisateur       : ").append(utilisateur != null ? utilisateur : "—").append('\n');
		if (comparisonId != null) {
			body.append("Réf. comparaison  : #").append(comparisonId).append('\n');
		}
		body.append("Fichier précédent : ").append(result.fileA().filename()).append('\n');
		body.append("Fichier actuel    : ").append(result.fileB().filename()).append('\n');
		body.append("Delta total       : ").append(formatAmount(result.totalDelta())).append('\n');
		body.append("Ajoutés           : ").append(result.addedCount()).append('\n');
		body.append("Supprimés         : ").append(result.removedCount()).append('\n');
		body.append("Modifiés          : ").append(result.modifiedCount()).append('\n');
		body.append("Stables           : ").append(result.unchangedCount()).append("\n\n");

		body.append("TABLEAU RÉCAPITULATIF — SIGNAUX BEAC\n");
		body.append("------------------------------------\n\n");

		Map<ComparisonSituation, List<ColLineDiff>> grouped = groupBySituation(result);
		appendSituationSection(body, ComparisonSituation.HAUSSE, grouped);
		appendSituationSection(body, ComparisonSituation.BAISSE, grouped);
		appendSituationSection(body, ComparisonSituation.STABILITE, grouped);
		appendSituationSection(body, ComparisonSituation.NOUVEAU_COMPTE, grouped);
		appendSituationSection(body, ComparisonSituation.COMPTE_DISPARU, grouped);
		appendAbnormalSection(body, result);

		body.append("\n---\n");
		body.append("Cette alerte concerne les postes de réserves obligatoires (compte courant, etc.).\n");
		body.append("Veuillez analyser les écarts signalés et documenter toute justification requise.\n");

		return body.toString();
	}

	private void appendSituationSection(StringBuilder body,
										ComparisonSituation situation,
										Map<ComparisonSituation, List<ColLineDiff>> grouped) {
		List<ColLineDiff> lines = grouped.getOrDefault(situation, List.of());
		if (lines.isEmpty()) {
			return;
		}

		body.append(situation.label().toUpperCase())
				.append(" — Signal BEAC : ")
				.append(situation.signalBeac())
				.append(" (")
				.append(lines.size())
				.append(" poste(s))\n");

		for (ColLineDiff diff : lines) {
			appendLineDetail(body, diff, isAbnormalVariation(diff));
		}
		body.append('\n');
	}

	private void appendAbnormalSection(StringBuilder body, ColComparisonResult result) {
		List<ColLineDiff> abnormal = result.diffs().stream()
				.filter(d -> d.changeType() != ChangeType.UNCHANGED)
				.filter(this::isAbnormalVariation)
				.toList();

		if (abnormal.isEmpty()) {
			return;
		}

		body.append(ComparisonSituation.VARIATION_ANORMALE.label().toUpperCase())
				.append(" — Signal BEAC : ")
				.append(ComparisonSituation.VARIATION_ANORMALE.signalBeac())
				.append(" (")
				.append(abnormal.size())
				.append(" poste(s))\n");

		for (ColLineDiff diff : abnormal) {
			appendLineDetail(body, diff, true);
			body.append("  → Écart : ")
					.append(formatVariationRate(diff))
					.append('\n');
		}
		body.append('\n');
	}

	private void appendLineDetail(StringBuilder body, ColLineDiff diff, boolean abnormal) {
		String libelle = diff.libelleB() != null ? diff.libelleB()
				: (diff.libelleA() != null ? diff.libelleA() : diff.code());

		body.append("  • ")
				.append(diff.code())
				.append(" | ")
				.append(libelle)
				.append(" | Préc. : ")
				.append(formatAmount(diff.montantA()))
				.append(" | Actuel : ")
				.append(formatAmount(diff.montantB()))
				.append(" | Δ : ")
				.append(formatAmount(diff.delta()));

		if (abnormal) {
			body.append(" [VARIATION ANORMALE]");
		}
		body.append('\n');
	}

	private Map<ComparisonSituation, List<ColLineDiff>> groupBySituation(ColComparisonResult result) {
		Map<ComparisonSituation, List<ColLineDiff>> grouped = new EnumMap<>(ComparisonSituation.class);
		for (ColLineDiff diff : result.diffs()) {
			if (diff.changeType() == ChangeType.UNCHANGED) {
				continue;
			}
			ComparisonSituation situation = classify(diff);
			grouped.computeIfAbsent(situation, key -> new ArrayList<>()).add(diff);
		}
		return grouped;
	}

	private ComparisonSituation classify(ColLineDiff diff) {
		return switch (diff.changeType()) {
			case ADDED -> ComparisonSituation.NOUVEAU_COMPTE;
			case REMOVED -> ComparisonSituation.COMPTE_DISPARU;
			case MODIFIED -> classifyModified(diff);
			case UNCHANGED -> ComparisonSituation.STABILITE;
		};
	}

	private ComparisonSituation classifyModified(ColLineDiff diff) {
		BigDecimal delta = diff.delta();
		if (delta == null || delta.compareTo(BigDecimal.ZERO) == 0) {
			return ComparisonSituation.STABILITE;
		}
		return delta.compareTo(BigDecimal.ZERO) > 0
				? ComparisonSituation.HAUSSE
				: ComparisonSituation.BAISSE;
	}

	private boolean isAbnormalVariation(ColLineDiff diff) {
		if (diff.changeType() == ChangeType.UNCHANGED) {
			return false;
		}

		BigDecimal delta = diff.delta();
		if (delta == null) {
			return false;
		}

		BigDecimal absDelta = delta.abs();
		if (absDelta.compareTo(abnormalAbsoluteThreshold) >= 0) {
			return true;
		}

		BigDecimal montantA = diff.montantA();
		if (montantA == null || montantA.compareTo(BigDecimal.ZERO) == 0) {
			BigDecimal montantB = diff.montantB();
			return montantB != null && montantB.abs().compareTo(abnormalAbsoluteThreshold) >= 0;
		}

		BigDecimal percent = absDelta
				.multiply(BigDecimal.valueOf(100))
				.divide(montantA.abs(), 2, RoundingMode.HALF_UP);

		return percent.compareTo(abnormalPercentThreshold) >= 0;
	}

	private int countAbnormalVariations(ColComparisonResult result) {
		return (int) result.diffs().stream()
				.filter(d -> d.changeType() != ChangeType.UNCHANGED)
				.filter(this::isAbnormalVariation)
				.count();
	}

	private String formatVariationRate(ColLineDiff diff) {
		BigDecimal delta = diff.delta();
		BigDecimal montantA = diff.montantA();

		if (delta == null) {
			return "—";
		}
		if (montantA == null || montantA.compareTo(BigDecimal.ZERO) == 0) {
			return "montant précédent nul — Δ " + formatAmount(delta);
		}

		BigDecimal percent = delta.abs()
				.multiply(BigDecimal.valueOf(100))
				.divide(montantA.abs(), 2, RoundingMode.HALF_UP);

		return percent.toPlainString() + " % (seuil " + abnormalPercentThreshold.toPlainString() + " %)";
	}

	private String formatAmount(BigDecimal amount) {
		if (amount == null) {
			return "—";
		}
		DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRENCH);
		symbols.setGroupingSeparator(' ');
		DecimalFormat format = new DecimalFormat("#,##0.##", symbols);
		return format.format(amount);
	}
}
