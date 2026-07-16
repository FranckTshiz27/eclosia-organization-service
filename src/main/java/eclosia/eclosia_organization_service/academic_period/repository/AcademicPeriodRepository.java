package eclosia.eclosia_organization_service.academic_period.repository;

import eclosia.eclosia_organization_service.academic_period.entity.AcademicPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AcademicPeriodRepository extends JpaRepository<AcademicPeriod, UUID> {

    boolean existsByCountry_IdAndCode(UUID countryId, String code);

    boolean existsByCountry_IdAndCodeAndIdNot(UUID countryId, String code, UUID id);

    List<AcademicPeriod> findByCountry_IdOrderByOrderNumberAsc(UUID countryId);
}
