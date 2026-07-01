ALTER TABLE classrooms DROP CONSTRAINT IF EXISTS uk_school_classroom;

ALTER TABLE classrooms DROP COLUMN IF EXISTS suffix;
ALTER TABLE classrooms DROP COLUMN IF EXISTS label;
ALTER TABLE classrooms DROP COLUMN IF EXISTS code;
ALTER TABLE classrooms DROP COLUMN IF EXISTS default_capacity;

ALTER TABLE classrooms
    ADD CONSTRAINT uk_school_classroom
    UNIQUE (school_id, academic_level_id, academic_section_id, academic_option_id, classroom_designation_id);
