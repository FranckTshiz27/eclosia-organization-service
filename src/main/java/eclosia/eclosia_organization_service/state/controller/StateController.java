package eclosia.eclosia_organization_service.state.controller;

import eclosia.eclosia_organization_service.state.dto.CreateStateDto;
import eclosia.eclosia_organization_service.state.dto.UpdateStateDto;
import eclosia.eclosia_organization_service.state.entity.State;
import eclosia.eclosia_organization_service.state.service.StateService;
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

@CrossOrigin(origins = "*")
@RestController
@RequestMapping(path = "state")
@RequiredArgsConstructor
public class StateController {

    private final StateService service;

    @PostMapping
    public ResponseEntity<State> create(@Valid @RequestBody CreateStateDto dto) {
        State state = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(state);
    }

    @GetMapping
    public List<State> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public State findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public State update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStateDto dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
