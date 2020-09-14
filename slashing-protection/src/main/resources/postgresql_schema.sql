CREATE TABLE validators (
  id SERIAL PRIMARY KEY,
  public_key BYTEA NOT NULL
);

CREATE TABLE signed_blocks (
    validator_id INTEGER NOT NULL,
    slot INTEGER NOT NULL,
    signing_root BYTEA NOT NULL,
    FOREIGN KEY(validator_id) REFERENCES validators(id),
    UNIQUE (validator_id, slot)
);

CREATE TABLE signed_attestations (
    validator_id INTEGER,
    source_epoch INTEGER NOT NULL,
    target_epoch INTEGER NOT NULL,
    signing_root BYTEA NOT NULL,
    FOREIGN KEY(validator_id) REFERENCES validators(id),
    UNIQUE (validator_id, target_epoch)
);