package it.pagopa.pn.deliverypush.action.completionworkflow;

import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SimpleRegisteredLetterDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.service.SaveLegalFactsService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK;

class PecDeliveryWorkflowLegalFactsGeneratorTest {
    @Mock
    private TimelineService timelineService;
    @Mock
    private SaveLegalFactsService saveLegalFactsService;
    @Mock
    private NotificationUtils notificationUtils;

    private PecDeliveryWorkflowLegalFactsGenerator pecDeliveryWorkflowLegalFactsGenerator;
    
    @BeforeEach
    public void setup() {
        pecDeliveryWorkflowLegalFactsGenerator = new PecDeliveryWorkflowLegalFactsGenerator(
                timelineService,
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

        List<TimelineElementInternal> timeline = getTimeline(notification.getIun(), recIndex);

        Mockito.when(timelineService.getTimeline(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(new HashSet<>(timeline));
        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);
        
        EndWorkflowStatus status = EndWorkflowStatus.SUCCESS;
        Instant completionWorkflowDate = Instant.now();

        //WHEN
        pecDeliveryWorkflowLegalFactsGenerator.generatePecDeliveryWorkflowLegalFact(notification, recIndex, status, completionWorkflowDate);

        TimelineElementInternal timelineElementInternal = timeline.get(0);
        SendDigitalFeedbackDetailsInt details = (SendDigitalFeedbackDetailsInt) timelineElementInternal.getDetails();

        //THEN
        Mockito.verify(saveLegalFactsService).savePecDeliveryWorkflowLegalFact(
                Collections.singletonList(details),
                notification,
                recipient,
                status,
                completionWorkflowDate
        );

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void generatePecDeliveryWorkflowLegalFactWithFeedbackAndRegisteredLetter() {
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

        List<TimelineElementInternal> timeline = getTimelineWithRegisteredLetter(notification.getIun(), recIndex);

        Mockito.when(timelineService.getTimeline(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(new HashSet<>(timeline));
        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);

        //WHEN
        EndWorkflowStatus status = EndWorkflowStatus.SUCCESS;
        Instant completionWorkflowDate = Instant.now();
        pecDeliveryWorkflowLegalFactsGenerator.generatePecDeliveryWorkflowLegalFact(notification, recIndex, status, completionWorkflowDate);

        TimelineElementInternal timelineElementInternal = timeline.get(0);
        SendDigitalFeedbackDetailsInt sendDigitalFeedbackDetailsInt = (SendDigitalFeedbackDetailsInt) timelineElementInternal.getDetails();

        TimelineElementInternal timelineElementInternal2 = timeline.get(1);
        SimpleRegisteredLetterDetailsInt registeredLetterDetails = (SimpleRegisteredLetterDetailsInt) timelineElementInternal2.getDetails();

        //THEN
        Mockito.verify(saveLegalFactsService).savePecDeliveryWorkflowLegalFact(
                Collections.singletonList(sendDigitalFeedbackDetailsInt),
                notification,
                recipient,
                status,
                completionWorkflowDate
        );
    }
    
    private List<TimelineElementInternal> getTimeline(String iun, int recIndex){
        List<TimelineElementInternal> timelineElementList = new ArrayList<>();
        TimelineElementInternal timelineElementInternal = getSendDigitalFeedbackDetailsTimelineElement(iun, recIndex);
        timelineElementList.add(timelineElementInternal);
        return timelineElementList;
    }

    private TimelineElementInternal getSendDigitalFeedbackDetailsTimelineElement(String iun, int recIndex) {
        SendDigitalFeedbackDetailsInt details =  SendDigitalFeedbackDetailsInt.builder()
                .recIndex(recIndex)
                .build();
        return TimelineElementInternal.builder()
                .timestamp(Instant.now())
                .elementId("elementId")
                .category(SEND_DIGITAL_FEEDBACK)
                .iun(iun)
                .details( details )
                .build();
    }

    private List<TimelineElementInternal> getTimelineWithRegisteredLetter(String iun, int recIndex){
        List<TimelineElementInternal> timelineElementList = new ArrayList<>();
        timelineElementList.add(getSendDigitalFeedbackDetailsTimelineElement(iun, recIndex));
        timelineElementList.add(getRegisteredLetterDetailsTimelineElement(iun, recIndex));
        return timelineElementList;
    }

    private TimelineElementInternal getRegisteredLetterDetailsTimelineElement(String iun, int recIndex) {
        SimpleRegisteredLetterDetailsInt details =  SimpleRegisteredLetterDetailsInt.builder()
                .recIndex(recIndex)
                .physicalAddress(
                        PhysicalAddressInt.builder()
                                .at("001")
                                .address("002")
                                .addressDetails("003")
                                .zip("004")
                                .municipality("005")
                                .province("007")
                                .foreignState("008").build()
                )
                .build();

        return TimelineElementInternal.builder()
                .elementId("elementId2")
                .timestamp(Instant.now())
                .category(TimelineElementCategoryInt.SEND_SIMPLE_REGISTERED_LETTER)
                .iun(iun)
                .details( details )
                .build();
    }
}