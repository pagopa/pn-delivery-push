package it.pagopa.pn.deliverypush.rest.it;

import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SafeStorageService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.impl.NotificationServiceImpl;
import it.pagopa.pn.deliverypush.service.impl.SafeStorageServiceImpl;
import it.pagopa.pn.deliverypush.service.impl.TimeLineServiceImpl;
import it.pagopa.pn.deliverypush.utils.AuthUtils;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;

public class TestConfiguration {

    @Mock
    private NotificationUtils notificationUtils;
    @Mock
    private AuthUtils authUtils;
    
    @Bean
    public TimelineService timeLineServiceImplTest() {
        return Mockito.mock(TimeLineServiceImpl.class);
    }

    @Bean
    public SafeStorageService safeStorageServiceTest() {
        return Mockito.mock(SafeStorageServiceImpl.class);
    }

    @Bean
    public NotificationService notificationServiceTest() {
        return Mockito.mock(NotificationServiceImpl.class);
    }

    @Bean
    public NotificationUtils notificationUtilsTest() {
        return new NotificationUtils();
    }

    @Bean
    public AuthUtils authTest() {
        return Mockito.mock(AuthUtils.class);
    }
    
}
