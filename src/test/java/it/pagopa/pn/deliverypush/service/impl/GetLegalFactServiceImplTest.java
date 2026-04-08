package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileDownloadInfoInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileDownloadResponseInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdIntWithRecIndex;
import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.deliverypush.service.GetLegalFactService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SafeStorageService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.utils.AuthUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_NOTFOUND;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;

class GetLegalFactServiceImplTest {

    private static final String IUN = "fake_iun";
    private static final String TAX_ID = "tax_id";
    private static final String TAX_ID_2 = "tax_id_2";
    private static final String GENERAL_KEY_LEGALFACT = "GENERAL_KEY_LEGALFACT";
    private static final String RECIPIENT_1_KEY_LEGALFACT = "RECIPIENT_1_KEY_LEGALFACT";
    private static final String RECIPIENT_2_KEY_LEGALFACT = "RECIPIENT_2_KEY_LEGALFACT";

    private static final String LEGAL_FACT_ID = "LEGAL_FACT_ID";

    private TimelineService timelineService;
    private SafeStorageService safeStorageService;
    private NotificationService notificationService;
    private NotificationUtils notificationUtils;
    private AuthUtils authUtils;
    private GetLegalFactService getLegalFactService;
    private PnDeliveryPushConfigs cfg;

    @BeforeEach
    void setup() {
        timelineService = Mockito.mock( TimelineService.class );
        safeStorageService = Mockito.mock( SafeStorageServiceImpl.class );
        notificationService = Mockito.mock(NotificationService.class);
        notificationUtils = Mockito.mock(NotificationUtils.class);
        cfg = Mockito.mock(PnDeliveryPushConfigs.class);

        authUtils = Mockito.mock(AuthUtils.class);

        getLegalFactService = new GetLegalFactServiceImpl(
                timelineService,
                safeStorageService,
                notificationService,
                notificationUtils,
                authUtils,
                cfg);
    }

