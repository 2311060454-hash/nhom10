CREATE TABLE partial_return_restocks (
 return_id VARCHAR(36) PRIMARY KEY,order_id VARCHAR(36) NOT NULL,request_hash VARCHAR(64) NOT NULL,created_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_partial_restock_order FOREIGN KEY(order_id) REFERENCES reservations(order_id),
 CONSTRAINT uq_partial_restock_order UNIQUE(order_id)
);
