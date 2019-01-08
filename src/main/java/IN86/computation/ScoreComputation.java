package IN86.computation;

import IN86.domain.InstanceScoreDomain;
import IN86.domain.MeasurementType;
import IN86.domain.ServiceScoreDomain;
import IN86.fetchMetrics.MetricDetails;
import IN86.main.AppConfiguration;
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

import java.time.Instant;
import java.util.*;

@Component
public class ScoreComputation {

    @Autowired
    private ApplicationMetricsMetaDataRepo metricsMetaDataRepo;

    @Autowired
    private InfluxDBTemplate<Point> influxDBTemplate;

    private static final Logger log = LoggerFactory.getLogger(ScoreComputation.class);

    String dbName = "telegraf";
    Map<String, Instant> metricLastTimeStampMap = new HashMap<>();

    public MeasurementType getMeasurementName(String metric){
        switch (metric){
            case "gc": return new MeasurementType("jvm_gc_pause", "timing");
            case "cpu": return new MeasurementType("system_cpu_usage", "gauge");
            case "errors": return new MeasurementType("log4j2_events", "counter");
            default: return null;
        }
    }

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

    public String getQuery(String metric, String host, Instant instant){
        switch (metric) {
            case "jvm_gc_pause":
                return "SELECT sum(count) FROM jvm_gc_pause where host = '" + host + "' AND time > '" + instant.minusSeconds(AppConfiguration.time_frame) + "'";
            case "system_cpu_usage":
                return "SELECT mean(value) from system_cpu_usage where host = '" + host + "' AND  time > '" + instant.minusSeconds(AppConfiguration.time_frame) + "'";
            case "log4j2_events":
                return "select sum(value) from log4j2_events where level = 'error' AND host = '" + host + "' AND time > '" + instant.minusSeconds(AppConfiguration.time_frame) + "'";
            default:
                return "";
        }

    }

    public MetricScoreDomain computeMetricScoreInflux(MetricDetails metricDetails) {
        String metric = metricDetails.getMetric();
        String host = metricDetails.getHost();
        Instant currentInstant = Instant.now();
        ApplicationMetricsMetaData metricsMetaData = metricsMetaDataRepo.findApplicationMetricsMetaDataByMetric(metric);
        MeasurementType measurementType = getMeasurementName(metricDetails.getMetric());
        String queryString = getQuery(measurementType.getMeasurment(), host, currentInstant);
        if(Objects.nonNull(metricLastTimeStampMap.get(metric))){
            queryString = queryString + " AND time > '" + metricLastTimeStampMap.get(metric) + "'";
        }
        Query query = new Query(queryString, dbName);
        QueryResult result = influxDBTemplate.query(query);
        double currentValue = 0;
        try {
            if(!CollectionUtils.isEmpty(result.getResults().get(0).getSeries())) {
                currentValue = (double) result.getResults().get(0).getSeries().get(0).getValues().get(0).get(1);
                metricLastTimeStampMap.put(metric, currentInstant);
            }
        }catch (Exception ex){
            log.error("Error while computing metric score for result " + result);
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

    public Map<String, ServiceScoreDomain> computeServiceScore(Map<String, InstanceScoreDomain> instanceScoreMap) {
        Map<String, ServiceScoreDomain> serviceScoreMap = new HashMap<>();
        Map<String, List<InstanceScoreDomain>> serviceInstanceScoreMap = new HashMap<>();

        for (String host: instanceScoreMap.keySet()) {
            InstanceScoreDomain instanceScoreDomain = instanceScoreMap.get(host);
            List<InstanceScoreDomain> instanceScoreDomainList = serviceInstanceScoreMap.get(instanceScoreDomain.getRole());
            if (Objects.isNull(instanceScoreDomainList)) {
                instanceScoreDomainList = new ArrayList<>();
            }

            instanceScoreDomainList.add(instanceScoreDomain);
            serviceInstanceScoreMap.put(instanceScoreDomain.getRole(), instanceScoreDomainList);
        }

        for (String role: serviceInstanceScoreMap.keySet()) {
            List<InstanceScoreDomain> instanceScoreDomainList = serviceInstanceScoreMap.get(role);
            if (CollectionUtils.isEmpty(instanceScoreDomainList)) {
                continue;
            }
            double serviceScore = 0;
            for (InstanceScoreDomain instanceScoreDomain: instanceScoreDomainList) {
                serviceScore += instanceScoreDomain.getScore();
            }
            serviceScore = serviceScore/instanceScoreDomainList.size();
            InstanceScoreDomain baseInstanceScoreDomain = instanceScoreDomainList.get(0);
            serviceScoreMap.put(role, new ServiceScoreDomain(serviceScore, baseInstanceScoreDomain.getEnv(),
                    baseInstanceScoreDomain.getRole(), baseInstanceScoreDomain.getStack()));
        }
        return serviceScoreMap;
    }

}
