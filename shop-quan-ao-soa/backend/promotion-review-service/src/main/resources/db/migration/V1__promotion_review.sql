CREATE TABLE promotion_guard(id BIGINT PRIMARY KEY);
INSERT INTO promotion_guard VALUES(1);
CREATE TABLE coupons (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,code VARCHAR(40) NOT NULL UNIQUE,type VARCHAR(20) NOT NULL,discount_value DECIMAL(19,2) NOT NULL,
 minimum_total DECIMAL(19,2) NOT NULL,maximum_discount DECIMAL(19,2),total_limit INT NOT NULL,per_customer_limit INT NOT NULL,
 active BOOLEAN NOT NULL,starts_at DATETIME(6) NOT NULL,ends_at DATETIME(6) NOT NULL,created_at DATETIME(6) NOT NULL,updated_at DATETIME(6) NOT NULL,
 INDEX ix_coupon_active(active,starts_at,ends_at),CHECK(type IN ('PERCENT','FIXED')),
 CHECK(discount_value>0 AND minimum_total>=0 AND (maximum_discount IS NULL OR maximum_discount>=0)),
 CHECK(total_limit>0 AND per_customer_limit>0),CHECK(starts_at<ends_at)
);
CREATE TABLE coupon_usages (
 order_id VARCHAR(36) PRIMARY KEY,coupon_id BIGINT NOT NULL,user_id BIGINT NOT NULL,state VARCHAR(20) NOT NULL,
 discount DECIMAL(19,2) NOT NULL,original_subtotal DECIMAL(19,2) NOT NULL,expires_at DATETIME(6) NOT NULL,created_at DATETIME(6) NOT NULL,updated_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_coupon_usage FOREIGN KEY(coupon_id) REFERENCES coupons(id),INDEX ix_usage_quota(coupon_id,user_id,state),INDEX ix_usage_expiry(state,expires_at),
 CHECK(discount>=0),CHECK(state IN ('HELD','COMMITTED','RELEASED','EXPIRED'))
);
CREATE TABLE promotions (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,name VARCHAR(120) NOT NULL,target_type VARCHAR(20) NOT NULL,target_id BIGINT NOT NULL,
 type VARCHAR(20) NOT NULL,discount_value DECIMAL(19,2) NOT NULL,maximum_discount DECIMAL(19,2),active BOOLEAN NOT NULL,
 starts_at DATETIME(6) NOT NULL,ends_at DATETIME(6) NOT NULL,created_at DATETIME(6) NOT NULL,updated_at DATETIME(6) NOT NULL,
 INDEX ix_promotion_target(target_type,target_id,active,starts_at,ends_at),CHECK(target_type IN ('PRODUCT','CATEGORY')),
 CHECK(type IN ('PERCENT','FIXED')),CHECK(discount_value>0 AND (maximum_discount IS NULL OR maximum_discount>=0)),CHECK(starts_at<ends_at)
);
CREATE TABLE reviews (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id BIGINT NOT NULL,product_id BIGINT NOT NULL,stars INT NOT NULL,comment VARCHAR(1200) NOT NULL,
 state VARCHAR(20) NOT NULL,created_at DATETIME(6) NOT NULL,updated_at DATETIME(6) NOT NULL,
 UNIQUE KEY uq_review_author_product(user_id,product_id),INDEX ix_review_public(product_id,state,created_at),CHECK(stars BETWEEN 1 AND 5),CHECK(state IN ('PENDING','APPROVED','HIDDEN'))
);
CREATE TABLE wishlists (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id BIGINT NOT NULL,product_id BIGINT NOT NULL,created_at DATETIME(6) NOT NULL,
 UNIQUE KEY uq_wishlist_user_product(user_id,product_id),INDEX ix_wishlist_user(user_id,created_at)
);


