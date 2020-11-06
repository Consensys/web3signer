ALTER TABLE signed_blocks
ALTER COLUMN signing_root DROP NOT NULL;

ALTER TABLE signed_attestations
ALTER COLUMN signing_root DROP NOT NULL;