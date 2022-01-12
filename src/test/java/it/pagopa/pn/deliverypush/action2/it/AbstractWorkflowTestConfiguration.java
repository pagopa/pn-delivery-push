package it.pagopa.pn.deliverypush.action2.it;

import it.pagopa.pn.api.dto.addressbook.AddressBookEntry;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.commons.pnclients.addressbook.AddressBook;
import it.pagopa.pn.commons_delivery.middleware.NotificationDao;
import it.pagopa.pn.deliverypush.action2.AnalogWorkflowHandler;
import it.pagopa.pn.deliverypush.action2.DigitalWorkFlowHandler;
import it.pagopa.pn.deliverypush.action2.PublicRegistryResponseHandler;
import it.pagopa.pn.deliverypush.action2.RefinementHandler;
import it.pagopa.pn.deliverypush.action2.it.mockbean.AddressBookMock;
import it.pagopa.pn.deliverypush.action2.it.mockbean.NotificationDaoMock;
import it.pagopa.pn.deliverypush.action2.it.mockbean.PublicRegistryMock;
import it.pagopa.pn.deliverypush.action2.it.mockbean.SchedulerServiceMock;
import it.pagopa.pn.deliverypush.action2.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactUtils;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class AbstractWorkflowTestConfiguration {

    private final Collection<Notification> notifications;
    private final Collection<AddressBookEntry> addressBookEntries;
    private final Map<String, DigitalAddress> publicRegistryDigitalAddresses;
    private final Map<String, PhysicalAddress> publicRegistryPhysicalAddresses;

    public AbstractWorkflowTestConfiguration(
            Collection<Notification> notifications,
            Collection<AddressBookEntry> addressBookEntries,
            Map<String, DigitalAddress> publicRegistryDigitalAddresses,
            Map<String, PhysicalAddress> publicRegistryPhysicalAddresses
    ) {
        this.notifications = notifications;
        this.addressBookEntries = addressBookEntries;
        this.publicRegistryDigitalAddresses = publicRegistryDigitalAddresses;
        this.publicRegistryPhysicalAddresses = publicRegistryPhysicalAddresses;
    }

    public AbstractWorkflowTestConfiguration(
            Notification notification,
            AddressBookEntry addressBookEntries,
            Map<String, DigitalAddress> publicRegistryDigitalAddresses,
            Map<String, PhysicalAddress> publicRegistryPhysicalAddresses
    ) {
        this.notifications = Collections.singletonList(notification);
        this.addressBookEntries = Collections.singletonList(addressBookEntries);
        this.publicRegistryDigitalAddresses = publicRegistryDigitalAddresses;
        this.publicRegistryPhysicalAddresses = publicRegistryPhysicalAddresses;
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
    public LegalFactUtils LegalFactsTest() {
        return Mockito.mock(LegalFactUtils.class);
    }

    @Bean
    public PublicRegistryMock publicRegistriesMapMock(@Lazy PublicRegistryResponseHandler publicRegistryResponseHandler) {
        return new PublicRegistryMock(
                this.publicRegistryDigitalAddresses,
                this.publicRegistryPhysicalAddresses,
                publicRegistryResponseHandler
        );
    }

    @Bean
    public InstantNowSupplier instantNowSupplierTest() {
        return Mockito.mock(InstantNowSupplier.class);
    }

    @Bean
    public SchedulerServiceMock schedulerServiceMockMock(@Lazy DigitalWorkFlowHandler digitalWorkFlowHandler, @Lazy AnalogWorkflowHandler analogWorkflowHandler,
                                                         @Lazy RefinementHandler refinementHandler, @Lazy InstantNowSupplier instantNowSupplier) {
        return new SchedulerServiceMock(
                digitalWorkFlowHandler,
                analogWorkflowHandler,
                refinementHandler,
                instantNowSupplier
        );
    }
}
