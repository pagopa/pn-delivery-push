package it.pagopa.pn.deliverypush.action.it;

import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.deliverypush.action.it.mockbean.*;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypush.action.startworkflow.StartWorkflowHandler;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.*;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.deliverypush.logtest.ConsoleAppenderCustom;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelSendRequest;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.util.Base64Utils;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static it.pagopa.pn.deliverypush.action.it.mockbean.ExternalChannelMock.EXTCHANNEL_SEND_SUCCESS;
import static it.pagopa.pn.deliverypush.action.it.mockbean.F24ClientMock.F24_VALIDATION_FAIL;
import static org.awaitility.Awaitility.await;

class ValidationTestIT extends CommonTestConfiguration{
    @Autowired
    StartWorkflowHandler startWorkflowHandler;
    @Autowired
    TimelineService timelineService;
    @SpyBean
    ExternalChannelMock externalChannelMock;
    @SpyBean
    PaperChannelMock paperChannelMock;
    @Autowired
    SafeStorageClientMock safeStorageClientMock;
    @Autowired
    PnDeliveryClientMock pnDeliveryClientMock;
    @Autowired
    UserAttributesClientMock addressBookMock;
    @Autowired
    NationalRegistriesClientMock nationalRegistriesClientMock;
    
    @Test
    void differentShaRefusedTest() throws PnIdConflictException {
        // GIVEN
        
        // Platform address is present and all sending attempts fail
        LegalDigitalAddressInt platformAddress = LegalDigitalAddressInt.builder()
                .address("platformAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();
        
        //Special address is present and all sending attempts fail
        LegalDigitalAddressInt digitalDomicile = LegalDigitalAddressInt.builder()
                .address("digitalDomicile@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();
        
        //General address is present and all sending attempts fail
        LegalDigitalAddressInt pbDigitalAddress = LegalDigitalAddressInt.builder()
                .address("pbDigitalAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId("TAXID01")
                .withDigitalDomicile(digitalDomicile)
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();

        byte[] differentFileSha = "error".getBytes();
        notification = TestUtils.firstFileUploadFromNotificationError(notification, safeStorageClientMock, differentFileSha);
        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));
        nationalRegistriesClientMock.addDigital(recipient.getTaxId(), pbDigitalAddress);

        String iun = notification.getIun();
        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //WHEN the workflow start
        startWorkflowHandler.startWorkflow(iun);
        
        //THEN
        await().untilAsserted(() ->
                //Check worfklow is failed
                Assertions.assertTrue(timelineService.getTimelineElement(
                        iun,
                        TimelineEventId.REQUEST_REFUSED.buildEventId(
                                EventId.builder()
                                        .iun(iun)
                                        .recIndex(recIndex)
                                        .build())).isPresent()
                )
        );

        Mockito.verify(externalChannelMock, Mockito.times(0)).sendLegalNotification(
                Mockito.any(NotificationInt.class),
                Mockito.any(NotificationRecipientInt.class),
                Mockito.any(LegalDigitalAddressInt.class),
                Mockito.anyString(),
                Mockito.anyList(),
                Mockito.anyString()
        );
        Mockito.verify(paperChannelMock, Mockito.times(0)).send(Mockito.any(PaperChannelSendRequest.class));

        ConsoleAppenderCustom.checkLogs();
    }

    @Test
    void taxIdNotValidTest() throws PnIdConflictException {
        // GIVEN

        // Platform address is present and all sending attempts fail
        LegalDigitalAddressInt platformAddress = LegalDigitalAddressInt.builder()
                .address("platformAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        //Special address is present and all sending attempts fail
        LegalDigitalAddressInt digitalDomicile = LegalDigitalAddressInt.builder()
                .address("digitalDomicile@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        //General address is present and all sending attempts fail
        LegalDigitalAddressInt pbDigitalAddress = LegalDigitalAddressInt.builder()
                .address("pbDigitalAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId("TAXID01_" + NationalRegistriesClientMock.NOT_VALID)
                .withDigitalDomicile(digitalDomicile)
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        notificationDocumentList = TestUtils.firstFileUploadFromNotification(listDocumentWithContent, notificationDocumentList, safeStorageClientMock);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();

        byte[] differentFileSha = "error".getBytes();
        notification = TestUtils.firstFileUploadFromNotificationError(notification, safeStorageClientMock, differentFileSha);
        
        
        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));
        nationalRegistriesClientMock.addDigital(recipient.getTaxId(), pbDigitalAddress);

        String iun = notification.getIun();

        //WHEN the workflow start
        startWorkflowHandler.startWorkflow(iun);

        //THEN
        await().untilAsserted(() ->
                //Check worfklow is failed
                Assertions.assertTrue(timelineService.getTimelineElement(
                        iun,
                        TimelineEventId.REQUEST_REFUSED.buildEventId(
                                EventId.builder()
                                        .iun(iun)
                                        .build())).isPresent()
                )
        );

        Mockito.verify(externalChannelMock, Mockito.times(0)).sendLegalNotification(
                Mockito.any(NotificationInt.class),
                Mockito.any(NotificationRecipientInt.class),
                Mockito.any(LegalDigitalAddressInt.class),
                Mockito.anyString(),
                Mockito.anyList(),
                Mockito.anyString()
        );
        Mockito.verify(paperChannelMock, Mockito.times(0)).send(Mockito.any(PaperChannelSendRequest.class));

        ConsoleAppenderCustom.checkLogs();
    }

    @Test
    void addressNotValidTest() {
        // GIVEN
        
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId("TAXID01")
                .withPhysicalAddress(PhysicalAddressBuilder.builder()
                        .withAddress("Via Nuova_" + AddressManagerClientMock.ADDRESS_MANAGER_NOT_VALID_ADDRESS)
                        .build())
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        notificationDocumentList = TestUtils.firstFileUploadFromNotification(listDocumentWithContent, notificationDocumentList, safeStorageClientMock);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();


        pnDeliveryClientMock.addNotification(notification);
        
        String iun = notification.getIun();

        //WHEN the workflow start
        startWorkflowHandler.startWorkflow(iun);

        //THEN
        await().untilAsserted(() ->
                //Check worfklow is failed
                Assertions.assertTrue(timelineService.getTimelineElement(
                        iun,
                        TimelineEventId.REQUEST_REFUSED.buildEventId(
                                EventId.builder()
                                        .iun(iun)
                                        .build())).isPresent()
                )
        );

        Mockito.verify(externalChannelMock, Mockito.times(0)).sendLegalNotification(
                Mockito.any(NotificationInt.class),
                Mockito.any(NotificationRecipientInt.class),
                Mockito.any(LegalDigitalAddressInt.class),
                Mockito.anyString(),
                Mockito.anyList(),
                Mockito.anyString()
        );
        Mockito.verify(paperChannelMock, Mockito.times(0)).send(Mockito.any(PaperChannelSendRequest.class));

        ConsoleAppenderCustom.checkLogs();
    }

    @Test
    void fileTooBig() {
        // GIVEN

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId("TAXID01")
                .withPhysicalAddress(PhysicalAddressBuilder.builder()
                        .withAddress("Via Nuova_" + AddressManagerClientMock.ADDRESS_MANAGER_TO_NORMALIZE)
                        .build())
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        notificationDocumentList = TestUtils.firstFileUploadFromNotificationTooBig(listDocumentWithContent, notificationDocumentList, safeStorageClientMock);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();


        pnDeliveryClientMock.addNotification(notification);

        String iun = notification.getIun();

        //WHEN the workflow start
        startWorkflowHandler.startWorkflow(iun);

        //THEN
        await().untilAsserted(() ->
                //Check worfklow is failed
                Assertions.assertTrue(timelineService.getTimelineElement(
                        iun,
                        TimelineEventId.REQUEST_REFUSED.buildEventId(
                                EventId.builder()
                                        .iun(iun)
                                        .build())).isPresent()
                )
        );

        Mockito.verify(externalChannelMock, Mockito.times(0)).sendLegalNotification(
                Mockito.any(NotificationInt.class),
                Mockito.any(NotificationRecipientInt.class),
                Mockito.any(LegalDigitalAddressInt.class),
                Mockito.anyString(),
                Mockito.anyList(),
                Mockito.anyString()
        );
        Mockito.verify(paperChannelMock, Mockito.times(0)).send(Mockito.any(PaperChannelSendRequest.class));

        ConsoleAppenderCustom.checkLogs();
    }



    @Test
    void fileNotValidPDF() {
        // GIVEN

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId("TAXID01")
                .withPhysicalAddress(PhysicalAddressBuilder.builder()
                        .withAddress("Via Nuova_" + AddressManagerClientMock.ADDRESS_MANAGER_TO_NORMALIZE)
                        .build())
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        notificationDocumentList = TestUtils.firstFileUploadFromNotificationNotAPDF(listDocumentWithContent, notificationDocumentList, safeStorageClientMock);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();


        pnDeliveryClientMock.addNotification(notification);

        String iun = notification.getIun();

        //WHEN the workflow start
        startWorkflowHandler.startWorkflow(iun);

        //THEN
        await().untilAsserted(() ->
                //Check worfklow is failed
                Assertions.assertTrue(timelineService.getTimelineElement(
                        iun,
                        TimelineEventId.REQUEST_REFUSED.buildEventId(
                                EventId.builder()
                                        .iun(iun)
                                        .build())).isPresent()
                )
        );

        Mockito.verify(externalChannelMock, Mockito.times(0)).sendLegalNotification(
                Mockito.any(NotificationInt.class),
                Mockito.any(NotificationRecipientInt.class),
                Mockito.any(LegalDigitalAddressInt.class),
                Mockito.anyString(),
                Mockito.anyList(),
                Mockito.anyString()
        );
        Mockito.verify(paperChannelMock, Mockito.times(0)).send(Mockito.any(PaperChannelSendRequest.class));

        ConsoleAppenderCustom.checkLogs();
    }
    @Test
    void f24ValidationKo() {
        // GIVEN
        PhysicalAddressInt paPhysicalAddress1 = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR + ExternalChannelMock.EXTCHANNEL_SEND_SUCCESS + " Via Nuova")
                .build();

        String paymentDocName = "metadata_0_0";
        NotificationDocumentInt paymentDoc = TestUtils.getDocumentList(paymentDocName).get(0);
        List<NotificationDocumentInt> listPaymentDoc = List.of(paymentDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContentForPayments = TestUtils.getDocumentWithContents(paymentDocName, listPaymentDoc );
        listPaymentDoc = TestUtils.firstFileUploadFromNotification(listDocumentWithContentForPayments, listPaymentDoc, safeStorageClientMock);

        final List<NotificationPaymentInfoInt> paymentWithF24 = TestUtils.getPaymentWithF24(listPaymentDoc.get(0));
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId("TAXID01")
                .withPhysicalAddress(paPhysicalAddress1)
                .withPayments(paymentWithF24)
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        notificationDocumentList = TestUtils.firstFileUploadFromNotification(listDocumentWithContent, notificationDocumentList, safeStorageClientMock);


/*
        //List which contains documents and payments
        List<TestUtils.DocumentWithContent> notificationDocuments = new ArrayList<>(listDocumentWithContent);
        notificationDocuments.addAll(listDocumentWithContentForPayments);
*/

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(TestUtils.getRandomIun() + F24_VALIDATION_FAIL)
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();


        pnDeliveryClientMock.addNotification(notification);

        String iun = notification.getIun();

        //WHEN the workflow start
        startWorkflowHandler.startWorkflow(iun);

        //THEN
        await().untilAsserted(() ->
                //Check worfklow is failed
                Assertions.assertTrue(timelineService.getTimelineElement(
                        iun,
                        TimelineEventId.REQUEST_REFUSED.buildEventId(
                                EventId.builder()
                                        .iun(iun)
                                        .build())).isPresent()
                )
        );

        Mockito.verify(externalChannelMock, Mockito.times(0)).sendLegalNotification(
                Mockito.any(NotificationInt.class),
                Mockito.any(NotificationRecipientInt.class),
                Mockito.any(LegalDigitalAddressInt.class),
                Mockito.anyString(),
                Mockito.anyList(),
                Mockito.anyString()
        );
        Mockito.verify(paperChannelMock, Mockito.times(0)).send(Mockito.any(PaperChannelSendRequest.class));

        ConsoleAppenderCustom.checkLogs();
    }

    @Test
    void validationPaymentInfoKO() {
        // GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withPayments(Collections.singletonList(
                        NotificationPaymentInfoInt.builder()
                                .pagoPA(PagoPaInt.builder()
                                        .creditorTaxId("creditorTaxId_"+PnExternalRegistriesClientReactiveMock.TO_FAIL+UUID.randomUUID())
                                        .noticeCode("noticeCode_"+UUID.randomUUID())
                                        .applyCost(true)
                                        .attachment(NotificationDocumentInt.builder()
                                                .ref(NotificationDocumentInt.Ref.builder()
                                                        .key("keyPagoPaForm")
                                                        .build())
                                                .digests(NotificationDocumentInt.Digests.builder()
                                                        .sha256(Base64Utils.encodeToString("keyPagoPaForm".getBytes()))
                                                        .build())
                                                .build())
                                        .build())
                                .build()
                ))
                .build();


        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        notificationDocumentList = TestUtils.firstFileUploadFromNotification(listDocumentWithContent, notificationDocumentList, safeStorageClientMock);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withNotificationRecipient(recipient)
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withPagoPaIntMode(PagoPaIntMode.ASYNC)
                .withPaFee(100)
                .build();


        pnDeliveryClientMock.addNotification(notification);

        String iun = notification.getIun();

        //WHEN the workflow start
        startWorkflowHandler.startWorkflow(iun);

        //THEN
        await().untilAsserted(() ->
                //Check worfklow is failed
                Assertions.assertTrue(timelineService.getTimelineElement(
                        iun,
                        TimelineEventId.REQUEST_REFUSED.buildEventId(
                                EventId.builder()
                                        .iun(iun)
                                        .build())).isPresent()
                )
        );

        Mockito.verify(externalChannelMock, Mockito.times(0)).sendLegalNotification(
                Mockito.any(NotificationInt.class),
                Mockito.any(NotificationRecipientInt.class),
                Mockito.any(LegalDigitalAddressInt.class),
                Mockito.anyString(),
                Mockito.anyList(),
                Mockito.anyString()
        );
        Mockito.verify(paperChannelMock, Mockito.times(0)).send(Mockito.any(PaperChannelSendRequest.class));

        ConsoleAppenderCustom.checkLogs("Payment information is not valid");
    }

