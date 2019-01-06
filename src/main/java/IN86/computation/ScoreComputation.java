package IN86.computation;

import IN86.domain.InstanceScoreDomain;
import IN86.fetchMetrics.MetricDetails;
import IN86.model.InstanceScore;
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
import org.springframework.util.CollectionUtils;

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

    public MetricScoreDomain computeMetricScore(MetricDetails metricDetails) {
        String metric = metricDetails.getMetric();
        String host = metricDetails.getHost();
        ApplicationMetricsMetaData metricsMetaData = metricsMetaDataRepo.findApplicationMetricsMetaDataByMetric(metric);
        Query query = new Query("SELECT * FROM metric_data where metric = '" + metric + "' AND host = '" + host + "' order by time desc limit 2", dbName);
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
        return new MetricScoreDomain(metric, metricsMetaData.getWeight(), score, metricDetails.getEnv(),
                metricDetails.getRole(), metricDetails.getStack(), metricDetails.getHost());
    }

    public Map<String, InstanceScoreDomain> computeInstanceScore(Map<String, List<MetricScoreDomain>> hostMetricScoreDomainMap) {
        Map<String, InstanceScoreDomain> instanceScoreMap = new HashMap<>();
        for (String host : hostMetricScoreDomainMap.keySet()) {
            double instanceScore = 0;
            double weightSum = 0;
            List<MetricScoreDomain> metricScoreDomains = hostMetricScoreDomainMap.get(host);
            if(CollectionUtils.isEmpty(metricScoreDomains)){
                continue;
            }
            for (MetricScoreDomain metricScoreDomain : metricScoreDomains) {
                instanceScore += metricScoreDomain.getScore() * metricScoreDomain.getWeight();
                weightSum += metricScoreDomain.getWeight();
            }
            instanceScore = instanceScore / weightSum;
            MetricScoreDomain baseMetricScoreDomain = metricScoreDomains.get(0);
            instanceScoreMap.put(host, new InstanceScoreDomain(instanceScore, baseMetricScoreDomain.getEnv(),
                    baseMetricScoreDomain.getRole(), baseMetricScoreDomain.getStack(), baseMetricScoreDomain.getHost()));

        }
        return instanceScoreMap;
    }

    public double computeServiceScore() {

        return 0;
    }

}
