package eclosia.eclosia_organization_service.school_currency.service;

import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.BusinessException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.currency.entity.Currency;
import eclosia.eclosia_organization_service.currency.repository.CurrencyRepository;
import eclosia.eclosia_organization_service.school.entity.School;
import eclosia.eclosia_organization_service.school.repository.SchoolRepository;
import eclosia.eclosia_organization_service.school_currency.dto.CreateSchoolCurrencyDto;
import eclosia.eclosia_organization_service.school_currency.dto.UpdateSchoolCurrencyDto;
import eclosia.eclosia_organization_service.school_currency.entity.SchoolCurrency;
import eclosia.eclosia_organization_service.school_currency.repository.SchoolCurrencyRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Data
public class SchoolCurrencyService {

    private final SchoolCurrencyRepository repository;
    private final SchoolRepository schoolRepository;
    private final CurrencyRepository currencyRepository;

    @Transactional
    public SchoolCurrency create(CreateSchoolCurrencyDto dto) {
        validateUniqueness(dto.getSchoolId(), dto.getCurrencyId(), null);

        School school = resolveSchool(dto.getSchoolId());
        Currency currency = resolveActiveCurrency(dto.getCurrencyId());

        SchoolCurrency schoolCurrency = new SchoolCurrency();
        mapFromDto(
                schoolCurrency,
                school,
                currency,
                dto.getIsDefault(),
                dto.getActive(),
                dto.getComment()
        );

        if (Boolean.TRUE.equals(schoolCurrency.getIsDefault())) {
            clearOtherDefaults(dto.getSchoolId(), null);
        }

        return repository.save(schoolCurrency);
    }

    public List<SchoolCurrency> findAll() {
        return repository.findAll();
    }

    public List<SchoolCurrency> findBySchoolId(UUID schoolId) {
        return repository.findBySchool_IdOrderByCurrency_CodeAsc(schoolId);
    }

    public SchoolCurrency findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("School currency not found"));
    }

    @Transactional
    public SchoolCurrency update(UUID id, UpdateSchoolCurrencyDto dto) {
        SchoolCurrency schoolCurrency = findById(id);
        validateUniqueness(dto.getSchoolId(), dto.getCurrencyId(), id);

        School school = resolveSchool(dto.getSchoolId());
        Currency currency = resolveActiveCurrency(dto.getCurrencyId());

        mapFromDto(
                schoolCurrency,
                school,
                currency,
                dto.getIsDefault(),
                dto.getActive(),
                dto.getComment()
        );

        if (Boolean.TRUE.equals(schoolCurrency.getIsDefault())) {
            clearOtherDefaults(dto.getSchoolId(), id);
        }

        return repository.save(schoolCurrency);
    }

    public void delete(UUID id) {
        SchoolCurrency schoolCurrency = findById(id);
        repository.delete(schoolCurrency);
    }

    private void validateUniqueness(UUID schoolId, UUID currencyId, UUID excludeId) {
        if (excludeId == null) {
            if (repository.existsBySchool_IdAndCurrency_Id(schoolId, currencyId)) {
                throw new BadRequestException("This currency is already linked to the school");
            }
            return;
        }

        if (repository.existsBySchool_IdAndCurrency_IdAndIdNot(schoolId, currencyId, excludeId)) {
            throw new BadRequestException("This currency is already linked to the school");
        }
    }

    private void mapFromDto(
            SchoolCurrency schoolCurrency,
            School school,
            Currency currency,
            Boolean isDefault,
            Boolean active,
            String comment
    ) {
        boolean defaultCurrency = isDefault != null ? isDefault : false;
        boolean activeCurrency = active != null ? active : true;

        if (defaultCurrency && !activeCurrency) {
            throw new BusinessException("La devise par défaut doit être active.");
        }

        schoolCurrency.setSchool(school);
        schoolCurrency.setCurrency(currency);
        schoolCurrency.setIsDefault(defaultCurrency);
        schoolCurrency.setActive(activeCurrency);
        schoolCurrency.setComment(comment);
    }

    private void clearOtherDefaults(UUID schoolId, UUID excludeId) {
        repository.findBySchool_IdAndIsDefaultTrue(schoolId).stream()
                .filter(schoolCurrency -> excludeId == null || !excludeId.equals(schoolCurrency.getId()))
                .forEach(schoolCurrency -> {
                    schoolCurrency.setIsDefault(false);
                    repository.save(schoolCurrency);
                });
    }

    private School resolveSchool(UUID schoolId) {
        return schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));
    }

    private Currency resolveActiveCurrency(UUID currencyId) {
        Currency currency = currencyRepository.findById(currencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Currency not found"));

        if (!Boolean.TRUE.equals(currency.getActive())) {
            throw new BusinessException("Cette devise n'est plus active.");
        }

        return currency;
    }
}
