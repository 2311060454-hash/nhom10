CREATE TABLE order_guard (id BIGINT PRIMARY KEY);
INSERT INTO order_guard VALUES (1);
CREATE TABLE carts (user_id BIGINT PRIMARY KEY, revision BIGINT NOT NULL, updated_at DATETIME(6) NOT NULL);
CREATE TABLE cart_items (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, cart_id BIGINT NOT NULL, variant_id BIGINT NOT NULL, quantity INT NOT NULL,
 CONSTRAINT fk_cart_item FOREIGN KEY (cart_id) REFERENCES carts(user_id), UNIQUE KEY uq_cart_variant(cart_id,variant_id), CHECK(quantity BETWEEN 1 AND 99)
);
CREATE TABLE orders (
 id VARCHAR(36) PRIMARY KEY, user_id BIGINT NOT NULL, request_key VARCHAR(100) NOT NULL, request_hash VARCHAR(64) NOT NULL,
 state VARCHAR(30) NOT NULL, recipient VARCHAR(120) NOT NULL, phone VARCHAR(20) NOT NULL, address VARCHAR(500) NOT NULL, note VARCHAR(500),
 subtotal DECIMAL(19,2) NOT NULL, shipping_fee DECIMAL(19,2) NOT NULL, total DECIMAL(19,2) NOT NULL,
 created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 UNIQUE KEY uq_order_request(user_id,request_key), INDEX ix_order_user(user_id,created_at), INDEX ix_order_state(state,updated_at),
 CHECK(subtotal>=0 AND shipping_fee>=0 AND total=subtotal+shipping_fee),
 CHECK(state IN ('PROCESSING','PLACED','CONFIRMED','PACKING','CANCEL_PENDING','CANCELLED','FAIL_PENDING','FAILED'))
);
CREATE TABLE order_items (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, order_id VARCHAR(36) NOT NULL, variant_id BIGINT NOT NULL, product_id BIGINT NOT NULL,
 sku VARCHAR(100) NOT NULL, product_name VARCHAR(200) NOT NULL, size VARCHAR(50) NOT NULL, color VARCHAR(50) NOT NULL,
 quantity INT NOT NULL, unit_price DECIMAL(19,2) NOT NULL,
 CONSTRAINT fk_order_item FOREIGN KEY(order_id) REFERENCES orders(id), UNIQUE KEY uq_order_variant(order_id,variant_id),
 CHECK(quantity BETWEEN 1 AND 99), CHECK(unit_price>=0)
);
CREATE TABLE order_status_history (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, order_id VARCHAR(36) NOT NULL, state VARCHAR(30) NOT NULL, actor_id BIGINT NOT NULL, created_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_order_history FOREIGN KEY(order_id) REFERENCES orders(id), INDEX ix_history_order(order_id,created_at)
);
