package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine.api.TemplateApi;
import it.pagopa.pn.deliverypush.utils.PnSendModeUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@ExtendWith(SpringExtension.class)
class LegalFactGeneratorTemplatesClientTest {

    LegalFactGeneratorTemplatesClient legalFactGeneratorTemplatesClient;

    @Mock
    CustomInstantWriter instantWriter;
    @Mock
    PhysicalAddressWriter physicalAddressWriter;
    @Mock
    PnDeliveryPushConfigs pnDeliveryPushConfigs;
    @Mock
    PnSendModeUtils pnSendModeUtils;
    @Mock
    TemplateApi templateEngineClient;

    @BeforeEach
    public void init() {
        legalFactGeneratorTemplatesClient = new LegalFactGeneratorTemplatesClient(instantWriter,
                physicalAddressWriter, pnDeliveryPushConfigs, pnSendModeUtils, templateEngineClient);
    }

    @Test
    void generateNotificationReceivedLegalFact() {
        List<NotificationRecipientInt> recipients = new ArrayList<>();
        NotificationRecipientInt recipientInt = NotificationRecipientInt.builder()
                .denomination("denomination_test")
                .digitalDomicile(new LegalDigitalAddressInt())
                .build();
        recipients.add(recipientInt);
        NotificationInt notificationInt = NotificationInt.builder()
                .recipients(recipients)
                .subject("subject_test")
                .sentAt(Instant.now())
                .sender(new NotificationSenderInt())
                .documents(new ArrayList<>())
                .build();

        Mockito.when(templateEngineClient.notificationReceivedLegalFact(Mockito.any(), Mockito.any()))
                .thenReturn(new File("C:\\Users\\adrian.borta\\workspaces_pagopa\\pn-delivery-push\\src\\test\\resources\\response.pdf"));
        var result = Assertions.assertDoesNotThrow(() -> legalFactGeneratorTemplatesClient.generateNotificationReceivedLegalFact(notificationInt));
        Assertions.assertNotNull(result);
    }

    @Test
    void generateNotificationViewedLegalFact() {
    }

    @Test
    void generatePecDeliveryWorkflowLegalFact() {
    }

    @Test
    void generateAnalogDeliveryFailureWorkflowLegalFact() {
    }

    @Test
    void generateNotificationCancelledLegalFact() {
    }

    @Test
    void generateNotificationAARSubject() {
    }

    @Test
    void generateNotificationAAR() {
    }

    @Test
    void generateNotificationAARBody() {
    }

    @Test
    void generateNotificationAARPECBody() {
    }

    @Test
    void generateNotificationAARForSMS() {
    }

    @Test
    void convertFileMonoToBytes() {
    }
}