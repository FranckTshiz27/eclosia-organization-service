package eclosia.eclosia_organization_service.teacher.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import eclosia.eclosia_organization_service.school.entity.School;
import eclosia.eclosia_organization_service.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Informations professionnelles uniquement.
 * Le profil personnel vit dans {@link User} (security_user).
 */
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(
        name = "teacher",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_teacher_registration_number",
                        columnNames = "registration_number"
                ),
                @UniqueConstraint(
                        name = "uk_teacher_security_user",
                        columnNames = "security_user_id"
                )
        }
)
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "security_user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_teacher_security_user")
    )
    private User securityUser;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "school_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_teacher_school")
    )
    private School school;

    @Column(name = "registration_number", nullable = false, length = 100)
    private String registrationNumber;

    @Column(name = "hiring_date", nullable = false)
    private LocalDate hiringDate;

    @Column(name = "leaving_date")
    private LocalDate leavingDate;

    @Column(length = 255)
    private String qualification;

    @Column(length = 255)
    private String specialty;

    @Column(length = 100)
    private String grade;

    /** Signature manuscrite (data URL / base64 ou URL), utilisée notamment sur les bulletins. */
    @Column(columnDefinition = "TEXT")
    private String signature;

    @Column(nullable = false)
    private Boolean titular = false;

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

    @JsonProperty("securityUserId")
    public UUID getSecurityUserId() {
        return securityUser != null ? securityUser.getId() : null;
    }

    @JsonProperty("schoolId")
    public UUID getSchoolId() {
        return school != null ? school.getId() : null;
    }
}
