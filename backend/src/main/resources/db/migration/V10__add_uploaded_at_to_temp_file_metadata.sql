-- Add missing uploaded_at column to temp_file_metadata
ALTER TABLE temp_file_metadata ADD COLUMN IF NOT EXISTS uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
