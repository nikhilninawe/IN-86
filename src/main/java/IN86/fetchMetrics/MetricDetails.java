package IN86.fetchMetrics;

import lombok.Data;

@Data
public class MetricDetails {
    String metric;
    double value;
    String env;
    String role;
    String stack;
    String host;
}
