CREATE TABLE validators (
    id SERIAL PRIMARY KEY,
    public_key BYTEA NOT NULL,
    UNIQUE(public_key)
);
CREATE TABLE signed_blocks (
    validator_id INTEGER NOT NULL,
    slot NUMERIC(20) NOT NULL,
    signing_root BYTEA,
    FOREIGN KEY(validator_id) REFERENCES validators(id),
    UNIQUE (validator_id, slot)
);
CREATE TABLE signed_attestations (
    validator_id INTEGER,
    source_epoch NUMERIC(20) NOT NULL,
    target_epoch NUMERIC(20) NOT NULL,
    signing_root BYTEA,
    FOREIGN KEY(validator_id) REFERENCES validators(id),
    UNIQUE (validator_id, target_epoch)
);