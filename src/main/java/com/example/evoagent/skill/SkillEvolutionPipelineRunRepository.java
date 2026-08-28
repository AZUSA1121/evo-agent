package com.example.evoagent.skill;

import com.example.evoagent.evaluation.EvaluationMetrics;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class SkillEvolutionPipelineRunRepository {

    private static final TypeReference<List<SkillActivationDecision>> ACTIVATION_DECISIONS =
            new TypeReference<>() {
            };

    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;

    public SkillEvolutionPipelineRunRepository(JdbcClient jdbcClient, ObjectMapper objectMapper) {
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper;
    }

    public SkillEvolutionPipelineRun save(SkillEvolutionPipelineRun run) {
        jdbcClient.sql("""
                        INSERT INTO skill_evolution_pipeline_run (
                            id,
                            status,
                            baseline_run_id,
                            final_run_id,
                            analyzed_failure_count,
                            generated_skill_count,
                            activated_skill_count,
                            rejected_skill_count,
                            before_metrics,
                            after_metrics,
                            analysis,
                            generation,
                            activation_decisions,
                            started_at,
                            finished_at
                        )
                        VALUES (
                            :id,
                            :status,
                            :baselineRunId,
                            :finalRunId,
                            :analyzedFailureCount,
                            :generatedSkillCount,
                            :activatedSkillCount,
                            :rejectedSkillCount,
                            CAST(:beforeMetrics AS jsonb),
                            CAST(:afterMetrics AS jsonb),
                            CAST(:analysis AS jsonb),
                            CAST(:generation AS jsonb),
                            CAST(:activationDecisions AS jsonb),
                            :startedAt,
                            :finishedAt
                        )
                        ON CONFLICT (id) DO UPDATE SET
                            status = EXCLUDED.status,
                            baseline_run_id = EXCLUDED.baseline_run_id,
                            final_run_id = EXCLUDED.final_run_id,
                            analyzed_failure_count = EXCLUDED.analyzed_failure_count,
                            generated_skill_count = EXCLUDED.generated_skill_count,
                            activated_skill_count = EXCLUDED.activated_skill_count,
                            rejected_skill_count = EXCLUDED.rejected_skill_count,
                            before_metrics = EXCLUDED.before_metrics,
                            after_metrics = EXCLUDED.after_metrics,
                            analysis = EXCLUDED.analysis,
                            generation = EXCLUDED.generation,
                            activation_decisions = EXCLUDED.activation_decisions,
                            finished_at = EXCLUDED.finished_at
                        """)
                .param("id", run.id())
                .param("status", run.status())
                .param("baselineRunId", run.baselineRunId())
                .param("finalRunId", run.finalRunId())
                .param("analyzedFailureCount", run.analyzedFailureCount())
                .param("generatedSkillCount", run.generatedSkillCount())
                .param("activatedSkillCount", run.activatedSkillCount())
                .param("rejectedSkillCount", run.rejectedSkillCount())
                .param("beforeMetrics", writeJson(run.beforeMetrics()))
                .param("afterMetrics", writeJson(run.afterMetrics()))
                .param("analysis", writeJson(run.analysis()))
                .param("generation", writeJson(run.generation()))
                .param("activationDecisions", writeJson(run.activationDecisions() == null ? List.of() : run.activationDecisions()))
                .param("startedAt", timestamp(run.startedAt()))
                .param("finishedAt", timestamp(run.finishedAt()))
                .update();
        return run;
    }

    public Optional<SkillEvolutionPipelineRun> findById(String runId) {
        return jdbcClient.sql("SELECT * FROM skill_evolution_pipeline_run WHERE id = :id")
                .param("id", runId)
                .query(this::mapRun)
                .optional();
    }

    public List<SkillEvolutionPipelineRun> findAll() {
        return jdbcClient.sql("SELECT * FROM skill_evolution_pipeline_run ORDER BY started_at DESC")
                .query(this::mapRun)
                .list();
    }

    private SkillEvolutionPipelineRun mapRun(ResultSet rs, int rowNum) throws SQLException {
        return new SkillEvolutionPipelineRun(
                rs.getString("id"),
                rs.getString("status"),
                rs.getString("baseline_run_id"),
                rs.getString("final_run_id"),
                rs.getInt("analyzed_failure_count"),
                rs.getInt("generated_skill_count"),
                rs.getInt("activated_skill_count"),
                rs.getInt("rejected_skill_count"),
                readJson(rs.getString("before_metrics"), EvaluationMetrics.class),
                readJson(rs.getString("after_metrics"), EvaluationMetrics.class),
                readJson(rs.getString("analysis"), FailureAnalysisResult.class),
                readJson(rs.getString("generation"), SkillGenerationResult.class),
                readJsonList(rs.getString("activation_decisions")),
                toInstant(rs, "started_at"),
                toInstant(rs, "finished_at")
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize skill evolution pipeline JSON", e);
        }
    }

    private <T> T readJson(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize skill evolution pipeline JSON", e);
        }
    }

    private List<SkillActivationDecision> readJsonList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, ACTIVATION_DECISIONS);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize skill evolution activation decisions", e);
        }
    }

    private Instant toInstant(ResultSet rs, String columnName) throws SQLException {
        java.sql.Timestamp timestamp = rs.getTimestamp(columnName);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
