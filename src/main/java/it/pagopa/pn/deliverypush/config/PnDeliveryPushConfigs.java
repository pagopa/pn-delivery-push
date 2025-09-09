package it.pagopa.pn.deliverypush.config;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.util.unit.DataSize;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;


@Configuration
@ConfigurationProperties( prefix = "pn.delivery-push")
@Data
@Import({SharedAutoConfiguration.class})
public class PnDeliveryPushConfigs {

    private String templatesEngineBaseUrl;

    private String deliveryBaseUrl;

    private String dataVaultBaseUrl;

    private String safeStorageBaseUrl;

    private String safeStorageCxId;
    
    private String safeStorageCxIdUpdatemetadata;

    private String externalRegistryBaseUrl;

    private String mandateBaseUrl;

    private Topics topics;

    private Webapp webapp;

    private FailedNotificationDao failedNotificationDao;

    private DocumentCreationRequestDao documentCreationRequestDao;

    private Integer retentionAttachmentDaysAfterRefinement;

    private DataSize checkPdfSize;

    private boolean checkPdfValidEnabled;

    private boolean checkCfEnabled;

    private int pagoPaNotificationBaseCost;

    private int pagoPaNotificationFee;

    private int pagoPaNotificationVat;

    private List<String> raddExperimentationStoresName;

    private List<String> listCategoriesPa;

    private ErrorCorrectionLevel errorCorrectionLevelQrCode;

    private boolean additionalLangsEnabled;

    private Duration timelineLockDuration;

    private String AAROnlyPECForRADDAndPF;

    private String templateURLforPEC;

    private String technicalRefusalCostMode;

    private String timelineClientBaseUrl;

    @Data
    public static class Topics {

        private String newNotifications;

        private String toExternalChannelPec;
        
        private String toExternalChannelEmail;
        
        private String toExternalChannelPaper;
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
    public static class FailedNotificationDao {
        private String tableName;
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
