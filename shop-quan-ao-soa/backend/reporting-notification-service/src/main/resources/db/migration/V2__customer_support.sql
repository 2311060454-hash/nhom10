CREATE TABLE support_guard(id BIGINT PRIMARY KEY);
INSERT INTO support_guard VALUES(1);
CREATE TABLE customer_support (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id BIGINT NOT NULL,request_key VARCHAR(100) NOT NULL,request_hash VARCHAR(64) NOT NULL,
 subject VARCHAR(160) NOT NULL,state VARCHAR(30) NOT NULL,created_at DATETIME(6) NOT NULL,updated_at DATETIME(6) NOT NULL,
 UNIQUE KEY uq_support_request(user_id,request_key),INDEX ix_support_owner(user_id,created_at),INDEX ix_support_state(state,updated_at),
 CHECK(state IN ('OPEN','IN_PROGRESS','WAITING_CUSTOMER','RESOLVED','CLOSED'))
);
CREATE TABLE support_messages (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,ticket_id BIGINT NOT NULL,author_id BIGINT NOT NULL,author_role VARCHAR(10) NOT NULL,
 request_key VARCHAR(100) NOT NULL,request_hash VARCHAR(64) NOT NULL,message_text TEXT NOT NULL,created_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_support_message_ticket FOREIGN KEY(ticket_id) REFERENCES customer_support(id),
 UNIQUE KEY uq_support_message_request(ticket_id,request_key),INDEX ix_support_message_time(ticket_id,created_at,id),
 CHECK(author_role IN ('CUSTOMER','STAFF','ADMIN'))
);
CREATE TABLE support_status_history (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,ticket_id BIGINT NOT NULL,state VARCHAR(30) NOT NULL,actor_id BIGINT NOT NULL,created_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_support_history_ticket FOREIGN KEY(ticket_id) REFERENCES customer_support(id),INDEX ix_support_history_time(ticket_id,created_at,id),
 CHECK(state IN ('OPEN','IN_PROGRESS','WAITING_CUSTOMER','RESOLVED','CLOSED'))
);
