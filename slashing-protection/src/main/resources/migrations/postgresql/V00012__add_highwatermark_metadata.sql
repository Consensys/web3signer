ALTER TABLE metadata
    ADD COLUMN high_watermark_epoch NUMERIC(20),
    ADD COLUMN high_watermark_slot NUMERIC(20);

-- inserted high watermark should be above low watermark

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

CREATE TRIGGER check_before_insert_or_update_high_watermarks
    BEFORE INSERT OR UPDATE ON metadata
                         FOR EACH ROW EXECUTE PROCEDURE check_high_watermarks();


-- inserted low watermark should be below or the same as high watermark

CREATE OR REPLACE FUNCTION check_low_watermarks() RETURNS TRIGGER AS $$
DECLARE
    high_slot NUMERIC(20);
    high_epoch NUMERIC(20);
BEGIN
SELECT MIN(high_watermark_slot) INTO high_slot FROM metadata;
SELECT MIN(high_watermark_epoch) INTO high_epoch FROM metadata;

IF NEW.slot > high_slot THEN
        RAISE EXCEPTION 'Insert/Update violates constraint: low_watermark slot must be less than or equal to high_watermark_slot in the metadata table';
END IF;

IF NEW.source_epoch > high_epoch THEN
        RAISE EXCEPTION 'Insert/Update violates constraint: low_watermark source epoch must be less than or equal to high_watermark_epoch in the metadata table';
END IF;

IF NEW.target_epoch > high_epoch THEN
        RAISE EXCEPTION 'Insert/Update violates constraint: low_watermark target epoch must be less than or equal to high_watermark_epoch in the metadata table';
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER check_before_insert_or_update_low_watermarks
    BEFORE INSERT OR UPDATE ON low_watermarks
                         FOR EACH ROW EXECUTE PROCEDURE check_low_watermarks();


UPDATE database_version SET version = 12 WHERE id = 1;