-- Create API keys table
CREATE TABLE api_keys (
    id BIGSERIAL PRIMARY KEY,
    api_key VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    permissions TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP,
    expires_at TIMESTAMP
);

-- Create index on api_key for fast lookups
CREATE INDEX idx_api_keys_api_key ON api_keys(api_key);

-- Create index on role for role-based queries
CREATE INDEX idx_api_keys_role ON api_keys(role);

-- Create index on is_active for filtering active keys
CREATE INDEX idx_api_keys_is_active ON api_keys(is_active);

-- Insert some default API keys for testing
-- Note: In production, these should be generated securely and stored securely
INSERT INTO api_keys (api_key, name, role, permissions, is_active, created_at) VALUES
('admin-key-123456789', 'Admin API Key', 'ADMIN', '["READ", "WRITE", "DELETE", "EXECUTE"]', true, CURRENT_TIMESTAMP),
('operator-key-987654321', 'Operator API Key', 'OPERATOR', '["READ", "WRITE", "EXECUTE"]', true, CURRENT_TIMESTAMP),
('viewer-key-456789123', 'Viewer API Key', 'VIEWER', '["READ"]', true, CURRENT_TIMESTAMP),
('executor-key-789123456', 'Executor API Key', 'EXECUTOR', '["EXECUTE"]', true, CURRENT_TIMESTAMP);