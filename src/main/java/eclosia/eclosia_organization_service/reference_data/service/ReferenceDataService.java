package eclosia.eclosia_organization_service.reference_data.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eclosia.eclosia_organization_service.reference_data.dto.ReferenceOptionDto;
import eclosia.eclosia_organization_service.reference_data.dto.SchoolTypesResponseDto;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
public class ReferenceDataService {

    private static final String SCHOOL_TYPES_FILE_PATH = "data/school-types.json";
    private static final String GENDERS_FILE_PATH = "data/genders.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public SchoolTypesResponseDto getSchoolTypes() {
        try (InputStream inputStream = new ClassPathResource(SCHOOL_TYPES_FILE_PATH).getInputStream()) {
            return objectMapper.readValue(inputStream, SchoolTypesResponseDto.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read school types json file", exception);
        }
    }

    public List<ReferenceOptionDto> getGenders() {
        try (InputStream inputStream = new ClassPathResource(GENDERS_FILE_PATH).getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<>() {
            });
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read genders json file", exception);
        }
    }
}
