package eclosia.eclosia_organization_service.security_module.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
        name = "security_modules",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_security_module_code",
                        columnNames = "code"
                )
        }
)
public class SecurityModule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(length = 100)
    private String icon;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "system_module", nullable = false)
    private Boolean systemModule = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
