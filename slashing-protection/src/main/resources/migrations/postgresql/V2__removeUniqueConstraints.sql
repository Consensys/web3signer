DO
$body$
DECLARE
  _signed_block_conn text := (
  SELECT conname
  FROM pg_constraint
  WHERE conrelid = 'signed_blocks'::regclass
  AND contype = 'u');

  _signed_attestation_conn text := (
  SELECT conname
  FROM pg_constraint
  WHERE conrelid = 'signed_attestations'::regclass
  AND contype = 'u');
BEGIN
  EXECUTE 'ALTER TABLE signed_blocks DROP CONSTRAINT ' || _signed_block_conn;
  EXECUTE 'ALTER TABLE signed_attestations DROP CONSTRAINT ' || _signed_attestation_conn;
  EXECUTE 'ALTER TABLE signed_blocks ADD COLUMN id SERIAL PRIMARY KEY';
  EXECUTE 'ALTER TABLE signed_attestations ADD COLUMN id SERIAL PRIMARY KEY';
END
$body$
