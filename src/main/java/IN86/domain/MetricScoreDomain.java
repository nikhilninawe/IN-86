package IN86.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MetricScoreDomain {
    String metric;
    double weight;
    double score;
    String env;
    String role;
    String stack;
    String host;
}
