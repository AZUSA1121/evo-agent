SELECT 'CREATE DATABASE evo_agent'
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = 'evo_agent'
)\gexec

\connect evo_agent

CREATE TABLE IF NOT EXISTS agent_task (
    id VARCHAR(36) PRIMARY KEY,
    owner VARCHAR(120) NOT NULL,
    repo VARCHAR(120) NOT NULL,
    pr_number INTEGER NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    status VARCHAR(40) NOT NULL,
    current_node VARCHAR(80),
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_agent_task_created_at
    ON agent_task (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_agent_task_repo_pr
    ON agent_task (owner, repo, pr_number);

CREATE INDEX IF NOT EXISTS idx_agent_task_status
    ON agent_task (status);

CREATE TABLE IF NOT EXISTS agent_execution (
    id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL REFERENCES agent_task(id) ON DELETE CASCADE,
    node_name VARCHAR(80) NOT NULL,
    status VARCHAR(40) NOT NULL,
    input TEXT,
    output TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ,
    duration_ms BIGINT
);

CREATE INDEX IF NOT EXISTS idx_agent_execution_task_started_at
    ON agent_execution (task_id, started_at);

CREATE INDEX IF NOT EXISTS idx_agent_execution_task_node
    ON agent_execution (task_id, node_name);

CREATE TABLE IF NOT EXISTS agent_checkpoint (
    task_id VARCHAR(36) PRIMARY KEY REFERENCES agent_task(id) ON DELETE CASCADE,
    context_json JSONB NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_agent_checkpoint_updated_at
    ON agent_checkpoint (updated_at DESC);
