package it.pagopa.pn.deliverypush.action2.it;

import freemarker.template.Configuration;
import freemarker.template.Version;
import freemarker.template._TemplateAPI;
import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action2.AnalogWorkflowHandler;
import it.pagopa.pn.deliverypush.action2.DigitalWorkFlowHandler;
import it.pagopa.pn.deliverypush.action2.PublicRegistryResponseHandler;
import it.pagopa.pn.deliverypush.action2.RefinementHandler;
import it.pagopa.pn.deliverypush.action2.it.mockbean.UserAttributesMock;
import it.pagopa.pn.deliverypush.action2.it.mockbean.PnDeliveryClientMock;
import it.pagopa.pn.deliverypush.action2.it.mockbean.PublicRegistryMock;
import it.pagopa.pn.deliverypush.action2.it.mockbean.SchedulerServiceMock;
import it.pagopa.pn.deliverypush.action2.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.externalclient.pnclient.userattributes.UserAttributes;
import it.pagopa.pn.deliverypush.legalfacts.*;
import it.pagopa.pn.deliverypush.externalclient.pnclient.delivery.PnDeliveryClient;
import it.pagopa.pn.deliverypush.validator.NotificationReceiverValidator;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;

import java.io.IOException;

public class AbstractWorkflowTestConfiguration {

    @Bean
    public PnDeliveryClient testPnDeliveryClient() {
        return new PnDeliveryClientMock();
    }

    @Bean
    public UserAttributes testAddressBook() {
        return new UserAttributesMock();
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
