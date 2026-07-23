package eclosia.eclosia_organization_service.security_module.repository;

import eclosia.eclosia_organization_service.security_module.entity.SecurityModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SecurityModuleRepository extends JpaRepository<SecurityModule, UUID> {

    List<SecurityModule> findAllByOrderByDisplayOrderAscCodeAsc();

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);
}
