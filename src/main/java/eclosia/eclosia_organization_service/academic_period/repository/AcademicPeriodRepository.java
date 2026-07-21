package eclosia.eclosia_organization_service.academic_period.repository;

import eclosia.eclosia_organization_service.academic_period.entity.AcademicPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AcademicPeriodRepository extends JpaRepository<AcademicPeriod, UUID> {

    boolean existsByAcademicTerm_IdAndCode(UUID academicTermId, String code);

    boolean existsByAcademicTerm_IdAndCodeAndIdNot(UUID academicTermId, String code, UUID id);

    List<AcademicPeriod> findByAcademicTerm_IdOrderByDisplayOrderAsc(UUID academicTermId);

    List<AcademicPeriod> findByAcademicTerm_IdInOrderByDisplayOrderAsc(Collection<UUID> academicTermIds);
}

