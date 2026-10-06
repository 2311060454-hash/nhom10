ALTER TABLE orders ADD COLUMN coupon_code VARCHAR(40),ADD COLUMN discount DECIMAL(19,2) NOT NULL DEFAULT 0;
SET @old_total_check=(SELECT tc.CONSTRAINT_NAME FROM information_schema.TABLE_CONSTRAINTS tc
 JOIN information_schema.CHECK_CONSTRAINTS cc ON cc.CONSTRAINT_SCHEMA=tc.CONSTRAINT_SCHEMA AND cc.CONSTRAINT_NAME=tc.CONSTRAINT_NAME
 WHERE tc.TABLE_SCHEMA=DATABASE() AND tc.TABLE_NAME='orders' AND tc.CONSTRAINT_TYPE='CHECK' AND cc.CHECK_CLAUSE LIKE '%shipping_fee%' LIMIT 1);
SET @drop_total=CONCAT('ALTER TABLE orders DROP CHECK `',@old_total_check,'`');
PREPARE drop_total_check FROM @drop_total;
EXECUTE drop_total_check;
DEALLOCATE PREPARE drop_total_check;
ALTER TABLE orders ADD CONSTRAINT chk_order_amount CHECK(subtotal>=0 AND discount>=0 AND discount<=subtotal AND shipping_fee>=0 AND total=subtotal-discount+shipping_fee);
