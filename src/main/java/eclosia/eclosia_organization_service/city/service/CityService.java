package eclosia.eclosia_organization_service.city.service;

import eclosia.eclosia_organization_service.city.dto.CreateCityDto;
import eclosia.eclosia_organization_service.city.dto.UpdateCityDto;
import eclosia.eclosia_organization_service.city.entity.City;
import eclosia.eclosia_organization_service.city.repository.CityRepository;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Data
public class CityService {

    private final CityRepository repository;

    public City create(CreateCityDto dto) {
        City city = new City();
        city.setProvinceId(dto.getProvinceId());
        city.setName(dto.getName());
        return repository.save(city);
    }

    public List<City> findAll() {
        return repository.findAll();
    }

    public City findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("City not found"));
    }

    public City update(UUID id, UpdateCityDto dto) {
        City city = findById(id);
        city.setProvinceId(dto.getProvinceId());
        city.setName(dto.getName());
        return repository.save(city);
    }

    public void delete(UUID id) {
        City city = findById(id);
        repository.delete(city);
    }
}
