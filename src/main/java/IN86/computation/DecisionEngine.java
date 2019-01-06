package IN86.computation;

import IN86.main.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DecisionEngine {

    @Autowired
    DecisionEngineActions decisionEngineActions;
    double threshold = 1;

    private static final Logger log = LoggerFactory.getLogger(DecisionEngine.class);

    public void makeDecision(String instance, double score){
        if (score >  threshold){
            decisionEngineActions.quarantine(instance, score);
        }else if (score > 0 && score < threshold) {
            decisionEngineActions.sendAlert(instance, score);
        }else{
            log.info("Not taking any action for instance " + instance + " with score " + score);
        }
    }
}
