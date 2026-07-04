package eclosia.eclosia_organization_service.currency_rate.controller;

import eclosia.eclosia_organization_service.currency_rate.dto.CreateCurrencyRateDto;
import eclosia.eclosia_organization_service.currency_rate.dto.UpdateCurrencyRateDto;
import eclosia.eclosia_organization_service.currency_rate.entity.CurrencyRate;
import eclosia.eclosia_organization_service.currency_rate.service.CurrencyRateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping(path = "currency-rate")
@RequiredArgsConstructor
public class CurrencyRateController {

    private final CurrencyRateService service;

    @PostMapping
    public ResponseEntity<CurrencyRate> create(@Valid @RequestBody CreateCurrencyRateDto dto) {
        CurrencyRate currencyRate = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(currencyRate);
    }

    @GetMapping
    public List<CurrencyRate> findAll(
            @RequestParam(required = false) UUID schoolId,
            @RequestParam(required = false) UUID sourceCurrencyId,
            @RequestParam(required = false) UUID targetCurrencyId
    ) {
        if (schoolId != null && sourceCurrencyId != null && targetCurrencyId != null) {
            return service.findBySchoolIdAndCurrencyPair(schoolId, sourceCurrencyId, targetCurrencyId);
        }
        if (schoolId != null) {
            return service.findBySchoolId(schoolId);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    public CurrencyRate findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public CurrencyRate update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCurrencyRateDto dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
