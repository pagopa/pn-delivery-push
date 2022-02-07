package it.pagopa.pn.deliverypush.action2.it;

import freemarker.template.Configuration;
import freemarker.template.Version;
import freemarker.template._TemplateAPI;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.commons_delivery.middleware.NotificationDao;
import it.pagopa.pn.commons_delivery.utils.LegalfactsMetadataUtils;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action2.AnalogWorkflowHandler;
import it.pagopa.pn.deliverypush.action2.DigitalWorkFlowHandler;
import it.pagopa.pn.deliverypush.action2.PublicRegistryResponseHandler;
import it.pagopa.pn.deliverypush.action2.RefinementHandler;
import it.pagopa.pn.deliverypush.action2.it.mockbean.*;
import it.pagopa.pn.deliverypush.action2.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.external.AddressBook;
import it.pagopa.pn.deliverypush.external.AddressBookEntry;
import it.pagopa.pn.deliverypush.legalfacts.*;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;

import java.io.IOException;
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
            Collection<AddressBookEntry> addressBookEntries,
            Map<String, DigitalAddress> publicRegistryDigitalAddresses,
            Map<String, PhysicalAddress> publicRegistryPhysicalAddresses
    ) {
        this.notifications = Collections.singletonList(notification);
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
    public FileStorage fileStorageTest() {
        return Mockito.mock(FileStorage.class);
    }

    @Bean
    public DocumentComposition documentCompositionTest() throws IOException {
        Configuration freemarker = new Configuration( new Version(_TemplateAPI.VERSION_INT_2_3_0));
        return new DocumentComposition(  freemarker );
    }
    
    @Bean
    public LegalFactGenerator legalFactPdfGeneratorTest( DocumentComposition dc ) {
        CustomInstantWriter instantWriter = new CustomInstantWriter();
        PhysicalAddressWriter physicalAddressWriter = new PhysicalAddressWriter();

        return new LegalFactGenerator( dc, instantWriter, physicalAddressWriter );
    }
    
    @Bean
    public LegalFactDao LegalFactsTest(FileStorage fileStorage,
                                       LegalFactGenerator pdfUtils,
                                       LegalfactsMetadataUtils legalfactMetadataUtils) {
        return new LegalFactDao(fileStorage, pdfUtils, legalfactMetadataUtils);
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
    public PnDeliveryPushConfigs pnDeliveryPushConfigsTest() {
        return Mockito.mock(PnDeliveryPushConfigs.class);
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
