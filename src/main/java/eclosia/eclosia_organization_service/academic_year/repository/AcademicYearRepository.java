package eclosia.eclosia_organization_service.academic_year.repository;

import eclosia.eclosia_organization_service.academic_year.entity.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, UUID> {

    boolean existsByCountry_IdAndCode(UUID countryId, String code);

    boolean existsByCountry_IdAndCodeAndIdNot(UUID countryId, String code, UUID id);

    List<AcademicYear> findByCountry_IdOrderByStartDateDesc(UUID countryId);
}
