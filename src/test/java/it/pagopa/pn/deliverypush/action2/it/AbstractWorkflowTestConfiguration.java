package it.pagopa.pn.deliverypush.action2.it;

import it.pagopa.pn.api.dto.addressbook.AddressBookEntry;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.commons.pnclients.addressbook.AddressBook;
import it.pagopa.pn.commons_delivery.middleware.NotificationDao;
import it.pagopa.pn.deliverypush.action2.it.mockbean.AddressBookMock;
import it.pagopa.pn.deliverypush.action2.it.mockbean.NotificationDaoMock;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactUtils;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;

import java.util.Collection;

public class AbstractWorkflowTestConfiguration {

    private final Collection<Notification> notifications;
    private final Collection<AddressBookEntry> addressBookEntries;

    public AbstractWorkflowTestConfiguration(Collection<Notification> notifications, Collection<AddressBookEntry> addressBookEntries) {
        this.notifications = notifications;
        this.addressBookEntries = addressBookEntries;
    }

    @Bean
    public NotificationDao testNotificationDao() {
        return new NotificationDaoMock(notifications);
    }

    @Bean
    public AddressBook testAddressBook() {
        return new AddressBookMock(addressBookEntries);
    }

    @Bean
    public LegalFactUtils testLegalFactsTest() {
        return Mockito.mock(LegalFactUtils.class);
    }

  /*  @Bean
    public ChooseDeliveryModeHandler temporaryChooseDeliveryHandler() {
        return Mockito.mock(ChooseDeliveryModeHandler.class);
    }

    @Bean
    public ExternalChannel externalChannelMock() {
        return Mockito.mock(ExternalChannel.class);
    }
*/
}
