package com.bcpme.gestion_reglementaire.service;

import com.bcpme.gestion_reglementaire.comparison.*;
import com.bcpme.gestion_reglementaire.entity.ColComparisonHistory;
import com.bcpme.gestion_reglementaire.repository.ColComparisonHistoryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ColComparisonHistoryService {

	private final ColComparisonHistoryRepository repository;
	private final ObjectMapper objectMapper;

	public ColComparisonHistoryService(ColComparisonHistoryRepository repository) {
		this.repository = repository;
		this.objectMapper = new ObjectMapper()
				.registerModule(new JavaTimeModule())
				.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	}

	public ColComparisonHistory save(ColComparisonResult result,
									 String mode,
									 String utilisateur,
									 Long comparisonScheduleId,
									 Long generationScheduleId) {
		ColComparisonHistory history = new ColComparisonHistory();
		history.setDateComparaison(java.time.LocalDateTime.now());
		history.setUtilisateur(utilisateur);
		history.setFichierA(result.fileA().filename());
		history.setFichierB(result.fileB().filename());
		history.setDateFichierA(result.fileA().fileDate());
		history.setDateFichierB(result.fileB().fileDate());
		history.setComptesA(result.fileA().accountCount());
		history.setComptesB(result.fileB().accountCount());
		history.setMode(mode);
		history.setComparisonScheduleId(comparisonScheduleId);
		history.setGenerationScheduleId(generationScheduleId);
		history.setAddedCount(result.addedCount());
		history.setRemovedCount(result.removedCount());
		history.setModifiedCount(result.modifiedCount());
		history.setUnchangedCount(result.unchangedCount());
		history.setTotalDelta(result.totalDelta());
		history.setDetailsJson(serializeDiffs(result.diffs()));
		return repository.save(history);
	}

	public ColComparisonResult toResult(ColComparisonHistory history) {
		ColFile fileA = new ColFile(
				history.getFichierA(),
				"",
				history.getDateFichierA(),
				Map.of(),
				history.getComptesA() != null ? history.getComptesA() : 0
		);
		ColFile fileB = new ColFile(
				history.getFichierB(),
				"",
				history.getDateFichierB(),
				Map.of(),
				history.getComptesB() != null ? history.getComptesB() : 0
		);

		List<ColLineDiff> diffs = deserializeDiffs(history.getDetailsJson());

		return new ColComparisonResult(
				fileA,
				fileB,
				diffs,
				history.getAddedCount() != null ? history.getAddedCount() : 0,
				history.getRemovedCount() != null ? history.getRemovedCount() : 0,
				history.getModifiedCount() != null ? history.getModifiedCount() : 0,
				history.getUnchangedCount() != null ? history.getUnchangedCount() : 0,
				history.getTotalDelta() != null ? history.getTotalDelta() : java.math.BigDecimal.ZERO
		);
	}

	public ColComparisonHistory findById(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Comparaison introuvable : " + id));
	}

	public List<ColComparisonHistory> findAll() {
		return repository.findAllByOrderByDateComparaisonDesc();
	}

	public List<ColComparisonHistory> findBySuivi(Long comparisonScheduleId) {
		return repository.findByComparisonScheduleIdOrderByDateComparaisonDesc(comparisonScheduleId);
	}

	public List<ColComparisonHistory> findBySuiviAsc(Long comparisonScheduleId) {
		return repository.findByComparisonScheduleIdOrderByDateComparaisonAsc(comparisonScheduleId);
	}

	public ComparisonTrendPoint toTrendPoint(ColComparisonHistory history) {
		return new ComparisonTrendPoint(
				history.getDateComparaison(),
				history.getFichierA(),
				history.getFichierB(),
				history.getComptesA() != null ? history.getComptesA() : 0,
				history.getComptesB() != null ? history.getComptesB() : 0,
				history.getAddedCount() != null ? history.getAddedCount() : 0,
				history.getRemovedCount() != null ? history.getRemovedCount() : 0,
				history.getModifiedCount() != null ? history.getModifiedCount() : 0,
				history.getTotalDelta() != null ? history.getTotalDelta() : java.math.BigDecimal.ZERO,
				history.getId()
		);
	}

	private String serializeDiffs(List<ColLineDiff> diffs) {
		try {
			List<Map<String, Object>> payload = diffs.stream().map(d -> {
				Map<String, Object> map = new LinkedHashMap<>();
				map.put("code", d.code());
				map.put("libelleA", d.libelleA());
				map.put("libelleB", d.libelleB());
				map.put("montantA", d.montantA() != null ? d.montantA().toPlainString() : null);
				map.put("montantB", d.montantB() != null ? d.montantB().toPlainString() : null);
				map.put("delta", d.delta() != null ? d.delta().toPlainString() : null);
				map.put("changeType", d.changeType().name());
				return map;
			}).toList();
			return objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Impossible de sérialiser les différences", e);
		}
	}

	private List<ColLineDiff> deserializeDiffs(String json) {
		if (json == null || json.isBlank()) {
			return Collections.emptyList();
		}
		try {
			List<Map<String, Object>> payload = objectMapper.readValue(json, new TypeReference<>() {});
			return payload.stream().map(this::toDiff).toList();
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Impossible de lire les différences enregistrées", e);
		}
	}

	private ColLineDiff toDiff(Map<String, Object> map) {
		return new ColLineDiff(
				(String) map.get("code"),
				(String) map.get("libelleA"),
				(String) map.get("libelleB"),
				parseDecimal(map.get("montantA")),
				parseDecimal(map.get("montantB")),
				parseDecimal(map.get("delta")),
				ChangeType.valueOf((String) map.get("changeType"))
		);
	}

	private java.math.BigDecimal parseDecimal(Object value) {
		if (value == null) {
			return null;
		}
		return new java.math.BigDecimal(value.toString());
	}
}
