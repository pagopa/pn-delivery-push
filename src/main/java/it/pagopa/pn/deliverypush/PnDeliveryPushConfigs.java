package it.pagopa.pn.deliverypush;

import it.pagopa.pn.deliverypush.abstractions.actionspool.impl.TimeParams;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;

@Configuration
@ConfigurationProperties( prefix = "pn.delivery-push")
@Data
public class PnDeliveryPushConfigs {

    private String deliveryBaseUrl;

    private String externalChannelBaseUrl;

    private TimeParams timeParams;

    private Topics topics;

    private Instant actionPoolEpoch;

    private Webhook webhook;
    
    private Webapp webapp;

    @Data
    public static class Topics {

        private String newNotifications;

        private String scheduledActions;

        private String executedActions;

        private String toExternalChannelPec;
        
        private String toExternalChannelEmail;
        
        private String toExternalChannelPaper;

        private String fromExternalChannel;

    }

    @Data
    public static class Webhook {
        private Long scheduleInterval;
        private Integer maxLength;
    }

   @Data
   public static class Webapp {
        private String directAccessUrlTemplate;
   }
}
