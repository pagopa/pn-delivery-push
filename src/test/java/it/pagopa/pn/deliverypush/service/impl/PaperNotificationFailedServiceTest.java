package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.papernotificationfailed.PaperNotificationFailed;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarGenerationDetailsInt;
import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.middleware.dao.failednotificationdao.PaperNotificationFailedDao;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.PaperNotificationFailedService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.test.StepVerifier;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

class PaperNotificationFailedServiceTest {
    private static final String IUN = "IUN";
    private static final String RECIPIENT_ID = "RECIPIENT_ID";
    private PaperNotificationFailedDao paperNotificationFailedDao;
    private PaperNotificationFailedService paperNotificationFailedService;

    private NotificationService notificationService;
    private NotificationUtils notificationUtils;
    private TimelineService timelineService;

    @BeforeEach
    void setup() {
        paperNotificationFailedDao = Mockito.mock( PaperNotificationFailedDao.class );
        notificationService = Mockito.mock( NotificationService.class );
        notificationUtils = Mockito.mock( NotificationUtils.class );
        timelineService = Mockito.mock( TimelineService.class );
        
        paperNotificationFailedService = new PaperNotificationFailedServiceImpl( paperNotificationFailedDao, notificationService, notificationUtils, timelineService);
    }
    
    @Test
    void getPaperNotificationsFailed() {
        //Given
        Set<PaperNotificationFailed> paperNotificationFailedSet = new HashSet<>();
        paperNotificationFailedSet.add( PaperNotificationFailed.builder()
                        .iun( IUN )
                        .recipientId( RECIPIENT_ID )
                        .build());
        
        //When
        Mockito.when( paperNotificationFailedDao.getPaperNotificationFailedByRecipientId( Mockito.anyString() ))
                .thenReturn( paperNotificationFailedSet );

        //Then
       StepVerifier.create(paperNotificationFailedService.getPaperNotificationByRecipientId( RECIPIENT_ID, false ))
               .expectNextMatches(paperNotificationFailedDto -> paperNotificationFailedDto.getIun().equalsIgnoreCase(IUN)
                       && paperNotificationFailedDto.getRecipientInternalId().equalsIgnoreCase(RECIPIENT_ID))
               .verifyComplete();
    }

    @Test
    void getPaperNotificationsFailedThrowsNotFound() {
        Set<PaperNotificationFailed> paperNotificationFailedSet = new HashSet<>();
        //When
        Mockito.when( paperNotificationFailedDao.getPaperNotificationFailedByRecipientId( Mockito.anyString() ))
                .thenReturn( paperNotificationFailedSet );

        //Then
        StepVerifier.create(paperNotificationFailedService.getPaperNotificationByRecipientId( RECIPIENT_ID, false ))
                .expectError(PnNotFoundException.class);
    }

    @Test
    void getPaperNotificationsFailedWithAAR() {
        //Given
        Set<PaperNotificationFailed> paperNotificationFailedSet = new HashSet<>();
        paperNotificationFailedSet.add( PaperNotificationFailed.builder()
                .iun( IUN )
                .recipientId( RECIPIENT_ID )
                .build());

        String aarUrl = "http://test.download.com/aar";
        AarGenerationDetailsInt aarGenerationDetailsInt = new AarGenerationDetailsInt();
        aarGenerationDetailsInt.setGeneratedAarUrl(aarUrl);

        //When
        Mockito.when( paperNotificationFailedDao.getPaperNotificationFailedByRecipientId( Mockito.anyString() ))
                .thenReturn( paperNotificationFailedSet );
        Mockito.when( notificationService.getNotificationByIun(Mockito.any())).thenReturn(new NotificationInt());
        Mockito.when( notificationUtils.getRecipientIndexFromInternalId(Mockito.any(), Mockito.any())).thenReturn(0);
        Mockito.when( timelineService.getTimelineElementDetails(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Optional.of(aarGenerationDetailsInt));

        //Then
        StepVerifier.create(paperNotificationFailedService.getPaperNotificationByRecipientId( RECIPIENT_ID, true ))
                .expectNextMatches(paperNotificationFailedDto -> paperNotificationFailedDto.getIun().equalsIgnoreCase(IUN)
                        && paperNotificationFailedDto.getRecipientInternalId().equalsIgnoreCase(RECIPIENT_ID)
                        && paperNotificationFailedDto.getAarUrl().equalsIgnoreCase(aarUrl))
                .verifyComplete();
    }

    @Test
    void getPaperNotificationsFailedWithAARFails() {
        //Given
        Set<PaperNotificationFailed> paperNotificationFailedSet = new HashSet<>();
        paperNotificationFailedSet.add( PaperNotificationFailed.builder()
                .iun( IUN )
                .recipientId( RECIPIENT_ID )
                .build());

        String aarUrl = "http://test.download.com/aar";
        AarGenerationDetailsInt aarGenerationDetailsInt = new AarGenerationDetailsInt();
        aarGenerationDetailsInt.setGeneratedAarUrl(aarUrl);

        //When
        Mockito.when( paperNotificationFailedDao.getPaperNotificationFailedByRecipientId( Mockito.anyString() ))
                .thenReturn( paperNotificationFailedSet );
        Mockito.when( notificationService.getNotificationByIun(Mockito.any())).thenReturn(new NotificationInt());
        Mockito.when( notificationUtils.getRecipientIndexFromInternalId(Mockito.any(), Mockito.any())).thenReturn(0);
        Mockito.when( timelineService.getTimelineElementDetails(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Optional.empty());

        //Then
        StepVerifier.create(paperNotificationFailedService.getPaperNotificationByRecipientId( RECIPIENT_ID, true ))
                .expectNextMatches(paperNotificationFailedDto -> paperNotificationFailedDto.getIun().equalsIgnoreCase(IUN)
                        && paperNotificationFailedDto.getRecipientInternalId().equalsIgnoreCase(RECIPIENT_ID)
                        && paperNotificationFailedDto.getAarUrl() == null)
                .verifyComplete();
    }
}
