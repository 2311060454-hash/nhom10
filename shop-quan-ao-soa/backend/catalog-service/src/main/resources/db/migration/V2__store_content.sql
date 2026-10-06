CREATE TABLE banners (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,title VARCHAR(160) NOT NULL,subtitle VARCHAR(320),link_path VARCHAR(120) NOT NULL,
 image_id VARCHAR(40) UNIQUE,sort_order INT NOT NULL,active BOOLEAN NOT NULL,
 starts_at DATETIME(6) NOT NULL,ends_at DATETIME(6) NOT NULL,created_at DATETIME(6) NOT NULL,updated_at DATETIME(6) NOT NULL,
 INDEX ix_banner_active(active,starts_at,ends_at,sort_order),CHECK(starts_at<ends_at),CHECK(sort_order BETWEEN 0 AND 1000)
);
CREATE TABLE store_content (
 content_key VARCHAR(40) PRIMARY KEY,content_text TEXT NOT NULL,updated_at DATETIME(6) NOT NULL
);
INSERT INTO store_content(content_key,content_text,updated_at) VALUES
 ('ABOUT','',UTC_TIMESTAMP(6)),('CONTACT','',UTC_TIMESTAMP(6)),('PURCHASE_POLICY','',UTC_TIMESTAMP(6)),
 ('RETURN_POLICY','',UTC_TIMESTAMP(6)),('SHIPPING_POLICY','',UTC_TIMESTAMP(6)),('ANNOUNCEMENT','',UTC_TIMESTAMP(6));
