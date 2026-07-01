package eclosia.eclosia_organization_service.academic_year.repository;

import eclosia.eclosia_organization_service.academic_year.entity.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, UUID> {

    boolean existsBySchool_IdAndCode(UUID schoolId, String code);

    boolean existsBySchool_IdAndCodeAndIdNot(UUID schoolId, String code, UUID id);

    List<AcademicYear> findBySchool_IdOrderByStartDateDesc(UUID schoolId);

    List<AcademicYear> findBySchoolAcademicModel_IdOrderByStartDateDesc(UUID schoolAcademicModelId);

    List<AcademicYear> findBySchool_IdAndCurrentTrue(UUID schoolId);

    List<AcademicYear> findBySchool_IdAndCurrentTrueAndIdNot(UUID schoolId, UUID id);
}
