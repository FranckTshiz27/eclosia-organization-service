package eclosia.eclosia_organization_service.classroom_designation.repository;

import eclosia.eclosia_organization_service.classroom_designation.entity.ClassroomDesignation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClassroomDesignationRepository extends JpaRepository<ClassroomDesignation, UUID> {

    List<ClassroomDesignation> findBySchool_IdOrderByDisplayOrderAsc(UUID schoolId);

    boolean existsBySchool_IdAndCode(UUID schoolId, String code);

    boolean existsBySchool_IdAndCodeAndIdNot(UUID schoolId, String code, UUID id);

    boolean existsBySchool_IdAndName(UUID schoolId, String name);

    boolean existsBySchool_IdAndNameAndIdNot(UUID schoolId, String name, UUID id);
}
