package it.pagopa.pn.deliverypush.action2.it;

import it.pagopa.pn.api.dto.events.ServiceLevelType;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action2.ChooseDeliveryModeHandler;
import it.pagopa.pn.deliverypush.action2.StartWorkflowHandler;
import it.pagopa.pn.deliverypush.action2.it.testbean.AddressBookTest;
import it.pagopa.pn.deliverypush.action2.it.testbean.TimelineDaoTest;
import it.pagopa.pn.deliverypush.action2.utils.CourtesyMessageUtils;
import it.pagopa.pn.deliverypush.action2.utils.ExternalChannelUtils;
import it.pagopa.pn.deliverypush.action2.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.actions.ExtChnEventUtils;
import it.pagopa.pn.deliverypush.service.impl.NotificationServiceImpl;
import it.pagopa.pn.deliverypush.service.impl.TimeLineServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.Collections;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        StartWorkflowHandler.class,
        NotificationServiceImpl.class,
        TimelineDaoTest.class,
        TimeLineServiceImpl.class,
        CourtesyMessageUtils.class,
        AddressBookTest.class,
        ExternalChannelUtils.class,
        ExtChnEventUtils.class,
        PnDeliveryPushConfigs.class,
        TimelineUtils.class,
        WorkflowTest.SpringTestConfiguration.class
})
class WorkflowTest {

    @TestConfiguration
    static class SpringTestConfiguration extends AbstractWorkflowTestConfiguration {

        public SpringTestConfiguration() {
            super(WorkflowTest.SIMPLE_NOTIFICATION);
        }

    }

    @Autowired
    private StartWorkflowHandler startWorkflowHandler;

    @Autowired
    private ChooseDeliveryModeHandler chooseDeliveryModeHandler;

    @Test
    void workflowTest() {

        startWorkflowHandler.startWorkflow("IUN_01");

        Mockito.verify(chooseDeliveryModeHandler, Mockito.times(1))
                .chooseDeliveryTypeAndStartWorkflow(SIMPLE_NOTIFICATION, SIMPLE_NOTIFICATION.getRecipients().get(0));
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
