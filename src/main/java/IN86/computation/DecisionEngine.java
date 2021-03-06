package IN86.computation;

import IN86.domain.InstanceHostMapping;
import IN86.domain.MetricScoreDomain;
import IN86.repository.InstanceHostMappingRepository;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class DecisionEngine {

    @Autowired
    DecisionEngineActions decisionEngineActions;
    @Autowired
    InstanceHostMappingRepository instanceHostMappingRepository;
    double threshold = 1;
    private final int MAX_NO_OF_CONTINUOUS_PEAKS = 2;
    static final double METRIC_SCORE_THRESHOLD = 1;

    private Map<String, CircularFifoQueue<Integer>> instanceContinuousAlertMapping = new HashMap<>();

    private static final Logger log = LoggerFactory.getLogger(DecisionEngine.class);

    public void makeDecision(String ipAddress, double score, List<MetricScoreDomain> metricScoreDomains){
        InstanceHostMapping instanceHostMapping = instanceHostMappingRepository.findByIp(ipAddress);

        CircularFifoQueue<Integer> instanceCircularFifoQueue = instanceContinuousAlertMapping.get(ipAddress);

        if (Objects.isNull(instanceCircularFifoQueue)) {
            instanceCircularFifoQueue = new CircularFifoQueue<>(MAX_NO_OF_CONTINUOUS_PEAKS);
        }

        if (score >  threshold){
            instanceCircularFifoQueue.add(1);

            int sum = 0;
            for (int index: instanceCircularFifoQueue) {
                sum += index;
            }

            log.info(String.format("Number of continuous peaks for %s: %s", ipAddress, sum));
            decisionEngineActions.sendAlert(instanceHostMapping.getInstanceId(), score, metricScoreDomains);
            decisionEngineActions.sendAlertWithPeakCount(instanceHostMapping.getInstanceId(), sum);

            // Quarantine only when we have three continuous alerts.
            if (sum == MAX_NO_OF_CONTINUOUS_PEAKS) {
                log.info(String.format("Number of continuous peaks for %s: %s", ipAddress, sum));
                decisionEngineActions.quarantine(instanceHostMapping.getInstanceId());
                instanceHostMapping.setQurantined(true);
                instanceHostMappingRepository.save(instanceHostMapping);
            }

        }else if (score > 0 && score < threshold) {
            instanceCircularFifoQueue.add(0);
            decisionEngineActions.sendAlert(instanceHostMapping.getInstanceId(), score, metricScoreDomains);
        }else{
            instanceCircularFifoQueue.add(0);
            log.info("Not taking any action for instance " + ipAddress + " with score " + score);
        }

        instanceContinuousAlertMapping.put(ipAddress, instanceCircularFifoQueue);
    }
}
