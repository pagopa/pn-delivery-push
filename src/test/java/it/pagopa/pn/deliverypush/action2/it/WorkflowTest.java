package it.pagopa.pn.deliverypush.action2.it;

import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.api.dto.events.ServiceLevelType;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.commons.abstractions.IdConflictException;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        StartWorkflowHandler.class,
        NotificationServiceImpl.class,
        TimelineDaoTest.class,
        TimeLineServiceImpl.class,
        CourtesyMessageServiceImpl.class,
        AddressBookTest.class,
        ExternalChannelServiceImpl.class,
        ExtChnEventUtils.class,
        PnDeliveryPushConfigs.class,
        WorkflowTest.SpringTestConfiguration.class
})
class WorkflowTest {

    @TestConfiguration
    static class SpringTestConfiguration extends AbstractWorkflowTestConfiguration {

        public SpringTestConfiguration() {
            super( WorkflowTest.SIMPLE_NOTIFICATION );
        }


        /*@Autowired
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
        }*/
    }

    @Autowired
    private StartWorkflowHandler startWorkflowHandler;

    @Autowired
    private ChooseDeliveryModeHandler chooseDeliveryModeHandler;

    @Test
    void workflowTest() {

        startWorkflowHandler.startWorkflow("IUN_01");

        Mockito.verify( chooseDeliveryModeHandler, Mockito.times(1) )
                .chooseDeliveryTypeAndStartWorkflow( SIMPLE_NOTIFICATION, SIMPLE_NOTIFICATION.getRecipients().get(0));
    }


    private static Notification SIMPLE_NOTIFICATION = Notification.builder()
                .iun("IUN_01")
                .paNotificationId("protocol_01")
                .subject("Subject 01")
                .physicalCommunicationType(ServiceLevelType.SIMPLE_REGISTERED_LETTER)
                .cancelledByIun("IUN_05")
                .cancelledIun("IUN_00")
                .sender(NotificationSender.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipient.builder()
                                .taxId("Codice Fiscale 01")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(DigitalAddress.builder()
                                        .type(DigitalAddressType.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .build()
                ))
                .documents(Arrays.asList(
                        NotificationAttachment.builder()
                                .ref(NotificationAttachment.Ref.builder()
                                        .key("key_doc00")
                                        .versionToken("v01_doc00")
                                        .build()
                                )
                                .digests(NotificationAttachment.Digests.builder()
                                        .sha256("sha256_doc00")
                                        .build()
                                )
                                .build(),
                        NotificationAttachment.builder()
                                .ref(NotificationAttachment.Ref.builder()
                                        .key("key_doc01")
                                        .versionToken("v01_doc01")
                                        .build()
                                )
                                .digests(NotificationAttachment.Digests.builder()
                                        .sha256("sha256_doc01")
                                        .build()
                                )
                                .build()
                ))
                .build();
}
