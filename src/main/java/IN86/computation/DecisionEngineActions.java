package IN86.computation;

import IN86.domain.MetricScoreDomain;
import IN86.main.Application;
import net.gpedro.integrations.slack.SlackApi;
import net.gpedro.integrations.slack.SlackMessage;
import org.apache.commons.math3.util.Precision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class DecisionEngineActions {

    @Autowired
    SlackApi slackApi;

    private static final Logger logger = LoggerFactory.getLogger(DecisionEngineActions.class);

    private Map<String, Double> getMetricScoreMap(List<MetricScoreDomain> metricScoreDomains){
        Map<String, Double> map = new HashMap<>();
        for (MetricScoreDomain metric : metricScoreDomains ){
            map.put(metric.getMetric(), Precision.round(metric.getScore(), 2));
        }
        return map;
    }

    public void sendAlert(String instance, double score, List<MetricScoreDomain> metricScoreDomains){
        slackApi.call(new SlackMessage("Score for instance " + instance + " is " + score +
                ".\nIndividual metric scores are :\n" + getMetricScoreMap(metricScoreDomains) ));
    }

    public void sendAlertWithPeakCount(String instance, int peakCount){
        slackApi.call(new SlackMessage(String.format("Number of continuous peaks for %s: %s", instance, peakCount)));
    }

    public void quarantine(String instance, double score, List<MetricScoreDomain> metricScoreDomains){
        slackApi.call(new SlackMessage("Quarantining instance " + instance + ". Its score is " + score  +
                ".\nIndividual metric scores are :- \n" + getMetricScoreMap(metricScoreDomains)));
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
}
