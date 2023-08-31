ALTER TABLE metadata
    ADD COLUMN high_watermark_epoch NUMERIC(20),
    ADD COLUMN high_watermark_slot NUMERIC(20);

UPDATE database_version SET version = 12 WHERE id = 1;