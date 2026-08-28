package com.example.evoagent.evaluation;

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
public class EvaluationRunRepository {

    private static final TypeReference<List<EvaluationCaseResult>> EVALUATION_CASE_RESULTS =
            new TypeReference<>() {
            };

    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;

    public EvaluationRunRepository(JdbcClient jdbcClient, ObjectMapper objectMapper) {
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper;
    }

    public EvaluationRun save(EvaluationRun run) {
        jdbcClient.sql("""
                        INSERT INTO evaluation_run (
                            id,
                            status,
                            dataset_name,
                            agent_name,
                            error_message,
                            metrics,
                            case_results,
                            created_at,
                            started_at,
                            finished_at
                        )
                        VALUES (
                            :id,
                            :status,
                            :datasetName,
                            :agentName,
                            :errorMessage,
                            CAST(:metrics AS jsonb),
                            CAST(:caseResults AS jsonb),
                            :createdAt,
                            :startedAt,
                            :finishedAt
                        )
                        ON CONFLICT (id) DO UPDATE SET
                            status = EXCLUDED.status,
                            dataset_name = EXCLUDED.dataset_name,
                            agent_name = EXCLUDED.agent_name,
                            error_message = EXCLUDED.error_message,
                            metrics = EXCLUDED.metrics,
                            case_results = EXCLUDED.case_results,
                            started_at = EXCLUDED.started_at,
                            finished_at = EXCLUDED.finished_at
                        """)
                .param("id", run.id())
                .param("status", run.status().name())
                .param("datasetName", run.datasetName())
                .param("agentName", run.agentName())
                .param("errorMessage", run.errorMessage())
                .param("metrics", writeJson(run.metrics()))
                .param("caseResults", writeJson(run.caseResults() == null ? List.of() : run.caseResults()))
                .param("createdAt", timestamp(run.createdAt()))
                .param("startedAt", timestamp(run.startedAt()))
                .param("finishedAt", timestamp(run.finishedAt()))
                .update();
        return run;
    }

    public Optional<EvaluationRun> findById(String runId) {
        return jdbcClient.sql("SELECT * FROM evaluation_run WHERE id = :id")
                .param("id", runId)
                .query(this::mapRun)
                .optional();
    }

    public List<EvaluationRun> findAll() {
        return jdbcClient.sql("SELECT * FROM evaluation_run ORDER BY created_at DESC")
                .query(this::mapRun)
                .list();
    }

    private EvaluationRun mapRun(ResultSet rs, int rowNum) throws SQLException {
        return new EvaluationRun(
                rs.getString("id"),
                EvaluationRunStatus.valueOf(rs.getString("status")),
                rs.getString("dataset_name"),
                rs.getString("agent_name"),
                rs.getString("error_message"),
                readJson(rs.getString("metrics"), EvaluationMetrics.class),
                readJsonList(rs.getString("case_results")),
                toInstant(rs, "created_at"),
                toInstant(rs, "started_at"),
                toInstant(rs, "finished_at")
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize evaluation run JSON", e);
        }
    }

    private <T> T readJson(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize evaluation run JSON", e);
        }
    }

    private List<EvaluationCaseResult> readJsonList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, EVALUATION_CASE_RESULTS);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize evaluation case results", e);
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
