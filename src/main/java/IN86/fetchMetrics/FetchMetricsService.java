package IN86.fetchMetrics;

import IN86.computation.ScoreComputation;
import IN86.domain.MetricScoreDomain;
import IN86.main.Application;
import org.influxdb.dto.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.influxdb.InfluxDBTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.*;
import java.util.concurrent.TimeUnit;


public class FetchMetricsService {

    @Autowired
    private ScoreComputation scoreComputation;

    @Autowired
    private InfluxDBTemplate<Point> influxDBTemplate;

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    private Map<String, List<MetricScoreDomain>> hostMetricScoreMap;

    @Scheduled(fixedRate = 1000*60)
    public void populateMetricDetailsToInflux(){
        logger.info("Inside populateMetricDetailsToInflux method");

        hostMetricScoreMap = new HashMap<>();

        List<MetricDetails> cpuMetricDetails = fetchMetricValueByName.fetchCPUUsageMetric();
        writeMetricDetailsToInflux(cpuMetricDetails);

        List<MetricDetails> logErrorMetricDetails = fetchMetricValueByName.fetchErrorLogMetric();
        writeMetricDetailsToInflux(logErrorMetricDetails);

        List<MetricDetails> gcMetricDetails = fetchMetricValueByName.fetchMajorGCMetric();
        writeMetricDetailsToInflux(gcMetricDetails);

        writeInstancecScoreToInflux();
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
        MetricScoreDomain metricScoreDomain = scoreComputation.computeMetricScore(metricDetails.getMetric(),
                metricDetails.getHost());

        Point metricPoint = Point.measurement("metric_score")
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .addField("score", metricScoreDomain.getScore())
                .tag("metric", metricScoreDomain.getMetric())
                .tag("env", metricDetails.getEnv())
                .tag("role", metricDetails.getRole())
                .tag("stack", metricDetails.getStack())
                .tag("host", metricDetails.getHost())
                .build();
        influxDBTemplate.write(metricPoint);

        List<MetricScoreDomain> metricScoreDomainList = hostMetricScoreMap.get(metricDetails.getHost());
        if (Objects.isNull(metricScoreDomainList)) {
            metricScoreDomainList = new ArrayList<>();
        }

        metricScoreDomainList.add(metricScoreDomain);
        hostMetricScoreMap.put(metricDetails.getHost(), metricScoreDomainList);
    }

    private void writeInstancecScoreToInflux() {
        Map<String, Double> instanceScoreMap = scoreComputation.computeInstanceScore(hostMetricScoreMap);
        for(String host : instanceScoreMap.keySet() ) {
            Point metricPoint = Point.measurement("instance_score")
                    .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                    .addField("score", instanceScoreMap.get(host))
                    .tag("env", "rehearsal")
                    .tag("role", "orders")
                    .tag("stack", "india")
                    .tag("host", host)
                    .build();
            influxDBTemplate.write(metricPoint);
        }
    }
}
