-- AcademicYear becomes a national (country-level) reference instead of school-owned.

ALTER TABLE academic_years
    ADD COLUMN IF NOT EXISTS country_id UUID,
    ADD COLUMN IF NOT EXISTS name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE academic_years ay
SET country_id = s.country_id,
    name = COALESCE(ay.name, ay.code),
    active = COALESCE(ay.active, TRUE)
FROM schools s
WHERE ay.school_id = s.id
  AND (ay.country_id IS NULL OR ay.name IS NULL);

-- If some rows still miss a country (orphan school country), keep migration fail-safe by
-- assigning a placeholder country only when a school has no country_id is not allowed.
-- Fail early for unresolved rows.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM academic_years
        WHERE country_id IS NULL
    ) THEN
        RAISE EXCEPTION 'Cannot migrate academic_years: some rows have no resolvable country_id';
    END IF;
END $$;

ALTER TABLE academic_years
    ALTER COLUMN country_id SET NOT NULL,
    ALTER COLUMN name SET NOT NULL;

-- Deduplicate (country_id, code): keep oldest row, remap FKs, delete duplicates.
DO $$
DECLARE
    duplicate_group RECORD;
    keeper_id UUID;
    duplicate_id UUID;
BEGIN
    FOR duplicate_group IN
        SELECT country_id, code
        FROM academic_years
        GROUP BY country_id, code
        HAVING COUNT(*) > 1
    LOOP
        SELECT id
        INTO keeper_id
        FROM academic_years
        WHERE country_id = duplicate_group.country_id
          AND code = duplicate_group.code
        ORDER BY created_at NULLS LAST, id
        LIMIT 1;

        FOR duplicate_id IN
            SELECT id
            FROM academic_years
            WHERE country_id = duplicate_group.country_id
              AND code = duplicate_group.code
              AND id <> keeper_id
        LOOP
            UPDATE enrollments
            SET academic_year_id = keeper_id
            WHERE academic_year_id = duplicate_id
              AND NOT EXISTS (
                  SELECT 1
                  FROM enrollments e2
                  WHERE e2.student_id = enrollments.student_id
                    AND e2.academic_year_id = keeper_id
              );

            DELETE FROM enrollments
            WHERE academic_year_id = duplicate_id;

            UPDATE academic_fees
            SET academic_year_id = keeper_id
            WHERE academic_year_id = duplicate_id;

            DELETE FROM academic_years
            WHERE id = duplicate_id;
        END LOOP;
    END LOOP;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_school_academic_year_code'
    ) THEN
        ALTER TABLE academic_years
            DROP CONSTRAINT uk_school_academic_year_code;
    END IF;
END $$;

DO $$
DECLARE
    fk_name TEXT;
BEGIN
    FOR fk_name IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        WHERE rel.relname = 'academic_years'
          AND con.contype = 'f'
          AND pg_get_constraintdef(con.oid) ILIKE '%school_id%'
    LOOP
        EXECUTE format('ALTER TABLE academic_years DROP CONSTRAINT %I', fk_name);
    END LOOP;
END $$;

DO $$
DECLARE
    fk_name TEXT;
BEGIN
    FOR fk_name IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        WHERE rel.relname = 'academic_years'
          AND con.contype = 'f'
          AND pg_get_constraintdef(con.oid) ILIKE '%school_academic_model_id%'
    LOOP
        EXECUTE format('ALTER TABLE academic_years DROP CONSTRAINT %I', fk_name);
    END LOOP;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'academic_years_status_check'
    ) THEN
        ALTER TABLE academic_years
            DROP CONSTRAINT academic_years_status_check;
    END IF;
END $$;

ALTER TABLE academic_years
    DROP COLUMN IF EXISTS school_id,
    DROP COLUMN IF EXISTS school_academic_model_id,
    DROP COLUMN IF EXISTS current,
    DROP COLUMN IF EXISTS status,
    DROP COLUMN IF EXISTS description;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_academic_years_country'
    ) THEN
        ALTER TABLE academic_years
            ADD CONSTRAINT fk_academic_years_country
                FOREIGN KEY (country_id) REFERENCES countries (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_country_academic_year_code'
    ) THEN
        ALTER TABLE academic_years
            ADD CONSTRAINT uk_country_academic_year_code UNIQUE (country_id, code);
    END IF;
END $$;