    @Test
    void getLegalFacts_nonPA_returnsFilteredLegalFacts() {
        List<LegalFactListElementV20> legalFactsExpectedResult = List.of(
                LegalFactListElementV20.builder()
                    .iun(IUN)
                    .taxId(null)
                    .legalFactsId(LegalFactsIdV20.builder()
                            .key(GENERAL_KEY_LEGALFACT)
                            .category(LegalFactCategoryV20.SENDER_ACK)
                            .build()
                    ).build(),
                LegalFactListElementV20.builder()
                        .iun(IUN)
                        .taxId(TAX_ID)
                        .legalFactsId(LegalFactsIdV20.builder()
                                .key(RECIPIENT_1_KEY_LEGALFACT)
                                .category(LegalFactCategoryV20.RECIPIENT_ACCESS)
                                .build()
                        ).build()
        );
        String mandateId = null;
        CxTypeAuthFleet cxType = CxTypeAuthFleet.PF;

        NotificationRecipientInt recipientInt = NotificationRecipientInt.builder()
                .taxId(TAX_ID)
                .internalId(TAX_ID + "ANON")
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(IUN)
                .withNotificationRecipient(recipientInt)
                .build();

        Mockito.when(notificationService.getNotificationByIun(anyString())).thenReturn(notification);
        Mockito.when(authUtils.checkUserPaAndMandateAuthorizationAndRetrieveRealRecipientId(any(), anyString(), any(), any(), any())).thenReturn(recipientInt.getInternalId());
        Mockito.when(notificationUtils.getRecipientIndexFromInternalId(any(), anyString())).thenReturn(0);
        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt()))
                .thenReturn(recipientInt);

        LegalFactsIdIntWithRecIndex notificationRelatedLegalFact = LegalFactsIdIntWithRecIndex.builder()
                .key(GENERAL_KEY_LEGALFACT)
                .category(LegalFactCategoryInt.SENDER_ACK)
                .build();
        LegalFactsIdIntWithRecIndex recipientRelatedLegalFact = LegalFactsIdIntWithRecIndex.builder()
                .key(RECIPIENT_1_KEY_LEGALFACT)
                .category(LegalFactCategoryInt.RECIPIENT_ACCESS)
                .recIndex(0)
                .build();
        Mockito.when(timelineService.getLegalFacts(IUN, 0)).thenReturn(List.of(notificationRelatedLegalFact, recipientRelatedLegalFact));

        List<LegalFactListElementV20> result = getLegalFactService.getLegalFacts(IUN, recipientInt.getInternalId(), mandateId, cxType, null);

        assertEquals(legalFactsExpectedResult, result);
    }

    @Test
    void getLegalFacts_PA_returnsAllLegalFacts() {
        List<LegalFactListElementV20> legalFactsExpectedResult = List.of(
                LegalFactListElementV20.builder()
                        .iun(IUN)
                        .taxId(null)
                        .legalFactsId(LegalFactsIdV20.builder()
                                .key(GENERAL_KEY_LEGALFACT)
                                .category(LegalFactCategoryV20.SENDER_ACK)
                                .build()
                        ).build(),
                LegalFactListElementV20.builder()
                        .iun(IUN)
                        .taxId(TAX_ID)
                        .legalFactsId(LegalFactsIdV20.builder()
                                .key(RECIPIENT_1_KEY_LEGALFACT)
                                .category(LegalFactCategoryV20.RECIPIENT_ACCESS)
                                .build()
                        ).build(),
                LegalFactListElementV20.builder()
                        .iun(IUN)
                        .taxId(TAX_ID_2)
                        .legalFactsId(LegalFactsIdV20.builder()
                                .key(RECIPIENT_2_KEY_LEGALFACT)
                                .category(LegalFactCategoryV20.RECIPIENT_ACCESS)
                                .build()
                        ).build()
        );
        String mandateId = null;
        CxTypeAuthFleet cxType = CxTypeAuthFleet.PA;

        NotificationRecipientInt recipient1Int = NotificationRecipientInt.builder()
                .taxId(TAX_ID)
                .internalId(TAX_ID + "ANON")
                .build();

        NotificationRecipientInt recipient2Int = NotificationRecipientInt.builder()
                .taxId(TAX_ID_2)
                .internalId(TAX_ID_2 + "ANON")
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(IUN)
                .withNotificationRecipients(List.of(recipient1Int, recipient2Int))
                .build();

        Mockito.when(notificationService.getNotificationByIun(anyString())).thenReturn(notification);
        Mockito.when(authUtils.checkUserPaAndMandateAuthorizationAndRetrieveRealRecipientId(any(), anyString(), any(), any(), any())).thenReturn(notification.getSender().getPaId());
        Mockito.when(notificationUtils.getRecipientIndexFromInternalId(notification, recipient1Int.getInternalId())).thenReturn(0);
        Mockito.when(notificationUtils.getRecipientIndexFromInternalId(notification, recipient2Int.getInternalId())).thenReturn(1);
        Mockito.when(notificationUtils.getRecipientFromIndex(notification, 0))
                .thenReturn(recipient1Int);
        Mockito.when(notificationUtils.getRecipientFromIndex(notification, 1))
                .thenReturn(recipient2Int);

        LegalFactsIdIntWithRecIndex notificationRelatedLegalFact = LegalFactsIdIntWithRecIndex.builder()
                .key(GENERAL_KEY_LEGALFACT)
                .category(LegalFactCategoryInt.SENDER_ACK)
                .build();
        LegalFactsIdIntWithRecIndex recipient1RelatedLegalFact = LegalFactsIdIntWithRecIndex.builder()
                .key(RECIPIENT_1_KEY_LEGALFACT)
                .category(LegalFactCategoryInt.RECIPIENT_ACCESS)
                .recIndex(0)
                .build();
        LegalFactsIdIntWithRecIndex recipient2RelatedLegalFact = LegalFactsIdIntWithRecIndex.builder()
                .key(RECIPIENT_2_KEY_LEGALFACT)
                .category(LegalFactCategoryInt.RECIPIENT_ACCESS)
                .recIndex(1)
                .build();
        Mockito.when(timelineService.getLegalFacts(IUN, null)).thenReturn(List.of(notificationRelatedLegalFact, recipient1RelatedLegalFact, recipient2RelatedLegalFact));

        List<LegalFactListElementV20> result = getLegalFactService.getLegalFacts(IUN, notification.getSender().getPaId(), mandateId, cxType, null);

        assertEquals(legalFactsExpectedResult, result);
    }

    @Test
    void getLegalFacts_exceptionThrown_logsAndPropagates() {
        String iun = "IUN";
        String senderReceiverId = "REC_ID";
        CxTypeAuthFleet cxType = CxTypeAuthFleet.PF;

        NotificationInt notification = NotificationInt.builder().iun(iun).build();

        Mockito.when(notificationService.getNotificationByIun(anyString())).thenReturn(notification);
        Mockito.when(authUtils.checkUserPaAndMandateAuthorizationAndRetrieveRealRecipientId(any(), anyString(), any(), any(), any())).thenReturn(senderReceiverId);
        Mockito.when(notificationUtils.getRecipientIndexFromInternalId(any(), anyString())).thenReturn(0);
        Mockito.when(timelineService.getLegalFacts(anyString(), any())).thenThrow(new RuntimeException("Test Exception"));

        assertThrows(RuntimeException.class, () ->
                getLegalFactService.getLegalFacts(iun, senderReceiverId, null, cxType, null)
        );
    }

    @Test
    void getLegalFactMetadataErrorCheckAuth() {
        //Given
        String[] urls = new String[1];
        try {
            Path path = Files.createTempFile(null, null);
            urls[0] = new File(path.toString()).toURI().toURL().toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileDownloadResponseInt fileDownloadResponse = new FileDownloadResponseInt();
        fileDownloadResponse.setContentType("application/pdf");
        fileDownloadResponse.setContentLength(new BigDecimal(0));
        fileDownloadResponse.setChecksum("123");
        fileDownloadResponse.setKey("123");
        fileDownloadResponse.setDownload(new FileDownloadInfoInt());
        fileDownloadResponse.getDownload().setUrl("https://www.url.qualcosa.it");
        fileDownloadResponse.getDownload().setRetryAfter(new BigDecimal(0));

        Mockito.when(safeStorageService.getFile(anyString(), eq(false), eq(true)))
                .thenReturn(Mono.just(fileDownloadResponse));

        NotificationInt notificationInt = newNotification();
        NotificationRecipientInt recipientInt = notificationInt.getRecipients().get(0);
        Mockito.when(notificationService.getNotificationByIun(anyString()))
                .thenReturn(notificationInt);

        doThrow(new PnNotFoundException("Not found", "", ERROR_CODE_DELIVERYPUSH_NOTFOUND)).when(authUtils).checkUserPaAndMandateAuthorization(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        //When
        Mono<LegalFactDownloadMetadataResponse> resultMono = getLegalFactService.getLegalFactMetadata(IUN, LegalFactCategory.RECIPIENT_ACCESS, LEGAL_FACT_ID, recipientInt.getInternalId(), null, CxTypeAuthFleet.PF, null);
        //Then
        assertThrows(PnNotFoundException.class, resultMono::block);
    }
    
    @Test
    void getAnalogLegalFactMetadataSuccess() {
        //Given
        String[] urls = new String[1];
        try {
            Path path = Files.createTempFile(null, null);
            urls[0] = new File(path.toString()).toURI().toURL().toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileDownloadResponseInt fileDownloadResponse = new FileDownloadResponseInt();
        fileDownloadResponse.setContentType("application/pdf");
        fileDownloadResponse.setContentLength(new BigDecimal(0));
        fileDownloadResponse.setChecksum("123");
        fileDownloadResponse.setKey("123");
        fileDownloadResponse.setDownload(new FileDownloadInfoInt());
        fileDownloadResponse.getDownload().setUrl("https://www.url.qualcosa.it");
        fileDownloadResponse.getDownload().setRetryAfter(new BigDecimal(0));

        //When
        Mockito.when(safeStorageService.getFile(anyString(), eq(false), eq(true)))
                .thenReturn(Mono.just(fileDownloadResponse));

        NotificationInt notificationInt = newNotification();
        NotificationRecipientInt recipientInt = notificationInt.getRecipients().get(0);
        Mockito.when(notificationService.getNotificationByIun(anyString()))
                .thenReturn(notificationInt);

        Mono<LegalFactDownloadMetadataResponse> resultMono = getLegalFactService.getLegalFactMetadata(IUN, LegalFactCategory.RECIPIENT_ACCESS, LEGAL_FACT_ID, recipientInt.getInternalId(), null, CxTypeAuthFleet.PF, null);

        //Then
        assertNotNull( resultMono );
        LegalFactDownloadMetadataResponse result = resultMono.block();
        assertNotNull(result);
        assertNotNull(result.getFilename());
        assertEquals(fileDownloadResponse.getDownload().getUrl(), result.getUrl());
        assertEquals(fileDownloadResponse.getDownload().getRetryAfter(), result.getRetryAfter());
        assertEquals(fileDownloadResponse.getContentLength(), result.getContentLength());
    }
    
    @Test
    void getAnalogLegalFactMetadataWithContentTypeSuccess() {
        //Given
        String[] urls = new String[1];
        try {
            Path path = Files.createTempFile(null, null);
            urls[0] = new File(path.toString()).toURI().toURL().toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileDownloadResponseInt fileDownloadResponse = new FileDownloadResponseInt();
        fileDownloadResponse.setContentType("application/pdf");
        fileDownloadResponse.setContentLength(new BigDecimal(0));
        fileDownloadResponse.setChecksum("123");
        fileDownloadResponse.setKey("123");
        fileDownloadResponse.setDownload(new FileDownloadInfoInt());
        fileDownloadResponse.getDownload().setUrl("https://www.url.qualcosa.it");
        fileDownloadResponse.getDownload().setRetryAfter(new BigDecimal(0));

        //When
        Mockito.when(safeStorageService.getFile(anyString(), eq(false), eq(true)))
                .thenReturn(Mono.just(fileDownloadResponse));

        NotificationInt notificationInt = newNotification();
        NotificationRecipientInt recipientInt = notificationInt.getRecipients().get(0);
        Mockito.when(notificationService.getNotificationByIun(anyString()))
                .thenReturn(notificationInt);

        Mono<LegalFactDownloadMetadataWithContentTypeResponse> resultMono = getLegalFactService.getLegalFactMetadataWithContentType(IUN, LEGAL_FACT_ID, recipientInt.getInternalId(), null, CxTypeAuthFleet.PF, null);

        //Then
        assertNotNull( resultMono );
        LegalFactDownloadMetadataWithContentTypeResponse result = resultMono.block();
        assertNotNull(result);
        assertNotNull(result.getFilename());
        assertEquals(fileDownloadResponse.getDownload().getUrl(), result.getUrl());
        assertEquals(fileDownloadResponse.getDownload().getRetryAfter(), result.getRetryAfter());
        assertEquals(fileDownloadResponse.getContentLength(), result.getContentLength());
    }

    @Test
    void getAnalogLegalFactMetadata_WithNumberOfPagesAsTag_Success() {
        //Given
        String[] urls = new String[1];
        try {
            Path path = Files.createTempFile(null, null);
            urls[0] = new File(path.toString()).toURI().toURL().toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileDownloadResponseInt fileDownloadResponse = new FileDownloadResponseInt();
        fileDownloadResponse.setContentType("application/pdf");
        fileDownloadResponse.setContentLength(new BigDecimal(0));
        fileDownloadResponse.setChecksum("123");
        fileDownloadResponse.setKey("123");
        fileDownloadResponse.setDownload(new FileDownloadInfoInt());
        fileDownloadResponse.getDownload().setUrl("https://www.url.qualcosa.it");
        fileDownloadResponse.getDownload().setRetryAfter(new BigDecimal(0));
        String tagKey = "document_number_of_pages";
        fileDownloadResponse.setTags(Map.of(tagKey, List.of("5")));

        //When
        Mockito.when(cfg.getDocumentNumberOfPagesTagKey()).thenReturn(tagKey);
        Mockito.when(safeStorageService.getFile(anyString(), eq(false), eq(true)))
                .thenReturn(Mono.just(fileDownloadResponse));

        NotificationInt notificationInt = newNotification();
        NotificationRecipientInt recipientInt = notificationInt.getRecipients().get(0);
        Mockito.when(notificationService.getNotificationByIun(anyString()))
                .thenReturn(notificationInt);

        Mono<LegalFactDownloadMetadataWithContentTypeResponse> resultMono = getLegalFactService.getLegalFactMetadataWithContentType(IUN, LEGAL_FACT_ID, recipientInt.getInternalId(), null, CxTypeAuthFleet.PF, null);

        //Then
        assertNotNull( resultMono );
        LegalFactDownloadMetadataWithContentTypeResponse result = resultMono.block();
        assertNotNull(result);
        assertNotNull(result.getFilename());
        assertEquals(5, result.getNumberOfPages());
        assertEquals(fileDownloadResponse.getDownload().getUrl(), result.getUrl());
        assertEquals(fileDownloadResponse.getDownload().getRetryAfter(), result.getRetryAfter());
        assertEquals(fileDownloadResponse.getContentLength(), result.getContentLength());
    }

    @Test
    void getAnalogLegalFactMetadata_WithInvalidNumberOfPagesFormat() {
        //Given
        String[] urls = new String[1];
        try {
            Path path = Files.createTempFile(null, null);
            urls[0] = new File(path.toString()).toURI().toURL().toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileDownloadResponseInt fileDownloadResponse = new FileDownloadResponseInt();
        fileDownloadResponse.setContentType("application/pdf");
        fileDownloadResponse.setContentLength(new BigDecimal(0));
        fileDownloadResponse.setChecksum("123");
        fileDownloadResponse.setKey("123");
        fileDownloadResponse.setDownload(new FileDownloadInfoInt());
        fileDownloadResponse.getDownload().setUrl("https://www.url.qualcosa.it");
        fileDownloadResponse.getDownload().setRetryAfter(new BigDecimal(0));
        String tagKey = "document_number_of_pages";
        fileDownloadResponse.setTags(Map.of(tagKey, List.of("invalidFormat")));

        //When
        Mockito.when(cfg.getDocumentNumberOfPagesTagKey()).thenReturn(tagKey);
        Mockito.when(safeStorageService.getFile(anyString(), eq(false), eq(true)))
                .thenReturn(Mono.just(fileDownloadResponse));

        NotificationInt notificationInt = newNotification();
        NotificationRecipientInt recipientInt = notificationInt.getRecipients().get(0);
        Mockito.when(notificationService.getNotificationByIun(anyString()))
                .thenReturn(notificationInt);

        Mono<LegalFactDownloadMetadataWithContentTypeResponse> resultMono = getLegalFactService.getLegalFactMetadataWithContentType(IUN, LEGAL_FACT_ID, recipientInt.getInternalId(), null, CxTypeAuthFleet.PF, null);

        //Then
        assertNotNull( resultMono );
        LegalFactDownloadMetadataWithContentTypeResponse result = resultMono.block();
        assertNotNull(result);
        assertNotNull(result.getFilename());
        assertNull(result.getNumberOfPages());
        assertEquals(fileDownloadResponse.getDownload().getUrl(), result.getUrl());
        assertEquals(fileDownloadResponse.getDownload().getRetryAfter(), result.getRetryAfter());
        assertEquals(fileDownloadResponse.getContentLength(), result.getContentLength());
    }

    @Test
    void getAnalogLegalFactMetadataSuccessXML() {
        //Given
        String[] urls = new String[1];
        try {
            Path path = Files.createTempFile(null, null);
            urls[0] = new File(path.toString()).toURI().toURL().toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileDownloadResponseInt fileDownloadResponse = new FileDownloadResponseInt();
        fileDownloadResponse.setContentType("application/xml");
        fileDownloadResponse.setContentLength(new BigDecimal(0));
        fileDownloadResponse.setChecksum("123");
        fileDownloadResponse.setKey("123");
        fileDownloadResponse.setDownload(new FileDownloadInfoInt());
        fileDownloadResponse.getDownload().setUrl("https://www.url.qualcosa.it");
        fileDownloadResponse.getDownload().setRetryAfter(new BigDecimal(0));

        //When
        Mockito.when(safeStorageService.getFile(anyString(), eq(false), eq(true)))
                .thenReturn(Mono.just(fileDownloadResponse));

        NotificationInt notificationInt = newNotification();
        NotificationRecipientInt recipientInt = notificationInt.getRecipients().get(0);
        Mockito.when(notificationService.getNotificationByIun(anyString()))
                .thenReturn(notificationInt);

        Mono<LegalFactDownloadMetadataResponse> resultMono = getLegalFactService.getLegalFactMetadata(IUN, LegalFactCategory.RECIPIENT_ACCESS, LEGAL_FACT_ID, recipientInt.getInternalId(), null, CxTypeAuthFleet.PF, null);
        //Then
        assertNotNull( resultMono );
        LegalFactDownloadMetadataResponse result = resultMono.block();
        assertNotNull(result);
        assertNotNull(result.getFilename());
        assertEquals("xml", result.getFilename().substring(result.getFilename().length() - 3));
        assertEquals(fileDownloadResponse.getDownload().getUrl(), result.getUrl());
        assertEquals(fileDownloadResponse.getDownload().getRetryAfter(), result.getRetryAfter());
        assertEquals(fileDownloadResponse.getContentLength(), result.getContentLength());
    }

    @Test
    void getAnalogLegalFactMetadataNoCategorySuccessXML() {
        //Given
        String[] urls = new String[1];
        try {
            Path path = Files.createTempFile(null, null);
            urls[0] = new File(path.toString()).toURI().toURL().toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileDownloadResponseInt fileDownloadResponse = new FileDownloadResponseInt();
        fileDownloadResponse.setContentType("application/xml");
        fileDownloadResponse.setContentLength(new BigDecimal(0));
        fileDownloadResponse.setChecksum("123");
        fileDownloadResponse.setKey("123");
        fileDownloadResponse.setDownload(new FileDownloadInfoInt());
        fileDownloadResponse.getDownload().setUrl("https://www.url.qualcosa.it");
        fileDownloadResponse.getDownload().setRetryAfter(new BigDecimal(0));

        //When
        Mockito.when(safeStorageService.getFile(anyString(), eq(false), eq(true)))
                .thenReturn(Mono.just(fileDownloadResponse));

        NotificationInt notificationInt = newNotification();
        NotificationRecipientInt recipientInt = notificationInt.getRecipients().get(0);
        Mockito.when(notificationService.getNotificationByIun(anyString()))
                .thenReturn(notificationInt);

        Mono<LegalFactDownloadMetadataResponse> resultMono = getLegalFactService.getLegalFactMetadata(IUN, null, LEGAL_FACT_ID, recipientInt.getInternalId(), null, CxTypeAuthFleet.PF, null);
        //Then
        assertNotNull( resultMono );
        LegalFactDownloadMetadataResponse result = resultMono.block();
        assertNotNull(result);
        assertNotNull(result.getFilename());
        assertEquals("xml", result.getFilename().substring(result.getFilename().length() - 3));
        assertEquals(fileDownloadResponse.getDownload().getUrl(), result.getUrl());
        assertEquals(fileDownloadResponse.getDownload().getRetryAfter(), result.getRetryAfter());
        assertEquals(fileDownloadResponse.getContentLength(), result.getContentLength());
    }

    private NotificationInt newNotification() {
        return NotificationInt.builder()
                .iun("IUN_01")
                .paProtocolNumber("protocol_01")
                .sender(NotificationSenderInt.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipientInt.builder()
                                .taxId(TAX_ID)
                                .internalId(TAX_ID +"ANON")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(LegalDigitalAddressInt.builder()
                                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .build()
                ))
                .build();
    }


    public byte[] readByte(InputStream inputStream) throws IOException {
        byte[] array = new byte[inputStream.available()];
        inputStream.read(array);

        return array;
    }
}
