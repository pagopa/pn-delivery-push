package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileCreationResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileCreationWithContentRequest;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactGenerator;
import it.pagopa.pn.deliverypush.service.SafeStorageService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;

class SaveLegalFactsServiceImplTest {

    public static final String LEGALFACTS_MEDIATYPE_STRING = "application/pdf";
    public static final String PN_LEGAL_FACTS = "PN_LEGAL_FACTS";
    public static final String SAVED = "SAVED";

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
    void saveNotificationViewedLegalFact() throws IOException {
        String denomination = "<h1>SSRF WITH IMAGE POC</h1> <img src='https://prova.it'></img>";
        NotificationInt notification = buildNotification(denomination);
        NotificationRecipientInt recipient = buildRecipient(denomination);
        Instant timeStamp = Instant.parse("2021-09-16T15:24:00.00Z");
        FileCreationWithContentRequest fileCreation = buildFileCreationWithContentRequest();
        FileCreationResponseInt file = buildFileCreationResponseInt();

        Mockito.when(legalFactBuilder.generateNotificationViewedLegalFact(
                notification.getIun(), recipient, null, timeStamp, notification)).thenReturn(denomination.getBytes());
        Mockito.when(safeStorageService.createAndUploadContent(fileCreation)).thenReturn(Mono.just(file));

        Mono<String> actualMono = saveLegalFactsService.sendCreationRequestForNotificationViewedLegalFact(notification, recipient, null, timeStamp);

        Assertions.assertEquals("safestorage://001", actualMono.block());
    }

    @Test
    void saveNotificationDelegateViewedLegalFact() throws IOException {
        String denomination = "<h1>SSRF WITH IMAGE POC</h1> <img src='https://prova.it'></img>";
        NotificationInt notification = buildNotification(denomination);
        NotificationRecipientInt recipient = buildRecipient(denomination);
        DelegateInfoInt delegateInfo = DelegateInfoInt.builder()
                .denomination("Mario Rossi")
                .taxId("RSSMRA80A01H501U")
                .build();
        Instant timeStamp = Instant.parse("2021-09-16T15:24:00.00Z");
        FileCreationWithContentRequest fileCreation = buildFileCreationWithContentRequest();
        FileCreationResponseInt file = buildFileCreationResponseInt();

        Mockito.when(legalFactBuilder.generateNotificationViewedLegalFact(
                notification.getIun(), recipient, delegateInfo, timeStamp, notification)).thenReturn(denomination.getBytes());
        Mockito.when(safeStorageService.createAndUploadContent(fileCreation)).thenReturn(Mono.just(file));

        Mono<String> actualMono = saveLegalFactsService.sendCreationRequestForNotificationViewedLegalFact(notification, recipient, delegateInfo, timeStamp);

        Assertions.assertEquals("safestorage://001", actualMono.block());
    }


    private FileCreationWithContentRequest buildFileCreationWithContentRequest() {
        String denomination = "<h1>SSRF WITH IMAGE POC</h1> <img src='https://prova.it'></img>";

        FileCreationWithContentRequest fileCreationRequest = new FileCreationWithContentRequest();
        fileCreationRequest.setContentType(LEGALFACTS_MEDIATYPE_STRING);
        fileCreationRequest.setDocumentType(SaveLegalFactsServiceImplTest.PN_LEGAL_FACTS);
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
                .documents(Collections.singletonList(
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

        return NotificationRecipientInt.builder()
                .taxId("CDCFSC11R99X001Z")
                .denomination(defaultDenomination)
                .digitalDomicile(LegalDigitalAddressInt.builder()
                        .address("test@dominioPec.it")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                        .build())
                .physicalAddress(buildPhysicalAddressInt())
                .build();
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
