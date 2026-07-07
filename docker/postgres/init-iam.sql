-- Initialize IAM Service Database
-- This script runs when the PostgreSQL container starts for the first time

-- Application schema for IAM-owned tables (users, roles, tokens, etc.)
CREATE SCHEMA IF NOT EXISTS iamservice;

-- Default tenant schema pre-provisioned for SINGLE_TENANT mode and demo data
CREATE SCHEMA IF NOT EXISTS t_platform;
CREATE SCHEMA IF NOT EXISTS t_demo0001;
CREATE SCHEMA IF NOT EXISTS t_acme0001;

-- Set default search path: tenant schema first, then app schema, then public
ALTER DATABASE iamservice SET search_path TO t_platform, iamservice, public;

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE iamservice TO svc_iam_dba;
GRANT ALL PRIVILEGES ON SCHEMA public TO svc_iam_dba;
GRANT ALL PRIVILEGES ON SCHEMA iamservice TO svc_iam_dba;
GRANT ALL PRIVILEGES ON SCHEMA t_platform TO svc_iam_dba;
GRANT ALL PRIVILEGES ON SCHEMA t_demo0001 TO svc_iam_dba;
GRANT ALL PRIVILEGES ON SCHEMA t_acme0001 TO svc_iam_dba;

-- Utility trigger function for maintaining updated_at columns
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

SELECT 'IAM Service Database initialized successfully' AS status;
