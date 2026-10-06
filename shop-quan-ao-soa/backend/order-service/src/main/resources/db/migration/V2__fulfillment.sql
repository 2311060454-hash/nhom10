ALTER TABLE orders ADD COLUMN payment_method VARCHAR(20) NOT NULL DEFAULT 'COD',
 ADD COLUMN payment_state VARCHAR(30) NOT NULL DEFAULT 'UNPAID',
 ADD COLUMN shipping_state VARCHAR(30) NOT NULL DEFAULT 'NEW',
 ADD COLUMN pending_command_id VARCHAR(36);
-- V1 dùng CHECK không đặt tên; tra tên thật để không phụ thuộc tên MySQL tự sinh.
SET @old_state_check=(SELECT tc.CONSTRAINT_NAME FROM information_schema.TABLE_CONSTRAINTS tc
 JOIN information_schema.CHECK_CONSTRAINTS cc ON cc.CONSTRAINT_SCHEMA=tc.CONSTRAINT_SCHEMA AND cc.CONSTRAINT_NAME=tc.CONSTRAINT_NAME
 WHERE tc.TABLE_SCHEMA=DATABASE() AND tc.TABLE_NAME='orders' AND tc.CONSTRAINT_TYPE='CHECK' AND cc.CHECK_CLAUSE LIKE '%state%' LIMIT 1);
SET @drop_check=CONCAT('ALTER TABLE orders DROP CHECK `',@old_state_check,'`');
PREPARE drop_state_check FROM @drop_check;
EXECUTE drop_state_check;
DEALLOCATE PREPARE drop_state_check;
ALTER TABLE orders ADD CONSTRAINT chk_order_state CHECK(state IN ('PROCESSING','AWAITING_PAYMENT','PLACED','CONFIRMED','PACKING','SHIPPED','DELIVERED','DELIVERY_FAILED','COMPLETED','CANCEL_PENDING','CANCELLED','FAIL_PENDING','FAILED'));
CREATE TABLE fulfillment_commands (
 id VARCHAR(36) PRIMARY KEY,order_id VARCHAR(36) NOT NULL,request_key VARCHAR(100) NOT NULL,request_hash VARCHAR(64) NOT NULL,
 action VARCHAR(30) NOT NULL,state VARCHAR(20) NOT NULL,actor_id BIGINT NOT NULL,payload TEXT NOT NULL,failure VARCHAR(500),created_at DATETIME(6) NOT NULL,updated_at DATETIME(6) NOT NULL,
 UNIQUE KEY uq_fulfillment_request(order_id,request_key),INDEX ix_fulfillment_order(order_id,created_at),
 CONSTRAINT fk_fulfillment_order FOREIGN KEY(order_id) REFERENCES orders(id),CHECK(state IN ('PENDING','DONE','REJECTED'))
);
