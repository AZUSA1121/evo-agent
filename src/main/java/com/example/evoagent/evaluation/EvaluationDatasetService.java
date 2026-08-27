package com.example.evoagent.evaluation;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class EvaluationDatasetService {

    private static final String CASE_RESOURCE_PATTERN = "classpath:evaluation/cases/*.json";

    private final ObjectMapper objectMapper;
    private final ResourcePatternResolver resourcePatternResolver;

    public EvaluationDatasetService(
            ObjectMapper objectMapper,
            ResourcePatternResolver resourcePatternResolver
    ) {
        this.objectMapper = objectMapper;
        this.resourcePatternResolver = resourcePatternResolver;
    }

    public List<EvaluationCase> loadCases() {
        try {
            Resource[] resources = resourcePatternResolver.getResources(CASE_RESOURCE_PATTERN);
            return List.of(resources).stream()
                    .map(this::readCase)
                    .sorted(Comparator.comparing(EvaluationCase::id))
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load evaluation cases", e);
        }
    }

    public Optional<EvaluationCase> findCase(String caseId) {
        return loadCases().stream()
                .filter(evaluationCase -> evaluationCase.id().equals(caseId))
                .findFirst();
    }

    private EvaluationCase readCase(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, EvaluationCase.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read evaluation case: " + resource.getFilename(), e);
        }
    }
}
