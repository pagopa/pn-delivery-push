package it.pagopa.pn.deliverypush.action.it;

import it.pagopa.pn.commons.abstractions.ParameterConsumer;
import it.pagopa.pn.deliverypush.action.it.mockbean.*;
import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.datavault.PnDataVaultClientReactive;
import it.pagopa.pn.deliverypush.legalfacts.CustomInstantWriter;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactGenerator;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactGeneratorTemplates;
import it.pagopa.pn.deliverypush.legalfacts.PhysicalAddressWriter;
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
import it.pagopa.pn.deliverypush.utils.PnSendModeUtils;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;

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
        PnDeliveryClientMock pnDeliveryClientMock = new PnDeliveryClientMock(pnDataVaultClientReactiveMock);
        pnDataVaultClientReactiveMock.setPnDeliveryClientMock(pnDeliveryClientMock);
        return pnDeliveryClientMock;
    }

    @Bean
    public PnDataVaultClientReactive testPnDataVaultClient() {
        return new PnDataVaultClientReactiveMock();
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
    public InstantNowSupplier instantNowSupplierTest() {
        return Mockito.mock(InstantNowSupplier.class);
    }

    @Bean
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
