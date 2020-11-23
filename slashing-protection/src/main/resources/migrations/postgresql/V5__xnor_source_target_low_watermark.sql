ALTER TABLE low_watermarks
ADD CONSTRAINT both_epochs_set_or_null
CHECK (
  (target_epoch IS NOT NULL and source_epoch IS NOT NULL)
  OR (target_epoch IS NULL and source_epoch IS NULL)
);