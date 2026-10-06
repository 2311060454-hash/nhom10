CREATE TABLE payment_guard(id BIGINT PRIMARY KEY);
INSERT INTO payment_guard VALUES(1);
CREATE TABLE payments (
 order_id VARCHAR(36) PRIMARY KEY, user_id BIGINT NOT NULL, method VARCHAR(20) NOT NULL, state VARCHAR(30) NOT NULL,
 amount DECIMAL(19,2) NOT NULL, reference VARCHAR(190), created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 INDEX ix_payment_user(user_id),INDEX ix_payment_state(state),CHECK(amount>=0),CHECK(method IN ('COD','SIMULATED')),
 CHECK(state IN ('UNPAID','PAID','SIMULATED_PAID','SIMULATED_FAILED','SIMULATED_REFUNDED','CANCELLED'))
);
CREATE TABLE shipments (
 order_id VARCHAR(36) PRIMARY KEY,state VARCHAR(30) NOT NULL,recipient VARCHAR(120) NOT NULL,phone VARCHAR(20) NOT NULL,address VARCHAR(500) NOT NULL,
 carrier VARCHAR(120),tracking VARCHAR(120),assignee VARCHAR(120),shipping_fee DECIMAL(19,2) NOT NULL,carrier_cost DECIMAL(19,2),
 created_at DATETIME(6) NOT NULL,updated_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_shipment_payment FOREIGN KEY(order_id) REFERENCES payments(order_id),UNIQUE KEY uq_tracking(carrier,tracking),INDEX ix_shipment_state(state),
 CHECK(shipping_fee>=0 AND (carrier_cost IS NULL OR carrier_cost>=0)),CHECK(state IN ('NEW','SHIPPING','DELIVERED','DELIVERY_FAILED','RETURNED','CANCELLED'))
);
CREATE TABLE fulfillment_events (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,order_id VARCHAR(36) NOT NULL,action VARCHAR(30) NOT NULL,actor_id BIGINT NOT NULL,note VARCHAR(500),created_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_event_payment FOREIGN KEY(order_id) REFERENCES payments(order_id),INDEX ix_event_order(order_id,id)
);
CREATE TABLE payment_commands (
 id VARCHAR(100) PRIMARY KEY,order_id VARCHAR(36) NOT NULL,request_hash VARCHAR(64) NOT NULL,response MEDIUMTEXT NOT NULL,created_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_command_payment FOREIGN KEY(order_id) REFERENCES payments(order_id)
);
CREATE TABLE refunds (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,order_id VARCHAR(36) NOT NULL UNIQUE,amount DECIMAL(19,2) NOT NULL,state VARCHAR(30) NOT NULL,reason VARCHAR(500) NOT NULL,created_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_refund_payment FOREIGN KEY(order_id) REFERENCES payments(order_id),CHECK(amount>=0),CHECK(state='SIMULATED_REFUNDED')
);
