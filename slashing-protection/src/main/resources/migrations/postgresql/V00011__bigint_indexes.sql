ALTER SEQUENCE signed_attestations_id_seq AS bigint;
ALTER TABLE signed_attestations ALTER COLUMN id TYPE bigint;

ALTER SEQUENCE signed_blocks_id_seq AS bigint;
ALTER TABLE signed_blocks ALTER COLUMN id TYPE bigint;

UPDATE database_version SET version = 11 WHERE id = 1;