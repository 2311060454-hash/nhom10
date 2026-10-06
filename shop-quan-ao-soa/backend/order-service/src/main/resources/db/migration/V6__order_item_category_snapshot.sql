ALTER TABLE order_items ADD COLUMN category_id BIGINT NULL;
CREATE INDEX ix_order_item_category ON order_items(category_id);
