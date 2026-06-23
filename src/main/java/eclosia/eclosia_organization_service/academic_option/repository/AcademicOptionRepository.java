package eclosia.eclosia_organization_service.academic_option.repository;

import eclosia.eclosia_organization_service.academic_option.entity.AcademicOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AcademicOptionRepository extends JpaRepository<AcademicOption, UUID> {

    boolean existsByAcademicSection_IdAndCode(UUID academicSectionId, String code);

    boolean existsByAcademicSection_IdAndCodeAndIdNot(UUID academicSectionId, String code, UUID id);

    List<AcademicOption> findByAcademicSection_IdOrderByDisplayOrderAsc(UUID academicSectionId);
}
