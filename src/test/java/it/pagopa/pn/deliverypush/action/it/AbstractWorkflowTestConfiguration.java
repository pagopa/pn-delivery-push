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
import it.pagopa.pn.deliverypush.action.it.mockbean.*;
import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.legalfacts.*;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery.PnDeliveryClient;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalregistry.PnExternalRegistriesClientReactive;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalregistry.PnExternalRegistryClient;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalregistry.PnExternalRegistryClientImpl;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.templatesengine.TemplatesClient;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.templatesengine.TemplatesClientPec;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.userattributes.UserAttributesClient;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.ActionHandler;
import it.pagopa.pn.deliverypush.middleware.responsehandler.NationalRegistriesResponseHandler;
import it.pagopa.pn.deliverypush.middleware.responsehandler.SafeStorageResponseHandler;
import it.pagopa.pn.deliverypush.service.*;
import it.pagopa.pn.deliverypush.service.impl.NotificationProcessCostServiceImpl;
import it.pagopa.pn.deliverypush.service.impl.SaveLegalFactsServiceImpl;
import it.pagopa.pn.deliverypush.utils.HtmlSanitizer;
import it.pagopa.pn.deliverypush.utils.PnSendModeUtils;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AbstractWorkflowTestConfiguration {
    static final int SEND_FEE = 100;

    @Bean
    public PnDeliveryPushConfigs pnDeliveryPushConfigs() {
        PnDeliveryPushConfigs pnDeliveryPushConfigs = Mockito.mock(PnDeliveryPushConfigs.class);

        // Base configuration
        List<String> pnSendModeList = new ArrayList<>();
        pnSendModeList.add("1970-01-01T00:00:00Z;AAR-DOCUMENTS-PAYMENTS;AAR-DOCUMENTS-PAYMENTS;AAR-DOCUMENTS-PAYMENTS;AAR_NOTIFICATION");
        pnSendModeList.add("2023-11-30T23:00:00Z;AAR;AAR;AAR-DOCUMENTS-PAYMENTS;AAR_NOTIFICATION");
        Mockito.when(pnDeliveryPushConfigs.getPnSendMode()).thenReturn(pnSendModeList);
        Mockito.when(pnDeliveryPushConfigs.getPagoPaNotificationBaseCost()).thenReturn(SEND_FEE);

        return pnDeliveryPushConfigs;
    }

    @Bean
    public NotificationProcessCostService notificationProcessCostService(@Lazy TimelineService timelineService,
                                                                         @Lazy PnExternalRegistriesClientReactive pnExternalRegistriesClientReactive,
                                                                         @Lazy PnDeliveryPushConfigs cfg) {
        return new NotificationProcessCostServiceImpl(timelineService, pnExternalRegistriesClientReactive, cfg);
    }

    @Bean
    public PnDeliveryClient testPnDeliveryClient(PnDataVaultClientReactiveMock pnDataVaultClientReactiveMock) {
        return new PnDeliveryClientMock(pnDataVaultClientReactiveMock);
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
        ObjectMapper objectMapper = ((JsonMapper.Builder) ((JsonMapper.Builder) JsonMapper.builder().configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false)).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)).build();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    @Bean
    public DocumentComposition documentCompositionTest(HtmlSanitizer htmlSanitizer) throws IOException {
        Configuration freemarker = new Configuration(new Version(_TemplateAPI.VERSION_INT_2_3_0));
        return new DocumentComposition(freemarker, htmlSanitizer);
    }

    @Bean
    public InstantNowSupplier instantNowSupplierTest() {
        return Mockito.mock(InstantNowSupplier.class);
    }

    @Bean
    @ConditionalOnProperty(name = "pn.delivery-push.enable-templates-engine", havingValue = "false")
    public LegalFactGenerator legalFactGeneratorDocComposition(DocumentComposition dc,
                                                               @Lazy PnSendModeUtils pnSendModeUtils,
                                                               PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        CustomInstantWriter instantWriter = new CustomInstantWriter();
        PhysicalAddressWriter physicalAddressWriter = new PhysicalAddressWriter();
        return new LegalFactGeneratorDocComposition(dc, instantWriter, physicalAddressWriter, pnDeliveryPushConfigs, instantNowSupplierTest(), pnSendModeUtils);
    }

    @Bean
    @ConditionalOnProperty(name = "pn.delivery-push.enable-templates-engine", havingValue = "true", matchIfMissing = true)
    public LegalFactGenerator legalFactGeneratorTemplatesClient(@Lazy PnSendModeUtils pnSendModeUtils, PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        CustomInstantWriter instantWriter = new CustomInstantWriter();
        PhysicalAddressWriter physicalAddressWriter = new PhysicalAddressWriter();
        return new LegalFactGeneratorTemplates(instantWriter, physicalAddressWriter, pnDeliveryPushConfigs, pnSendModeUtils, templatesClient(), templatesClientPec());
    }

    @Bean
    public TemplatesClient templatesClient() {
        return new TemplatesClientMock();
    }

    @Bean
    public TemplatesClientPec templatesClientPec() {
        return new TemplatesClientMockPec();
    }

    @Bean
    public SaveLegalFactsServiceImpl LegalFactsTest(SafeStorageService safeStorageService,
                                                    LegalFactGenerator pdfUtils) {
        return new SaveLegalFactsServiceImpl(pdfUtils, safeStorageService);
    }

    @Bean
    public NationalRegistriesClientMock publicRegistriesMapMock(@Lazy NationalRegistriesResponseHandler nationalRegistriesResponseHandler,
                                                                @Lazy TimelineService timelineService) {
        return new NationalRegistriesClientMock(
                nationalRegistriesResponseHandler,
                timelineService
        );
    }

    @Bean
    public ActionHandlerMock ActionHandlerMock(@Lazy ActionHandler actionHandler
    ) {
        return new ActionHandlerMock(actionHandler);
    }

    @Bean
    public SchedulerServiceMock schedulerServiceMockMock(@Lazy ActionPoolMock actionPoolMock) {
        return new SchedulerServiceMock(actionPoolMock);
    }


    @Bean
    public PnExternalRegistryClient pnExternalRegistryClientTest() {
        return Mockito.mock(PnExternalRegistryClientImpl.class);
    }

    @Bean
    public ParameterConsumer pnParameterConsumerClientTest() {
        return new AbstractCachedSsmParameterConsumerMock();
    }

    @Bean
    public F24Service f24Service() {
        return Mockito.mock(F24Service.class);
    }

    @Bean
    public LockProviderMock lockProviderTimeline() {
        return new LockProviderMock();
    }

}
