package IN86.model;

import lombok.Data;
import org.influxdb.annotation.Measurement;

import java.time.Instant;

@Data
@Measurement(name = "service_score")
public class ServiceScore {
    private Instant time;
    private double score;
    String env;
    String role;
    String stack;
}
