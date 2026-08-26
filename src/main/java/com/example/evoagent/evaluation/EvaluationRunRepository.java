package com.example.evoagent.evaluation;

import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class EvaluationRunRepository {

    private final Map<String, EvaluationRun> runs = new ConcurrentHashMap<>();

    public EvaluationRun save(EvaluationRun run) {
        runs.put(run.id(), run);
        return run;
    }

    public Optional<EvaluationRun> findById(String runId) {
        return Optional.ofNullable(runs.get(runId));
    }

    public List<EvaluationRun> findAll() {
        return runs.values().stream()
                .sorted(Comparator.comparing(EvaluationRun::createdAt).reversed())
                .toList();
    }
}
