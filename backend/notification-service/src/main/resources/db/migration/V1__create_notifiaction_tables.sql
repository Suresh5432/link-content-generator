
-- In-app notifications
CREATE TABLE notifications (
                               id UUID PRIMARY KEY,
                               event_id UUID NOT NULL UNIQUE,
                               user_id VARCHAR(255) NOT NULL,
                               link_id UUID NOT NULL,

                               notification_type VARCHAR(40) NOT NULL,
                               title VARCHAR(150) NOT NULL,
                               message VARCHAR(1000) NOT NULL,
                                in_app_visible BOOLEAN not NULL DEFAULT true,

                               is_read BOOLEAN NOT NULL DEFAULT FALSE,
                               read_at TIMESTAMPTZ,

                               created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                               CONSTRAINT chk_notification_read_state
                                   CHECK (
                                       (is_read = FALSE AND read_at IS NULL)
                                           OR
                                       (is_read = TRUE AND read_at IS NOT NULL)
                                       )
);

CREATE INDEX idx_notifications_user_created
    ON notifications(user_id, created_at DESC);

CREATE INDEX idx_notifications_user_unread
    ON notifications(user_id, created_at DESC)
    WHERE is_read = FALSE;


-- Email delivery tracking
CREATE TABLE email_deliveries (
                                  id UUID PRIMARY KEY,
                                  notification_id UUID NOT NULL UNIQUE,

                                  recipient_email VARCHAR(320),
                                  status VARCHAR(30) NOT NULL DEFAULT 'PENDING',

                                  attempt_count INTEGER NOT NULL DEFAULT 0,
                                  max_attempts INTEGER NOT NULL DEFAULT 5,

                                  next_attempt_at TIMESTAMPTZ,
                                  sent_at TIMESTAMPTZ,

                                  provider_message_id VARCHAR(255),
                                  last_error TEXT,

                                  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                  CONSTRAINT fk_email_notification
                                      FOREIGN KEY (notification_id)
                                          REFERENCES notifications(id),

                                  CONSTRAINT chk_email_attempts
                                      CHECK (
                                          attempt_count >= 0
                                              AND max_attempts > 0
                                          )
);

CREATE INDEX idx_email_delivery_pending
    ON email_deliveries(status, next_attempt_at);


-- User notification preferences
CREATE TABLE notification_preferences (
                                          user_id VARCHAR(255) PRIMARY KEY,

                                          in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
                                          email_enabled BOOLEAN NOT NULL DEFAULT TRUE,

                                          created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                          updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- RabbitMQ event idempotency
CREATE TABLE processed_events (
                                  event_id UUID PRIMARY KEY,
                                  event_type VARCHAR(100) NOT NULL,
                                  processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_processed_events_processed_at
    ON processed_events(processed_at);
