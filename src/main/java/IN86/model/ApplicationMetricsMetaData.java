package IN86.model;

import lombok.Data;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity(name = "application_metrics_metadata")
@Data
public class ApplicationMetricsMetaData {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;
    private String component;
    private String metric;
    private long goodValue;
    private long criticalValue;
    private double weight;
    private boolean simple; // cpu is linear, but gc is not
}
