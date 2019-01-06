package IN86.model;

import lombok.Data;
import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;

import java.time.Instant;

@Data
@Measurement(name = "metric_score")
public class MetricScore {
    private Instant time;
    private String metric;
    private double score;
    String env;
    String role;
    String stack;
    String host;
}
