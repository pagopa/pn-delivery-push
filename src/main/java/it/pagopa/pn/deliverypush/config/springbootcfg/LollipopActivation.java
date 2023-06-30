package it.pagopa.pn.deliverypush.config.springbootcfg;

import it.pagopa.pn.commons.configs.lollipop.PNHttpVerifierConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix="lollipop", name="active", havingValue = "true")
public class LollipopActivation extends PNHttpVerifierConfiguration {
}
