package eclosia.eclosia_organization_service.currency.controller;

import eclosia.eclosia_organization_service.currency.dto.CreateCurrencyDto;
import eclosia.eclosia_organization_service.currency.dto.UpdateCurrencyDto;
import eclosia.eclosia_organization_service.currency.entity.Currency;
import eclosia.eclosia_organization_service.currency.service.CurrencyService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping(path = "currency")
@RequiredArgsConstructor
public class CurrencyController {

    private final CurrencyService service;

    @PostMapping
    public ResponseEntity<Currency> create(@Valid @RequestBody CreateCurrencyDto dto) {
        Currency currency = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(currency);
    }

    @GetMapping
    public List<Currency> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Currency findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public Currency update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCurrencyDto dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
