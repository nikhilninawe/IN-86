package IN86.computation;

import IN86.main.Application;
import net.gpedro.integrations.slack.SlackApi;
import net.gpedro.integrations.slack.SlackMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

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
        ClassLoader classLoader = DecisionEngineActions.class.getClassLoader();
        String fileName = "quarantine.sh";
        File file = new File(classLoader.getResource(fileName).getFile());

        String[] cmd = {file.getAbsolutePath(), instance};
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(cmd);
            Thread.sleep(20*1000);
            logger.info("Quarantined instance " + instance + " successfully.");
            slackApi.call(new SlackMessage("Quarantined instance " + instance + " successfully."));
        } catch (Exception e) {
            logger.error("Error in Quarantining instance " + instance + ".", e);
            slackApi.call(new SlackMessage("Error in Quarantining instance " + instance + "."));
        }
        if(Objects.nonNull(p)) {
            logger.info("Exit value" + p.exitValue());
        }
    }

    public static void main(String[] args){
        DecisionEngineActions actions = new DecisionEngineActions();
        actions.quarantine("I1", 1.2);
    }
}
