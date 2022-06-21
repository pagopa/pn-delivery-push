package it.pagopa.pn.deliverypush.middleware.externalclient;

import it.pagopa.pn.commons.pnclients.RestTemplateFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateFactoryActivation extends RestTemplateFactory {

    @Bean
    @Qualifier("withTracing")
    @Primary
    @Override
    public RestTemplate restTemplateWithTracing() {
        RestTemplate restTemplate =  super.restTemplateWithTracing();
        restTemplate.setErrorHandler(new RestTemplateResponseErrorHandler());
        return restTemplate;
    }
}