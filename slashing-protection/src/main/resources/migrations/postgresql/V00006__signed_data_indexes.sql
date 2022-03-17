CREATE INDEX ON signed_attestations (validator_id, target_epoch);

CREATE INDEX on signed_blocks (validator_id, slot);