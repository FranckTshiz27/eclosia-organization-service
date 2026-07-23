package eclosia.eclosia_organization_service.teacher_course_assignment.repository;

import eclosia.eclosia_organization_service.teacher_course_assignment.entity.TeacherCourseAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeacherCourseAssignmentRepository extends JpaRepository<TeacherCourseAssignment, UUID> {

    boolean existsByTeacher_IdAndClassroom_IdAndSubject_IdAndAcademicYear_IdAndActiveTrue(
            UUID teacherId,
            UUID classroomId,
            UUID subjectId,
            UUID academicYearId
    );

    boolean existsByTeacher_IdAndClassroom_IdAndSubject_IdAndAcademicYear_IdAndActiveTrueAndIdNot(
            UUID teacherId,
            UUID classroomId,
            UUID subjectId,
            UUID academicYearId,
            UUID id
    );

    @Query("""
            SELECT a FROM TeacherCourseAssignment a
            JOIN FETCH a.teacher t
            JOIN FETCH t.securityUser
            JOIN FETCH a.school
            JOIN FETCH a.academicYear
            JOIN FETCH a.classroom c
            JOIN FETCH c.academicLevel
            JOIN FETCH c.classroomDesignation
            LEFT JOIN FETCH c.academicSection
            LEFT JOIN FETCH c.academicOption
            JOIN FETCH a.subject
            WHERE a.id = :id
            """)
    Optional<TeacherCourseAssignment> findDetailedById(@Param("id") UUID id);

    @Query("""
            SELECT a FROM TeacherCourseAssignment a
            JOIN FETCH a.teacher t
            JOIN FETCH t.securityUser
            JOIN FETCH a.school
            JOIN FETCH a.academicYear
            JOIN FETCH a.classroom c
            JOIN FETCH c.academicLevel
            JOIN FETCH c.classroomDesignation
            LEFT JOIN FETCH c.academicSection
            LEFT JOIN FETCH c.academicOption
            JOIN FETCH a.subject
            WHERE a.teacher.id = :teacherId
              AND (:academicYearId IS NULL OR a.academicYear.id = :academicYearId)
            ORDER BY a.createdAt DESC
            """)
    List<TeacherCourseAssignment> findByTeacher(
            @Param("teacherId") UUID teacherId,
            @Param("academicYearId") UUID academicYearId
    );

    @Query("""
            SELECT a FROM TeacherCourseAssignment a
            JOIN FETCH a.teacher t
            JOIN FETCH t.securityUser
            JOIN FETCH a.school
            JOIN FETCH a.academicYear
            JOIN FETCH a.classroom c
            JOIN FETCH c.academicLevel
            JOIN FETCH c.classroomDesignation
            LEFT JOIN FETCH c.academicSection
            LEFT JOIN FETCH c.academicOption
            JOIN FETCH a.subject
            WHERE a.classroom.id = :classroomId
              AND a.academicYear.id = :academicYearId
            ORDER BY a.createdAt DESC
            """)
    List<TeacherCourseAssignment> findByClassroomAndYear(
            @Param("classroomId") UUID classroomId,
            @Param("academicYearId") UUID academicYearId
    );

    @Query("""
            SELECT a FROM TeacherCourseAssignment a
            JOIN FETCH a.teacher t
            JOIN FETCH t.securityUser
            JOIN FETCH a.school
            JOIN FETCH a.academicYear
            JOIN FETCH a.classroom c
            JOIN FETCH c.academicLevel
            JOIN FETCH c.classroomDesignation
            LEFT JOIN FETCH c.academicSection
            LEFT JOIN FETCH c.academicOption
            JOIN FETCH a.subject
            WHERE a.subject.id = :subjectId
              AND (:schoolId IS NULL OR a.school.id = :schoolId)
              AND (:academicYearId IS NULL OR a.academicYear.id = :academicYearId)
            ORDER BY a.createdAt DESC
            """)
    List<TeacherCourseAssignment> findBySubject(
            @Param("subjectId") UUID subjectId,
            @Param("schoolId") UUID schoolId,
            @Param("academicYearId") UUID academicYearId
    );

    @Query("""
            SELECT a FROM TeacherCourseAssignment a
            JOIN FETCH a.teacher t
            JOIN FETCH t.securityUser
            JOIN FETCH a.school
            JOIN FETCH a.academicYear
            JOIN FETCH a.classroom c
            JOIN FETCH c.academicLevel
            JOIN FETCH c.classroomDesignation
            LEFT JOIN FETCH c.academicSection
            LEFT JOIN FETCH c.academicOption
            JOIN FETCH a.subject
            WHERE a.academicYear.id = :academicYearId
              AND (:schoolId IS NULL OR a.school.id = :schoolId)
            ORDER BY a.createdAt DESC
            """)
    List<TeacherCourseAssignment> findByYearAndSchool(
            @Param("academicYearId") UUID academicYearId,
            @Param("schoolId") UUID schoolId
    );
}
