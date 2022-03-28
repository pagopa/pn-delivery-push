package it.pagopa.pn.deliverypush.action2.it;

import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action2.AnalogWorkflowHandler;
import it.pagopa.pn.deliverypush.action2.DigitalWorkFlowHandler;
import it.pagopa.pn.deliverypush.action2.PublicRegistryResponseHandler;
import it.pagopa.pn.deliverypush.action2.RefinementHandler;
import it.pagopa.pn.deliverypush.action2.it.mockbean.*;
import it.pagopa.pn.deliverypush.action2.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.external.AddressBook;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactPdfGenerator;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactUtils;
import it.pagopa.pn.deliverypush.legalfacts.LegalfactsMetadataUtils;
import it.pagopa.pn.deliverypush.legalfacts.OpenhtmltopdfLegalFactPdfGenerator;
import it.pagopa.pn.deliverypush.pnclient.delivery.PnDeliveryClient;
import it.pagopa.pn.deliverypush.validator.NotificationReceiverValidator;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;

public class AbstractWorkflowTestConfiguration {

    @Bean
    public PnDeliveryClient testPnDeliveryClient() {
        return new PnDeliveryClientMock();
    }

    @Bean
    public AddressBook testAddressBook() {
        return new AddressBookMock();
    }
    
    @Bean
    public FileStorage fileStorageTest() {
        return Mockito.mock(FileStorage.class);
    }
    
    @Bean
    public LegalFactPdfGenerator legalFactPdfGeneratorTest(TimelineDaoMock timelineDaoMock) {
        return new OpenhtmltopdfLegalFactPdfGenerator(timelineDaoMock);
    }
    
    @Bean
    public LegalFactUtils LegalFactsTest(FileStorage fileStorage,
                                         LegalFactPdfGenerator pdfUtils,
                                         LegalfactsMetadataUtils legalfactMetadataUtils) {
        return new LegalFactUtils(fileStorage, pdfUtils, legalfactMetadataUtils);
    }

    @Bean
    public PublicRegistryMock publicRegistriesMapMock(@Lazy PublicRegistryResponseHandler publicRegistryResponseHandler) {
        return new PublicRegistryMock(
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

    @Bean
    public NotificationReceiverValidator notificationReceiverValidatorTest() {
        return Mockito.mock(NotificationReceiverValidator.class);
    }

}
