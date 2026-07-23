package eclosia.eclosia_organization_service.teacher_class_assignment.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import eclosia.eclosia_organization_service.academic_year.entity.AcademicYear;
import eclosia.eclosia_organization_service.classroom.entity.Classroom;
import eclosia.eclosia_organization_service.school.entity.School;
import eclosia.eclosia_organization_service.teacher.entity.Teacher;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "teacher_class_assignment")
public class TeacherClassAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "teacher_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_tca_teacher")
    )
    private Teacher teacher;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "school_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_tca_school")
    )
    private School school;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "academic_year_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_tca_academic_year")
    )
    private AcademicYear academicYear;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "classroom_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_tca_classroom")
    )
    private Classroom classroom;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(length = 1000)
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("teacherId")
    public UUID getTeacherId() {
        return teacher != null ? teacher.getId() : null;
    }

    @JsonProperty("schoolId")
    public UUID getSchoolId() {
        return school != null ? school.getId() : null;
    }

    @JsonProperty("academicYearId")
    public UUID getAcademicYearId() {
        return academicYear != null ? academicYear.getId() : null;
    }

    @JsonProperty("classroomId")
    public UUID getClassroomId() {
        return classroom != null ? classroom.getId() : null;
    }
}
