package IN86.computation;

import IN86.main.Application;
import net.gpedro.integrations.slack.SlackApi;
import net.gpedro.integrations.slack.SlackMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class DecisionEngineActions {

    @Autowired
    SlackApi slackApi;

    private static final Logger logger = LoggerFactory.getLogger(DecisionEngineActions.class);


    public void sendAlert(String instance, double score){
        slackApi.call(new SlackMessage("Score for instance " + instance + " is " + score));
    }

    public void quarantine(String instance, double score){
        slackApi.call(new SlackMessage("Quarantining instance " + instance + ". Its score is " + score));

        String[] cmd = {"resources/quarantine.sh", instance};
        try {
            Process p = Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            logger.error("Error in Quarantining instance " + instance + ".", e);
            slackApi.call(new SlackMessage("Error in Quarantining instance " + instance + "."));git add
        }
    }
}
