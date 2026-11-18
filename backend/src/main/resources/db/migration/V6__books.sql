CREATE TABLE books (
 id UUID PRIMARY KEY,
 title VARCHAR(200) NOT NULL,
 description TEXT NOT NULL DEFAULT '',
 category VARCHAR(100) NOT NULL DEFAULT '',
 level_id UUID REFERENCES quiz_levels(id),
 storage_name VARCHAR(80) NOT NULL UNIQUE,
 original_name VARCHAR(255) NOT NULL,
 published BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE reading_progress (
 user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
 book_id UUID NOT NULL REFERENCES books(id) ON DELETE CASCADE,
 page INTEGER NOT NULL CHECK (page > 0),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 PRIMARY KEY (user_id,book_id)
);
