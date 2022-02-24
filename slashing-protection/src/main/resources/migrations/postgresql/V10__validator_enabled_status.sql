ALTER TABLE validators
ADD COLUMN enabled boolean NOT NULL default true;

UPDATE database_version SET version = 10 WHERE id = 1;