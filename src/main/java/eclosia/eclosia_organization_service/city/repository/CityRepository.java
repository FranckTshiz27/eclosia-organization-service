package eclosia.eclosia_organization_service.city.repository;

import eclosia.eclosia_organization_service.city.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CityRepository extends JpaRepository<City, UUID> {
}
