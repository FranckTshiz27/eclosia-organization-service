ALTER TABLE academic_periods
    ADD COLUMN IF NOT EXISTS maximum_score_ratio NUMERIC(5, 4);

UPDATE academic_periods
SET maximum_score_ratio = 1.0000
WHERE maximum_score_ratio IS NULL;

ALTER TABLE academic_periods
    ALTER COLUMN maximum_score_ratio SET NOT NULL;
