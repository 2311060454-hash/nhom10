SET @payment_method_check=(SELECT tc.CONSTRAINT_NAME FROM information_schema.TABLE_CONSTRAINTS tc
 JOIN information_schema.CHECK_CONSTRAINTS cc ON cc.CONSTRAINT_SCHEMA=tc.CONSTRAINT_SCHEMA AND cc.CONSTRAINT_NAME=tc.CONSTRAINT_NAME
 WHERE tc.TABLE_SCHEMA=DATABASE() AND tc.TABLE_NAME='payments' AND tc.CONSTRAINT_TYPE='CHECK' AND cc.CHECK_CLAUSE LIKE '%method%' LIMIT 1);
SET @drop_payment_method_check=CONCAT('ALTER TABLE payments DROP CHECK `',@payment_method_check,'`');
PREPARE payment_method_statement FROM @drop_payment_method_check;
EXECUTE payment_method_statement;
DEALLOCATE PREPARE payment_method_statement;
ALTER TABLE payments ADD CONSTRAINT chk_payment_method CHECK(method IN ('COD','SIMULATED','BANK_TRANSFER'));
ALTER TABLE payments ADD COLUMN bank_name VARCHAR(120),ADD COLUMN bank_account_number VARCHAR(50),ADD COLUMN bank_account_name VARCHAR(160);
