package it.pagopa.pn.deliverypush.action2.it;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.commons_delivery.middleware.NotificationDao;
import it.pagopa.pn.deliverypush.action2.ChooseDeliveryModeHandler;
import it.pagopa.pn.deliverypush.action2.it.testbean.NotificationDaoTest;
import it.pagopa.pn.deliverypush.external.ExternalChannel;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactUtils;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;
import java.util.Collection;

public class AbstractWorkflowTestConfiguration {

    private final Collection<Notification> notifications;

    public AbstractWorkflowTestConfiguration(Collection<Notification> notifications) {
        this.notifications = notifications;
    }

    public AbstractWorkflowTestConfiguration(Notification ... notifications) {
        this.notifications = Arrays.asList(notifications);
    }

    @Bean
    public NotificationDao testNotificationDao() {
        return new NotificationDaoTest( notifications );
    }

    @Bean
    public LegalFactUtils testLegalFactsTest() {
        return Mockito.mock(LegalFactUtils.class);
    }

    @Bean
    public ChooseDeliveryModeHandler temporaryChooseDeliveryHandler() {
        return Mockito.mock(ChooseDeliveryModeHandler.class);
    }

    @Bean
    public ExternalChannel externalChannelMock() {
        return Mockito.mock(ExternalChannel.class);
    }

}
