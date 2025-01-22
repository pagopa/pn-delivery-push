package it.pagopa.pn.deliverypush.action.it;

import it.pagopa.pn.deliverypush.action.it.mockbean.ExternalChannelMock;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.*;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.SendAttachmentMode;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.deliverypush.legalfacts.AarTemplateType;
import it.pagopa.pn.deliverypush.legalfacts.StaticAarTemplateChooseStrategy;
import it.pagopa.pn.deliverypush.legalfacts.DocumentComposition;
import it.pagopa.pn.deliverypush.utils.PnSendMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.awaitility.Awaitility.await;

@ContextConfiguration(classes = SentAttachmentSimpleRegisteredLetterAarDocumentOldAARIT.InnerTestConfiguration.class)
class SentAttachmentSimpleRegisteredLetterAarDocumentOldAARIT extends SendAarAttachment {
    static Instant sentNotificationTime = Instant.now();

    //Viene valorizzata la configurazione attuale, cioè INSTANT.NOW meno 10 minuti
    static AarTemplateType firstCurrentConfTemplateType = AarTemplateType.AAR_NOTIFICATION;

    static PnSendMode firstCurrentConf = PnSendMode.builder()
            .startConfigurationTime(sentNotificationTime.minus(10, ChronoUnit.MINUTES))
            .simpleRegisteredLetterSendAttachmentMode(SendAttachmentMode.AAR_DOCUMENTS)
            .digitalSendAttachmentMode(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS)
            .aarTemplateTypeChooseStrategy(new StaticAarTemplateChooseStrategy(firstCurrentConfTemplateType))
            .build();

    //Viene valorizzata la configurazione futura, cioè INSTANT.NOW più 10 giorni
    static AarTemplateType secondConfTemplateType = AarTemplateType.AAR_NOTIFICATION;
    static PnSendMode secondConf = PnSendMode.builder()
            .startConfigurationTime(sentNotificationTime.plus(10, ChronoUnit.DAYS))
            .analogSendAttachmentMode(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS)
            .simpleRegisteredLetterSendAttachmentMode(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS)
            .digitalSendAttachmentMode(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS)
            .aarTemplateTypeChooseStrategy(new StaticAarTemplateChooseStrategy(secondConfTemplateType))
            .build();

    @TestConfiguration
    static class InnerTestConfiguration {

        public InnerTestConfiguration() {
            super();
        }

        @Bean
        @Primary
        public PnDeliveryPushConfigs pnDeliveryPushConfigs() {
            PnDeliveryPushConfigs pnDeliveryPushConfigs = Mockito.mock(PnDeliveryPushConfigs.class);

            final String firstCurrentConfString = getStringConfiguration(firstCurrentConf, firstCurrentConfTemplateType);

            final String secondConfString = getStringConfiguration(secondConf, secondConfTemplateType);

            List<String> pnSendModeList = new ArrayList<>();
            pnSendModeList.add(firstCurrentConfString);
            pnSendModeList.add(secondConfString);

            Mockito.when(pnDeliveryPushConfigs.getPnSendMode()).thenReturn(pnSendModeList);

            return pnDeliveryPushConfigs;
        }
    }

    @Test
    @Disabled("Test fail sometimes")
    void simpleRegisteredLetterAarDocumentOldAAR() throws IOException {
        /*
       - Platform address vuoto (Ottenuto non valorizzando il platformAddress in addressBookEntry)
       - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)
       
       - Pa physical address presente ed effettua invio con successo
        */

        LegalDigitalAddressInt digitalDomicile = LegalDigitalAddressInt.builder()
                .address("digitalDomicile@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();


        PhysicalAddressInt paPhysicalAddress = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXTCHANNEL_SEND_SUCCESS + " Via Nuova")
                .build();

        String taxId01 = TestUtils.getTaxId();

        String pagoPaAttachment = "thisIsAnAttachment";
        List<NotificationDocumentInt> pagoPaAttachmentList = TestUtils.getDocumentList(pagoPaAttachment);

        PagoPaInt paGoPaPayment= PagoPaInt.builder()
                .creditorTaxId("cred")
                .noticeCode("notice")
                .attachment(pagoPaAttachmentList.get(0))
                .build();

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId01)
                .withInternalId("ANON_"+taxId01)
                .withPhysicalAddress(paPhysicalAddress)
                .withPayments(Collections.singletonList(
                        NotificationPaymentInfoInt.builder()
                                .pagoPA(paGoPaPayment)
                                .build()
                ))
                .withDigitalDomicile(digitalDomicile)
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        List<TestUtils.DocumentWithContent> listAttachmentWithContent = TestUtils.getDocumentWithContents(pagoPaAttachment, pagoPaAttachmentList);
        TestUtils.firstFileUploadFromNotification(listAttachmentWithContent, safeStorageClientMock);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withSentAt(sentNotificationTime)
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipient(recipient)
                .build();

        pnDeliveryClientMock.addNotification(notification);

        String iun = notification.getIun();
        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che lo stato non passi in EFFECTIVE DATE
        await().untilAsserted(() ->
                Assertions.assertEquals(NotificationStatusInt.EFFECTIVE_DATE, TestUtils.getNotificationStatus(notification, timelineService, statusUtils))
        );

        final List<String> listAttachmentExpectedToSend = getListAttachmentExpectedToSend(firstCurrentConf, notification, recIndex, notificationDocumentList, pagoPaAttachmentList);

        //Vengono ottenuti gli attachment inviati nella richiesta di PREPARE verso paperChannel
        final List<String> prepareAttachmentKeySent = getSentAttachmentKeyFromPrepare();
        //Viene verificata che gli attachment inviati in fase di PREPARE siano esattamente quelli attesi
        checkSentAndExpectedAttachmentAreEquals(listAttachmentExpectedToSend, prepareAttachmentKeySent);

        //Vengono ottenuti gli attachment inviati nella richiesta di SEND verso paperChannel
        final List<String> sendAttachmentKeySent = getSentAttachmentKeyFromPrepare();
        //Viene verificata che gli attachment inviati in fase di SEND siano esattamente quelli attesi
        checkSentAndExpectedAttachmentAreEquals(listAttachmentExpectedToSend, sendAttachmentKeySent);

        //Viene ottenuta la lista di tutti i documenti generati
        final List<DocumentComposition.TemplateType> listDocumentTypeGenerated = getListDocumentTypeGenerated(3);
        
        //Viene quindi verificato se nella lista dei documenti generati c'è il documento atteso
        Assertions.assertTrue(listDocumentTypeGenerated.contains(firstCurrentConfTemplateType.getTemplateType()));
    }
}
