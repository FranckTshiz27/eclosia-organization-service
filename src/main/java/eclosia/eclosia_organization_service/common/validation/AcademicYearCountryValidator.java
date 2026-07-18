package eclosia.eclosia_organization_service.common.validation;

import eclosia.eclosia_organization_service.academic_year.entity.AcademicYear;
import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.school.entity.School;

import java.util.UUID;

public final class AcademicYearCountryValidator {

    private AcademicYearCountryValidator() {
    }

    public static void requireSameCountry(School school, AcademicYear academicYear) {
        if (school == null) {
            throw new BadRequestException("School is required");
        }
        requireSameCountry(school.getCountryId(), academicYear);
    }

    public static void requireSameCountry(UUID schoolCountryId, AcademicYear academicYear) {
        if (schoolCountryId == null) {
            throw new BadRequestException("School has no country configured");
        }
        if (academicYear == null || academicYear.getCountryId() == null
                || !schoolCountryId.equals(academicYear.getCountryId())) {
            throw new BadRequestException("Academic year does not belong to the school's country");
        }
    }
}
