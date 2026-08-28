package com.example.evoagent.skill;

import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class SkillEvolutionPipelineRunRepository {

    private final Map<String, SkillEvolutionPipelineRun> runs = new ConcurrentHashMap<>();

    public SkillEvolutionPipelineRun save(SkillEvolutionPipelineRun run) {
        runs.put(run.id(), run);
        return run;
    }

    public Optional<SkillEvolutionPipelineRun> findById(String runId) {
        return Optional.ofNullable(runs.get(runId));
    }

    public List<SkillEvolutionPipelineRun> findAll() {
        return runs.values().stream()
                .sorted(Comparator.comparing(SkillEvolutionPipelineRun::startedAt).reversed())
                .toList();
    }
}
