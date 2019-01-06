package IN86.main;

import net.gpedro.integrations.slack.SlackApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfiguration {

    private final String webhook_url = "https://hooks.slack.com/services/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";

    @Bean
    public SlackApi slackApi(){
        return new SlackApi(webhook_url);
    }
}
