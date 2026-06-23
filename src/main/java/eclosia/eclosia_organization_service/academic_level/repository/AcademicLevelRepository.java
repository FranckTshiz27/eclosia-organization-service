package eclosia.eclosia_organization_service.academic_level.repository;

import eclosia.eclosia_organization_service.academic_level.entity.AcademicLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AcademicLevelRepository extends JpaRepository<AcademicLevel, UUID> {

    boolean existsByAcademicCycle_IdAndCode(UUID academicCycleId, String code);

    boolean existsByAcademicCycle_IdAndCodeAndIdNot(UUID academicCycleId, String code, UUID id);
}
