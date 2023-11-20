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
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationPaymentInfoInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.SendAttachmentMode;
import it.pagopa.pn.deliverypush.legalfacts.DocumentComposition;
import it.pagopa.pn.deliverypush.utils.PaperSendMode;
import org.junit.jupiter.api.Assertions;
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
import java.util.List;

import static org.awaitility.Awaitility.await;

@ContextConfiguration(classes = SentAttachmentSimpleRegisteredLetterAarDocumentOldAARIT.InnerTestConfiguration.class)
class SentAttachmentSimpleRegisteredLetterAarDocumentOldAARIT extends SendAarAttachment {
    static Instant sentNotificationTime = Instant.now();

    //Viene valorizzata la configurazione attuale, cioè INSTANT.NOW meno 10 minuti
    static PaperSendMode firstCurrentConf = PaperSendMode.builder()
            .startConfigurationTime(sentNotificationTime.minus(10, ChronoUnit.MINUTES))
            .simpleRegisteredLetterSendAttachmentMode(SendAttachmentMode.AAR_DOCUMENTS)
            .aarTemplateType(DocumentComposition.TemplateType.AAR_NOTIFICATION)
            .build();

    //Viene valorizzata la configurazione futura, cioè INSTANT.NOW più 10 giorni
    static PaperSendMode secondConf = PaperSendMode.builder()
            .startConfigurationTime(sentNotificationTime.plus(10, ChronoUnit.DAYS))
            .analogSendAttachmentMode(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS)
            .simpleRegisteredLetterSendAttachmentMode(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS)
            .aarTemplateType(DocumentComposition.TemplateType.AAR_NOTIFICATION)
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

            final String firstCurrentConfString = getStringConfiguration(firstCurrentConf);

            final String secondConfString = getStringConfiguration(secondConf);

            List<String> paperSendModeList = new ArrayList<>();
            paperSendModeList.add(firstCurrentConfString);
            paperSendModeList.add(secondConfString);

            Mockito.when(pnDeliveryPushConfigs.getPaperSendMode()).thenReturn(paperSendModeList);

            return pnDeliveryPushConfigs;
        }
    }

    @Test
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

        String taxId01 = "TAXID01";

        String pagoPaAttachment = "thisIsAnAttachment";
        List<NotificationDocumentInt> pagoPaAttachmentList = TestUtils.getDocumentList(pagoPaAttachment);
        
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId01)
                .withInternalId("ANON_"+taxId01)
                .withPhysicalAddress(paPhysicalAddress)
                .withPayment(NotificationPaymentInfoInt.builder()
                        .creditorTaxId("cred")
                        .noticeCode("notice")
                        .pagoPaForm(pagoPaAttachmentList.get(0))
                        .build()
                )
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
        Assertions.assertTrue(listDocumentTypeGenerated.contains(firstCurrentConf.getAarTemplateType()));
    }
}
