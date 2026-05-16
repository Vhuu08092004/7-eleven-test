-- V11: Fix carts unique constraint for soft-delete support
-- Drop the old unique constraint that includes soft-deleted records
ALTER TABLE carts DROP CONSTRAINT carts_user_id_product_id_key;

-- Delete existing soft-deleted cart records that would conflict with the partial unique index
DELETE FROM carts WHERE is_deleted = true;

-- Create a partial unique index: only enforces uniqueness for non-deleted records
CREATE UNIQUE INDEX idx_carts_user_product_active ON carts(user_id, product_id) WHERE is_deleted = false;
