package it.pagopa.pn.deliverypush.action.it;

import freemarker.template.Configuration;
import freemarker.template.Version;
import freemarker.template._TemplateAPI;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.AnalogWorkflowHandler;
import it.pagopa.pn.deliverypush.action.DigitalWorkFlowHandler;
import it.pagopa.pn.deliverypush.action.RefinementHandler;
import it.pagopa.pn.deliverypush.action.it.mockbean.PnDeliveryClientMock;
import it.pagopa.pn.deliverypush.action.it.mockbean.PublicRegistryMock;
import it.pagopa.pn.deliverypush.action.it.mockbean.SchedulerServiceMock;
import it.pagopa.pn.deliverypush.action.it.mockbean.UserAttributesClientMock;
import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.legalfacts.CustomInstantWriter;
import it.pagopa.pn.deliverypush.legalfacts.DocumentComposition;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactGenerator;
import it.pagopa.pn.deliverypush.legalfacts.PhysicalAddressWriter;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery.PnDeliveryClient;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalregistry.PnExternalRegistryClient;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalregistry.PnExternalRegistryClientImpl;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.userattributes.UserAttributesClient;
import it.pagopa.pn.deliverypush.middleware.responsehandler.PublicRegistryResponseHandler;
import it.pagopa.pn.deliverypush.service.impl.SaveLegalFactsServiceImpl;
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
    public UserAttributesClient testAddressBook() {
        return new UserAttributesClientMock();
    }
    
    @Bean
    public PnSafeStorageClient safeStorageTest() {
        return Mockito.mock(PnSafeStorageClient.class);
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

        return new LegalFactGenerator( dc, instantWriter, physicalAddressWriter,  Mockito.mock(PnDeliveryPushConfigs.class) );
    }
    
    @Bean
    public SaveLegalFactsServiceImpl LegalFactsTest(PnSafeStorageClient safeStorageClient,
                                                    LegalFactGenerator pdfUtils) {
        return new SaveLegalFactsServiceImpl(pdfUtils, safeStorageClient);
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

    @Bean
    public PnExternalRegistryClient pnExternalRegistryClientTest() {
        return Mockito.mock(PnExternalRegistryClientImpl.class);
    }

}
