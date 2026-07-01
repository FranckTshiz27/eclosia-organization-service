package eclosia.eclosia_organization_service.enrollment.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import eclosia.eclosia_organization_service.academic_year.entity.AcademicYear;
import eclosia.eclosia_organization_service.classroom.entity.Classroom;
import eclosia.eclosia_organization_service.file.entity.FileResource;
import eclosia.eclosia_organization_service.guardian.entity.Guardian;
import eclosia.eclosia_organization_service.student.entity.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(
        name = "enrollments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_student_academic_year",
                        columnNames = {
                                "student_id",
                                "academic_year_id"
                        }
                ),
                @UniqueConstraint(
                        name = "uk_enrollment_number",
                        columnNames = "enrollment_number"
                )
        }
)
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Genere automatiquement par l'API.
     */
    @Column(name = "enrollment_number", nullable = false, unique = true, length = 50)
    private String enrollmentNumber;

    /**
     * Date de l'inscription.
     */
    @Column(name = "enrollment_date", nullable = false)
    private LocalDate enrollmentDate;

    /**
     * ACTIVE
     * INACTIVE
     * SUSPENDED
     * TRANSFERRED
     * DROPPED
     * GRADUATED
     * CANCELLED
     */
    @Column(nullable = false, length = 30)
    private String status = "ACTIVE";

    /**
     * Date du changement de statut.
     */
    @Column(name = "status_date")
    private LocalDate statusDate;

    /**
     * Motif du changement de statut.
     */
    @Column(name = "status_reason", length = 500)
    private String statusReason;

    /**
     * Observations.
     */
    @Column(length = 500)
    private String comment;

    /**
     * Eleve concerne.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /**
     * Responsable de l'eleve.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guardian_id", nullable = false)
    private Guardian guardian;

    /**
     * Classe de l'annee.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    /**
     * Annee scolaire.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    /**
     * Photo officielle utilisee pour les documents de cette annee scolaire.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photo_id")
    private FileResource photo;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("studentId")
    public UUID getStudentId() {
        return student != null ? student.getId() : null;
    }

    @JsonProperty("guardianId")
    public UUID getGuardianId() {
        return guardian != null ? guardian.getId() : null;
    }

    @JsonProperty("classroomId")
    public UUID getClassroomId() {
        return classroom != null ? classroom.getId() : null;
    }

    @JsonProperty("academicYearId")
    public UUID getAcademicYearId() {
        return academicYear != null ? academicYear.getId() : null;
    }

    @JsonProperty("photoId")
    public UUID getPhotoId() {
        return photo != null ? photo.getId() : null;
    }
}
