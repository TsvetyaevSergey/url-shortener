ALTER TABLE links
DROP CONSTRAINT IF EXISTS links_slug_key;

ALTER TABLE links
ADD CONSTRAINT links_slug_uidx UNIQUE (slug);
