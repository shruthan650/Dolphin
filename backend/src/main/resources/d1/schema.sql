-- Dolphin schema for Cloudflare D1 (SQLite). Applied automatically at startup when DOLPHIN_STORAGE=d1;
-- every statement is idempotent. Columns added after the first release are also added to existing databases by
-- D1StorageConfig (see MIGRATIONS there), which creates the indexes that depend on them.
-- Timestamps are fixed-width UTC ISO-8601 text so they sort correctly.

CREATE TABLE IF NOT EXISTS users (
    id            TEXT PRIMARY KEY,
    name          TEXT NOT NULL,
    email         TEXT NOT NULL,
    email_key     TEXT NOT NULL UNIQUE,  -- trimmed, lower-cased email for case-insensitive lookups
    password_hash TEXT NOT NULL,
    role          TEXT NOT NULL,
    github_url    TEXT,
    leetcode_url  TEXT,
    active        INTEGER NOT NULL,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,  -- consecutive wrong passwords since the last success
    locked_until  TEXT,                               -- login rejected until this instant (NULL = not locked)
    token_version INTEGER NOT NULL DEFAULT 0,         -- bumped on password/email change to revoke older JWTs
    created_at    TEXT,
    updated_at    TEXT
);
CREATE INDEX IF NOT EXISTS idx_users_role ON users (role);

CREATE TABLE IF NOT EXISTS classes (
    id         TEXT PRIMARY KEY,
    class_name TEXT NOT NULL,
    semester   INTEGER NOT NULL,
    branch     TEXT NOT NULL,
    section    TEXT NOT NULL,
    class_code TEXT NOT NULL UNIQUE,
    teacher_id TEXT NOT NULL,
    created_at TEXT,
    updated_at TEXT
);
CREATE INDEX IF NOT EXISTS idx_classes_teacher ON classes (teacher_id);

-- Enrolment; position keeps students in the order they joined.
CREATE TABLE IF NOT EXISTS class_students (
    class_id   TEXT NOT NULL,
    student_id TEXT NOT NULL,
    position   INTEGER NOT NULL,
    PRIMARY KEY (class_id, student_id)
);
CREATE INDEX IF NOT EXISTS idx_class_students_student ON class_students (student_id);

CREATE TABLE IF NOT EXISTS projects (
    id           TEXT PRIMARY KEY,
    owner_id     TEXT NOT NULL,
    class_id     TEXT,  -- the class this project belongs to (NULL = legacy or class deleted)
    title        TEXT NOT NULL,
    description  TEXT,
    github_url   TEXT,
    live_url     TEXT,
    technologies TEXT NOT NULL DEFAULT '[]',  -- JSON array of strings
    created_at   TEXT,
    updated_at   TEXT
);
CREATE INDEX IF NOT EXISTS idx_projects_owner ON projects (owner_id);

CREATE TABLE IF NOT EXISTS leetcode_entries (
    id           TEXT PRIMARY KEY,
    student_id   TEXT NOT NULL,
    class_id     TEXT,  -- the class this entry belongs to (NULL = legacy or class deleted)
    problem_name TEXT NOT NULL,
    problem_url  TEXT,
    difficulty   TEXT NOT NULL,
    status       TEXT NOT NULL,
    topic        TEXT,
    solved_at    TEXT,
    created_at   TEXT,
    updated_at   TEXT
);
CREATE INDEX IF NOT EXISTS idx_leetcode_student ON leetcode_entries (student_id);

-- A teacher's advice on one of a student's projects (target_type PROJECT) or LeetCode entries (LEETCODE).
CREATE TABLE IF NOT EXISTS advice (
    id          TEXT PRIMARY KEY,
    teacher_id  TEXT NOT NULL,
    student_id  TEXT NOT NULL,
    target_type TEXT NOT NULL,
    target_id   TEXT NOT NULL,
    message     TEXT NOT NULL,
    created_at  TEXT,
    updated_at  TEXT
);
CREATE INDEX IF NOT EXISTS idx_advice_student ON advice (student_id);
CREATE INDEX IF NOT EXISTS idx_advice_target ON advice (target_id);
