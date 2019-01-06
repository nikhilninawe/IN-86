package IN86.computation;

import IN86.fetchMetrics.MetricDetails;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ScoreComputation {

    @Autowired
    private ApplicationMetricsMetaDataRepo metricsMetaDataRepo;

    @Autowired
    private InfluxDBTemplate<Point> influxDBTemplate;

    private static final Logger log = LoggerFactory.getLogger(ScoreComputation.class);

    String dbName = "telegraf";

    public MetricScoreDomain computeMetricScore(String metric, String host) {
        ApplicationMetricsMetaData metricsMetaData = metricsMetaDataRepo.findApplicationMetricsMetaDataByMetric(metric);
        Query query = new Query("SELECT * FROM metric_data where host = '" + host + "' order by time desc limit 2", dbName);
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

    public Map<String, Double> computeInstanceScore(Map<String, List<MetricScoreDomain>> hostMetricScoreDomainMap) {
        Map<String, Double> instanceScoreMap = new HashMap<>();
        for (String host : hostMetricScoreDomainMap.keySet()) {
            double instanceScore = 0;
            double weightSum = 0;
            List<MetricScoreDomain> metricScoreDomains = hostMetricScoreDomainMap.get(host);
            for (MetricScoreDomain metricScoreDomain : metricScoreDomains) {
                instanceScore += metricScoreDomain.getScore() * metricScoreDomain.getWeight();
                weightSum += metricScoreDomain.getWeight();
            }
            instanceScore = instanceScore / weightSum;
            instanceScoreMap.put(host, instanceScore);

        }
        return instanceScoreMap;
    }

    public double computeServiceScore() {

        return 0;
    }

}
