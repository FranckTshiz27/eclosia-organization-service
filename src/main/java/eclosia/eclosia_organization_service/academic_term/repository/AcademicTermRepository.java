package eclosia.eclosia_organization_service.academic_term.repository;

import eclosia.eclosia_organization_service.academic_term.entity.AcademicTerm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AcademicTermRepository extends JpaRepository<AcademicTerm, UUID> {

    boolean existsByAcademicYear_IdAndCode(UUID academicYearId, String code);

    boolean existsByAcademicYear_IdAndCodeAndIdNot(UUID academicYearId, String code, UUID id);

    List<AcademicTerm> findByAcademicYear_IdOrderByDisplayOrderAsc(UUID academicYearId);
}
