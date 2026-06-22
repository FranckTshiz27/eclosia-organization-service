package eclosia.eclosia_organization_service.country.controller;

import eclosia.eclosia_organization_service.country.dto.CreateCountryDto;
import eclosia.eclosia_organization_service.country.dto.UpdateCountryDto;
import eclosia.eclosia_organization_service.country.entity.Country;
import eclosia.eclosia_organization_service.country.service.CountryService;
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
@RequestMapping(path = "country")
@RequiredArgsConstructor
public class CountryController {

    private final CountryService service;

    @PostMapping
    public ResponseEntity<Country> create(@Valid @RequestBody CreateCountryDto dto) {
        Country country = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(country);
    }

    @GetMapping
    public List<Country> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Country findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public Country update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCountryDto dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
