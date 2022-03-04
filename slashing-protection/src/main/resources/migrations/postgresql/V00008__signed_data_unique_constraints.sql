ALTER TABLE signed_blocks
ADD CONSTRAINT unique_signed_block
UNIQUE (validator_id, slot);

ALTER TABLE signed_attestations
ADD CONSTRAINT unique_signed_attestation
UNIQUE (validator_id, target_epoch);

UPDATE database_version SET version = 8 WHERE id = 1;