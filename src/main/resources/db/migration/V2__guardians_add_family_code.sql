ALTER TABLE guardians
ADD COLUMN IF NOT EXISTS family_code VARCHAR(6);

WITH ranked_guardians AS (
    SELECT
        id,
        school_id,
        LPAD(ROW_NUMBER() OVER (PARTITION BY school_id ORDER BY created_at, id)::text, 6, '0') AS generated_family_code
    FROM guardians
)
UPDATE guardians g
SET family_code = rg.generated_family_code
FROM ranked_guardians rg
WHERE g.id = rg.id
  AND g.family_code IS NULL;

ALTER TABLE guardians
ALTER COLUMN family_code SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_school_family_code'
    ) THEN
        ALTER TABLE guardians
        ADD CONSTRAINT uk_school_family_code UNIQUE (school_id, family_code);
    END IF;
END $$;
