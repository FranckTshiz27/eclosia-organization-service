ALTER TABLE academic_periods
    ALTER COLUMN maximum_score_ratio TYPE INTEGER
        USING ROUND(maximum_score_ratio)::INTEGER;
