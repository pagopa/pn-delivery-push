package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileDownloadInfoInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileDownloadResponseInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.GetAddressInfoDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationRequestAcceptedDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationViewedDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
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
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_NOTFOUND;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;

class GetLegalFactServiceImplTest {

    private static final String IUN = "fake_iun";
    private static final int REC_INDEX = 0;
    private static final String TAX_ID = "tax_id";
    private static final String KEY = "key";
    public static final String VERSION_TOKEN = "VERSION_TOKEN";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final long CONTENT_LENGTH = 0L;
    private static final String LEGAL_FACT_ID = "LEGAL_FACT_ID";

    private TimelineService timelineService;
    private SafeStorageService safeStorageService;
    private NotificationService notificationService;
    private NotificationUtils notificationUtils;
    private AuthUtils authUtils;
    private GetLegalFactService getLegalFactService;

    @BeforeEach
    void setup() {
        timelineService = Mockito.mock( TimelineService.class );
        safeStorageService = Mockito.mock( SafeStorageServiceImpl.class );
        notificationService = Mockito.mock(NotificationService.class);
        notificationUtils = Mockito.mock(NotificationUtils.class);

        authUtils = Mockito.mock(AuthUtils.class);

        getLegalFactService = new GetLegalFactServiceImpl(
                timelineService,
                safeStorageService,
                notificationService,
                notificationUtils,
                authUtils);
    }

