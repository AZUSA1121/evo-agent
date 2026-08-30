SELECT 'CREATE DATABASE evo_agent'
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = 'evo_agent'
)\gexec

\echo 'Database evo_agent is ready.'
\echo 'Runtime schema is managed by Flyway migrations under src/main/resources/db/migration.'
\echo 'Start the Spring Boot application to apply or verify schema migrations automatically.'
