CREATE TABLE job_runs (
    id              SERIAL PRIMARY KEY,
    scheduled_at    TIMESTAMP NOT NULL,
    finished_at     TIMESTAMP,
    status          TEXT CHECK (status IN ('pending', 'running', 'success', 'failed')),
    notes           TEXT
);

CREATE TABLE s3_files (
    id              SERIAL PRIMARY KEY,
    job_run_id      INTEGER REFERENCES job_runs(id),
    s3_key          TEXT NOT NULL UNIQUE,
    started_at      TIMESTAMP,
    finished_at     TIMESTAMP,
    record_count    INTEGER,
    page_number     INTEGER,
    status          TEXT CHECK (status IN ('new', 'processing', 'success', 'partial', 'failed')),
    error_message   TEXT
);

CREATE TABLE records (
    id              SERIAL PRIMARY KEY,
    s3_file_id      INTEGER REFERENCES s3_files(id),
    job_run_id      INTEGER REFERENCES job_runs(id),
    raw_data        TEXT,
    status          TEXT CHECK (status IN ('new', 'processing', 'success', 'partial', 'failed')),
    downloaded_at   TIMESTAMP,
    started_at      TIMESTAMP,
    finished_at     TIMESTAMP,
    error_flag      BOOLEAN DEFAULT FALSE
);

CREATE TABLE record_errors (
    record_id       INTEGER PRIMARY KEY REFERENCES records(id),
    error_type      TEXT,
    error_message   TEXT,
    traceback       TEXT
); 