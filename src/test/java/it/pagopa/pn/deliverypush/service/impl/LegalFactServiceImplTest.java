package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.deliverypush.legalfacts.LegalfactsMetadataUtils;
import it.pagopa.pn.deliverypush.middleware.timelinedao.TimelineDao;
import it.pagopa.pn.deliverypush.pnclient.externalchannel.ExternalChannelClient;
import it.pagopa.pn.deliverypush.service.LegalFactService;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

class LegalFactServiceImplTest {

    private static final String IUN = "fake_iun";
    private static final String TAX_ID = "tax_id";
    private static final String KEY = "key";
    public static final String VERSION_TOKEN = "VERSION_TOKEN";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final long CONTENT_LENGTH = 0L;
    private static final String LEGAL_FACT_ID = "LEGAL_FACT_ID";

    private TimelineDao timelineDao;
    private FileStorage fileStorage;
    private LegalfactsMetadataUtils legalfactsUtils;
    private ExternalChannelClient externalChannelClient;

    private LegalFactService legalFactService;

    @BeforeEach
    void setup() {
        timelineDao = Mockito.mock( TimelineDao.class );
        fileStorage = Mockito.mock( FileStorage.class );
        legalfactsUtils = Mockito.mock( LegalfactsMetadataUtils.class );
        externalChannelClient = Mockito.mock( ExternalChannelClient.class );

        legalFactService = new LegalFactServiceImpl(
                timelineDao,
                fileStorage,
                legalfactsUtils,
                externalChannelClient
        );

    }
    
    //TODO DA capire gestione legalFacts
    /*
    @Test
    void getLegalFactsSuccess() {
        List<LegalFactsListEntry> legalFactsExpectedResult = Collections.singletonList( LegalFactsListEntry.builder()
                .iun( IUN )
                .taxId( TAX_ID )
                .legalFactsId( LegalFactsListEntryId.builder()
                        .key( KEY )
                        .type( LegalFactType.SENDER_ACK )
                        .build()
                ).build()
        );

        Set<TimelineElement> timelineElementsResult = Collections.singleton( TimelineElement.builder()
                .iun( IUN )
                .details( new ScheduleAnalogWorkflow( TAX_ID ))
                .category( TimelineElementCategory.REQUEST_ACCEPTED )
                .elementId( "element_id" )
                .legalFactsIds( Collections.singletonList( LegalFactsListEntryId.builder()
                                .key( KEY )
                                .type( LegalFactType.SENDER_ACK )
                        .build())
                ).build()
        );


        Mockito.when( timelineDao.getTimeline( Mockito.anyString() ) )
                .thenReturn( timelineElementsResult );

        List<LegalFactsListEntry> result = legalFactService.getLegalFacts( IUN );

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
        ResponseEntity<Resource> result = legalFactService.getLegalfact( IUN, LegalFactType.ANALOG_DELIVERY, LEGAL_FACT_ID);
        //Then
        assertNotNull( result );
    }
    
     */
}
