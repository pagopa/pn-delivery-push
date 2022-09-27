package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.papernotificationfailed.PaperNotificationFailed;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ResponsePaperNotificationFailedDto;
import it.pagopa.pn.deliverypush.middleware.dao.failednotificationdao.PaperNotificationFailedDao;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.PaperNotificationFailedService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        List<ResponsePaperNotificationFailedDto> paperNotificationFailedList = paperNotificationFailedService.getPaperNotificationByRecipientId( RECIPIENT_ID, false );
        
        //Then
        ResponsePaperNotificationFailedDto elem = paperNotificationFailedList.get(0);
        
        assertEquals( IUN, elem.getIun() );
        assertEquals( RECIPIENT_ID, elem.getRecipientInternalId() );
    }
}
