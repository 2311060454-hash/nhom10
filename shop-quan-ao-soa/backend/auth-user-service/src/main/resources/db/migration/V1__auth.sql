CREATE TABLE roles (
 name VARCHAR(20) PRIMARY KEY
) ENGINE=InnoDB;
INSERT INTO roles(name) VALUES ('ADMIN'),('STAFF'),('CUSTOMER');
CREATE TABLE users (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 email VARCHAR(190) NOT NULL,
 full_name VARCHAR(120) NOT NULL,
 phone VARCHAR(20) NOT NULL,
 password_hash VARCHAR(100) NOT NULL,
 active BIT NOT NULL DEFAULT 1,
 inventory_write BIT NOT NULL DEFAULT 0,
 marketing_consent BIT NOT NULL DEFAULT 0,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 CONSTRAINT uq_users_email UNIQUE(email),
 INDEX ix_users_name(full_name), INDEX ix_users_phone(phone)
) ENGINE=InnoDB;
CREATE TABLE user_roles (
 user_id BIGINT NOT NULL,
 role_name VARCHAR(20) NOT NULL,
 PRIMARY KEY(user_id,role_name),
 FOREIGN KEY(user_id) REFERENCES users(id),
 FOREIGN KEY(role_name) REFERENCES roles(name)
) ENGINE=InnoDB;
CREATE TABLE addresses (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL,
 recipient VARCHAR(120) NOT NULL, phone VARCHAR(20) NOT NULL, detail VARCHAR(500) NOT NULL,
 default_address BIT NOT NULL DEFAULT 0,
 created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 INDEX ix_addresses_user(user_id), FOREIGN KEY(user_id) REFERENCES users(id)
) ENGINE=InnoDB;
CREATE TABLE auth_sessions (
 id VARCHAR(36) PRIMARY KEY, user_id BIGINT NOT NULL, expires_at DATETIME(6) NOT NULL,
 revoked BIT NOT NULL DEFAULT 0, created_at DATETIME(6) NOT NULL,
 INDEX ix_sessions_user(user_id), INDEX ix_sessions_expiry(expires_at), FOREIGN KEY(user_id) REFERENCES users(id)
) ENGINE=InnoDB;
CREATE TABLE password_reset_tokens (
 token_hash VARCHAR(64) PRIMARY KEY, user_id BIGINT NOT NULL, expires_at DATETIME(6) NOT NULL,
 used BIT NOT NULL DEFAULT 0, created_at DATETIME(6) NOT NULL,
 INDEX ix_reset_user(user_id), FOREIGN KEY(user_id) REFERENCES users(id)
) ENGINE=InnoDB;
CREATE TABLE audit_logs (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, actor_id BIGINT, action VARCHAR(80) NOT NULL,
 target_id BIGINT, created_at DATETIME(6) NOT NULL, INDEX ix_audit_created(created_at), INDEX ix_audit_actor(actor_id)
) ENGINE=InnoDB;
