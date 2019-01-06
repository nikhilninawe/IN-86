package IN86.computation;

import IN86.model.MetricData;
import IN86.domain.MetricScoreDomain;
import IN86.model.ApplicationMetricsMetaData;
import IN86.repository.ApplicationMetricsMetaDataRepo;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.influxdb.impl.InfluxDBResultMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.influxdb.InfluxDBTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ScoreComputation {

    String role = "orders";
    String env = "rehearsal";
    String stack = "india";
    String host = "I1";

    @Autowired
    private ApplicationMetricsMetaDataRepo metricsMetaDataRepo;

    @Autowired
    private InfluxDBTemplate<Point> influxDBTemplate;

    private static final Logger log = LoggerFactory.getLogger(ScoreComputation.class);

    String dbName = "telegraf";

    public MetricScoreDomain computeMetricScore(String metric) {
        ApplicationMetricsMetaData metricsMetaData = metricsMetaDataRepo.findApplicationMetricsMetaDataByMetric(metric);
        Query query = new Query("SELECT * FROM metric_data order by time desc limit 2", dbName);
        QueryResult result = influxDBTemplate.query(query);
        InfluxDBResultMapper resultMapper = new InfluxDBResultMapper();
        List<MetricData> data = resultMapper.toPOJO(result, MetricData.class);
        double currentValue;
        if (metricsMetaData.isSimple()) {
            currentValue = data.get(0).getValue();
        } else {
            currentValue = data.get(0).getValue() - data.get(1).getValue();
        }
        double score = (currentValue - metricsMetaData.getGoodValue()) / (metricsMetaData.getCriticalValue() - metricsMetaData.getGoodValue());
        return new MetricScoreDomain(metric, metricsMetaData.getWeight(), score);
    }

    public double computeInstanceScore(String instance, List<MetricScoreDomain> metricScoreDomains) {
        double instanceScore = 0;
        double weightSum = 0;
        for (MetricScoreDomain metricScoreDomain : metricScoreDomains) {
            instanceScore += metricScoreDomain.getScore() * metricScoreDomain.getWeight();
            weightSum += metricScoreDomain.getWeight();
        }
        return instanceScore / weightSum;
    }

    public double computeServiceScore() {

        return 0;
    }

}
