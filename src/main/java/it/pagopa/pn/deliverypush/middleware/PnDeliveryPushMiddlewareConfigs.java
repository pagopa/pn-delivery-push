package it.pagopa.pn.deliverypush.middleware;


import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PnDeliveryPushMiddlewareConfigs {

    private final PnDeliveryPushConfigs cfg;

    public PnDeliveryPushMiddlewareConfigs(PnDeliveryPushConfigs cfg) {
        this.cfg = cfg;
    }

}