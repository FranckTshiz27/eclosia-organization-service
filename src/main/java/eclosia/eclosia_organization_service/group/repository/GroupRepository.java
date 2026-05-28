package eclosia.eclosia_organization_service.group.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import eclosia.eclosia_organization_service.group.entity.Group;


import java.util.Optional;
import java.util.UUID;

public interface GroupRepository extends JpaRepository<Group, UUID> {

    boolean existsByName(String name);

    Optional<Group> findByName(String name);
}
