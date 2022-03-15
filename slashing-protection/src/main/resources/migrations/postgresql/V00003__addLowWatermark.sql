CREATE TABLE low_watermarks (
    validator_id INTEGER NOT NULL,
    slot NUMERIC(20),
    target_epoch NUMERIC(20),
    source_epoch NUMERIC(20),
    FOREIGN KEY(validator_id) REFERENCES validators(id),
    UNIQUE (validator_id)
);