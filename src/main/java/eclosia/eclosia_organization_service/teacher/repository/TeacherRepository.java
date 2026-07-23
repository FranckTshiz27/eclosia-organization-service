package eclosia.eclosia_organization_service.teacher.repository;

import eclosia.eclosia_organization_service.teacher.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeacherRepository extends JpaRepository<Teacher, UUID> {

    boolean existsByRegistrationNumber(String registrationNumber);

    boolean existsByRegistrationNumberAndIdNot(String registrationNumber, UUID id);

    boolean existsBySecurityUser_Id(UUID securityUserId);

    Optional<Teacher> findBySecurityUser_IdAndActiveTrue(UUID securityUserId);

    Optional<Teacher> findBySecurityUser_Id(UUID securityUserId);

    List<Teacher> findAllByOrderByCreatedAtDesc();

    List<Teacher> findBySchool_IdOrderByCreatedAtDesc(UUID schoolId);

    @Query("""
            SELECT t FROM Teacher t
            JOIN FETCH t.securityUser u
            JOIN FETCH t.school s
            LEFT JOIN FETCH u.roles
            WHERE t.id = :id
            """)
    Optional<Teacher> findDetailedById(@Param("id") UUID id);

    @Query("""
            SELECT DISTINCT t FROM Teacher t
            JOIN FETCH t.securityUser u
            JOIN FETCH t.school s
            LEFT JOIN FETCH u.roles
            ORDER BY t.createdAt DESC
            """)
    List<Teacher> findAllDetailed();

    @Query("""
            SELECT DISTINCT t FROM Teacher t
            JOIN FETCH t.securityUser u
            JOIN FETCH t.school s
            LEFT JOIN FETCH u.roles
            WHERE s.id = :schoolId
            ORDER BY t.createdAt DESC
            """)
    List<Teacher> findDetailedBySchoolId(@Param("schoolId") UUID schoolId);
}
