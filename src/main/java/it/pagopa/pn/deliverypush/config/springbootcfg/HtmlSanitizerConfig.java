package it.pagopa.pn.deliverypush.config.springbootcfg;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.utils.HtmlSanitizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HtmlSanitizerConfig {

    @Bean
    HtmlSanitizer htmlSanitizer(ObjectMapper objectMapper, PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        return new HtmlSanitizer(objectMapper, pnDeliveryPushConfigs.getLegalfacts().getSanitizeMode());
    }
}
