package IN86.main;

import net.gpedro.integrations.slack.SlackApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfiguration {

    private final String webhook_url = "https://hooks.slack.com/services/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";
    public static final String[] BASE_METRICS_URLS = { "http://192.168.1.87:9004", "http://192.168.1.87:9010" };
    public static final String[] hosts = {"Turvo-nikhil.n"};
    public static final String[] metrics = {"cpu", "gc", "errors"};

    @Bean
    public SlackApi slackApi(){
        return new SlackApi(webhook_url);
    }
}
