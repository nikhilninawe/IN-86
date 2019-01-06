package IN86.model;

import lombok.Data;
import org.influxdb.annotation.Measurement;

import java.time.Instant;

@Data
@Measurement(name = "instance_score")
public class InstanceScore {
    private Instant time;
    private double score;
    String env;
    String role;
    String stack;
    String host;
}
