package it.pagopa.pn.deliverypush.config;

import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.impl.TimeParams;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.util.unit.DataSize;

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

    private TimelinecounterDao timelinecounterDao;

    private FailedNotificationDao failedNotificationDao;

    private ActionDao actionDao;

    private WebhookDao webhookDao;

    private FutureActionDao futureActionDao;
    
    private LastPollForFutureActionDao lastPollForFutureActionDao;

    private DocumentCreationRequestDao documentCreationRequestDao;

    private ExternalChannel externalChannel;

    private PaperChannel paperChannel;
    
    private LegalFacts legalfacts;

    private Integer retentionAttachmentDaysAfterRefinement;

    private String nationalRegistriesBaseUrl;

    private String addressManagerBaseUrl;

    private String f24BaseUrl;

    private String addressManagerApiKey;
    
    private Duration[] validationRetryIntervals;

    private DataSize checkPdfSize;

    private boolean checkPdfValidEnabled;
    private Duration actionTtl;
    private boolean checkCfEnabled;

    private String f24CxId;

    private String sendAnalogNotificationAttachments;
    private String sendSimpleRegisteredLetterAttachments;
    private int pagoPaNotificationBaseCost;

    @Data
    public static class Topics {

        private String newNotifications;

        private String scheduledActions;

        private String executedActions;

        private String toExternalChannelPec;
        
        private String toExternalChannelEmail;
        
        private String toExternalChannelPaper;

        private String fromExternalChannel;

        private String safeStorageEvents;

        private String nationalRegistriesEvents;

        private String addressManagerEvents;

        private String f24Events;
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

        private SenderAddress senderAddress;

        public PhysicalAddressInt getSenderPhysicalAddress(){
            return PhysicalAddressInt.builder()
                    .fullname(senderAddress.getFullname())
                    .address(senderAddress.getAddress())
                    .zip(senderAddress.getZipcode())
                    .province(senderAddress.getPr())
                    .municipality(senderAddress.getCity())
                    .foreignState(senderAddress.getCountry())
                    .build();
        }
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
        private String directAccessUrlTemplatePhysical;
        private String directAccessUrlTemplateLegal;
        private String faqUrlTemplateSuffix;
        private String faqCompletionMomentHash;
        private String faqSendHash;
        private String quickAccessUrlAarDetailSuffix;
        private String landingUrl;
   }

    @Data
    public static class TimelineDao {
        private String tableName;
    }


    @Data
    public static class TimelinecounterDao {
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
    public static class DocumentCreationRequestDao {
        private String tableName;
    }

    @Data
    public static class LegalFacts {
        private SanitizeMode sanitizeMode;
    }

    private boolean safeStorageFileNotFoundRetry;

}
