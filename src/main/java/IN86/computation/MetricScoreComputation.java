package IN86.computation;

import IN86.domain.MetricData;
import IN86.main.Application;
import IN86.model.ApplicationMetricsMetaData;
import IN86.repository.ApplicationMetricsMetaDataRepo;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.influxdb.impl.InfluxDBResultMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.influxdb.InfluxDBTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

@Component
public class MetricScoreComputation {

    @Autowired
    private ApplicationMetricsMetaDataRepo metricsMetaDataRepo;

    @Autowired
    private InfluxDBTemplate<Point> influxDBTemplate;

    private static final Logger log = LoggerFactory.getLogger(MetricScoreComputation.class);

    String dbName = "telegraf";

    public double computeMetricScore(String metric){
        ApplicationMetricsMetaData metricsMetaData = metricsMetaDataRepo.findApplicationMetricsMetaDataRepoByMetric(metric);
        Query query = new Query("SELECT * FROM metric_data order by time desc limit 2", dbName);
        QueryResult result = influxDBTemplate.query(query);
        InfluxDBResultMapper resultMapper = new InfluxDBResultMapper();
        List<MetricData> data = resultMapper.toPOJO(result, MetricData.class);
        double currentValue = data.get(0).getValue();
        return (currentValue - metricsMetaData.getGoodValue())/(metricsMetaData.getCriticalValue() - metricsMetaData.getGoodValue());
    }
}
