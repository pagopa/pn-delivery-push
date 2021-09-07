package it.pagopa.pn.deliverypush;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties( prefix = "pn.delivery-push")
@Data
public class PnDeliveryPushConfigs {

    private Topics topics;

    @Data
    public static class Topics {

        private String newNotifications;

        private String scheduledActions;

        private String toExternalChannelPec;
        
        private String toExternalChannelEmail;

        private String fromExternalChannel;

    }
}
