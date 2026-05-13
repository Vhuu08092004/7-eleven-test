-- V5: Insert default users (password is BCrypt encoded)
-- Admin: Admin@123 (BCrypt hash)
-- User: User@123 (BCrypt hash)
INSERT INTO users (email, password, role, created_at, updated_at, is_deleted) VALUES
('admin@7eleven.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.S8G/4hOiLHEJ7qQ3K', 'ROLE_ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
('user@7eleven.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.S8G/4hOiLHEJ7qQ3K', 'ROLE_USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE);
