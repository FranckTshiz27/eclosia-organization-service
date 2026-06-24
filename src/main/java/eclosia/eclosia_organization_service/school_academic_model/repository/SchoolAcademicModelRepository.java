package eclosia.eclosia_organization_service.school_academic_model.repository;

import eclosia.eclosia_organization_service.school_academic_model.entity.SchoolAcademicModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SchoolAcademicModelRepository extends JpaRepository<SchoolAcademicModel, UUID> {

    boolean existsBySchool_IdAndAcademicModel_IdAndStartDate(
            UUID schoolId, UUID academicModelId, LocalDate startDate);

    boolean existsBySchool_IdAndAcademicModel_IdAndStartDateAndIdNot(
            UUID schoolId, UUID academicModelId, LocalDate startDate, UUID id);

    List<SchoolAcademicModel> findBySchool_IdOrderByStartDateDesc(UUID schoolId);

    List<SchoolAcademicModel> findByAcademicModel_IdOrderByStartDateDesc(UUID academicModelId);
}
