package IN86.computation;

import IN86.domain.InstanceHostMapping;
import IN86.main.Application;
import IN86.repository.InstanceHostMappingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DecisionEngine {

    @Autowired
    DecisionEngineActions decisionEngineActions;
    @Autowired
    InstanceHostMappingRepository instanceHostMappingRepository;
    double threshold = 1;

    private static final Logger log = LoggerFactory.getLogger(DecisionEngine.class);

    public void makeDecision(String ipAddress, double score){
        InstanceHostMapping instanceHostMapping = instanceHostMappingRepository.findByIp(ipAddress);
        if (score >  threshold){
            decisionEngineActions.quarantine(instanceHostMapping.getInstanceId(), score);
            instanceHostMapping.setQurantined(true);
            instanceHostMappingRepository.save(instanceHostMapping);
        }else if (score > 0 && score < threshold) {
            decisionEngineActions.sendAlert(instanceHostMapping.getInstanceId(), score);
        }else{
            log.info("Not taking any action for instance " + ipAddress + " with score " + score);
        }
    }
}
