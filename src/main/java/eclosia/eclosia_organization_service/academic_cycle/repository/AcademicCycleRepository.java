package eclosia.eclosia_organization_service.academic_cycle.repository;

import eclosia.eclosia_organization_service.academic_cycle.entity.AcademicCycle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AcademicCycleRepository extends JpaRepository<AcademicCycle, UUID> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);
}
