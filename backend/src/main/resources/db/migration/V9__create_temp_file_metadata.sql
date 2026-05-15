-- Create temp_file_metadata table for temporary upload management
CREATE TABLE IF NOT EXISTS temp_file_metadata (
    id BIGSERIAL PRIMARY KEY,
    file_key VARCHAR(64) NOT NULL UNIQUE,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(512) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    uploader_ip VARCHAR(45),
    owner_id BIGINT,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_promoted BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for cleanup queries
CREATE INDEX IF NOT EXISTS idx_temp_file_expires_at ON temp_file_metadata (expires_at);
CREATE INDEX IF NOT EXISTS idx_temp_file_is_deleted ON temp_file_metadata (is_deleted);
CREATE INDEX IF NOT EXISTS idx_temp_file_owner ON temp_file_metadata (owner_id);
