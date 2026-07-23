package eclosia.eclosia_organization_service.role.repository;

import eclosia.eclosia_organization_service.role.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    List<Role> findAllByOrderByDisplayOrderAscCodeAsc();

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);
}
