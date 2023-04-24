package it.pagopa.pn.deliverypush.action.analogworkflow;

import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.service.SaveLegalFactsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;

class AnalogDeliveryWorkflowFailedLegalFactsGeneratorTest {

    @Mock
    private SaveLegalFactsService saveLegalFactsService;
    @Mock
    private NotificationUtils notificationUtils;

    private AnalogDeliveryFailureWorkflowLegalFactsGenerator analogDeliveryFailureWorkflowLegalFactsGenerator;
    
    @BeforeEach
    public void setup() {
        analogDeliveryFailureWorkflowLegalFactsGenerator = new AnalogDeliveryFailureWorkflowLegalFactsGenerator(
                saveLegalFactsService,
                notificationUtils
        );
    }
    
    @Test
    @ExtendWith(SpringExtension.class)
    void generatePecDeliveryWorkflowLegalFact() {
        //GIVEN

        int recIndex = 0;
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId("taxId")
                .withInternalId("ANON_"+"taxId")
                .withPhysicalAddress(
                        PhysicalAddressBuilder.builder()
                                .withAddress("_Via Nuova")
                                .build()
                )
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("iun")
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();


        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);
        
        EndWorkflowStatus status = EndWorkflowStatus.SUCCESS;
        Instant completionWorkflowDate = Instant.now();

        //WHEN
        analogDeliveryFailureWorkflowLegalFactsGenerator.generateAndSendCreationRequestForAnalogDeliveryFailureWorkflowLegalFact(notification, recIndex, status, completionWorkflowDate);


        //THEN
        Mockito.verify(saveLegalFactsService).sendCreationRequestForAnalogDeliveryFailureWorkflowLegalFact(
                notification,
                recipient,
                status,
                completionWorkflowDate
        );

    }
}
