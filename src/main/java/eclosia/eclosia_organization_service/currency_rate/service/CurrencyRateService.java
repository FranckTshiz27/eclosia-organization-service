package eclosia.eclosia_organization_service.currency_rate.service;

import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.BusinessException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.currency.entity.Currency;
import eclosia.eclosia_organization_service.currency.repository.CurrencyRepository;
import eclosia.eclosia_organization_service.currency_rate.dto.CreateCurrencyRateDto;
import eclosia.eclosia_organization_service.currency_rate.dto.UpdateCurrencyRateDto;
import eclosia.eclosia_organization_service.currency_rate.entity.CurrencyRate;
import eclosia.eclosia_organization_service.currency_rate.entity.RateSource;
import eclosia.eclosia_organization_service.currency_rate.repository.CurrencyRateRepository;
import eclosia.eclosia_organization_service.school.entity.School;
import eclosia.eclosia_organization_service.school.repository.SchoolRepository;
import eclosia.eclosia_organization_service.school_currency.repository.SchoolCurrencyRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Data
public class CurrencyRateService {

    private final CurrencyRateRepository repository;
    private final SchoolRepository schoolRepository;
    private final CurrencyRepository currencyRepository;
    private final SchoolCurrencyRepository schoolCurrencyRepository;

    @Transactional
    public CurrencyRate create(CreateCurrencyRateDto dto) {
        ResolvedRelations relations = resolveRelations(dto);
        validateDuplicate(
                dto.getSchoolId(),
                dto.getSourceCurrencyId(),
                dto.getTargetCurrencyId(),
                dto.getEffectiveDate(),
                null
        );

        CurrencyRate currencyRate = new CurrencyRate();
        mapFromDto(
                currencyRate,
                relations,
                dto.getRate(),
                dto.getEffectiveDate(),
                dto.getActive(),
                dto.getSource(),
                dto.getComment()
        );

        if (Boolean.TRUE.equals(currencyRate.getActive())) {
            clearOtherActiveRates(
                    dto.getSchoolId(),
                    dto.getSourceCurrencyId(),
                    dto.getTargetCurrencyId(),
                    null
            );
        }

        return repository.save(currencyRate);
    }

    public List<CurrencyRate> findAll() {
        return repository.findAll();
    }

    public List<CurrencyRate> findBySchoolId(UUID schoolId) {
        return repository.findBySchool_IdOrderByEffectiveDateDesc(schoolId);
    }

    public List<CurrencyRate> findBySchoolIdAndCurrencyPair(
            UUID schoolId,
            UUID sourceCurrencyId,
            UUID targetCurrencyId
    ) {
        validateDistinctCurrencies(sourceCurrencyId, targetCurrencyId);
        return repository.findBySchool_IdAndSourceCurrency_IdAndTargetCurrency_IdOrderByEffectiveDateDesc(
                schoolId,
                sourceCurrencyId,
                targetCurrencyId
        );
    }

