package eclosia.eclosia_organization_service.state.repository;

import eclosia.eclosia_organization_service.state.entity.State;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StateRepository extends JpaRepository<State, Long> {
}
