package eclosia.eclosia_organization_service.school_currency.controller;

import eclosia.eclosia_organization_service.school_currency.dto.CreateSchoolCurrencyDto;
import eclosia.eclosia_organization_service.school_currency.dto.UpdateSchoolCurrencyDto;
import eclosia.eclosia_organization_service.school_currency.entity.SchoolCurrency;
import eclosia.eclosia_organization_service.school_currency.service.SchoolCurrencyService;
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
@RequestMapping(path = "school-currency")
@RequiredArgsConstructor
public class SchoolCurrencyController {

    private final SchoolCurrencyService service;

    @PostMapping
    public ResponseEntity<SchoolCurrency> create(@Valid @RequestBody CreateSchoolCurrencyDto dto) {
        SchoolCurrency schoolCurrency = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(schoolCurrency);
    }

    @GetMapping
    public List<SchoolCurrency> findAll(
            @RequestParam(required = false) UUID schoolId
    ) {
        if (schoolId != null) {
            return service.findBySchoolId(schoolId);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    public SchoolCurrency findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public SchoolCurrency update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSchoolCurrencyDto dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
