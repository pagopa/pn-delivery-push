package it.pagopa.pn.deliverypush.action2.it;

import it.pagopa.pn.commons.pnclients.addressbook.AddressBook;
import it.pagopa.pn.commons_delivery.middleware.NotificationDao;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.commons_delivery.middleware.failednotification.PaperNotificationFailedDao;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action2.*;
import it.pagopa.pn.deliverypush.action2.it.testbean.*;
import it.pagopa.pn.deliverypush.actions.ExtChnEventUtils;
import it.pagopa.pn.deliverypush.external.ExternalChannel;
import it.pagopa.pn.deliverypush.external.PublicRegistry;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactUtils;
import it.pagopa.pn.deliverypush.service.*;
import it.pagopa.pn.deliverypush.service.impl.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.PostConstruct;

/*@ContextConfiguration(classes = {
        CassandraPaperNotificationFailedDao.class,
        CassandraPaperNotificationFailedEntityDao.class,
        DtoToEntityNotificationFailedMapper.class,
        EntityToDtoNotificationFailedMapper.class,
        CassandraAutoConfiguration.class,
        CassandraDataAutoConfiguration.class,
})*/
@ExtendWith(SpringExtension.class)
class WorkflowTest {

    @TestConfiguration
    static class SpringTestConfiguration {
        @Autowired
        private NotificationDao notificationDao;
        @Autowired
        private AddressBook addressBook;
        @Autowired
        private TimelineDao timelineDao;
        @Autowired
        private TimelineService timelineService;
        @Autowired
        private RefinementHandler refinementHandler;
        @Autowired
        private SchedulerService schedulerService;
        @Autowired
        private PublicRegistry publicRegistry;
        @Autowired
        private PaperNotificationFailedDao paperNotificationFailedDao;

        @Autowired
        private LegalFactUtils legalFactUtils;
        @Autowired
        private PnDeliveryPushConfigs pnDeliveryPushConfigs;
        @Autowired
        private ExtChnEventUtils extChnEventUtils;
        @Autowired
        private NotificationService notificationService;
        @Autowired
        private CourtesyMessageService courtesyMessageService;
        @Autowired
        private CompletionWorkFlowHandler completionWorkFlowHandler;
        @Autowired
        private DigitalWorkFlowHandler digitalWorkFlowHandler;
        @Autowired
        private ExternalChannelResponseHandler externalChannelResponseHandler;
        @Autowired
        private ExternalChannel externalChannel;
        @Autowired
        private ExternalChannelService externalChannelService;
        @Autowired
        private ChooseDeliveryModeHandler chooseDeliveryModeHandler;
        @Autowired
        private PublicRegistryService publicRegistryService;
        @Autowired
        private DigitalWorkFlowService digitalWorkflowService;
        @Autowired
        private CompletelyUnreachableService completelyUnreachableService;

        @Bean
        public RefinementHandler refinementHandler() {
            return new RefinementHandler(timelineService);
        }

        @Bean
        public PaperNotificationFailedDao paperNotificationFailedDaoTest() {
            return new PaperNotificationFailedDaoTest();
        }

        @Bean
        public PublicRegistry publicRegistryTest() {
            return new PublicRegistryTest();
        }

        @Bean
        public SchedulerService schedulerServiceTest() {
            return new SchedulerServiceTest(digitalWorkFlowHandler, null,
                    refinementHandler);
        }

        @Bean
        public TimelineDao timelineDaoTest() {
            return new TimelineDaoTest();
        }

        @Bean
        public AddressBook addressBook() {
            return new AddressBookTest();
        }

        @Bean
        public NotificationDao notificationDaoTest() {
            return new NotificationDaoTest();
        }

        @Bean
        public ExternalChannel externalChannelTest() {
            return new ExternalChannelTest(externalChannelResponseHandler);
        }

        @Bean
        public CompletelyUnreachableService completelyUnreachableServiceTest() {
            return new CompletelyUnreachableServiceImpl(paperNotificationFailedDao,
                    timelineService);
        }

        @Bean
        public CompletionWorkFlowHandler completionWorkFlowHandlerTest() {
            return new CompletionWorkFlowHandler(notificationService,
                    schedulerService, externalChannelService,
                    timelineService, completelyUnreachableService);
        }

        @Bean
        public DigitalWorkFlowHandler digitalWorkFlowHandlerTest() {
            return new DigitalWorkFlowHandler(externalChannelService,
                    notificationService, schedulerService,
                    digitalWorkflowService, completionWorkFlowHandler,
                    timelineService, publicRegistryService);
        }

        @Bean
        public ExternalChannelResponseHandler externalChannelResponseHandlerTest() {
            return new ExternalChannelResponseHandler(digitalWorkFlowHandler, null);
        }

        @Bean
        public DigitalWorkFlowService digitalWorkFlowServiceImplTest() {
            return new DigitalWorkFlowServiceImpl(timelineService, publicRegistryService, addressBook);
        }

        //TODO Capire come generare una configurazione in fase di test e non utilizzare il mock
        @Bean
        public PnDeliveryPushConfigs pnDeliveryPushConfigsTest() {
            return Mockito.mock(PnDeliveryPushConfigs.class);
        }

        @Bean
        public ExternalChannelService externalChannelServiceTest() {
            return new ExternalChannelServiceImpl(timelineService, extChnEventUtils, externalChannel);
        }

        @Bean
        public PublicRegistryServiceImpl publicRegistryServiceTest() {
            return new PublicRegistryServiceImpl(publicRegistry, timelineService);
        }

        @Bean
        public CourtesyMessageService courtesyMessageServiceTest() {
            return new CourtesyMessageServiceImpl(addressBook, externalChannelService, timelineService);
        }

        @Bean
        public NotificationService notificationServiceTest() {
            return new NotificationServiceImpl(notificationDao);
        }

        @Bean
        public LegalFactUtils testLegalFactsTest() {
            return Mockito.mock(LegalFactUtils.class);
        }

        @Bean
        public ExtChnEventUtils extChnEventUtilsTest() {
            return new ExtChnEventUtils(pnDeliveryPushConfigs);
        }

        @Bean
        public TimelineService timelineServiceTest() {
            return new TimeLineServiceImpl(timelineDao);
        }

        @Bean
        public ChooseDeliveryModeHandler chooseDeliveryModeHandlerTest() {
            return new ChooseDeliveryModeHandler(addressBook, timelineService,
                    notificationService, externalChannelService,
                    courtesyMessageService, schedulerService,
                    publicRegistryService);
        }

        @Bean
        public StartWorkflowHandler testStartWorkflow() {
            return new StartWorkflowHandler(legalFactUtils, notificationService, courtesyMessageService, chooseDeliveryModeHandler, timelineService);
        }

        @PostConstruct
        public void initMock() {
            Mockito.when(pnDeliveryPushConfigs.getWebapp()).thenReturn(new it.pagopa.pn.deliverypush.PnDeliveryPushConfigs.Webapp());
        }
    }

    @Autowired
    private StartWorkflowHandler startWorkflowHandler;

    @Test
    void workflowTest() {

        startWorkflowHandler.startWorkflow("IUN_01");
    }

}
