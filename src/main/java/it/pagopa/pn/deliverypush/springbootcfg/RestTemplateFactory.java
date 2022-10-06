package it.pagopa.pn.deliverypush.springbootcfg;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;

@Configuration
public class RestTemplateFactory {

    private final it.pagopa.pn.commons.pnclients.RestTemplateFactory templateFactory;

    public RestTemplateFactory(it.pagopa.pn.commons.pnclients.RestTemplateFactory restTemplateFactory) {
        this.templateFactory = restTemplateFactory;
    }


    @Bean
    @Qualifier("withOffsetDateTimeFormatter")
    public RestTemplate restTemplateWithOffsetDateTimeFormatter() {
        // Override del comportamento di serializzazione delle date
        // per ovviare al problema del numero di cifre nella frazione di secondo
        RestTemplate template = new RestTemplate();
        template.getMessageConverters().stream()
                .filter(AbstractJackson2HttpMessageConverter.class::isInstance)
                .map(AbstractJackson2HttpMessageConverter.class::cast)
                .forEach(converter -> converter.getObjectMapper()
                        .configOverride(OffsetDateTime.class)
                        .setFormat(JsonFormat.Value.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX"))
                );
        templateFactory.enrichWithTracing(template);
        return template;
    }

}
