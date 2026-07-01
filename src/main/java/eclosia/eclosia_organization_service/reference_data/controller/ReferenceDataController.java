package eclosia.eclosia_organization_service.reference_data.controller;

import eclosia.eclosia_organization_service.reference_data.dto.SchoolTypesResponseDto;
import eclosia.eclosia_organization_service.reference_data.service.ReferenceDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import eclosia.eclosia_organization_service.reference_data.dto.ReferenceOptionDto;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping(path = "reference-data")
@RequiredArgsConstructor
public class ReferenceDataController {

    private final ReferenceDataService service;

    @GetMapping("/school-types")
    public SchoolTypesResponseDto getSchoolTypes() {
        return service.getSchoolTypes();
    }

    @GetMapping("/genders")
    public List<ReferenceOptionDto> getGenders() {
        return service.getGenders();
    }
}
