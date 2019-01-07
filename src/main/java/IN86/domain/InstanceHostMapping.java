package IN86.domain;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity(name = "instance_host_mapping")
@Data
public class InstanceHostMapping {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    Long id;

    String instanceId;
    String ip;
    boolean qurantined;
}
