CREATE TABLE notification_guard(id BIGINT PRIMARY KEY);
INSERT INTO notification_guard VALUES(1);
CREATE TABLE notifications (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id BIGINT NOT NULL,event_key VARCHAR(180) NOT NULL UNIQUE,
 order_id VARCHAR(36) NOT NULL,state VARCHAR(30) NOT NULL,title VARCHAR(160) NOT NULL,body VARCHAR(500) NOT NULL,
 created_at DATETIME(6) NOT NULL,read_at DATETIME(6),INDEX ix_notifications_owner(user_id,created_at,id),
 INDEX ix_notifications_unread(user_id,read_at)
);
