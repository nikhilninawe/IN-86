package IN86.repository;

import IN86.model.ApplicationMetricsMetaData;
import org.springframework.data.repository.CrudRepository;

public interface ApplicationMetricsMetaDataRepo extends CrudRepository<ApplicationMetricsMetaData, Long> {
    ApplicationMetricsMetaData findApplicationMetricsMetaDataRepoByMetric(String metric);
}