    @Test
    void validationPaymentInfoOK() {
        String iun = TestUtils.getRandomIun();

        String fileDocPayment = "keyPagoPaForm_doc00";
        List<NotificationDocumentInt> paymentDocuments = TestUtils.getDocumentList(fileDocPayment);
        List<TestUtils.DocumentWithContent> listPaymentDocumentWithContent = TestUtils.getDocumentWithContents(fileDocPayment, paymentDocuments);
        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        notificationDocumentList = TestUtils.firstFileUploadFromNotification(listDocumentWithContent,notificationDocumentList, safeStorageClientMock);
        paymentDocuments = TestUtils.firstFileUploadFromNotification(listPaymentDocumentWithContent, paymentDocuments, safeStorageClientMock);

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withPhysicalAddress(
                        PhysicalAddressBuilder.builder()
                                .withAddress(EXTCHANNEL_SEND_SUCCESS + "_Via Nuova")
                                .build()
                )
                .withPayments(Collections.singletonList(
                        NotificationPaymentInfoInt.builder()
                                .pagoPA(PagoPaInt.builder()
                                        .creditorTaxId("creditorTaxId_"+ UUID.randomUUID())
                                        .noticeCode("noticeCode_"+UUID.randomUUID())
                                        .applyCost(true)
                                        .attachment(paymentDocuments.get(0))
                                        .build())
                                .build()
                ))
                .build();


        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withIun(iun)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withPagoPaIntMode(PagoPaIntMode.ASYNC)
                .withPaFee(100)
                .withNotificationRecipient(recipient)
                .build();

        
        pnDeliveryClientMock.addNotification(notification);
        
        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        String timelineId = TimelineEventId.REQUEST_ACCEPTED.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .build()
        );

