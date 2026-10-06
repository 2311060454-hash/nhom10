ALTER TABLE orders ADD COLUMN returned_amount DECIMAL(19,2) NOT NULL DEFAULT 0;
ALTER TABLE return_requests ADD COLUMN mode VARCHAR(10) NOT NULL DEFAULT 'FULL', ADD COLUMN refund_amount DECIMAL(19,2) NOT NULL DEFAULT 0;
UPDATE return_requests r JOIN orders o ON o.id=r.order_id SET r.refund_amount=o.total;
ALTER TABLE return_requests ADD CONSTRAINT chk_return_mode CHECK(mode IN ('FULL','PARTIAL'));
CREATE TABLE return_items (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,return_id VARCHAR(36) NOT NULL,variant_id BIGINT NOT NULL,quantity INT NOT NULL,
 CONSTRAINT fk_return_item_request FOREIGN KEY(return_id) REFERENCES return_requests(id),
 CONSTRAINT uq_return_item_variant UNIQUE(return_id,variant_id),CHECK(quantity>0)
);
