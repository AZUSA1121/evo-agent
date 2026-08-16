package com.example.evoagent.runtime;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class AgentRuntimeRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    private final RowMapper<AgentTask> taskRowMapper = (rs, rowNum) -> mapTask(rs);
    private final RowMapper<AgentExecution> executionRowMapper = (rs, rowNum) -> mapExecution(rs);

    public AgentRuntimeRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public AgentTask saveTask(AgentTask task) {
        jdbcTemplate.update("""
                        INSERT INTO agent_task (
                            id, owner, repo, pr_number, event_type, status, current_node,
                            error_message, created_at, updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (id) DO UPDATE SET
                            owner = EXCLUDED.owner,
                            repo = EXCLUDED.repo,
                            pr_number = EXCLUDED.pr_number,
                            event_type = EXCLUDED.event_type,
                            status = EXCLUDED.status,
                            current_node = EXCLUDED.current_node,
                            error_message = EXCLUDED.error_message,
                            created_at = EXCLUDED.created_at,
                            updated_at = EXCLUDED.updated_at
                        """,
                task.id(),
                task.owner(),
                task.repo(),
                task.prNumber(),
                task.eventType(),
                task.status().name(),
                task.currentNode() == null ? null : task.currentNode().name(),
                task.errorMessage(),
                timestamp(task.createdAt()),
                timestamp(task.updatedAt())
        );
        return task;
    }

    public Optional<AgentTask> findTask(String taskId) {
        try {
            AgentTask task = jdbcTemplate.queryForObject(
                    """
                            SELECT id, owner, repo, pr_number, event_type, status, current_node,
                                   error_message, created_at, updated_at
                            FROM agent_task
                            WHERE id = ?
                            """,
                    taskRowMapper,
                    taskId
            );
            return Optional.ofNullable(task);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<AgentTask> findTasksByIdPrefix(String taskIdPrefix) {
        return jdbcTemplate.query(
                """
                        SELECT id, owner, repo, pr_number, event_type, status, current_node,
                               error_message, created_at, updated_at
                        FROM agent_task
                        WHERE id LIKE ?
                        ORDER BY created_at DESC
                        """,
                taskRowMapper,
                taskIdPrefix + "%"
        );
    }

    public List<AgentTask> findAllTasks() {
        return jdbcTemplate.query(
                """
                        SELECT id, owner, repo, pr_number, event_type, status, current_node,
                               error_message, created_at, updated_at
                        FROM agent_task
                        ORDER BY created_at DESC
                        """,
                taskRowMapper
        );
    }

    public AgentExecution saveExecution(AgentExecution execution) {
        jdbcTemplate.update("""
                        INSERT INTO agent_execution (
                            id, task_id, node_name, status, input, output, retry_count,
                            error_message, started_at, finished_at, duration_ms
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (id) DO UPDATE SET
                            task_id = EXCLUDED.task_id,
                            node_name = EXCLUDED.node_name,
                            status = EXCLUDED.status,
                            input = EXCLUDED.input,
                            output = EXCLUDED.output,
                            retry_count = EXCLUDED.retry_count,
                            error_message = EXCLUDED.error_message,
                            started_at = EXCLUDED.started_at,
                            finished_at = EXCLUDED.finished_at,
                            duration_ms = EXCLUDED.duration_ms
                        """,
                execution.id(),
                execution.taskId(),
                execution.nodeName().name(),
                execution.status().name(),
                execution.input(),
                execution.output(),
                execution.retryCount(),
                execution.errorMessage(),
                timestamp(execution.startedAt()),
                timestamp(execution.finishedAt()),
                execution.durationMs()
        );
        return execution;
    }

    public List<AgentExecution> findExecutions(String taskId) {
        return jdbcTemplate.query(
                """
                        SELECT id, task_id, node_name, status, input, output, retry_count,
                               error_message, started_at, finished_at, duration_ms
                        FROM agent_execution
                        WHERE task_id = ?
                        ORDER BY started_at
                        """,
                executionRowMapper,
                taskId
        );
    }

    public void saveCheckpoint(String taskId, RuntimeContext context) {
        String contextJson = toJson(context.copy());
        jdbcTemplate.update("""
                        INSERT INTO agent_checkpoint (task_id, context_json, updated_at)
                        VALUES (?, ?::jsonb, ?)
                        ON CONFLICT (task_id) DO UPDATE SET
                            context_json = EXCLUDED.context_json,
                            updated_at = EXCLUDED.updated_at
                        """,
                taskId,
                contextJson,
                timestamp(Instant.now())
        );
    }

    public Optional<RuntimeContext> findCheckpoint(String taskId) {
        try {
            String contextJson = jdbcTemplate.queryForObject(
                    """
                            SELECT context_json::text
                            FROM agent_checkpoint
                            WHERE task_id = ?
                            """,
                    String.class,
                    taskId
            );
            return Optional.of(fromJson(contextJson));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    private AgentTask mapTask(ResultSet rs) throws SQLException {
        return new AgentTask(
                rs.getString("id"),
                rs.getString("owner"),
                rs.getString("repo"),
                rs.getInt("pr_number"),
                rs.getString("event_type"),
                TaskStatus.valueOf(rs.getString("status")),
                runtimeNodeNameOrNull(rs.getString("current_node")),
                rs.getString("error_message"),
                instant(rs, "created_at"),
                instant(rs, "updated_at")
        );
    }

    private AgentExecution mapExecution(ResultSet rs) throws SQLException {
        return new AgentExecution(
                rs.getString("id"),
                rs.getString("task_id"),
                RuntimeNodeName.valueOf(rs.getString("node_name")),
                NodeStatus.valueOf(rs.getString("status")),
                rs.getString("input"),
                rs.getString("output"),
                rs.getInt("retry_count"),
                rs.getString("error_message"),
                instant(rs, "started_at"),
                instantOrNull(rs, "finished_at"),
                longOrNull(rs, "duration_ms")
        );
    }

    private RuntimeNodeName runtimeNodeNameOrNull(String value) {
        if (value == null) {
            return null;
        }
        return RuntimeNodeName.valueOf(value);
    }

    private Instant instant(ResultSet rs, String columnName) throws SQLException {
        return rs.getTimestamp(columnName).toInstant();
    }

    private Timestamp timestamp(Instant instant) {
        if (instant == null) {
            return null;
        }
        return Timestamp.from(instant);
    }

    private Instant instantOrNull(ResultSet rs, String columnName) throws SQLException {
        java.sql.Timestamp timestamp = rs.getTimestamp(columnName);
        if (timestamp == null) {
            return null;
        }
        return timestamp.toInstant();
    }

    private Long longOrNull(ResultSet rs, String columnName) throws SQLException {
        long value = rs.getLong(columnName);
        if (rs.wasNull()) {
            return null;
        }
        return value;
    }

    private String toJson(RuntimeContext context) {
        try {
            return objectMapper.writeValueAsString(context);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialize runtime checkpoint", e);
        }
    }

    private RuntimeContext fromJson(String contextJson) {
        try {
            return objectMapper.readValue(contextJson, RuntimeContext.class);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to deserialize runtime checkpoint", e);
        }
    }
}
