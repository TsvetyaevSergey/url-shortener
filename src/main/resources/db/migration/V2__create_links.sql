CREATE TABLE links (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    slug VARCHAR(32) UNIQUE NOT NULL,
    target_url TEXT NOT NULL,
    expires_at TIMESTAMPTZ,
    clicks_total BIGINT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT now()
);
