package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.api.dto.legalfacts.LegalFactType;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.commons.abstractions.FileData;
import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.deliverypush.action2.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.externalclient.pnclient.externalchannel.ExternalChannelGetClient;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.deliverypush.legalfacts.LegalfactsMetadataUtils;
import it.pagopa.pn.deliverypush.service.LegalFactService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
    private FileStorage fileStorage;
    private LegalfactsMetadataUtils legalfactsUtils;
    private ExternalChannelGetClient externalChannelClient;
    private NotificationService notificationService;
    private NotificationUtils notificationUtils;

    private LegalFactService legalFactService;

    @BeforeEach
    void setup() {
        timelineService = Mockito.mock( TimelineService.class );
        fileStorage = Mockito.mock( FileStorage.class );
        legalfactsUtils = Mockito.mock( LegalfactsMetadataUtils.class );
        externalChannelClient = Mockito.mock( ExternalChannelGetClient.class );
        notificationService = Mockito.mock(NotificationService.class);
        notificationUtils = Mockito.mock(NotificationUtils.class);
        
        legalFactService = new LegalFactServiceImpl(
                timelineService,
                fileStorage,
                legalfactsUtils,
                externalChannelClient,
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
        

        List<LegalFactListElement> result = legalFactService.getLegalFacts( IUN );

        assertEquals( legalFactsExpectedResult, result );
    }

    @Test
    void getLegalFactSuccess() {
        //Given
        NotificationAttachment.Ref ref = NotificationAttachment.Ref.builder()
                .key( KEY )
                .versionToken( VERSION_TOKEN )
                .build();

        FileData fileStorageResponse = FileData.builder()
                .contentLength( CONTENT_LENGTH )
                .contentType( CONTENT_TYPE )
                .content(InputStream.nullInputStream())
                .build();

        ResponseEntity<Resource> response = ResponseEntity.ok()
                .headers( fileStorage.headers() )
                .contentLength( CONTENT_LENGTH )
                .contentType( MediaType.APPLICATION_PDF )
                .body( new InputStreamResource( fileStorageResponse.getContent()) );
            
        //When
        Mockito.when( legalfactsUtils.fromIunAndLegalFactId( Mockito.anyString(), Mockito.anyString() ))
                .thenReturn( ref );
        Mockito.when( fileStorage.loadAttachment( Mockito.any( NotificationAttachment.Ref.class ) ) )
                .thenReturn( response );
        Mockito.when( notificationService.getNotificationByIun( Mockito.anyString() ) )
                .thenReturn( newNotification() );

        ResponseEntity<Resource> result = legalFactService.getLegalfact( IUN, LegalFactType.SENDER_ACK, LEGAL_FACT_ID);
        //Then
        assertNotNull( result );
    }

    @Test
    void getAnalogLegalFactSuccess() {
        //Given
        NotificationAttachment.Ref ref = NotificationAttachment.Ref.builder()
                .key( KEY )
                .versionToken( VERSION_TOKEN )
                .build();

        FileData fileStorageResponse = FileData.builder()
                .contentLength( CONTENT_LENGTH )
                .contentType( CONTENT_TYPE )
                .content(InputStream.nullInputStream())
                .build();

        ResponseEntity<Resource> response = ResponseEntity.ok()
                .headers( fileStorage.headers() )
                .contentLength( CONTENT_LENGTH )
                .contentType( MediaType.APPLICATION_PDF )
                .body( new InputStreamResource( fileStorageResponse.getContent()) );

        String[] urls = new String[1];
        try {
            Path path = Files.createTempFile( null,null );
            urls[0] =  new File(path.toString()).toURI().toURL().toString();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //When
        Mockito.when( legalfactsUtils.fromIunAndLegalFactId( Mockito.anyString(), Mockito.anyString() ))
                .thenReturn( ref );
        Mockito.when( fileStorage.loadAttachment( Mockito.any( NotificationAttachment.Ref.class ) ) )
                .thenReturn( response );
        Mockito.when( externalChannelClient.getResponseAttachmentUrl( Mockito.any(String[].class) ))
                .thenReturn( urls );
        Mockito.when( notificationService.getNotificationByIun( Mockito.anyString() ) )
                .thenReturn( newNotification() );
        
        ResponseEntity<Resource> result = legalFactService.getLegalfact( IUN, LegalFactType.ANALOG_DELIVERY, LEGAL_FACT_ID);
        //Then
        assertNotNull( result );
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
                                .digitalDomicile(DigitalAddress.builder()
                                        .type(DigitalAddress.TypeEnum.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .build()
                ))
                .build();
    }


}
