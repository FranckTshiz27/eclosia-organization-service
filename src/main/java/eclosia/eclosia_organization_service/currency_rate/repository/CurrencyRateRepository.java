package eclosia.eclosia_organization_service.currency_rate.repository;

import eclosia.eclosia_organization_service.currency_rate.entity.CurrencyRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CurrencyRateRepository extends JpaRepository<CurrencyRate, UUID> {

    List<CurrencyRate> findBySchool_IdOrderByEffectiveDateDesc(UUID schoolId);

    List<CurrencyRate> findBySchool_IdAndSourceCurrency_IdAndTargetCurrency_IdOrderByEffectiveDateDesc(
            UUID schoolId,
            UUID sourceCurrencyId,
            UUID targetCurrencyId
    );

    List<CurrencyRate> findBySchool_IdAndSourceCurrency_IdAndTargetCurrency_IdAndActiveTrue(
            UUID schoolId,
            UUID sourceCurrencyId,
            UUID targetCurrencyId
    );

    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM CurrencyRate r
            WHERE r.school.id = :schoolId
              AND r.sourceCurrency.id = :sourceCurrencyId
              AND r.targetCurrency.id = :targetCurrencyId
              AND r.effectiveDate = :effectiveDate
              AND (:excludeId IS NULL OR r.id <> :excludeId)
            """)
    boolean existsDuplicate(
            @Param("schoolId") UUID schoolId,
            @Param("sourceCurrencyId") UUID sourceCurrencyId,
            @Param("targetCurrencyId") UUID targetCurrencyId,
            @Param("effectiveDate") LocalDate effectiveDate,
            @Param("excludeId") UUID excludeId
    );
}
