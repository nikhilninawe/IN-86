package IN86.computation;

import IN86.domain.MetricScoreDomain;
import IN86.main.Application;
import net.gpedro.integrations.slack.SlackApi;
import net.gpedro.integrations.slack.SlackAttachment;
import net.gpedro.integrations.slack.SlackField;
import net.gpedro.integrations.slack.SlackMessage;
import org.apache.commons.math3.util.Precision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static IN86.computation.DecisionEngine.METRIC_SCORE_THRESHOLD;

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
        SlackMessage slackMessage = new SlackMessage(
                String.format("Health Engine Score for union-charlie-orders-%s: %s", instance, score));

        List<SlackAttachment> slackAttachments = new ArrayList<>();

        for (MetricScoreDomain metricScoreDomain: metricScoreDomains) {
            double metricScore = metricScoreDomain.getScore();
            SlackAttachment slackAttachment = new SlackAttachment();
            slackAttachment.setTitle(
                    String.format("%s score: %s", metricScoreDomain.getMetric(),
                            Precision.round(metricScoreDomain.getScore(), 2)));

            if (metricScore > METRIC_SCORE_THRESHOLD) {
                slackAttachment.setColor("danger");
            } else if (0 < metricScore && metricScore < METRIC_SCORE_THRESHOLD) {
                slackAttachment.setColor("warning");
            } else {
                slackAttachment.setColor("good");
            }

            slackAttachments.add(slackAttachment);
        }
        slackMessage.setAttachments(slackAttachments);
        slackApi.call(slackMessage);
    }

    public void sendAlertWithPeakCount(String instance, int peakCount){
        slackApi.call(new SlackMessage(String.format("Number of continuous peaks for %s: %s", instance, peakCount)));
    }

    @Async
    public void quarantine(String instance){
        slackApi.call(new SlackMessage(String.format("Quarantining union-charlie-orders-%s", instance)));
        ClassLoader classLoader = DecisionEngineActions.class.getClassLoader();
        String fileName = "quarantine.sh";
        File file = new File(classLoader.getResource(fileName).getFile());

        String[] cmd = {file.getAbsolutePath(), instance};
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(cmd);
            Thread.sleep(20*1000);
            logger.info("Quarantined instance " + instance + " successfully.");
            slackApi.call(new SlackMessage(String.format("Quarantined union-charlie-orders-%s successfully", instance)));
        } catch (Exception e) {
            logger.error("Error in Quarantining instance " + instance + ".", e);
            slackApi.call(new SlackMessage(String.format("Error in quarantining union-charlie-orders-%s", instance)));
        }
    }
}
