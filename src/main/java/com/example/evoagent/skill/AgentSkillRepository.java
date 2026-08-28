package com.example.evoagent.skill;

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
public class AgentSkillRepository {

    private static final TypeReference<List<SkillEvaluationResult>> SKILL_EVALUATION_RESULTS =
            new TypeReference<>() {
            };

    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;

    public AgentSkillRepository(JdbcClient jdbcClient, ObjectMapper objectMapper) {
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper;
    }

    public AgentSkill save(AgentSkill skill) {
        jdbcClient.sql("""
                        INSERT INTO agent_skill (
                            id,
                            name,
                            version,
                            status,
                            source,
                            category,
                            description,
                            content,
                            source_run_id,
                            source_case_id,
                            evaluation_results,
                            created_at,
                            updated_at,
                            activated_at
                        )
                        VALUES (
                            :id,
                            :name,
                            :version,
                            :status,
                            :source,
                            :category,
                            :description,
                            :content,
                            :sourceRunId,
                            :sourceCaseId,
                            CAST(:evaluationResults AS jsonb),
                            :createdAt,
                            :updatedAt,
                            :activatedAt
                        )
                        ON CONFLICT (id) DO UPDATE SET
                            name = EXCLUDED.name,
                            version = EXCLUDED.version,
                            status = EXCLUDED.status,
                            source = EXCLUDED.source,
                            category = EXCLUDED.category,
                            description = EXCLUDED.description,
                            content = EXCLUDED.content,
                            source_run_id = EXCLUDED.source_run_id,
                            source_case_id = EXCLUDED.source_case_id,
                            evaluation_results = EXCLUDED.evaluation_results,
                            updated_at = EXCLUDED.updated_at,
                            activated_at = EXCLUDED.activated_at
                        """)
                .param("id", skill.id())
                .param("name", skill.name())
                .param("version", skill.version())
                .param("status", skill.status().name())
                .param("source", skill.source().name())
                .param("category", skill.category())
                .param("description", skill.description())
                .param("content", skill.content())
                .param("sourceRunId", skill.sourceRunId())
                .param("sourceCaseId", skill.sourceCaseId())
                .param("evaluationResults", writeJson(skill.evaluationResults() == null ? List.of() : skill.evaluationResults()))
                .param("createdAt", timestamp(skill.createdAt()))
                .param("updatedAt", timestamp(skill.updatedAt()))
                .param("activatedAt", timestamp(skill.activatedAt()))
                .update();
        return skill;
    }

    public Optional<AgentSkill> findById(String skillId) {
        return jdbcClient.sql("SELECT * FROM agent_skill WHERE id = :id")
                .param("id", skillId)
                .query(this::mapSkill)
                .optional();
    }

    public List<AgentSkill> findAll() {
        return jdbcClient.sql("SELECT * FROM agent_skill ORDER BY created_at DESC")
                .query(this::mapSkill)
                .list();
    }

    public List<AgentSkill> findActiveSkills() {
        return jdbcClient.sql("""
                        SELECT *
                        FROM agent_skill
                        WHERE status = 'ACTIVE'
                        ORDER BY activated_at DESC NULLS LAST, updated_at DESC
                        """)
                .query(this::mapSkill)
                .list();
    }

    private AgentSkill mapSkill(ResultSet rs, int rowNum) throws SQLException {
        return new AgentSkill(
                rs.getString("id"),
                rs.getString("name"),
                rs.getInt("version"),
                SkillStatus.valueOf(rs.getString("status")),
                SkillSource.valueOf(rs.getString("source")),
                rs.getString("category"),
                rs.getString("description"),
                rs.getString("content"),
                rs.getString("source_run_id"),
                rs.getString("source_case_id"),
                readJsonList(rs.getString("evaluation_results")),
                toInstant(rs, "created_at"),
                toInstant(rs, "updated_at"),
                toInstant(rs, "activated_at")
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize skill JSON", e);
        }
    }

    private List<SkillEvaluationResult> readJsonList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, SKILL_EVALUATION_RESULTS);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize skill evaluation results", e);
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
