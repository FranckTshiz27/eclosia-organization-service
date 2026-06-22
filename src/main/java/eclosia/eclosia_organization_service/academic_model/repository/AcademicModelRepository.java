package eclosia.eclosia_organization_service.academic_model.repository;

import eclosia.eclosia_organization_service.academic_model.entity.AcademicModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AcademicModelRepository extends JpaRepository<AcademicModel, UUID> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);
}
