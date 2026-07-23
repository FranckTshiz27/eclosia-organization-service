package eclosia.eclosia_organization_service.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import eclosia.eclosia_organization_service.common.enums.Gender;
import eclosia.eclosia_organization_service.group.entity.Group;
import eclosia.eclosia_organization_service.role.entity.Role;
import eclosia.eclosia_organization_service.school.entity.School;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(
        name = "security_user",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_security_user_keycloak_id", columnNames = "keycloak_id"),
                @UniqueConstraint(name = "uk_security_user_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_security_user_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_security_user_phone", columnNames = "phone")
        }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "keycloak_id", nullable = false, updatable = false)
    private UUID keycloakId;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(length = 255)
    private String email;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Column(length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(length = 500)
    private String address;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "group_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_security_user_group")
    )
    private Group group;

    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "security_user_role",
            joinColumns = @JoinColumn(
                    name = "user_id",
                    foreignKey = @ForeignKey(name = "fk_security_user_role_user")
            ),
            inverseJoinColumns = @JoinColumn(
                    name = "role_id",
                    foreignKey = @ForeignKey(name = "fk_security_user_role_role")
            )
    )
    private Set<Role> roles = new HashSet<>();

    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "security_user_school",
            joinColumns = @JoinColumn(
                    name = "user_id",
                    foreignKey = @ForeignKey(name = "fk_security_user_school_user")
            ),
            inverseJoinColumns = @JoinColumn(
                    name = "school_id",
                    foreignKey = @ForeignKey(name = "fk_security_user_school_school")
            )
    )
    private Set<School> schools = new HashSet<>();

    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("groupId")
    public UUID groupId() {
        return group != null ? group.getId() : null;
    }

    @JsonProperty("roleIds")
    public Set<UUID> roleIds() {
        return roles == null
                ? Set.of()
                : roles.stream().map(Role::getId).collect(Collectors.toSet());
    }

    @JsonProperty("schoolIds")
    public Set<UUID> schoolIds() {
        return schools == null
                ? Set.of()
                : schools.stream().map(School::getId).collect(Collectors.toSet());
    }
}
