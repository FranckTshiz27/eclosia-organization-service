package eclosia.eclosia_organization_service.commune.service;

import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.commune.dto.CreateCommuneDto;
import eclosia.eclosia_organization_service.commune.dto.UpdateCommuneDto;
import eclosia.eclosia_organization_service.commune.entity.Commune;
import eclosia.eclosia_organization_service.commune.repository.CommuneRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Data
public class CommuneService {

    private final CommuneRepository repository;

    public Commune create(CreateCommuneDto dto) {
        if (repository.existsByCityIdAndName(dto.getCityId(), dto.getName())) {
            throw new BadRequestException("Commune name already exists for this city");
        }

        Commune commune = new Commune();
        commune.setCityId(dto.getCityId());
        commune.setName(dto.getName());
        commune.setCode(dto.getCode());
        commune.setLatitude(dto.getLatitude());
        commune.setLongitude(dto.getLongitude());
        commune.setPopulation(dto.getPopulation());
        return repository.save(commune);
    }

    public List<Commune> findAll() {
        return repository.findAll();
    }

    public Commune findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commune not found"));
    }

    public Commune update(UUID id, UpdateCommuneDto dto) {
        Commune commune = findById(id);

        if (repository.existsByCityIdAndNameAndIdNot(dto.getCityId(), dto.getName(), id)) {
            throw new BadRequestException("Commune name already exists for this city");
        }

        commune.setCityId(dto.getCityId());
        commune.setName(dto.getName());
        commune.setCode(dto.getCode());
        commune.setLatitude(dto.getLatitude());
        commune.setLongitude(dto.getLongitude());
        commune.setPopulation(dto.getPopulation());
        return repository.save(commune);
    }

    public void delete(UUID id) {
        Commune commune = findById(id);
        repository.delete(commune);
    }
}
