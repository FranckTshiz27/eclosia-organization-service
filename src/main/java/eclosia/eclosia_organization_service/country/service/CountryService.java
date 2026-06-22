package eclosia.eclosia_organization_service.country.service;

import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.country.dto.CreateCountryDto;
import eclosia.eclosia_organization_service.country.dto.UpdateCountryDto;
import eclosia.eclosia_organization_service.country.entity.Country;
import eclosia.eclosia_organization_service.country.repository.CountryRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Data
public class CountryService {

    private final CountryRepository repository;

    public Country create(CreateCountryDto dto) {
        Country country = new Country();
        country.setIso2(dto.getIso2());
        country.setIso3(dto.getIso3());
        country.setNameFr(dto.getNameFr());
        country.setNameEn(dto.getNameEn());
        country.setPhoneCode(dto.getPhoneCode());
        country.setCurrencyCode(dto.getCurrencyCode());
        return repository.save(country);
    }

    public List<Country> findAll() {
        return repository.findAll();
    }

    public Country findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Country not found"));
    }

    public Country update(UUID id, UpdateCountryDto dto) {
        Country country = findById(id);
        country.setIso2(dto.getIso2());
        country.setIso3(dto.getIso3());
        country.setNameFr(dto.getNameFr());
        country.setNameEn(dto.getNameEn());
        country.setPhoneCode(dto.getPhoneCode());
        country.setCurrencyCode(dto.getCurrencyCode());
        return repository.save(country);
    }

    public void delete(UUID id) {
        Country country = findById(id);
        repository.delete(country);
    }
}
