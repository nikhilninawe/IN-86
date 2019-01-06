package IN86.service;

import IN86.computation.DecisionEngine;
import IN86.computation.ScoreComputation;
import IN86.domain.MetricScoreDomain;
import IN86.model.MetricScore;
import org.influxdb.dto.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.influxdb.InfluxDBTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/test")
public class TestService {

    @Autowired
    private ScoreComputation scoreComputation;

    @Autowired
    private InfluxDBTemplate<Point> influxDBTemplate;

    @Autowired
    private DecisionEngine decisionEngine;

    @RequestMapping("/data")
    public void populateTestData(){
        for(int i=0; i < 100; i++ ) {
//            Point point1 = Point.measurement("metric_data")
//                    .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
//                    .addField("value", Math.random() * 100)
//                    .tag("metric", "cpu")
//                    .tag("env", "rehearsal")
//                    .tag("role", "orders")
//                    .tag("stack", "india")
//                    .tag("host", "I1")
//                    .build();
//            influxDBTemplate.write(point1);

            Point point1 = Point.measurement("service_score")
                    .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                    .addField("score", Math.random())
//                    .tag("metric", "cpu")
                    .tag("env", "rehearsal")
                    .tag("role", "orders")
                    .tag("stack", "india")
//                    .tag("host", "I1")
                    .build();
            influxDBTemplate.write(point1);
        }
    }

    @RequestMapping("/score")
    public MetricScoreDomain getScore(){
        MetricScoreDomain metricScoreDomain = scoreComputation.computeMetricScore("cpu");
        double instanceScore = scoreComputation.computeInstanceScore("I1", Arrays.asList(metricScoreDomain));
        System.out.println(instanceScore);
        decisionEngine.makeDecision("I1", instanceScore);
        return metricScoreDomain;
    }
}
