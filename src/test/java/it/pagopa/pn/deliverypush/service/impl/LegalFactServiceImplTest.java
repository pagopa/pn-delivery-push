package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadInfo;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.deliverypush.action2.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.externalclient.pnclient.safestorage.PnSafeStorageClient;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.deliverypush.service.LegalFactService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.utils.AuthUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LegalFactServiceImplTest {

    private static final String IUN = "fake_iun";
    private static final int REC_INDEX = 0;
    private static final String TAX_ID = "tax_id";
    private static final String KEY = "key";
    public static final String VERSION_TOKEN = "VERSION_TOKEN";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final long CONTENT_LENGTH = 0L;
    private static final String LEGAL_FACT_ID = "LEGAL_FACT_ID";

    private TimelineService timelineService;
    private PnSafeStorageClient safeStorageClient;
    private NotificationService notificationService;
    private NotificationUtils notificationUtils;
    private AuthUtils authUtils;
    private LegalFactService legalFactService;

    @BeforeEach
    void setup() {
        timelineService = Mockito.mock( TimelineService.class );
        safeStorageClient = Mockito.mock( PnSafeStorageClient.class );
        notificationService = Mockito.mock(NotificationService.class);
        notificationUtils = Mockito.mock(NotificationUtils.class);
        authUtils = Mockito.mock(AuthUtils.class);

        legalFactService = new LegalFactServiceImpl(
                timelineService,
                safeStorageClient,
                notificationService,
                notificationUtils,
                authUtils);
    }



    @Test
    void getLegalFactsSuccess() {
        List<LegalFactListElement> legalFactsExpectedResult = Collections.singletonList( LegalFactListElement.builder()
                .iun( IUN )
                .taxId( TAX_ID )
                .legalFactsId( LegalFactsId.builder()
                        .key( KEY )
                        .category( LegalFactCategory.SENDER_ACK )
                        .build()
                ).build()
        );
        
        Set<TimelineElementInternal> timelineElementsResult = Collections.singleton( TimelineElementInternal.timelineInternalBuilder()
                .iun( IUN )
                .details( TimelineElementDetails.builder()
                        .recIndex(0)
                        .build() )
                .category( TimelineElementCategory.REQUEST_ACCEPTED )
                .elementId( "element_id" )
                .legalFactsIds( Collections.singletonList( LegalFactsId.builder()
                                .key( KEY )
                                .category( LegalFactCategory.SENDER_ACK )
                        .build())
                ).build()
        );

        Mockito.when( timelineService.getTimeline( Mockito.anyString() ) )
                .thenReturn( timelineElementsResult );
        Mockito.when( notificationService.getNotificationByIun( Mockito.anyString() ) )
                .thenReturn( newNotification() );

        NotificationRecipientInt recipientInt = NotificationRecipientInt.builder()
                .taxId(TAX_ID)
                .build();
        
        Mockito.when( notificationUtils.getRecipientFromIndex( Mockito.any(NotificationInt.class), Mockito.anyInt() ) )
                .thenReturn( recipientInt );
        

        List<LegalFactListElement> result = legalFactService.getLegalFacts( IUN , "taxId", null );

        assertEquals( legalFactsExpectedResult, result );
    }

    @Test
    void getLegalFactSuccess() {
        //Given

        FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
        fileDownloadResponse.setContentType("application/pdf");
        fileDownloadResponse.setContentLength(new BigDecimal(0));
        fileDownloadResponse.setChecksum("123");
        fileDownloadResponse.setKey("123");
        fileDownloadResponse.setDownload(new FileDownloadInfo());
        fileDownloadResponse.getDownload().setUrl("https://www.url.qualcosa.it");
        fileDownloadResponse.getDownload().setRetryAfter(new BigDecimal(0));

        //When
        Mockito.when( safeStorageClient.getFile( Mockito.anyString(), Mockito.eq(false) ) )
                .thenReturn( fileDownloadResponse );
        Mockito.when( notificationService.getNotificationByIun( Mockito.anyString() ) )
                .thenReturn( newNotification() );

        ResponseEntity<Resource> result = legalFactService.getLegalfact( IUN, LegalFactCategory.RECIPIENT_ACCESS, LEGAL_FACT_ID);
        //Then
        assertNotNull( result );
    }


    @Test
    void getLegalFactBadUrl() {
        //Given

        FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
        fileDownloadResponse.setContentType("application/pdf");
        fileDownloadResponse.setContentLength(new BigDecimal(0));
        fileDownloadResponse.setChecksum("123");
        fileDownloadResponse.setKey("123");
        fileDownloadResponse.setDownload(new FileDownloadInfo());
        fileDownloadResponse.getDownload().setUrl("error not a url");
        fileDownloadResponse.getDownload().setRetryAfter(new BigDecimal(0));

        //When
        Mockito.when( safeStorageClient.getFile( Mockito.anyString(), Mockito.eq(false) ) )
                .thenReturn( fileDownloadResponse );
        Mockito.when( notificationService.getNotificationByIun( Mockito.anyString() ) )
                .thenReturn( newNotification() );

        assertThrows(PnInternalException.class, () -> legalFactService.getLegalfact( IUN, LegalFactCategory.RECIPIENT_ACCESS, LEGAL_FACT_ID));
    }

    @Test
    void getAnalogLegalFactSuccess() {
        //Given
        NotificationAttachment.Ref ref = NotificationAttachment.Ref.builder()
                .key( KEY )
                .versionToken( VERSION_TOKEN )
                .build();


        String[] urls = new String[1];
        try {
            Path path = Files.createTempFile( null,null );
            urls[0] =  new File(path.toString()).toURI().toURL().toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
        fileDownloadResponse.setContentType("application/pdf");
        fileDownloadResponse.setContentLength(new BigDecimal(0));
        fileDownloadResponse.setChecksum("123");
        fileDownloadResponse.setKey("123");
        fileDownloadResponse.setDownload(new FileDownloadInfo());
        fileDownloadResponse.getDownload().setUrl("https://www.url.qualcosa.it");
        fileDownloadResponse.getDownload().setRetryAfter(new BigDecimal(0));

        //When
        Mockito.when( safeStorageClient.getFile( Mockito.anyString(), Mockito.eq(false) ) )
                .thenReturn( fileDownloadResponse );

        Mockito.when( notificationService.getNotificationByIun( Mockito.anyString() ) )
                .thenReturn( newNotification() );

        ResponseEntity<Resource> result = legalFactService.getLegalfact( IUN, LegalFactCategory.RECIPIENT_ACCESS, LEGAL_FACT_ID);
        //Then
        assertNotNull( result );
    }

    @Test
    void getAnalogLegalFactMetadataSuccess() {
        //Given
        NotificationAttachment.Ref ref = NotificationAttachment.Ref.builder()
                .key( KEY )
                .versionToken( VERSION_TOKEN )
                .build();


        String[] urls = new String[1];
        try {
            Path path = Files.createTempFile( null,null );
            urls[0] =  new File(path.toString()).toURI().toURL().toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
        fileDownloadResponse.setContentType("application/pdf");
        fileDownloadResponse.setContentLength(new BigDecimal(0));
        fileDownloadResponse.setChecksum("123");
        fileDownloadResponse.setKey("123");
        fileDownloadResponse.setDownload(new FileDownloadInfo());
        fileDownloadResponse.getDownload().setUrl("https://www.url.qualcosa.it");
        fileDownloadResponse.getDownload().setRetryAfter(new BigDecimal(0));

        //When
        Mockito.when( safeStorageClient.getFile( Mockito.anyString(), Mockito.eq(false) ) )
                .thenReturn( fileDownloadResponse );

        Mockito.when( notificationService.getNotificationByIun( Mockito.anyString() ) )
                .thenReturn( newNotification() );

        LegalFactDownloadMetadataResponse result = legalFactService.getLegalFactMetadata( IUN, LegalFactCategory.RECIPIENT_ACCESS, LEGAL_FACT_ID);
        //Then
        assertNotNull( result );
        assertNotNull(result.getFilename());
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
