package IN86.computation;

import IN86.domain.InstanceHostMapping;
import IN86.domain.MetricScoreDomain;
import IN86.main.Application;
import IN86.repository.InstanceHostMappingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DecisionEngine {

    @Autowired
    DecisionEngineActions decisionEngineActions;
    @Autowired
    InstanceHostMappingRepository instanceHostMappingRepository;
    double threshold = 1;

    private static final Logger log = LoggerFactory.getLogger(DecisionEngine.class);

    public void makeDecision(String ipAddress, double score, List<MetricScoreDomain> metricScoreDomains){
        InstanceHostMapping instanceHostMapping = instanceHostMappingRepository.findByIp(ipAddress);
        if (score >  threshold){
            decisionEngineActions.quarantine(instanceHostMapping.getInstanceId(), score, metricScoreDomains);
            instanceHostMapping.setQurantined(true);
            instanceHostMappingRepository.save(instanceHostMapping);
        }else if (score > 0 && score < threshold) {
            decisionEngineActions.sendAlert(instanceHostMapping.getInstanceId(), score, metricScoreDomains);
        }else{
            log.info("Not taking any action for instance " + ipAddress + " with score " + score);
        }
    }
}
