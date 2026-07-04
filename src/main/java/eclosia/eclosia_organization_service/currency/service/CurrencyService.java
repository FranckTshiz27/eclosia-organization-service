package eclosia.eclosia_organization_service.currency.service;

import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.currency.dto.CreateCurrencyDto;
import eclosia.eclosia_organization_service.currency.dto.UpdateCurrencyDto;
import eclosia.eclosia_organization_service.currency.entity.Currency;
import eclosia.eclosia_organization_service.currency.entity.CurrencySymbolPosition;
import eclosia.eclosia_organization_service.currency.repository.CurrencyRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Data
public class CurrencyService {

    private final CurrencyRepository repository;

    public Currency create(CreateCurrencyDto dto) {
        String code = normalizeCode(dto.getCode());
        validateUniqueness(code, dto.getName(), null);

        Currency currency = new Currency();
        mapFromDto(
                currency,
                code,
                dto.getName(),
                dto.getSymbol(),
                dto.getDecimalPlaces(),
                dto.getNumericCode(),
                dto.getSymbolPosition(),
                dto.getActive(),
                dto.getComment()
        );
        return repository.save(currency);
    }

    public List<Currency> findAll() {
        return repository.findAllByOrderByCodeAsc();
    }

    public Currency findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Currency not found"));
    }

    public Currency update(UUID id, UpdateCurrencyDto dto) {
        Currency currency = findById(id);
        String code = normalizeCode(dto.getCode());
        validateUniqueness(code, dto.getName(), id);

        mapFromDto(
                currency,
                code,
                dto.getName(),
                dto.getSymbol(),
                dto.getDecimalPlaces(),
                dto.getNumericCode(),
                dto.getSymbolPosition(),
                dto.getActive(),
                dto.getComment()
        );
        return repository.save(currency);
    }

    public void delete(UUID id) {
        Currency currency = findById(id);
        repository.delete(currency);
    }

    private void validateUniqueness(String code, String name, UUID excludeId) {
        if (excludeId == null) {
            if (repository.existsByCode(code)) {
                throw new BadRequestException("Currency code already exists");
            }
            if (repository.existsByName(name)) {
                throw new BadRequestException("Currency name already exists");
            }
            return;
        }

        if (repository.existsByCodeAndIdNot(code, excludeId)) {
            throw new BadRequestException("Currency code already exists");
        }
        if (repository.existsByNameAndIdNot(name, excludeId)) {
            throw new BadRequestException("Currency name already exists");
        }
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }

    private void mapFromDto(
            Currency currency,
            String code,
            String name,
            String symbol,
            Integer decimalPlaces,
            String numericCode,
            CurrencySymbolPosition symbolPosition,
            Boolean active,
            String comment
    ) {
        currency.setCode(code);
        currency.setName(name);
        currency.setSymbol(symbol);
        currency.setDecimalPlaces(decimalPlaces != null ? decimalPlaces : 2);
        currency.setNumericCode(numericCode);
        currency.setSymbolPosition(symbolPosition != null ? symbolPosition : CurrencySymbolPosition.BEFORE);
        currency.setActive(active != null ? active : true);
        currency.setComment(comment);
    }
}