    public CurrencyRate findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Currency rate not found"));
    }

    @Transactional
    public CurrencyRate update(UUID id, UpdateCurrencyRateDto dto) {
        CurrencyRate currencyRate = findById(id);
        ResolvedRelations relations = resolveRelations(dto);
        validateDuplicate(
                dto.getSchoolId(),
                dto.getSourceCurrencyId(),
                dto.getTargetCurrencyId(),
                dto.getEffectiveDate(),
                id
        );

        mapFromDto(
                currencyRate,
                relations,
                dto.getRate(),
                dto.getEffectiveDate(),
                dto.getActive(),
                dto.getSource(),
                dto.getComment()
        );

        if (Boolean.TRUE.equals(currencyRate.getActive())) {
            clearOtherActiveRates(
                    dto.getSchoolId(),
                    dto.getSourceCurrencyId(),
                    dto.getTargetCurrencyId(),
                    id
            );
        }

        return repository.save(currencyRate);
    }

    public void delete(UUID id) {
        CurrencyRate currencyRate = findById(id);
        repository.delete(currencyRate);
    }

    private ResolvedRelations resolveRelations(CreateCurrencyRateDto dto) {
        return resolveRelations(
                dto.getSchoolId(),
                dto.getSourceCurrencyId(),
                dto.getTargetCurrencyId()
        );
    }

    private ResolvedRelations resolveRelations(UpdateCurrencyRateDto dto) {
        return resolveRelations(
                dto.getSchoolId(),
                dto.getSourceCurrencyId(),
                dto.getTargetCurrencyId()
        );
    }

    private ResolvedRelations resolveRelations(
            UUID schoolId,
            UUID sourceCurrencyId,
            UUID targetCurrencyId
    ) {
        validateDistinctCurrencies(sourceCurrencyId, targetCurrencyId);

        School school = resolveSchool(schoolId);
        Currency sourceCurrency = resolveSchoolAuthorizedCurrency(schoolId, sourceCurrencyId, "Source currency");
        Currency targetCurrency = resolveSchoolAuthorizedCurrency(schoolId, targetCurrencyId, "Target currency");

        return new ResolvedRelations(school, sourceCurrency, targetCurrency);
    }

    private void validateDistinctCurrencies(UUID sourceCurrencyId, UUID targetCurrencyId) {
        if (sourceCurrencyId.equals(targetCurrencyId)) {
            throw new BadRequestException("Source and target currencies must be different");
        }
    }

    private void validateDuplicate(
            UUID schoolId,
            UUID sourceCurrencyId,
            UUID targetCurrencyId,
            LocalDate effectiveDate,
            UUID excludeId
    ) {
        if (repository.existsDuplicate(
                schoolId,
                sourceCurrencyId,
                targetCurrencyId,
                effectiveDate,
                excludeId
        )) {
            throw new BadRequestException("A currency rate already exists for this school, pair and effective date");
        }
    }

    private void mapFromDto(
            CurrencyRate currencyRate,
            ResolvedRelations relations,
            java.math.BigDecimal rate,
            LocalDate effectiveDate,
            Boolean active,
            RateSource source,
            String comment
    ) {
        currencyRate.setSchool(relations.school());
        currencyRate.setSourceCurrency(relations.sourceCurrency());
        currencyRate.setTargetCurrency(relations.targetCurrency());
        currencyRate.setRate(rate);
        currencyRate.setEffectiveDate(effectiveDate);
        currencyRate.setActive(active != null ? active : true);
        currencyRate.setSource(source != null ? source : RateSource.MANUAL);
        currencyRate.setComment(comment);
    }

    private void clearOtherActiveRates(
            UUID schoolId,
            UUID sourceCurrencyId,
            UUID targetCurrencyId,
            UUID excludeId
    ) {
        repository.findBySchool_IdAndSourceCurrency_IdAndTargetCurrency_IdAndActiveTrue(
                        schoolId,
                        sourceCurrencyId,
                        targetCurrencyId
                ).stream()
                .filter(currencyRate -> excludeId == null || !excludeId.equals(currencyRate.getId()))
                .forEach(currencyRate -> {
                    currencyRate.setActive(false);
                    repository.save(currencyRate);
                });
    }

    private School resolveSchool(UUID schoolId) {
        return schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));
    }

    private Currency resolveSchoolAuthorizedCurrency(UUID schoolId, UUID currencyId, String label) {
        Currency currency = currencyRepository.findById(currencyId)
                .orElseThrow(() -> new ResourceNotFoundException(label + " not found"));

        if (!Boolean.TRUE.equals(currency.getActive())) {
            throw new BusinessException(label + " is not active");
        }

        if (!schoolCurrencyRepository.existsBySchool_IdAndCurrency_IdAndActiveTrue(schoolId, currencyId)) {
            throw new BusinessException(label + " is not authorized for this school");
        }

        return currency;
    }

    private record ResolvedRelations(
            School school,
            Currency sourceCurrency,
            Currency targetCurrency
    ) {
    }
}
