-- Liaison année scolaire ↔ période (activation pour le moteur de bulletin)
CREATE TABLE IF NOT EXISTS academic_year_period (
    id UUID PRIMARY KEY,
    academic_year_id UUID NOT NULL,
    academic_period_id UUID NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_academic_year_period'
    ) THEN
        ALTER TABLE academic_year_period
            ADD CONSTRAINT uk_academic_year_period
                UNIQUE (academic_year_id, academic_period_id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_ayp_academic_year'
    ) THEN
        ALTER TABLE academic_year_period
            ADD CONSTRAINT fk_ayp_academic_year
                FOREIGN KEY (academic_year_id) REFERENCES academic_years (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_ayp_academic_period'
    ) THEN
        ALTER TABLE academic_year_period
            ADD CONSTRAINT fk_ayp_academic_period
                FOREIGN KEY (academic_period_id) REFERENCES academic_periods (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_ayp_academic_year
    ON academic_year_period (academic_year_id);

CREATE INDEX IF NOT EXISTS idx_ayp_academic_year_active
    ON academic_year_period (academic_year_id, active);

-- Initialise une ligne par période existante (année dérivée du trimestre)
INSERT INTO academic_year_period (id, academic_year_id, academic_period_id, active, created_at, updated_at)
SELECT gen_random_uuid(),
       t.academic_year_id,
       p.id,
       COALESCE(p.active, TRUE),
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
FROM academic_periods p
         INNER JOIN academic_terms t ON t.id = p.academic_term_id
WHERE NOT EXISTS (
    SELECT 1
    FROM academic_year_period ayp
    WHERE ayp.academic_year_id = t.academic_year_id
      AND ayp.academic_period_id = p.id
);
