package eclosia.eclosia_organization_service.common.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import eclosia.eclosia_organization_service.teacher_course_assignment.dto.CreateTeacherCourseAssignmentBatch;
import eclosia.eclosia_organization_service.teacher_course_assignment.dto.CreateTeacherCourseAssignmentDto;

import java.io.IOException;
import java.util.List;

public class CreateTeacherCourseAssignmentListDeserializer
        extends JsonDeserializer<CreateTeacherCourseAssignmentBatch> {

    private static final TypeReference<List<CreateTeacherCourseAssignmentDto>> LIST_TYPE =
            new TypeReference<>() {
            };

    @Override
    public CreateTeacherCourseAssignmentBatch deserialize(
            JsonParser parser,
            DeserializationContext context
    ) throws IOException {
        ObjectMapper mapper = (ObjectMapper) parser.getCodec();
        JsonToken token = parser.currentToken();
        CreateTeacherCourseAssignmentBatch batch = new CreateTeacherCourseAssignmentBatch();

        if (token == JsonToken.START_ARRAY) {
            batch.addAll(mapper.readValue(parser, LIST_TYPE));
            return batch;
        }

        if (token == JsonToken.START_OBJECT) {
            batch.add(mapper.readValue(parser, CreateTeacherCourseAssignmentDto.class));
            return batch;
        }

        throw InvalidFormatException.from(
                parser,
                "Expected a course assignment object or an array of course assignments",
                parser.getText(),
                CreateTeacherCourseAssignmentBatch.class
        );
    }
}
