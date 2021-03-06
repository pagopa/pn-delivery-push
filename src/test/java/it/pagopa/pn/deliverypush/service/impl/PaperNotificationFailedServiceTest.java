package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.api.dto.notification.failednotification.PaperNotificationFailed;
import it.pagopa.pn.deliverypush.middleware.dao.failednotificationdao.PaperNotificationFailedDao;
import it.pagopa.pn.deliverypush.service.PaperNotificationFailedService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaperNotificationFailedServiceTest {
    private static final String IUN = "IUN";
    private static final String RECIPIENT_ID = "RECIPIENT_ID";
    private PaperNotificationFailedDao paperNotificationFailedDao;
    private PaperNotificationFailedService paperNotificationFailedService;
    
    @BeforeEach
    void setup() {
       paperNotificationFailedDao = Mockito.mock( PaperNotificationFailedDao.class );
       paperNotificationFailedService = new PaperNotificationFailedServiceImpl( paperNotificationFailedDao );
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
        List<PaperNotificationFailed> paperNotificationFailedList = paperNotificationFailedService.getPaperNotificationByRecipientId( RECIPIENT_ID );
        
        //Then
        assertEquals( paperNotificationFailedList, new ArrayList<>(paperNotificationFailedSet) );
    }
}
