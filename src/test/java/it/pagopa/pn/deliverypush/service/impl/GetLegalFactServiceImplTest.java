package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadInfo;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.GetAddressInfoDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactDownloadMetadataResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactListElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactsId;
import it.pagopa.pn.deliverypush.service.GetLegalFactService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
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
    private PnSafeStorageClient safeStorageClient;
    private NotificationService notificationService;
    private NotificationUtils notificationUtils;

    private GetLegalFactService getLegalFactService;

    @BeforeEach
    void setup() {
        timelineService = Mockito.mock( TimelineService.class );
        safeStorageClient = Mockito.mock( PnSafeStorageClient.class );
        notificationService = Mockito.mock(NotificationService.class);
        notificationUtils = Mockito.mock(NotificationUtils.class);
        
        getLegalFactService = new GetLegalFactServiceImpl(
                timelineService,
                safeStorageClient,
                notificationService,
                notificationUtils
        );

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
        
        Set<TimelineElementInternal> timelineElementsResult = Collections.singleton( TimelineElementInternal.builder()
                .iun( IUN )
                .details( GetAddressInfoDetailsInt.builder()
                        .recIndex(0)
                        .build() )
                .category( TimelineElementCategoryInt.GET_ADDRESS )
                .elementId( "element_id" )
                .legalFactsIds( Collections.singletonList( LegalFactsIdInt.builder()
                                .key( KEY )
                                .category( LegalFactCategoryInt.SENDER_ACK )
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
        

        List<LegalFactListElement> result = getLegalFactService.getLegalFacts( IUN );

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

        ResponseEntity<Resource> result = getLegalFactService.getLegalfact( IUN, LegalFactCategory.RECIPIENT_ACCESS, LEGAL_FACT_ID);
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

        assertThrows(PnInternalException.class, () -> getLegalFactService.getLegalfact( IUN, LegalFactCategory.RECIPIENT_ACCESS, LEGAL_FACT_ID));
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

        ResponseEntity<Resource> result = getLegalFactService.getLegalfact( IUN, LegalFactCategory.RECIPIENT_ACCESS, LEGAL_FACT_ID);
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

        LegalFactDownloadMetadataResponse result = getLegalFactService.getLegalFactMetadata( IUN, LegalFactCategory.RECIPIENT_ACCESS, LEGAL_FACT_ID);
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
                .paNotificationId("protocol_01")
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
