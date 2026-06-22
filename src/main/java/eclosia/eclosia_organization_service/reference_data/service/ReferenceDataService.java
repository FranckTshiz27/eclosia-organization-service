package eclosia.eclosia_organization_service.reference_data.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import eclosia.eclosia_organization_service.reference_data.dto.SchoolTypesResponseDto;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
public class ReferenceDataService {

    private static final String SCHOOL_TYPES_FILE_PATH = "data/school-types.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public SchoolTypesResponseDto getSchoolTypes() {
        try (InputStream inputStream = new ClassPathResource(SCHOOL_TYPES_FILE_PATH).getInputStream()) {
            return objectMapper.readValue(inputStream, SchoolTypesResponseDto.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read school types json file", exception);
        }
    }
}
