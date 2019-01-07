package IN86.repository;

import IN86.domain.InstanceHostMapping;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface InstanceHostMappingRepository extends CrudRepository<InstanceHostMapping, Long> {
    List<InstanceHostMapping> findByQurantinedFalse();
    InstanceHostMapping findByIp(String ip);
}
