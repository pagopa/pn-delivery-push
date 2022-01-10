package it.pagopa.pn.deliverypush.action2.it;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action2.*;
import it.pagopa.pn.deliverypush.action2.it.mockbean.*;
import it.pagopa.pn.deliverypush.action2.utils.*;
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

import java.util.ArrayList;
import java.util.List;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        StartWorkflowHandler.class,
        AnalogWorkflowHandler.class,
        ChooseDeliveryModeHandler.class,
        DigitalWorkFlowHandler.class,
        CompletionWorkFlowHandler.class,
        PublicRegistryResponseHandler.class,
        ExternalChannelResponseHandler.class,
        RefinementHandler.class,
        DigitalWorkFlowUtils.class,
        CourtesyMessageUtils.class,
        ExternalChannelUtils.class,
        CompletelyUnreachableUtils.class,
        ExtChnEventUtils.class,
        AnalogWorkflowUtils.class,
        TimelineUtils.class,
        PublicRegistryUtils.class,
        NotificationServiceImpl.class,
        TimeLineServiceImpl.class,
        PnDeliveryPushConfigs.class,
        PaperNotificationFailedDaoMock.class,
        TimelineDaoMock.class,
        SchedulerServiceMock.class,
        PublicRegistryMock.class,
        ExternalChannelMock.class,
        WorkflowTest.SpringTestConfiguration.class
})
class WorkflowTest {
    private static final List<Notification> listNotification = new ArrayList<>(TestUtils.getListNotification());
    private static final String taxId = listNotification.get(0).getRecipients().get(0).getTaxId();

    @TestConfiguration
    static class SpringTestConfiguration extends AbstractWorkflowTestConfiguration {

        public SpringTestConfiguration() {
            super(listNotification, TestUtils.getListAddressBook(taxId));
        }

    }

    @Autowired
    private StartWorkflowHandler startWorkflowHandler;

    @Autowired
    private ChooseDeliveryModeHandler chooseDeliveryModeHandler;

    @Test
    void AnalogWorkflowTest() {
        /*Workflow analogico
           - Platform address vuoto (Ottenuto non valorizzando il digitalAddresses.getPlatform() dei digitaladdresses dell'address book Definito in LIST_ADDRESS_BOOK)
           - Special address vuoto (Ottenuto non valorizzando recipient.getDigitalDomicile() della notifica)
           - General address vuoto (Ottenuto inserendo PB_DIGITAL_FAILURE nel taxId)
           
           - Courtesy message presente dunque inviato (Ottenuto valorizzando courtesyAddresses dell'addressboook in LIST_ADDRESS_BOOK)
           - Pa physical address presente (Ottenuto valorizzando recipient.physicalAddress della notifica)
           - External channel First send KO (Ottenuto inserendo la dicitura EXT_ANALOG_FAILURE in PhysicalAddress.address nella notifica)
           - Public Registry Indirizzo non trovato KO (Ottenuto inserendo PB_ANALOG_FAILURE nel taxId)
           - Indirizzo investigazione presente ma con successivo fallimento in invio (Ottenuto inserendo INVESTIGATION_ADDRESS_PRESENT_FAILURE PhysicalAddress.address)
         */
        //Notifica utilizzata
        Notification notification = listNotification.get(0);
        //Start del workflow
        startWorkflowHandler.startWorkflow(notification.getIun());

        Mockito.verify(chooseDeliveryModeHandler, Mockito.times(1))
                .chooseDeliveryTypeAndStartWorkflow(notification, notification.getRecipients().get(0));
    }


}
