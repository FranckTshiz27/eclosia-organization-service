package eclosia.eclosia_organization_service.student_category.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import eclosia.eclosia_organization_service.school.entity.School;
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

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(
        name = "student_categories",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_school_student_category_code",
                        columnNames = {"school_id", "code"}
                ),
                @UniqueConstraint(
                        name = "uk_school_student_category_name",
                        columnNames = {"school_id", "name"}
                )
        }
)
public class StudentCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Code unique dans une ecole.
     * Exemple :
     * NEW
     * OLD
     * SCHOLARSHIP
     */
    @Column(nullable = false, length = 30)
    private String code;

    /**
     * Libelle.
     * Exemple :
     * Nouvel eleve
     * Ancien eleve
     * Boursier
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Description.
     */
    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Boolean active = true;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("schoolId")
    public UUID getSchoolId() {
        return school != null ? school.getId() : null;
    }
}
