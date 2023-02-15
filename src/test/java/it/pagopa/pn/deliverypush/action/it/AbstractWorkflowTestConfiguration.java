package it.pagopa.pn.deliverypush.action.it;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import freemarker.template.Configuration;
import freemarker.template.Version;
import freemarker.template._TemplateAPI;
import it.pagopa.pn.commons.abstractions.ParameterConsumer;
import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogWorkflowHandler;
import it.pagopa.pn.deliverypush.action.choosedeliverymode.ChooseDeliveryModeHandler;
import it.pagopa.pn.deliverypush.action.digitalworkflow.DigitalWorkFlowHandler;
import it.pagopa.pn.deliverypush.action.digitalworkflow.DigitalWorkFlowRetryHandler;
import it.pagopa.pn.deliverypush.action.it.mockbean.*;
import it.pagopa.pn.deliverypush.action.refinement.RefinementHandler;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.NotificationValidationActionHandler;
import it.pagopa.pn.deliverypush.action.startworkflowrecipient.StartWorkflowForRecipientHandler;
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
import it.pagopa.pn.deliverypush.middleware.responsehandler.DocumentCreationResponseHandler;
import it.pagopa.pn.deliverypush.middleware.responsehandler.PublicRegistryResponseHandler;
import it.pagopa.pn.deliverypush.middleware.responsehandler.SafeStorageResponseHandler;
import it.pagopa.pn.deliverypush.service.DocumentCreationRequestService;
import it.pagopa.pn.deliverypush.service.SafeStorageService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.impl.SaveLegalFactsServiceImpl;
import it.pagopa.pn.deliverypush.utils.HtmlSanitizer;
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
    public PnSafeStorageClient safeStorageTest(DocumentCreationRequestService creationRequestService,
                                               SafeStorageResponseHandler safeStorageResponseHandler) {
        return new SafeStorageClientMock(creationRequestService, safeStorageResponseHandler);
    }

    @Bean
    public HtmlSanitizer htmlSanitizer() {
        return new HtmlSanitizer(buildObjectMapper(), HtmlSanitizer.SanitizeMode.DELETE_HTML);
    }

    private ObjectMapper buildObjectMapper() {
        ObjectMapper objectMapper = ((JsonMapper.Builder)((JsonMapper.Builder)JsonMapper.builder().configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false)).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)).build();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    @Bean
    public DocumentComposition documentCompositionTest(HtmlSanitizer htmlSanitizer) throws IOException {
        Configuration freemarker = new Configuration( new Version(_TemplateAPI.VERSION_INT_2_3_0));
        return new DocumentComposition(  freemarker, htmlSanitizer );
    }

    @Bean
    public InstantNowSupplier instantNowSupplierTest() {
        return Mockito.mock(InstantNowSupplier.class);
    }
    
    @Bean
    public LegalFactGenerator legalFactPdfGeneratorTest(DocumentComposition dc , @Lazy MVPParameterConsumer mvpParameterConsumer) {
        CustomInstantWriter instantWriter = new CustomInstantWriter();
        PhysicalAddressWriter physicalAddressWriter = new PhysicalAddressWriter();
        PnDeliveryPushConfigs pnDeliveryPushConfigs =  Mockito.mock(PnDeliveryPushConfigs.class);
        Mockito.when(pnDeliveryPushConfigs.getWebapp()).thenReturn(new PnDeliveryPushConfigs.Webapp());
        pnDeliveryPushConfigs.getWebapp().setQuickAccessUrlAarDetailSuffix("aar=%s");
        pnDeliveryPushConfigs.getWebapp().setFaqUrlTemplateSuffix("faq.html");
        pnDeliveryPushConfigs.getWebapp().setDirectAccessUrlTemplatePhysical("https://notifichedigitali.it");
        pnDeliveryPushConfigs.getWebapp().setDirectAccessUrlTemplateLegal("https://notifichedigitali.legal.it");
        return new LegalFactGenerator( dc, instantWriter, physicalAddressWriter,  pnDeliveryPushConfigs, new InstantNowSupplier(), mvpParameterConsumer);
    }
    
    @Bean
    public SaveLegalFactsServiceImpl LegalFactsTest(SafeStorageService safeStorageService,
                                                    LegalFactGenerator pdfUtils) {
        return new SaveLegalFactsServiceImpl(pdfUtils, safeStorageService);
    }

    @Bean
    public NationalRegistriesClientMock publicRegistriesMapMock(@Lazy PublicRegistryResponseHandler publicRegistryResponseHandler,
                                                                @Lazy TimelineService timelineService) {
        return new NationalRegistriesClientMock(
                publicRegistryResponseHandler,
                timelineService
            );
    }
    
    @Bean
    public SchedulerServiceMock schedulerServiceMockMock(@Lazy DigitalWorkFlowHandler digitalWorkFlowHandler,
                                                         @Lazy DigitalWorkFlowRetryHandler digitalWorkFlowRetryHandler,
                                                         @Lazy AnalogWorkflowHandler analogWorkflowHandler,
                                                         @Lazy RefinementHandler refinementHandler, 
                                                         @Lazy InstantNowSupplier instantNowSupplier,
                                                         @Lazy StartWorkflowForRecipientHandler startWorkflowForRecipientHandler,
                                                         @Lazy ChooseDeliveryModeHandler chooseDeliveryModeHandler,
                                                         @Lazy DocumentCreationResponseHandler documentCreationResponseHandler,
                                                         @Lazy NotificationValidationActionHandler notificationValidationActionHandler) {
        return new SchedulerServiceMock(
                digitalWorkFlowHandler,
                digitalWorkFlowRetryHandler,
                analogWorkflowHandler,
                refinementHandler,
                instantNowSupplier,
                startWorkflowForRecipientHandler, 
                chooseDeliveryModeHandler, 
                documentCreationResponseHandler,
                notificationValidationActionHandler);
    }

    
    @Bean
    public PnExternalRegistryClient pnExternalRegistryClientTest() {
        return Mockito.mock(PnExternalRegistryClientImpl.class);
    }
    
    @Bean
    public ParameterConsumer pnParameterConsumerClientTest(){
        return new AbstractCachedSsmParameterConsumerMock();
    }
    
}
