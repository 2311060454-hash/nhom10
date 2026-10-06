CREATE TABLE guest_contacts (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,request_key VARCHAR(100) NOT NULL,request_hash VARCHAR(64) NOT NULL,
 full_name VARCHAR(120) NOT NULL,email VARCHAR(190) NOT NULL,phone VARCHAR(20),subject VARCHAR(160) NOT NULL,
 message TEXT NOT NULL,state VARCHAR(20) NOT NULL,staff_note VARCHAR(1000),actor_id BIGINT,
 created_at DATETIME(6) NOT NULL,updated_at DATETIME(6) NOT NULL,
 CONSTRAINT uq_guest_email_key UNIQUE(email,request_key),INDEX ix_guest_state(state,updated_at),INDEX ix_guest_email_time(email,created_at),
 CHECK(state IN ('OPEN','IN_PROGRESS','RESOLVED'))
);
