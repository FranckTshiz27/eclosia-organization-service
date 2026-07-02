package eclosia.eclosia_organization_service.student_category.repository;

import eclosia.eclosia_organization_service.student_category.entity.StudentCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StudentCategoryRepository extends JpaRepository<StudentCategory, UUID> {

    List<StudentCategory> findBySchool_IdOrderByNameAsc(UUID schoolId);

    boolean existsBySchool_IdAndCode(UUID schoolId, String code);

    boolean existsBySchool_IdAndCodeAndIdNot(UUID schoolId, String code, UUID id);

    boolean existsBySchool_IdAndName(UUID schoolId, String name);

    boolean existsBySchool_IdAndNameAndIdNot(UUID schoolId, String name, UUID id);
}
