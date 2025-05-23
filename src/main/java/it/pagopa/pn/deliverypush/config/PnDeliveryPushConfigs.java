package it.pagopa.pn.deliverypush.config;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.impl.TimeParams;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.util.unit.DataSize;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.List;


@Configuration
@ConfigurationProperties( prefix = "pn.delivery-push")
@Data
@Import({SharedAutoConfiguration.class})
public class PnDeliveryPushConfigs {

    private String templatesEngineBaseUrl;

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

    private Duration actionPoolBeforeDelay;
    
    private Webapp webapp;

    private TimelineDao timelineDao;

    private TimelinecounterDao timelinecounterDao;

    private FailedNotificationDao failedNotificationDao;

    private ActionDao actionDao;

    private FutureActionDao futureActionDao;

    private LastPollForFutureActionDao lastPollForFutureActionDao;

    private DocumentCreationRequestDao documentCreationRequestDao;

    private ExternalChannel externalChannel;

    private PaperChannel paperChannel;

    private Integer retentionAttachmentDaysAfterRefinement;

    private String nationalRegistriesBaseUrl;

    private String addressManagerBaseUrl;

    private String f24BaseUrl;

    private String emdIntegrationBaseUrl;

    private String addressManagerApiKey;
    
    private Duration[] validationRetryIntervals;

    private DataSize checkPdfSize;

    private boolean checkPdfValidEnabled;
    
    private String actionTtlDays;
    
    private boolean checkCfEnabled;

    private String f24CxId;
    
    private int pagoPaNotificationBaseCost;

    private int pagoPaNotificationFee;

    private int pagoPaNotificationVat;

    private List<String> paperSendMode;

    private List<String> raddExperimentationStoresName;

    private List<String> pnSendMode;

    private String activationDeceasedWorkflowDate;

    //quickWorkAroundForPN-9116
    private boolean sendMoreThan20GramsDefaultValue;

    private List<String> listCategoriesPa;

    private ErrorCorrectionLevel errorCorrectionLevelQrCode;

    private boolean additionalLangsEnabled;

    private Instant featureUnreachableRefinementPostAARStartDate;

    private Instant startWriteBusinessTimestamp;

    private Instant stopWriteBusinessTimestamp;

    private String pfNewWorkflowStart;

    private String pfNewWorkflowStop;

    private Duration timelineLockDuration;

    private String AAROnlyPECForRADDAndPF;

    private String templateURLforPEC;

    private String technicalRefusalCostMode;

    @Data
    public static class Topics {

        private String newNotifications;

        private String toExternalChannelPec;
        
        private String toExternalChannelEmail;
        
        private String toExternalChannelPaper;

        private String fromExternalChannel;

        private String safeStorageEvents;

        private String nationalRegistriesEvents;

        private String addressManagerEvents;

        private String f24Events;
        
        private String deliveryValidationEvents;
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
        private String faqSendHash;
        private String quickAccessUrlAarDetailSuffix;
        private String landingUrl;
        private String raddPhoneNumber;
        private String aarSenderLogoUrlTemplate;
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
    public static class LastPollForFutureActionDao {
        private String lockTableName;
    }

    @Data
    public static class DocumentCreationRequestDao {
        private String tableName;
    }

    private boolean safeStorageFileNotFoundRetry;

    @PostConstruct
    public void init() {
        System.out.println(this);
    }

}