        await().untilAsserted(() ->
                Assertions.assertTrue(timelineService.getTimelineElement(iun, timelineId).isPresent())
        );
    }

    @Test
    void notificationDeliveryModeAsyncWithoutPayment() throws PnIdConflictException {
        // GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId("TAXID01")
                .withPayments(null)
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, notificationDocumentList, safeStorageClientMock);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withPagoPaIntMode(PagoPaIntMode.ASYNC)
                .withNotificationRecipient(recipient)
                .build();
        
        pnDeliveryClientMock.addNotification(notification);

        String iun = notification.getIun();
        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //WHEN the workflow start
        startWorkflowHandler.startWorkflow(iun);

        //THEN
        await().untilAsserted(() ->
                //Check worfklow is failed
                Assertions.assertTrue(timelineService.getTimelineElement(
                        iun,
                        TimelineEventId.REQUEST_REFUSED.buildEventId(
                                EventId.builder()
                                        .iun(iun)
                                        .recIndex(recIndex)
                                        .build())).isPresent()
                )
        );

        Mockito.verify(externalChannelMock, Mockito.times(0)).sendLegalNotification(
                Mockito.any(NotificationInt.class),
                Mockito.any(NotificationRecipientInt.class),
                Mockito.any(LegalDigitalAddressInt.class),
                Mockito.anyString(),
                Mockito.anyList(),
                Mockito.anyString()
        );
        Mockito.verify(paperChannelMock, Mockito.times(0)).send(Mockito.any(PaperChannelSendRequest.class));

        ConsoleAppenderCustom.checkLogs();
    }


}
