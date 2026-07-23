package eclosia.eclosia_organization_service.feature.repository;

import eclosia.eclosia_organization_service.feature.entity.Feature;
import eclosia.eclosia_organization_service.feature.enums.FeatureAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FeatureRepository extends JpaRepository<Feature, UUID> {

    List<Feature> findAllByOrderByDisplayOrderAscActionAsc();

    List<Feature> findByModule_IdOrderByDisplayOrderAscActionAsc(UUID moduleId);

    boolean existsByModule_IdAndAction(UUID moduleId, FeatureAction action);

    boolean existsByModule_IdAndActionAndIdNot(UUID moduleId, FeatureAction action, UUID id);
}
