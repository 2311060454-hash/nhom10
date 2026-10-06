CREATE TABLE categories(id BIGINT PRIMARY KEY AUTO_INCREMENT,name VARCHAR(120) NOT NULL UNIQUE,parent_id BIGINT NULL,active BIT NOT NULL DEFAULT 1,FOREIGN KEY(parent_id) REFERENCES categories(id));
CREATE TABLE brands(id BIGINT PRIMARY KEY AUTO_INCREMENT,name VARCHAR(120) NOT NULL UNIQUE,active BIT NOT NULL DEFAULT 1);
CREATE TABLE products(
 id BIGINT PRIMARY KEY AUTO_INCREMENT,code VARCHAR(50) NOT NULL UNIQUE,name VARCHAR(200) NOT NULL,category_id BIGINT NOT NULL,brand_id BIGINT NOT NULL,
 description VARCHAR(10000) NOT NULL,material VARCHAR(120) NOT NULL,style VARCHAR(120) NOT NULL,gender VARCHAR(10) NOT NULL,active BIT NOT NULL,featured BIT NOT NULL,version BIGINT NOT NULL DEFAULT 0,
 created_at DATETIME(6) NOT NULL,updated_at DATETIME(6) NOT NULL,
 FOREIGN KEY(category_id) REFERENCES categories(id),FOREIGN KEY(brand_id) REFERENCES brands(id),INDEX ix_product_name(name),INDEX ix_product_category(category_id,active),INDEX ix_product_brand(brand_id,active),
 CHECK(gender IN ('NAM','NU','UNISEX')));
CREATE TABLE product_variants(
 id BIGINT PRIMARY KEY AUTO_INCREMENT,product_id BIGINT NOT NULL,sku VARCHAR(80) NOT NULL UNIQUE,size VARCHAR(20) NOT NULL,color VARCHAR(50) NOT NULL,
 cost_price DECIMAL(15,2) NOT NULL,price DECIMAL(15,2) NOT NULL,sale_price DECIMAL(15,2),active BIT NOT NULL,
 UNIQUE(product_id,size,color),FOREIGN KEY(product_id) REFERENCES products(id),INDEX ix_variant_filter(size,color,active),
 CHECK(cost_price>=0),CHECK(price>0),CHECK(sale_price IS NULL OR (sale_price>0 AND sale_price<=price)));
CREATE TABLE product_images(id VARCHAR(40) PRIMARY KEY,product_id BIGINT NOT NULL,media_type VARCHAR(30) NOT NULL,byte_size BIGINT NOT NULL,created_at DATETIME(6) NOT NULL,
 FOREIGN KEY(product_id) REFERENCES products(id),INDEX ix_image_product(product_id));
