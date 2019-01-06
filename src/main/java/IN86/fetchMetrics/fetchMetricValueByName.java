package IN86.fetchMetrics;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static IN86.main.AppConfiguration.BASE_METRICS_URLS;

public class fetchMetricValueByName {

    public static List<MetricDetails> fetchCPUUsageMetric() {
        String metric = "system.cpu.usage";
        String metricName = "cpu";

        List<MetricDetails> metricDetailsList = new ArrayList<>();

        fetchMetricDetailsFromAllHosts(metric, metricName, metricDetailsList);

        return metricDetailsList;
    }

    public static List<MetricDetails> fetchErrorLogMetric() {
        String metric = "log4j2.events?tag=level:error";
        String metricName = "errors";

        List<MetricDetails> metricDetailsList = new ArrayList<>();

        fetchMetricDetailsFromAllHosts(metric, metricName, metricDetailsList);

        return metricDetailsList;
    }

    public static List<MetricDetails> fetchMajorGCMetric() {
        String metric = "jvm.gc.pause?tag=action:end%20of%20major%20GC";
        String metricName = "gc";

        List<MetricDetails> metricDetailsList = new ArrayList<>();

        fetchMetricDetailsFromAllHosts(metric, metricName, metricDetailsList);

        return metricDetailsList;
    }

    private static void fetchMetricDetailsFromAllHosts(String metric, String metricName, List<MetricDetails> metricDetailsList) {
        for (String baseMetricUrl: BASE_METRICS_URLS) {
            MetricDetails metricDetails;
            try {
                double value = fetchMetricValue(metric, baseMetricUrl);
                if (value == -1) {
                    continue;
                }
                metricDetails = new MetricDetails();
                metricDetails.setMetric(metricName);
                metricDetails.setValue(value);
                metricDetails.setEnv("env");
                metricDetails.setRole("orders");
                metricDetails.setStack("india");
                metricDetails.setHost(baseMetricUrl);
            } catch (IOException e) {
                continue;
            }
            metricDetailsList.add(metricDetails);
        }
    }

    private static double fetchMetricValue(String metricName, String baseMetricsUrl) throws IOException {
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(baseMetricsUrl + "/metrics/" + metricName);
        HttpResponse response;
        double value  = -1;

        response = client.execute(request);

        int statuscode = response.getStatusLine().getStatusCode();

        if (statuscode != HttpStatus.SC_OK) {
            return value;
        }

        String responseString=  EntityUtils.toString(response.getEntity(),"UTF-8");
        JSONObject responseJSON = new JSONObject(responseString);

        value = (double) ((JSONObject)((JSONArray) responseJSON.get("measurements")).get(0)).get("value");
        return value;
    }
}
