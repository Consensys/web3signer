CREATE TABLE database_version (
    id INTEGER PRIMARY KEY,
    version INTEGER NOT NULL
);
-- Start at version 7, should have previously existed (but now represents migration index).
INSERT INTO database_version (id, version) VALUES (1, 7);