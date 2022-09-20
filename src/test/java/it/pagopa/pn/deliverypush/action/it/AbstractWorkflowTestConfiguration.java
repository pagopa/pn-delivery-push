package it.pagopa.pn.deliverypush.action.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.Configuration;
import freemarker.template.Version;
import freemarker.template._TemplateAPI;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.*;
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
import it.pagopa.pn.deliverypush.service.SafeStorageService;
import it.pagopa.pn.deliverypush.service.impl.SaveLegalFactsServiceImpl;
import it.pagopa.pn.deliverypush.utils.HtmlSanitizer;
import it.pagopa.pn.deliverypush.validator.NotificationReceiverValidator;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;

import javax.validation.Validation;
import javax.validation.ValidatorFactory;
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
    public HtmlSanitizer htmlSanitizer(ObjectMapper objectMapper) {
        return new HtmlSanitizer(objectMapper);
    }

    @Bean
    public DocumentComposition documentCompositionTest(HtmlSanitizer htmlSanitizer) throws IOException {
        Configuration freemarker = new Configuration( new Version(_TemplateAPI.VERSION_INT_2_3_0));
        return new DocumentComposition(  freemarker, htmlSanitizer );
    }
    
    @Bean
    public LegalFactGenerator legalFactPdfGeneratorTest( DocumentComposition dc ) {
        CustomInstantWriter instantWriter = new CustomInstantWriter();
        PhysicalAddressWriter physicalAddressWriter = new PhysicalAddressWriter();

        return new LegalFactGenerator( dc, instantWriter, physicalAddressWriter,  Mockito.mock(PnDeliveryPushConfigs.class) );
    }
    
    @Bean
    public SaveLegalFactsServiceImpl LegalFactsTest(SafeStorageService safeStorageService,
                                                    LegalFactGenerator pdfUtils) {
        return new SaveLegalFactsServiceImpl(pdfUtils, safeStorageService);
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
    public SchedulerServiceMock schedulerServiceMockMock(@Lazy DigitalWorkFlowHandler digitalWorkFlowHandler, 
                                                         @Lazy AnalogWorkflowHandler analogWorkflowHandler,
                                                         @Lazy RefinementHandler refinementHandler, 
                                                         @Lazy InstantNowSupplier instantNowSupplier,
                                                         @Lazy StartWorkflowForRecipientHandler startWorkflowForRecipientHandler,
                                                         @Lazy ChooseDeliveryModeHandler chooseDeliveryModeHandler) {
        return new SchedulerServiceMock(
                digitalWorkFlowHandler,
                analogWorkflowHandler,
                refinementHandler,
                instantNowSupplier,
                startWorkflowForRecipientHandler, 
                chooseDeliveryModeHandler);
    }

    @Bean
    @ConditionalOnProperty( name = "pn.delivery-push.validation-document-test", havingValue = "true")
    public NotificationReceiverValidator notificationReceiverValidatorTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        return new NotificationReceiverValidator( factory.getValidator() );
    }

    @Bean
    @ConditionalOnProperty( name = "pn.delivery-push.validation-document-test", havingValue = "false")
    public NotificationReceiverValidator notificationReceiverValidatorTestMock() {
        return Mockito.mock(NotificationReceiverValidator.class);
    }
    
    @Bean
    public PnExternalRegistryClient pnExternalRegistryClientTest() {
        return Mockito.mock(PnExternalRegistryClientImpl.class);
    }

}
