package it.pagopa.pn.deliverypush.action.it;

import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.deliverypush.action.notificationview.NotificationViewedRequestHandler;
import it.pagopa.pn.deliverypush.action.notificationview.ViewNotification;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.logtest.ConsoleAppenderCustom;
import it.pagopa.pn.deliverypush.service.impl.*;
import it.pagopa.pn.deliverypush.service.mapper.SmartMapper;
import it.pagopa.pn.deliverypush.utils.AuthUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.time.Instant;

import static org.awaitility.Awaitility.setDefaultTimeout;

@ContextConfiguration(classes = {
        AuditLogServiceImpl.class,
        SafeStorageServiceImpl.class,
        NotificationViewedRequestHandler.class,
        TimelineUtils.class,
        NotificationServiceImpl.class,
        PaperNotificationFailedServiceImpl.class,
        ConfidentialInformationServiceImpl.class,
        AttachmentUtils.class,
        NotificationUtils.class,
        MVPParameterConsumer.class,
        ViewNotification.class,
        DocumentCreationRequestServiceImpl.class,
        NotificationCancellationServiceImpl.class,
        AuthUtils.class,
        MandateServiceImpl.class,
        SmartMapper.class,
})
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/application-testIT.properties")
@DirtiesContext
@EnableScheduling
public class CommonTestConfiguration {
    @Autowired
    PnDeliveryPushConfigs cfg;
    
    @BeforeEach
    public void setup() {
        setDefaultTimeout(Duration.ofSeconds(120));

        // Viene creato un oggetto Answer per ottenere l'istante corrente al momento della chiamata ...
        Answer<Instant> answer = invocation -> Instant.now();
        // e configurato Mockito per restituire l'istante corrente al momento della chiamata
        //Mockito.when(instantNowSupplier.get()).thenAnswer(answer);
        
        setcCommonsConfigurationPropertiesForTest(cfg);

        ConsoleAppenderCustom.initializeLog();
    }

    private void setcCommonsConfigurationPropertiesForTest(PnDeliveryPushConfigs cfg) {
        // Impostazione delle proprietà di retention degli allegati
        Mockito.when(cfg.getRetentionAttachmentDaysAfterRefinement()).thenReturn(120);
        //Set send fee
        Mockito.when(cfg.getPagoPaNotificationBaseCost()).thenReturn(100);
        Mockito.when(cfg.getTemplatesEngineBaseUrl()).thenReturn("http://localhost:8090");
    }

}
