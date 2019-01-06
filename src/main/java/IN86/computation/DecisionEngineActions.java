package IN86.computation;

import net.gpedro.integrations.slack.SlackApi;
import net.gpedro.integrations.slack.SlackMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DecisionEngineActions {

    @Autowired
    SlackApi slackApi;

    public void sendAlert(String instance, double score){
        slackApi.call(new SlackMessage("Score for instance " + instance + " is " + score));
    }

    public void quarantine(String instance, double score){
        slackApi.call(new SlackMessage("Please quarantine instance " + instance + ". Its score is " + score));
    }
}