    @Test
    void getLegalFactsSuccess() {
        List<LegalFactListElementV28> legalFactsExpectedResult = Collections.singletonList(LegalFactListElementV28.builder()
                .iun(IUN)
                .taxId(TAX_ID)
                .legalFactsId(LegalFactsIdV28.builder()
                        .key(KEY)
                        .category(LegalFactCategoryV28.SENDER_ACK)
                        .build()
                ).build()
        );

        Set<TimelineElementInternal> timelineElementsResult = Collections.singleton(TimelineElementInternal.builder()
                .iun(IUN)
                .details(GetAddressInfoDetailsInt.builder()
                        .recIndex(0)
                        .build())
                .category(TimelineElementCategoryInt.GET_ADDRESS)
                .elementId("element_id")
                .legalFactsIds(Collections.singletonList(LegalFactsIdInt.builder()
                        .key(KEY)
                        .category(LegalFactCategoryInt.SENDER_ACK)
                        .build())
                ).build()
        );

        Mockito.when(timelineService.getTimeline(anyString(), anyBoolean()))
                .thenReturn(timelineElementsResult);

        NotificationRecipientInt recipientInt = NotificationRecipientInt.builder()
                .taxId(TAX_ID)
                .internalId(TAX_ID + "ANON")
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(IUN)
                .withNotificationRecipient(recipientInt)
                .build();

        Mockito.when(notificationService.getNotificationByIun(anyString()))
                .thenReturn(notification);

        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt()))
                .thenReturn(recipientInt);

        Mockito.when(authUtils.checkUserPaAndMandateAuthorizationAndRetrieveRealRecipientId(Mockito.any(NotificationInt.class), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(TAX_ID + "ANON");

        List<LegalFactListElementV28> result = getLegalFactService.getLegalFacts(IUN, recipientInt.getInternalId(), null, CxTypeAuthFleet.PF, null);

        assertEquals(legalFactsExpectedResult, result);
    }

    @Test
    void getLegalFactsSuccessFilteredPF() {
        List<LegalFactListElementV28> legalFactsExpectedResult = List.of(
                LegalFactListElementV28.builder()
                        .iun(IUN)
                        .legalFactsId(LegalFactsIdV28.builder()
                                .key(KEY+"all")
                                .category(LegalFactCategoryV28.SENDER_ACK)
                                .build()
                        ).build(),
                LegalFactListElementV28.builder()
                        .iun(IUN)
                        .taxId(TAX_ID)
                        .legalFactsId(LegalFactsIdV28.builder()
                                .key(KEY)
                                .category(LegalFactCategoryV28.RECIPIENT_ACCESS)
                                .build()
                        ).build()
        );

        Set<TimelineElementInternal> timelineElementsResult = Set.of(TimelineElementInternal.builder()
                .iun(IUN)
                .details(NotificationViewedDetailsInt.builder()
                        .recIndex(0)
                        .build())
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED)
                .elementId("element_id")
                .timestamp(Instant.EPOCH.plusMillis(100))
                .legalFactsIds(Collections.singletonList(LegalFactsIdInt.builder()
                        .key(KEY)
                        .category(LegalFactCategoryInt.RECIPIENT_ACCESS)
                        .build())
                ).build(),
                TimelineElementInternal.builder()
                        .iun(IUN)
                        .details(GetAddressInfoDetailsInt.builder()
                                .recIndex(1)
                                .build())
                        .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED)
                        .elementId("element_id")
                        .timestamp(Instant.EPOCH.plusMillis(10))
                        .legalFactsIds(Collections.singletonList(LegalFactsIdInt.builder()
                                .key(KEY+"1")
                                .category(LegalFactCategoryInt.RECIPIENT_ACCESS)
                                .build())
                        ).build(),
                TimelineElementInternal.builder()
                        .iun(IUN)
                        .details(NotificationRequestAcceptedDetailsInt.builder()
                                .build())
                        .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                        .elementId("element_id")
                        .timestamp(Instant.EPOCH.plusMillis(1))
                        .legalFactsIds(Collections.singletonList(LegalFactsIdInt.builder()
                                .key(KEY+"all")
                                .category(LegalFactCategoryInt.SENDER_ACK)
                                .build())
                        ).build()
        );

        Mockito.when(timelineService.getTimeline(anyString(), anyBoolean()))
                .thenReturn(timelineElementsResult);

        NotificationRecipientInt recipientInt = NotificationRecipientInt.builder()
                .taxId(TAX_ID)
                .internalId(TAX_ID + "ANON")
                .build();
        NotificationRecipientInt recipientInt1 = NotificationRecipientInt.builder()
                .taxId(TAX_ID+"1")
                .internalId(TAX_ID + "ANON1")
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(IUN)
                .withNotificationRecipients(List.of(recipientInt, recipientInt1))
                .build();

        Mockito.when(notificationService.getNotificationByIun(anyString()))
                .thenReturn(notification);

        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt()))
                .thenReturn(recipientInt);

        Mockito.when(authUtils.checkUserPaAndMandateAuthorizationAndRetrieveRealRecipientId(Mockito.any(NotificationInt.class), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(TAX_ID + "ANON");


        List<LegalFactListElementV28> result = getLegalFactService.getLegalFacts(IUN, recipientInt.getInternalId(), null, CxTypeAuthFleet.PF, null);

        assertEquals(legalFactsExpectedResult, result);
    }

    @Test
    void getLegalFactsSuccessFilteredPA() {
        List<LegalFactListElementV28> legalFactsExpectedResult = List.of(
                LegalFactListElementV28.builder()
                        .iun(IUN)
                        .legalFactsId(LegalFactsIdV28.builder()
                                .key(KEY+"all")
                                .category(LegalFactCategoryV28.SENDER_ACK)
                                .build()
                        ).build(),
                LegalFactListElementV28.builder()
                        .iun(IUN)
                        .taxId(TAX_ID)
                        .legalFactsId(LegalFactsIdV28.builder()
                                .key(KEY+"1")
                                .category(LegalFactCategoryV28.RECIPIENT_ACCESS)
                                .build()
                        ).build(),
                LegalFactListElementV28.builder()
                        .iun(IUN)
                        .taxId(TAX_ID)
                        .legalFactsId(LegalFactsIdV28.builder()
                                .key(KEY)
                                .category(LegalFactCategoryV28.RECIPIENT_ACCESS)
                                .build()
                        ).build()
        );

        Set<TimelineElementInternal> timelineElementsResult = Set.of(TimelineElementInternal.builder()
                        .iun(IUN)
                        .details(NotificationViewedDetailsInt.builder()
                                .recIndex(0)
                                .build())
                        .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED)
                        .elementId("element_id")
                        .timestamp(Instant.EPOCH.plusMillis(100))
                        .legalFactsIds(Collections.singletonList(LegalFactsIdInt.builder()
                                .key(KEY)
                                .category(LegalFactCategoryInt.RECIPIENT_ACCESS)
                                .build())
                        ).build(),
                TimelineElementInternal.builder()
                        .iun(IUN)
                        .details(GetAddressInfoDetailsInt.builder()
                                .recIndex(1)
                                .build())
                        .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED)
                        .elementId("element_id")
                        .timestamp(Instant.EPOCH.plusMillis(10))
                        .legalFactsIds(Collections.singletonList(LegalFactsIdInt.builder()
                                .key(KEY+"1")
                                .category(LegalFactCategoryInt.RECIPIENT_ACCESS)
                                .build())
                        ).build(),
                TimelineElementInternal.builder()
                        .iun(IUN)
                        .details(NotificationRequestAcceptedDetailsInt.builder()
                                .build())
                        .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                        .elementId("element_id")
                        .timestamp(Instant.EPOCH.plusMillis(1))
                        .legalFactsIds(Collections.singletonList(LegalFactsIdInt.builder()
                                .key(KEY+"all")
                                .category(LegalFactCategoryInt.SENDER_ACK)
                                .build())
                        ).build()
        );

        Mockito.when(timelineService.getTimeline(anyString(), anyBoolean()))
                .thenReturn(timelineElementsResult);

        NotificationRecipientInt recipientInt = NotificationRecipientInt.builder()
                .taxId(TAX_ID)
                .internalId(TAX_ID + "ANON")
                .build();
        NotificationRecipientInt recipientInt1 = NotificationRecipientInt.builder()
                .taxId(TAX_ID+"1")
                .internalId(TAX_ID + "ANON1")
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(IUN)
                .withNotificationRecipients(List.of(recipientInt, recipientInt1))
                .build();

        Mockito.when(notificationService.getNotificationByIun(anyString()))
                .thenReturn(notification);

        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt()))
                .thenReturn(recipientInt);

        Mockito.when(authUtils.checkUserPaAndMandateAuthorizationAndRetrieveRealRecipientId(Mockito.any(NotificationInt.class), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn("PARECIPIENTID");

        List<LegalFactListElementV28> result = getLegalFactService.getLegalFacts(IUN, recipientInt.getInternalId(), null, CxTypeAuthFleet.PA, null);

        assertEquals(legalFactsExpectedResult, result);
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

        Mockito.when(safeStorageService.getFile(anyString(), eq(false)))
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
        Mockito.when(safeStorageService.getFile(anyString(), eq(false)))
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
        Mockito.when(safeStorageService.getFile(anyString(), eq(false)))
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
        Mockito.when(safeStorageService.getFile(anyString(), eq(false)))
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
        Mockito.when(safeStorageService.getFile(anyString(), eq(false)))
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
