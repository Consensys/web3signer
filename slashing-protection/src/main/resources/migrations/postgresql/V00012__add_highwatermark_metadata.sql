ALTER TABLE metadata
    ADD COLUMN high_watermark_epoch NUMERIC(20),
    ADD COLUMN high_watermark_slot NUMERIC(20);

CREATE OR REPLACE FUNCTION check_high_watermarks() RETURNS TRIGGER AS $$
DECLARE
    max_slot NUMERIC(20);
    max_epoch NUMERIC(20);
BEGIN
SELECT MAX(slot) INTO max_slot FROM low_watermarks;
SELECT GREATEST(MAX(target_epoch), MAX(source_epoch)) INTO max_epoch FROM low_watermarks;

IF NEW.high_watermark_slot <= max_slot THEN
        RAISE EXCEPTION 'Insert/Update violates constraint: high_watermark_slot must be greater than max slot in low_watermarks table';
END IF;

IF NEW.high_watermark_epoch <= max_epoch THEN
        RAISE EXCEPTION 'Insert/Update violates constraint: high_watermark_epoch must be greater than max epoch in low_watermarks table';
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER check_before_insert_or_update
    BEFORE INSERT OR UPDATE ON metadata
                         FOR EACH ROW EXECUTE PROCEDURE check_high_watermarks();

UPDATE database_version SET version = 12 WHERE id = 1;