ALTER TABLE orders DROP CHECK chk_order_state;
ALTER TABLE orders ADD CONSTRAINT chk_order_state CHECK(state IN ('PROCESSING','AWAITING_PAYMENT','PLACED','CONFIRMED','PACKING','SHIPPED','DELIVERED','DELIVERY_FAILED','COMPLETED','RETURNED','CANCEL_PENDING','CANCELLED','FAIL_PENDING','FAILED'));
CREATE TABLE return_requests (
 id VARCHAR(36) PRIMARY KEY,order_id VARCHAR(36) NOT NULL,user_id BIGINT NOT NULL,request_key VARCHAR(100) NOT NULL,
 reason VARCHAR(500) NOT NULL,state VARCHAR(30) NOT NULL,decision_note VARCHAR(500),refund_reference VARCHAR(190),
 actor_id BIGINT,created_at DATETIME(6) NOT NULL,updated_at DATETIME(6) NOT NULL,last_error VARCHAR(500),
 CONSTRAINT fk_return_order FOREIGN KEY(order_id) REFERENCES orders(id),
 UNIQUE KEY uq_return_order(order_id),INDEX ix_return_state(state,updated_at),INDEX ix_return_user(user_id,created_at),
 CHECK(state IN ('REQUESTED','APPROVED','REJECTED','RECEIVED','REFUND_PENDING','REFUND_CONFIRMING','REFUNDED'))
);
CREATE TABLE return_status_history (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,return_id VARCHAR(36) NOT NULL,state VARCHAR(30) NOT NULL,actor_id BIGINT NOT NULL,
 note VARCHAR(500),created_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_return_history FOREIGN KEY(return_id) REFERENCES return_requests(id),INDEX ix_return_history(return_id,id)
);
