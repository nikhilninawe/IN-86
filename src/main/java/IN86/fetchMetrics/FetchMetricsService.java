package IN86.fetchMetrics;

import IN86.computation.DecisionEngine;
import IN86.computation.ScoreComputation;
import IN86.domain.InstanceScoreDomain;
import IN86.domain.MetricScoreDomain;
import IN86.domain.ServiceScoreDomain;
import IN86.main.Application;
import org.influxdb.dto.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.influxdb.InfluxDBTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class FetchMetricsService {

    @Autowired
    private ScoreComputation scoreComputation;

    @Autowired
    private InfluxDBTemplate<Point> influxDBTemplate;

    @Autowired
    private DecisionEngine decisionEngine;

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    private Map<String, List<MetricScoreDomain>> hostMetricScoreMap;
    private Map<String, InstanceScoreDomain> instanceScoreMap;
    private Map<String, ServiceScoreDomain> serviceScoreMap;

    @Scheduled(fixedRate = 1000*5)
    public void populateMetricDetailsToInflux(){
        logger.info("Inside populateMetricDetailsToInflux method");

        hostMetricScoreMap = new HashMap<>();
        instanceScoreMap = new HashMap<>();
        serviceScoreMap = new HashMap<>();

        List<MetricDetails> cpuMetricDetails = fetchMetricValueByName.fetchCPUUsageMetric();
        writeMetricDetailsToInflux(cpuMetricDetails);

        List<MetricDetails> logErrorMetricDetails = fetchMetricValueByName.fetchErrorLogMetric();
        writeMetricDetailsToInflux(logErrorMetricDetails);

        List<MetricDetails> gcMetricDetails = fetchMetricValueByName.fetchMajorGCMetric();
        writeMetricDetailsToInflux(gcMetricDetails);

        writeInstanceScoreToInflux();
        writeServiceScoreToInflux();

        logger.info("End of populateMetricDetailsToInflux method");
    }

    private void writeMetricDetailsToInflux(List<MetricDetails> metricDetailsList) {
        for (MetricDetails metricDetails: metricDetailsList) {
            Point metricPoint = Point.measurement("metric_data")
                    .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                    .addField("value", metricDetails.getValue())
                    .tag("metric", metricDetails.getMetric())
                    .tag("env", metricDetails.getEnv())
                    .tag("role", metricDetails.getRole())
                    .tag("stack", metricDetails.getStack())
                    .tag("host", metricDetails.getHost())
                    .build();
            influxDBTemplate.write(metricPoint);

            writeMetricScoreToInflux(metricDetails);

        }
    }

    private void writeMetricScoreToInflux(MetricDetails metricDetails) {
        MetricScoreDomain metricScoreDomain = scoreComputation.computeMetricScore(metricDetails);

        Point metricPoint = Point.measurement("metric_score")
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .addField("score", metricScoreDomain.getScore())
                .tag("metric", metricScoreDomain.getMetric())
                .tag("env", metricScoreDomain.getEnv())
                .tag("role", metricScoreDomain.getRole())
                .tag("stack", metricScoreDomain.getStack())
                .tag("host", metricScoreDomain.getHost())
                .build();
        influxDBTemplate.write(metricPoint);

        List<MetricScoreDomain> metricScoreDomainList = hostMetricScoreMap.get(metricDetails.getHost());
        if (Objects.isNull(metricScoreDomainList)) {
            metricScoreDomainList = new ArrayList<>();
        }

        metricScoreDomainList.add(metricScoreDomain);
        hostMetricScoreMap.put(metricDetails.getHost(), metricScoreDomainList);
    }

    private void writeInstanceScoreToInflux() {
        instanceScoreMap = scoreComputation.computeInstanceScore(hostMetricScoreMap);
        for(String host : instanceScoreMap.keySet() ) {
            InstanceScoreDomain instanceScoreDetails = instanceScoreMap.get(host);
            Point metricPoint = Point.measurement("instance_score")
                    .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                    .addField("score", instanceScoreDetails.getScore())
                    .tag("env", instanceScoreDetails.getEnv())
                    .tag("role", instanceScoreDetails.getRole())
                    .tag("stack", instanceScoreDetails.getStack())
                    .tag("host", host)
                    .build();
            influxDBTemplate.write(metricPoint);
            decisionEngine.makeDecision(host, instanceScoreDetails.getScore());
        }
    }

    private void writeServiceScoreToInflux() {
        serviceScoreMap = scoreComputation.computeServiceScore(instanceScoreMap);
        for(String role : serviceScoreMap.keySet() ) {
            ServiceScoreDomain serviceScoreDetails = serviceScoreMap.get(role);
            Point metricPoint = Point.measurement("service_score")
                    .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                    .addField("score", serviceScoreDetails.getScore())
                    .tag("env", serviceScoreDetails.getEnv())
                    .tag("role", serviceScoreDetails.getRole())
                    .tag("stack", serviceScoreDetails.getStack())
                    .build();
            influxDBTemplate.write(metricPoint);
        }

    }
}
