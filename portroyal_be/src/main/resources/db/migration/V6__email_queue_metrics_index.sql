CREATE INDEX idx_emails_queue_pending_created
    ON emails_queue (created_at)
    WHERE sent = false;
