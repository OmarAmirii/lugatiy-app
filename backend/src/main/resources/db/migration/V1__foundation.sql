CREATE TABLE app_users (
 id UUID PRIMARY KEY, email VARCHAR(255) NOT NULL UNIQUE, password_hash VARCHAR(255) NOT NULL,
 display_name VARCHAR(120) NOT NULL, enabled BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE user_roles (
 user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
 role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN','TEACHER','STUDENT','PARENT')),
 PRIMARY KEY (user_id, role)
);
CREATE TABLE words (
 id UUID PRIMARY KEY, arabic VARCHAR(255) NOT NULL, arabic_normalized VARCHAR(255) NOT NULL,
 dutch VARCHAR(255) NOT NULL, part_of_speech VARCHAR(100), root VARCHAR(100),
 example_arabic TEXT, example_dutch TEXT, created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX words_normalized_idx ON words (arabic_normalized);
CREATE INDEX words_dutch_idx ON words (lower(dutch));
