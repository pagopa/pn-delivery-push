package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ResponseStatusInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileCreationResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileCreationWithContentRequest;
import it.pagopa.pn.deliverypush.dto.legalfacts.PdfInfo;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactGenerator;
import it.pagopa.pn.deliverypush.service.SafeStorageService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class SaveLegalFactsServiceImplTest {

    private static final String SAVE_LEGAL_FACT_EXCEPTION_MESSAGE = "Generating %s legal fact for IUN=%s and recipientId=%s";
    public static final String LEGALFACTS_MEDIATYPE_STRING = "application/pdf";
    public static final String PN_LEGAL_FACTS = "PN_LEGAL_FACTS";
    public static final String SAVED = "SAVED";
    public static final String PN_AAR = "PN_AAR";

    @Mock
    private LegalFactGenerator legalFactBuilder;

    @Mock
    private SafeStorageService safeStorageService;

    private SaveLegalFactsServiceImpl saveLegalFactsService;

    @BeforeEach
    void setUp() {

        legalFactBuilder = Mockito.mock(LegalFactGenerator.class);
        safeStorageService = Mockito.mock(SafeStorageService.class);

        saveLegalFactsService = new SaveLegalFactsServiceImpl(legalFactBuilder, safeStorageService);
    }

    @Test
    void saveAAR() throws IOException {
        String denomination = "<h1>SSRF WITH IMAGE POC</h1> <img src='https://prova.it'></img>";
        NotificationInt notification = buildNotification(denomination);
        NotificationRecipientInt recipient = buildRecipient(denomination);
        FileCreationWithContentRequest fileCreation = buildFileCreationWithContentRequest(PN_AAR);
        FileCreationResponseInt file = buildFileCreationResponseInt();

        Mockito.when(legalFactBuilder.generateNotificationAAR(notification, recipient)).thenReturn(denomination.getBytes());
        Mockito.when(legalFactBuilder.getNumberOfPages(denomination.getBytes())).thenReturn(1);
        Mockito.when(safeStorageService.createAndUploadContent(fileCreation)).thenReturn(file);

        PdfInfo actual = saveLegalFactsService.saveAAR(notification, recipient);

        Assertions.assertAll(
                () -> Assertions.assertEquals("safestorage://001", actual.getKey()),
                () -> Assertions.assertEquals(1, actual.getNumberOfPages())
        );
    }

    @Test
    void saveAARFailed() {
        String denomination = "<h1>SSRF WITH IMAGE POC</h1> <img src='https://prova.it'></img>";
        NotificationInt notification = buildNotification(denomination);
        NotificationRecipientInt recipient = buildRecipient(denomination);
        FileCreationWithContentRequest fileCreation = buildFileCreationWithContentRequest(PN_AAR);
        FileCreationResponseInt file = buildFileCreationResponseInt();

        PnInternalException pnInternalException = Assertions.assertThrows(PnInternalException.class, () -> {
            saveLegalFactsService.saveAAR(notification, recipient);
        });

        String expectErrorMsg = "PN_DELIVERYPUSH_SAVELEGALFACTSFAILED";

        Assertions.assertEquals(expectErrorMsg, pnInternalException.getProblem().getErrors().get(0).getCode());

    }

    @Test
    void saveNotificationReceivedLegalFact() throws IOException {
        String denomination = "<h1>SSRF WITH IMAGE POC</h1> <img src='https://prova.it'></img>";
        NotificationInt notification = buildNotification(denomination);
        FileCreationWithContentRequest fileCreation = buildFileCreationWithContentRequest(PN_LEGAL_FACTS);
        FileCreationResponseInt file = buildFileCreationResponseInt();

        Mockito.when(legalFactBuilder.generateNotificationReceivedLegalFact(notification)).thenReturn(denomination.getBytes());
        Mockito.when(safeStorageService.createAndUploadContent(fileCreation)).thenReturn(file);

        String actual = saveLegalFactsService.saveNotificationReceivedLegalFact(notification);

        Assertions.assertEquals("safestorage://001", actual);
    }

    @Test
    void saveNotificationReceivedLegalFactFailed() {
        String denomination = "<h1>SSRF WITH IMAGE POC</h1> <img src='https://prova.it'></img>";
        NotificationInt notification = buildNotification(denomination);
        FileCreationWithContentRequest fileCreation = buildFileCreationWithContentRequest(PN_LEGAL_FACTS);
        FileCreationResponseInt file = buildFileCreationResponseInt();

        PnInternalException pnInternalException = Assertions.assertThrows(PnInternalException.class, () -> {
            saveLegalFactsService.saveNotificationReceivedLegalFact(notification);
        });

        String expectErrorMsg = "PN_DELIVERYPUSH_SAVELEGALFACTSFAILED";

        Assertions.assertEquals(expectErrorMsg, pnInternalException.getProblem().getErrors().get(0).getCode());
    }

    @Test
    void savePecDeliveryWorkflowLegalFact() throws IOException {
        SendDigitalFeedbackDetailsInt sdf = buildSendDigitalFeedbackDetailsInt();
        String denomination = "<h1>SSRF WITH IMAGE POC</h1> <img src='https://prova.it'></img>";
        List<SendDigitalFeedbackDetailsInt> listFeedbackFromExtChannel = Collections.singletonList(sdf);
        NotificationInt notification = buildNotification(denomination);
        NotificationRecipientInt recipient = buildRecipient(denomination);
        EndWorkflowStatus status = EndWorkflowStatus.SUCCESS;
        Instant completionWorkflowDate = Instant.parse("2021-09-16T15:24:00.00Z");
        FileCreationWithContentRequest fileCreation = buildFileCreationWithContentRequest(PN_LEGAL_FACTS);
        FileCreationResponseInt file = buildFileCreationResponseInt();

        Mockito.when(legalFactBuilder.generatePecDeliveryWorkflowLegalFact(
                listFeedbackFromExtChannel, notification, recipient, status, completionWorkflowDate)).thenReturn(denomination.getBytes());
        Mockito.when(safeStorageService.createAndUploadContent(fileCreation)).thenReturn(file);

        String actual = saveLegalFactsService.savePecDeliveryWorkflowLegalFact(listFeedbackFromExtChannel,
                notification, recipient, status, completionWorkflowDate);

        Assertions.assertEquals("safestorage://001", actual);
    }

    @Test
    void savePecDeliveryWorkflowLegalFactFailed() {
        SendDigitalFeedbackDetailsInt sdf = buildSendDigitalFeedbackDetailsInt();
        String denomination = "<h1>SSRF WITH IMAGE POC</h1> <img src='https://prova.it'></img>";
        List<SendDigitalFeedbackDetailsInt> listFeedbackFromExtChannel = Collections.singletonList(sdf);
        NotificationInt notification = buildNotification(denomination);
        NotificationRecipientInt recipient = buildRecipient(denomination);
        EndWorkflowStatus status = EndWorkflowStatus.SUCCESS;
        Instant completionWorkflowDate = Instant.parse("2021-09-16T15:24:00.00Z");
        FileCreationWithContentRequest fileCreation = buildFileCreationWithContentRequest(PN_LEGAL_FACTS);
        FileCreationResponseInt file = buildFileCreationResponseInt();

        PnInternalException pnInternalException = Assertions.assertThrows(PnInternalException.class, () -> {
            saveLegalFactsService.savePecDeliveryWorkflowLegalFact(listFeedbackFromExtChannel,
                    notification, recipient, status, completionWorkflowDate);
        });

        String expectErrorMsg = "PN_DELIVERYPUSH_SAVELEGALFACTSFAILED";

        Assertions.assertEquals(expectErrorMsg, pnInternalException.getProblem().getErrors().get(0).getCode());
    }

    @Test
    void saveNotificationViewedLegalFact() throws IOException {
        String denomination = "<h1>SSRF WITH IMAGE POC</h1> <img src='https://prova.it'></img>";
        NotificationInt notification = buildNotification(denomination);
        NotificationRecipientInt recipient = buildRecipient(denomination);
        Instant timeStamp = Instant.parse("2021-09-16T15:24:00.00Z");
        FileCreationWithContentRequest fileCreation = buildFileCreationWithContentRequest(PN_LEGAL_FACTS);
        FileCreationResponseInt file = buildFileCreationResponseInt();

        Mockito.when(legalFactBuilder.generateNotificationViewedLegalFact(
                notification.getIun(), recipient, timeStamp)).thenReturn(denomination.getBytes());
        Mockito.when(safeStorageService.createAndUploadContent(fileCreation)).thenReturn(file);

        String actual = saveLegalFactsService.saveNotificationViewedLegalFact(notification, recipient, timeStamp);

        Assertions.assertEquals("safestorage://001", actual);
    }

    @Test
    void saveNotificationViewedLegalFactFailed() {
        String denomination = "<h1>SSRF WITH IMAGE POC</h1> <img src='https://prova.it'></img>";
        NotificationInt notification = buildNotification(denomination);
        NotificationRecipientInt recipient = buildRecipient(denomination);
        Instant timeStamp = Instant.parse("2021-09-16T15:24:00.00Z");
        FileCreationWithContentRequest fileCreation = buildFileCreationWithContentRequest(PN_LEGAL_FACTS);
        FileCreationResponseInt file = buildFileCreationResponseInt();

        PnInternalException pnInternalException = Assertions.assertThrows(PnInternalException.class, () -> {
            saveLegalFactsService.saveNotificationViewedLegalFact(notification, recipient, timeStamp);
        });

        String expectErrorMsg = "PN_DELIVERYPUSH_SAVENOTIFICATIONFAILED";

        Assertions.assertEquals(expectErrorMsg, pnInternalException.getProblem().getErrors().get(0).getCode());
    }


    private SendDigitalFeedbackDetailsInt buildSendDigitalFeedbackDetailsInt() {
        return SendDigitalFeedbackDetailsInt.builder()
                .recIndex(0)
                .digitalAddress(LegalDigitalAddressInt.builder()
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                        .address("prova@test.com")
                        .build())
                .responseStatus(ResponseStatusInt.KO)
                .notificationDate(Instant.now())
                .build();
    }

    private FileCreationWithContentRequest buildFileCreationWithContentRequest(String type) {
        String denomination = "<h1>SSRF WITH IMAGE POC</h1> <img src='https://prova.it'></img>";

        FileCreationWithContentRequest fileCreationRequest = new FileCreationWithContentRequest();
        fileCreationRequest.setContentType(LEGALFACTS_MEDIATYPE_STRING);
        fileCreationRequest.setDocumentType(type);
        fileCreationRequest.setStatus(SAVED);
        fileCreationRequest.setContent(denomination.getBytes());
        return fileCreationRequest;
    }

    private FileCreationResponseInt buildFileCreationResponseInt() {
        return FileCreationResponseInt.builder()
                .key("001")
                .build();
    }

    private NotificationInt buildNotification(String denomination) {
        return NotificationInt.builder()
                .sender(createSender())
                .sentAt(Instant.now())
                .iun("Example_IUN_1234_Test")
                .subject("notification test subject")
                .documents(Arrays.asList(
                                NotificationDocumentInt.builder()
                                        .ref(NotificationDocumentInt.Ref.builder()
                                                .key("doc00")
                                                .versionToken("v01_doc00")
                                                .build()
                                        )
                                        .digests(NotificationDocumentInt.Digests.builder()
                                                .sha256((Base64Utils.encodeToString("sha256_doc01".getBytes())))
                                                .build()
                                        )
                                        .build()
                        )
                )
                .recipients(Collections.singletonList(buildRecipient(denomination)))
                .build();
    }

    private NotificationRecipientInt buildRecipient(String denomination) {
        String defaultDenomination = StringUtils.hasText(denomination) ? denomination : "Galileo Bruno";
        NotificationRecipientInt rec1 = NotificationRecipientInt.builder()
                .taxId("CDCFSC11R99X001Z")
                .denomination(defaultDenomination)
                .digitalDomicile(LegalDigitalAddressInt.builder()
                        .address("test@dominioPec.it")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                        .build())
                .physicalAddress(buildPhysicalAddressInt())
                .build();

        return rec1;
    }

    private PhysicalAddressInt buildPhysicalAddressInt() {
        return new PhysicalAddressInt(
                "Galileo Bruno",
                "Palazzo dell'Inquisizione",
                "corso Italia 666",
                "Piano Terra (piatta)",
                "00100",
                "Roma",
                null,
                "RM",
                "IT"
        );
    }

    private NotificationSenderInt createSender() {
        return NotificationSenderInt.builder()
                .paId("TEST_PA_ID")
                .paTaxId("TEST_TAX_ID")
                .paDenomination("TEST_PA_DENOMINATION")
                .build();
    }
}