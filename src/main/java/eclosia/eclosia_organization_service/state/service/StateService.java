package eclosia.eclosia_organization_service.state.service;

import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.state.dto.CreateStateDto;
import eclosia.eclosia_organization_service.state.dto.UpdateStateDto;
import eclosia.eclosia_organization_service.state.entity.State;
import eclosia.eclosia_organization_service.state.repository.StateRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Data
public class StateService {

    private final StateRepository repository;

    public State create(CreateStateDto dto) {
        State state = new State();
        state.setId(dto.getId());
        state.setName(dto.getName());
        state.setCountryId(dto.getCountryId());
        state.setCountryCode(dto.getCountryCode());
        state.setCountryName(dto.getCountryName());
        state.setIso2(dto.getIso2());
        state.setIso3166_2(dto.getIso3166_2());
        state.setFipsCode(dto.getFipsCode());
        state.setType(dto.getType());
        state.setLevel(dto.getLevel());
        state.setParentId(dto.getParentId());
        state.setNativeName(dto.getNativeName());
        state.setLatitude(dto.getLatitude());
        state.setLongitude(dto.getLongitude());
        state.setTimezone(dto.getTimezone());
        state.setWikiDataId(dto.getWikiDataId());
        state.setPopulation(dto.getPopulation());
        return repository.save(state);
    }

    public List<State> findAll() {
        return repository.findAll();
    }

    public State findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("State not found"));
    }

    public State update(Long id, UpdateStateDto dto) {
        State state = findById(id);
        state.setName(dto.getName());
        state.setCountryId(dto.getCountryId());
        state.setCountryCode(dto.getCountryCode());
        state.setCountryName(dto.getCountryName());
        state.setIso2(dto.getIso2());
        state.setIso3166_2(dto.getIso3166_2());
        state.setFipsCode(dto.getFipsCode());
        state.setType(dto.getType());
        state.setLevel(dto.getLevel());
        state.setParentId(dto.getParentId());
        state.setNativeName(dto.getNativeName());
        state.setLatitude(dto.getLatitude());
        state.setLongitude(dto.getLongitude());
        state.setTimezone(dto.getTimezone());
        state.setWikiDataId(dto.getWikiDataId());
        state.setPopulation(dto.getPopulation());
        return repository.save(state);
    }

    public void delete(Long id) {
        State state = findById(id);
        repository.delete(state);
    }
}
