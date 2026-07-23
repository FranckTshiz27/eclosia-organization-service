package eclosia.eclosia_organization_service.role_feature.repository;

import eclosia.eclosia_organization_service.role_feature.entity.RoleFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RoleFeatureRepository extends JpaRepository<RoleFeature, UUID> {

    List<RoleFeature> findAllByOrderByCreatedAtAsc();

    List<RoleFeature> findByRole_IdOrderByCreatedAtAsc(UUID roleId);

    List<RoleFeature> findByFeature_IdOrderByCreatedAtAsc(UUID featureId);

    boolean existsByRole_IdAndFeature_Id(UUID roleId, UUID featureId);

    boolean existsByRole_IdAndFeature_IdAndIdNot(UUID roleId, UUID featureId, UUID id);

    @Query("""
            SELECT DISTINCT rf FROM RoleFeature rf
            JOIN FETCH rf.feature f
            JOIN FETCH f.module
            WHERE rf.role.id IN :roleIds
              AND rf.active = true
              AND f.active = true
            """)
    List<RoleFeature> findActivePermissionsByRoleIds(@Param("roleIds") Collection<UUID> roleIds);
}
