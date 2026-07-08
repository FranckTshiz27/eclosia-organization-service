package eclosia.eclosia_organization_service.school_currency.repository;

import eclosia.eclosia_organization_service.school_currency.entity.SchoolCurrency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SchoolCurrencyRepository extends JpaRepository<SchoolCurrency, UUID> {

    List<SchoolCurrency> findBySchool_IdOrderByCurrency_CodeAsc(UUID schoolId);

    List<SchoolCurrency> findBySchool_IdAndIsDefaultTrue(UUID schoolId);

    @Query("""
            SELECT sc
            FROM SchoolCurrency sc
            JOIN FETCH sc.currency
            WHERE sc.school.id = :schoolId
              AND sc.isDefault = true
              AND sc.active = true
            """)
    Optional<SchoolCurrency> findActiveDefaultBySchoolId(@Param("schoolId") UUID schoolId);

    boolean existsBySchool_IdAndCurrency_Id(UUID schoolId, UUID currencyId);

    boolean existsBySchool_IdAndCurrency_IdAndActiveTrue(UUID schoolId, UUID currencyId);

    boolean existsBySchool_IdAndCurrency_IdAndIdNot(UUID schoolId, UUID currencyId, UUID id);
}
