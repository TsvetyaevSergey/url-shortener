CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- case-insensitive уникальность email
CREATE UNIQUE INDEX users_email_lower_uidx ON users (LOWER(email));

CREATE TABLE links (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    slug VARCHAR(32) NOT NULL,
    target_url TEXT NOT NULL,
    expires_at TIMESTAMPTZ,
    clicks_total BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT links_slug_uidx UNIQUE (slug)
);
