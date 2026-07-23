package eclosia.eclosia_organization_service.teacher_class_assignment.repository;

import eclosia.eclosia_organization_service.teacher_class_assignment.entity.TeacherClassAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeacherClassAssignmentRepository extends JpaRepository<TeacherClassAssignment, UUID> {

    boolean existsByClassroom_IdAndAcademicYear_IdAndActiveTrue(UUID classroomId, UUID academicYearId);

    boolean existsByClassroom_IdAndAcademicYear_IdAndActiveTrueAndIdNot(
            UUID classroomId,
            UUID academicYearId,
            UUID id
    );

    @Query("""
            SELECT a FROM TeacherClassAssignment a
            JOIN FETCH a.teacher t
            JOIN FETCH t.securityUser
            JOIN FETCH a.school
            JOIN FETCH a.academicYear
            JOIN FETCH a.classroom c
            JOIN FETCH c.academicLevel
            JOIN FETCH c.classroomDesignation
            LEFT JOIN FETCH c.academicSection
            LEFT JOIN FETCH c.academicOption
            WHERE a.id = :id
            """)
    Optional<TeacherClassAssignment> findDetailedById(@Param("id") UUID id);

    @Query("""
            SELECT a FROM TeacherClassAssignment a
            JOIN FETCH a.teacher t
            JOIN FETCH t.securityUser
            JOIN FETCH a.school
            JOIN FETCH a.academicYear
            JOIN FETCH a.classroom c
            JOIN FETCH c.academicLevel
            JOIN FETCH c.classroomDesignation
            LEFT JOIN FETCH c.academicSection
            LEFT JOIN FETCH c.academicOption
            WHERE a.school.id = :schoolId
              AND a.academicYear.id = :academicYearId
            ORDER BY a.createdAt DESC
            """)
    List<TeacherClassAssignment> findBySchoolAndYear(
            @Param("schoolId") UUID schoolId,
            @Param("academicYearId") UUID academicYearId
    );

    @Query("""
            SELECT a FROM TeacherClassAssignment a
            JOIN FETCH a.teacher t
            JOIN FETCH t.securityUser
            JOIN FETCH a.school
            JOIN FETCH a.academicYear
            JOIN FETCH a.classroom c
            JOIN FETCH c.academicLevel
            JOIN FETCH c.classroomDesignation
            LEFT JOIN FETCH c.academicSection
            LEFT JOIN FETCH c.academicOption
            WHERE a.classroom.id = :classroomId
              AND a.academicYear.id = :academicYearId
              AND a.active = TRUE
            """)
    Optional<TeacherClassAssignment> findActiveByClassroomAndYear(
            @Param("classroomId") UUID classroomId,
            @Param("academicYearId") UUID academicYearId
    );

    @Query("""
            SELECT a FROM TeacherClassAssignment a
            JOIN FETCH a.teacher t
            JOIN FETCH t.securityUser
            JOIN FETCH a.school
            JOIN FETCH a.academicYear
            JOIN FETCH a.classroom c
            JOIN FETCH c.academicLevel
            JOIN FETCH c.classroomDesignation
            LEFT JOIN FETCH c.academicSection
            LEFT JOIN FETCH c.academicOption
            WHERE a.teacher.id = :teacherId
              AND (:academicYearId IS NULL OR a.academicYear.id = :academicYearId)
            ORDER BY a.createdAt DESC
            """)
    List<TeacherClassAssignment> findByTeacher(
            @Param("teacherId") UUID teacherId,
            @Param("academicYearId") UUID academicYearId
    );
}
