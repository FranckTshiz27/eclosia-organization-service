package eclosia.eclosia_organization_service.commune.repository;

import eclosia.eclosia_organization_service.commune.entity.Commune;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CommuneRepository extends JpaRepository<Commune, UUID> {

    boolean existsByCityIdAndName(UUID cityId, String name);

    boolean existsByCityIdAndNameAndIdNot(UUID cityId, String name, UUID id);
}
