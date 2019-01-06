package IN86.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MetricScore {
    String metric;
    double weight;
    double score;
}
