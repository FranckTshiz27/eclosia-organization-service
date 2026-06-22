package eclosia.eclosia_organization_service.reference_data.dto;

import lombok.Data;

import java.util.List;

@Data
public class SchoolTypesResponseDto {

    private List<ReferenceOptionDto> schoolTypes;
}
