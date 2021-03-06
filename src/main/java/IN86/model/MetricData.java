package IN86.model;

import lombok.Data;
import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;

import java.time.Instant;

@Data
@Measurement(name = "metric_data")
public class MetricData {

    @Column(name = "time")
    private Instant time;

    @Column(name = "metric")
    private String metric;

    @Column(name = "value")
    private double value;

    String env;
    String role;
    String stack;
    String host;

}
