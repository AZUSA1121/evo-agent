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

CREATE TABLE IF NOT EXISTS agent_skill (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    version INTEGER NOT NULL,
    status VARCHAR(40) NOT NULL,
    source VARCHAR(40) NOT NULL,
    category VARCHAR(80),
    description TEXT,
    content TEXT NOT NULL,
    source_run_id VARCHAR(36),
    source_case_id VARCHAR(120),
    evaluation_results JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    activated_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_agent_skill_status
    ON agent_skill (status);

CREATE INDEX IF NOT EXISTS idx_agent_skill_category
    ON agent_skill (category);

CREATE INDEX IF NOT EXISTS idx_agent_skill_created_at
    ON agent_skill (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_agent_skill_source_run
    ON agent_skill (source_run_id);

CREATE TABLE IF NOT EXISTS evaluation_run (
    id VARCHAR(36) PRIMARY KEY,
    status VARCHAR(40) NOT NULL,
    dataset_name VARCHAR(160) NOT NULL,
    agent_name VARCHAR(160) NOT NULL,
    error_message TEXT,
    metrics JSONB NOT NULL DEFAULT '{}'::jsonb,
    case_results JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_evaluation_run_created_at
    ON evaluation_run (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_evaluation_run_status
    ON evaluation_run (status);

CREATE INDEX IF NOT EXISTS idx_evaluation_run_dataset
    ON evaluation_run (dataset_name);

CREATE TABLE IF NOT EXISTS skill_evolution_pipeline_run (
    id VARCHAR(36) PRIMARY KEY,
    status VARCHAR(40) NOT NULL,
    baseline_run_id VARCHAR(36),
    final_run_id VARCHAR(36),
    analyzed_failure_count INTEGER NOT NULL DEFAULT 0,
    generated_skill_count INTEGER NOT NULL DEFAULT 0,
    activated_skill_count INTEGER NOT NULL DEFAULT 0,
    rejected_skill_count INTEGER NOT NULL DEFAULT 0,
    before_metrics JSONB NOT NULL DEFAULT '{}'::jsonb,
    after_metrics JSONB NOT NULL DEFAULT '{}'::jsonb,
    analysis JSONB NOT NULL DEFAULT '{}'::jsonb,
    generation JSONB NOT NULL DEFAULT '{}'::jsonb,
    activation_decisions JSONB NOT NULL DEFAULT '[]'::jsonb,
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_skill_evolution_pipeline_run_started_at
    ON skill_evolution_pipeline_run (started_at DESC);

CREATE INDEX IF NOT EXISTS idx_skill_evolution_pipeline_run_status
    ON skill_evolution_pipeline_run (status);

CREATE INDEX IF NOT EXISTS idx_skill_evolution_pipeline_run_baseline
    ON skill_evolution_pipeline_run (baseline_run_id);

CREATE INDEX IF NOT EXISTS idx_skill_evolution_pipeline_run_final
    ON skill_evolution_pipeline_run (final_run_id);

CREATE TABLE IF NOT EXISTS review_report (
    id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(36) REFERENCES agent_task(id) ON DELETE SET NULL,
    repo VARCHAR(240) NOT NULL,
    pr_number INTEGER NOT NULL,
    status VARCHAR(60) NOT NULL,
    summary TEXT,
    ai_summary TEXT,
    risk_level VARCHAR(40),
    changed_file_count INTEGER NOT NULL DEFAULT 0,
    total_additions INTEGER NOT NULL DEFAULT 0,
    total_deletions INTEGER NOT NULL DEFAULT 0,
    key_changes JSONB NOT NULL DEFAULT '[]'::jsonb,
    test_suggestions JSONB NOT NULL DEFAULT '[]'::jsonb,
    context_files JSONB NOT NULL DEFAULT '[]'::jsonb,
    changed_files JSONB NOT NULL DEFAULT '[]'::jsonb,
    findings JSONB NOT NULL DEFAULT '[]'::jsonb,
    markdown TEXT,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_review_report_repo_pr
    ON review_report (repo, pr_number);

CREATE INDEX IF NOT EXISTS idx_review_report_task
    ON review_report (task_id);

CREATE INDEX IF NOT EXISTS idx_review_report_created_at
    ON review_report (created_at DESC);
