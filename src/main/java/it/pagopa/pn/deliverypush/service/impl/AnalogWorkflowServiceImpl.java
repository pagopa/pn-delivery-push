package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.api.dto.notification.timeline.SendPaperDetails;
import it.pagopa.pn.api.dto.notification.timeline.SendPaperFeedbackDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
import it.pagopa.pn.deliverypush.service.AnalogWorkflowService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class AnalogWorkflowServiceImpl implements AnalogWorkflowService {
    private final TimelineService timelineService;

    public AnalogWorkflowServiceImpl(TimelineService timelineService) {
        this.timelineService = timelineService;
    }

    /**
     * Get user sent attempt from timeline
     *
     * @param iun   Notification unique identifier
     * @param taxId User identifier
     * @return user sent attempt
     */
    @Override
    public int getSentAttemptFromTimeLine(String iun, String taxId) {
        return (int) timelineService.getTimeline(iun).stream()
                .filter(timelineElement -> filterTimelineForTaxIdAndSource(timelineElement, taxId)).count();
    }

    private boolean filterTimelineForTaxIdAndSource(TimelineElement el, String taxId) {
        boolean availableAddressCategory = TimelineElementCategory.SEND_ANALOG_DOMICILE.equals(el.getCategory());
        if (availableAddressCategory) {
            SendPaperDetails details = (SendPaperDetails) el.getDetails();
            return taxId.equalsIgnoreCase(details.getTaxId());
        }
        return false;
    }

    /**
     * Get external channel last feedback information from timeline
     *
     * @param iun   Notification unique identifier
     * @param taxId User identifier
     * @return last sent feedback information
     */
    @Override
    public SendPaperFeedbackDetails getLastTimelineSentFeedback(String iun, String taxId) {
        Set<TimelineElement> timeline = timelineService.getTimeline(iun);

        Optional<SendPaperFeedbackDetails> sendPaperFeedbackDetailsOpt = timeline.stream()
                .filter(timelineElement -> filterLastAttemptDateInTimeline(timelineElement, taxId))
                .map(timelineElement -> (SendPaperFeedbackDetails) timelineElement.getDetails()).findFirst();

        if (sendPaperFeedbackDetailsOpt.isPresent()) {
            return sendPaperFeedbackDetailsOpt.get();
        } else {
            //TODO Gestisci casistica di errore
            throw new RuntimeException();
        }
    }

    private boolean filterLastAttemptDateInTimeline(TimelineElement el, String taxId) {
        boolean availableAddressCategory = TimelineElementCategory.SEND_PAPER_FEEDBACK.equals(el.getCategory());
        if (availableAddressCategory) {
            SendPaperFeedbackDetails details = (SendPaperFeedbackDetails) el.getDetails();
            return taxId.equalsIgnoreCase(details.getTaxId());
        }
        return false;
    }

}
