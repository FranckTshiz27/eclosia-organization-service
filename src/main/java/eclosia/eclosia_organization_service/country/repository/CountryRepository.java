package eclosia.eclosia_organization_service.country.repository;

import eclosia.eclosia_organization_service.country.entity.Country;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CountryRepository extends JpaRepository<Country, UUID> {
}
