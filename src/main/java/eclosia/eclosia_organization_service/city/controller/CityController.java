package eclosia.eclosia_organization_service.city.controller;

import eclosia.eclosia_organization_service.city.dto.CreateCityDto;
import eclosia.eclosia_organization_service.city.dto.UpdateCityDto;
import eclosia.eclosia_organization_service.city.entity.City;
import eclosia.eclosia_organization_service.city.service.CityService;
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
@RequestMapping(path = "city")
@RequiredArgsConstructor
public class CityController {

    private final CityService service;

    @PostMapping
    public ResponseEntity<City> create(@Valid @RequestBody CreateCityDto dto) {
        City city = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(city);
    }

    @GetMapping
    public List<City> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public City findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public City update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCityDto dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
