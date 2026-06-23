package eclosia.eclosia_organization_service.academic_section.repository;

import eclosia.eclosia_organization_service.academic_section.entity.AcademicSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AcademicSectionRepository extends JpaRepository<AcademicSection, UUID> {

    boolean existsByAcademicCycle_IdAndCode(UUID academicCycleId, String code);

    boolean existsByAcademicCycle_IdAndCodeAndIdNot(UUID academicCycleId, String code, UUID id);

    List<AcademicSection> findByAcademicCycle_IdOrderByDisplayOrderAsc(UUID academicCycleId);
}
