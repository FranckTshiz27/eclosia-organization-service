package eclosia.eclosia_organization_service.academic_cycle.repository;

import eclosia.eclosia_organization_service.academic_cycle.entity.AcademicCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface AcademicCycleRepository extends JpaRepository<AcademicCycle, UUID> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);

    @Query("""
            SELECT c FROM AcademicCycle c
            ORDER BY COALESCE(c.displayOrder, 2147483647), c.code
            """)
    List<AcademicCycle> findAllOrdered();
}
