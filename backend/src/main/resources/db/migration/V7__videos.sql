CREATE TABLE video_series (
 id UUID PRIMARY KEY, title VARCHAR(200) NOT NULL, description TEXT NOT NULL DEFAULT '', published BOOLEAN NOT NULL DEFAULT FALSE, created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE videos (
 id UUID PRIMARY KEY, title VARCHAR(200) NOT NULL, description TEXT NOT NULL DEFAULT '', category VARCHAR(100) NOT NULL DEFAULT '',
 series_id UUID REFERENCES video_series(id) ON DELETE SET NULL, episode_number INTEGER CHECK (episode_number > 0),
 storage_name VARCHAR(80) NOT NULL UNIQUE, published BOOLEAN NOT NULL DEFAULT FALSE, created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE(series_id, episode_number)
);
CREATE TABLE watch_progress (
 user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE, video_id UUID NOT NULL REFERENCES videos(id) ON DELETE CASCADE,
 seconds DOUBLE PRECISION NOT NULL DEFAULT 0 CHECK (seconds >= 0), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), PRIMARY KEY(user_id,video_id)
);
CREATE INDEX videos_publication_idx ON videos(published,created_at DESC);
