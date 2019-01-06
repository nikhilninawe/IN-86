package IN86.computation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DecisionEngine {

    @Autowired
    DecisionEngineActions decisionEngineActions;
    double threshold = 1;

    public void makeDecision(String instance, double score){
        if (score >  threshold){
            decisionEngineActions.quarantine(instance, score);
        }else if (score > 0 && score < threshold) {
            decisionEngineActions.sendAlert(instance, score);
        }else{
            //do nothing
        }
    }
}
