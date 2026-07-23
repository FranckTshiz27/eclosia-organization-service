package eclosia.eclosia_organization_service.teacher_course_assignment.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import eclosia.eclosia_organization_service.common.jackson.CreateTeacherCourseAssignmentListDeserializer;

import java.util.ArrayList;

@JsonDeserialize(using = CreateTeacherCourseAssignmentListDeserializer.class)
public class CreateTeacherCourseAssignmentBatch extends ArrayList<CreateTeacherCourseAssignmentDto> {
}
