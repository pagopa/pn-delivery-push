package it.pagopa.pn.deliverypush;

import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.impl.TimeParams;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static it.pagopa.pn.deliverypush.utils.HtmlSanitizer.SanitizeMode;

@Configuration
@ConfigurationProperties( prefix = "pn.delivery-push")
@Data
@Import({SharedAutoConfiguration.class})
public class PnDeliveryPushConfigs {

    private String deliveryBaseUrl;

    private String paperChannelBaseUrl;

    private String externalChannelBaseUrl;

    private String externalchannelCxId;

    private String externalchannelSenderPec;

    private String externalchannelSenderEmail;

    private String externalchannelSenderSms;

    private String dataVaultBaseUrl;

    private String safeStorageBaseUrl;

    private String safeStorageCxId;
    private String safeStorageCxIdUpdatemetadata;

    private String userAttributesBaseUrl;

    private String externalRegistryBaseUrl;

    private String mandateBaseUrl;

    private TimeParams timeParams;

    private Topics topics;

    private Instant actionPoolEpoch;

    private Webhook webhook;
    
    private Webapp webapp;

    private TimelineDao timelineDao;

    private FailedNotificationDao failedNotificationDao;

    private ActionDao actionDao;

    private WebhookDao webhookDao;

    private FutureActionDao futureActionDao;
    
    private LastPollForFutureActionDao lastPollForFutureActionDao;

    private ExternalChannel externalChannel;

    private PaperChannel paperChannel;
    
    private LegalFacts legalfacts;

    private Integer retentionAttachmentDaysAfterRefinement;

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
        private Integer purgeDeletionWaittime;
        private Integer readBufferDelay;
        private Integer maxStreams;
        private Duration ttl;
    }

    @Data
    public static class ExternalChannel {

        private List<String> digitalCodesProgress;
        private List<String> digitalCodesSuccess;
        private List<String> digitalCodesFail;
        private List<String> digitalCodesRetryable;

        private List<String> digitalCodesFatallog;

        private int digitalRetryCount;
        private Duration digitalRetryDelay;
        private Duration digitalSendNoresponseTimeout;

    }

    @Data
    public static class PaperChannel {
        private List<String> analogCodesProgress;
        private List<String> analogCodesSuccess;
        private List<String> analogCodesFail;

        private SenderAddress senderAddress;

    }

    @Data
    public static class SenderAddress {
        private String fullname;
        private String address;
        private String zipcode;
        private String city;
        private String pr;
        private String country;
    }

   @Data
   public static class Webapp {
        private String directAccessUrlTemplate;
        private String faqUrlTemplate;
        private String quickAccessUrlAarDetailPfTemplate;
        private String quickAccessUrlAarDetailPgTemplate;
   }

    @Data
    public static class TimelineDao {
        private String tableName;
    }

    @Data
    public static class FailedNotificationDao {
        private String tableName;
    }

    @Data
    public static class ActionDao {
        private String tableName;
    }

    @Data
    public static class FutureActionDao {
        private String tableName;
    }

    @Data
    public static class WebhookDao {
        private String streamsTableName;
        private String eventsTableName;
    }

    @Data
    public static class LastPollForFutureActionDao {
        private String tableName;
        private String lockTableName;
    }

    @Data
    public static class LegalFacts {
        private SanitizeMode sanitizeMode;
    }


}
