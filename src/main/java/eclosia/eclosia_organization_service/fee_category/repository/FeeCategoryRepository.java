package eclosia.eclosia_organization_service.fee_category.repository;

import eclosia.eclosia_organization_service.fee_category.entity.FeeCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FeeCategoryRepository extends JpaRepository<FeeCategory, UUID> {

    List<FeeCategory> findBySchool_IdOrderByNameAsc(UUID schoolId);

    boolean existsBySchool_IdAndCode(UUID schoolId, String code);

    boolean existsBySchool_IdAndCodeAndIdNot(UUID schoolId, String code, UUID id);

    boolean existsBySchool_IdAndName(UUID schoolId, String name);

    boolean existsBySchool_IdAndNameAndIdNot(UUID schoolId, String name, UUID id);
}
