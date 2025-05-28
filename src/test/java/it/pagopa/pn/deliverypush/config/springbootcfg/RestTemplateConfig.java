package it.pagopa.pn.deliverypush.config.springbootcfg;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@TestConfiguration
public class RestTemplateConfig {
    @Bean
    @Qualifier("withTracing")
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
