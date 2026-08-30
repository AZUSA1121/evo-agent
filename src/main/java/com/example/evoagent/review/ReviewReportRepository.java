package com.example.evoagent.review;

import com.example.evoagent.github.PullRequestFile;
import com.example.evoagent.runtime.AgentTask;
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
import java.util.UUID;

@Repository
public class ReviewReportRepository {

    private static final TypeReference<List<String>> STRING_LIST =
            new TypeReference<>() {
            };
    private static final TypeReference<List<PullRequestFile>> PULL_REQUEST_FILES =
            new TypeReference<>() {
            };
    private static final TypeReference<List<Finding>> FINDINGS =
            new TypeReference<>() {
            };

    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;

    public ReviewReportRepository(JdbcClient jdbcClient, ObjectMapper objectMapper) {
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper;
    }

    public ReviewReport save(AgentTask task, ReviewReport report, String markdown) {
        jdbcClient.sql("""
                        INSERT INTO review_report (
                            id,
                            task_id,
                            repo,
                            pr_number,
                            status,
                            summary,
                            ai_summary,
                            risk_level,
                            changed_file_count,
                            total_additions,
                            total_deletions,
                            key_changes,
                            test_suggestions,
                            context_files,
                            changed_files,
                            findings,
                            markdown,
                            created_at
                        )
                        VALUES (
                            :id,
                            :taskId,
                            :repo,
                            :prNumber,
                            :status,
                            :summary,
                            :aiSummary,
                            :riskLevel,
                            :changedFileCount,
                            :totalAdditions,
                            :totalDeletions,
                            CAST(:keyChanges AS jsonb),
                            CAST(:testSuggestions AS jsonb),
                            CAST(:contextFiles AS jsonb),
                            CAST(:changedFiles AS jsonb),
                            CAST(:findings AS jsonb),
                            :markdown,
                            :createdAt
                        )
                        """)
                .param("id", UUID.randomUUID().toString())
                .param("taskId", task.id())
                .param("repo", report.repo())
                .param("prNumber", report.prNumber())
                .param("status", report.status())
                .param("summary", report.summary())
                .param("aiSummary", report.aiSummary())
                .param("riskLevel", report.riskLevel())
                .param("changedFileCount", report.changedFileCount())
                .param("totalAdditions", report.totalAdditions())
                .param("totalDeletions", report.totalDeletions())
                .param("keyChanges", writeJson(report.keyChanges()))
                .param("testSuggestions", writeJson(report.testSuggestions()))
                .param("contextFiles", writeJson(report.contextFiles()))
                .param("changedFiles", writeJson(report.changedFiles()))
                .param("findings", writeJson(report.findings()))
                .param("markdown", markdown)
                .param("createdAt", timestamp(report.createdAt()))
                .update();
        return report;
    }

    public List<ReviewReportView> findAll() {
        return jdbcClient.sql("SELECT * FROM review_report ORDER BY created_at DESC")
                .query(this::mapReportView)
                .list();
    }

    public List<ReviewReportView> findByRepositoryAndPullRequest(String repo, int prNumber) {
        return jdbcClient.sql("""
                        SELECT *
                        FROM review_report
                        WHERE repo = :repo AND pr_number = :prNumber
                        ORDER BY created_at DESC
                        """)
                .param("repo", repo)
                .param("prNumber", prNumber)
                .query(this::mapReportView)
                .list();
    }

    public Optional<ReviewReportView> findLatestByTaskId(String taskId) {
        return jdbcClient.sql("""
                        SELECT *
                        FROM review_report
                        WHERE task_id = :taskId
                        ORDER BY created_at DESC
                        LIMIT 1
                        """)
                .param("taskId", taskId)
                .query(this::mapReportView)
                .optional();
    }

    private ReviewReportView mapReportView(ResultSet rs, int rowNum) throws SQLException {
        String taskId = rs.getString("task_id");
        return new ReviewReportView(
                rs.getString("id"),
                taskId,
                taskRef(taskId),
                rs.getString("repo"),
                rs.getInt("pr_number"),
                rs.getString("status"),
                rs.getString("summary"),
                rs.getInt("changed_file_count"),
                rs.getInt("total_additions"),
                rs.getInt("total_deletions"),
                rs.getString("ai_summary"),
                rs.getString("risk_level"),
                readJsonList(rs.getString("key_changes")),
                readJsonList(rs.getString("test_suggestions")),
                readJsonList(rs.getString("context_files")),
                readPullRequestFiles(rs.getString("changed_files")),
                readFindings(rs.getString("findings")),
                rs.getString("markdown"),
                toInstant(rs, "created_at")
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize review report JSON", e);
        }
    }

    private List<String> readJsonList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize review report string list", e);
        }
    }

    private List<PullRequestFile> readPullRequestFiles(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, PULL_REQUEST_FILES);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize review report changed files", e);
        }
    }

    private List<Finding> readFindings(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, FINDINGS);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize review report findings", e);
        }
    }

    private Instant toInstant(ResultSet rs, String columnName) throws SQLException {
        java.sql.Timestamp timestamp = rs.getTimestamp(columnName);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private String taskRef(String taskId) {
        if (taskId == null || taskId.length() <= 8) {
            return taskId;
        }
        return taskId.substring(0, 8);
    }
}
