package eclosia.eclosia_organization_service.school.repository;

import eclosia.eclosia_organization_service.school.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SchoolRepository extends JpaRepository<School, UUID> {

    boolean existsByCode(String code);

    List<School> findByGroupId(UUID groupId);

    List<School> findByIdIn(Collection<UUID> ids);
}
