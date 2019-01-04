package IN86.service;

import IN86.computation.MetricScoreComputation;
import org.influxdb.dto.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.influxdb.InfluxDBTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/test")
public class TestService {

    @Autowired
    private MetricScoreComputation metricScoreComputation;

    @Autowired
    private InfluxDBTemplate<Point> influxDBTemplate;

    @RequestMapping("/data")
    public void populateTestData(){
        Point point1 = Point.measurement("metric_data")
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .addField("value", 50.02)
                .tag("metric", "cpu")
                .build();
        Point point2 = Point.measurement("metric_data")
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .addField("value", 70.02)
                .tag("metric", "cpu")
                .build();
        influxDBTemplate.write(point1);
        influxDBTemplate.write(point2);
    }

    @RequestMapping("/score")
    public double getScore(){
        return metricScoreComputation.computeMetricScore("cpu");
    }

}
