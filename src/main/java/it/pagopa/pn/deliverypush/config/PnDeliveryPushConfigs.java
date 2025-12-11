package it.pagopa.pn.deliverypush.config;

import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;




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

    private FailedNotificationDao failedNotificationDao;

    private DocumentCreationRequestDao documentCreationRequestDao;

    private NotificationReworksDao notificationReworksDao;

    private Integer retentionAttachmentDaysAfterRefinement;

    private int pagoPaNotificationBaseCost;

    private int pagoPaNotificationFee;

    private int pagoPaNotificationVat;

    private boolean additionalLangsEnabled;

    private String timelineClientBaseUrl;

    private String actionManagerBaseUrl;

    private String paperTrackerClientBaseUrl;

    private boolean notificationReworkEnabled;

    @Data
    public static class FailedNotificationDao {
        private String tableName;
    }

    @Data
    public static class DocumentCreationRequestDao {
        private String tableName;
    }

    @Data
    public static class NotificationReworksDao {
        private String tableName;
    }

    @PostConstruct
    public void init() {
        System.out.println(this);
    }

}
