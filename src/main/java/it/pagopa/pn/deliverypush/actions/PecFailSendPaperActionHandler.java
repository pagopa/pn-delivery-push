package it.pagopa.pn.deliverypush.actions;

import it.pagopa.pn.api.dto.events.CommunicationType;
import it.pagopa.pn.api.dto.events.PnExtChnPaperEvent;
import it.pagopa.pn.api.dto.events.ServiceLevelType;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.timeline.*;
import it.pagopa.pn.commons.abstractions.MomProducer;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PecFailSendPaperActionHandler extends AbstractActionHandler {

    private final MomProducer<PnExtChnPaperEvent> paperRequestProducer;
    private final TimelineDao timelineDao;
    private final ExtChnEventUtils extChnEventUtils;

    public PecFailSendPaperActionHandler(TimelineDao timelineDao, ActionsPool actionsPool,
                                         PnDeliveryPushConfigs pnDeliveryPushConfigs,
                                         MomProducer<PnExtChnPaperEvent> paperRequestProducer,
                                         ExtChnEventUtils extChnEventUtils
    ) {
		super(timelineDao, actionsPool, pnDeliveryPushConfigs);
        this.paperRequestProducer = paperRequestProducer;
        this.timelineDao = timelineDao;
        this.extChnEventUtils = extChnEventUtils;
    }
    
    @Override
    public void handleAction( Action action, Notification notification ) {
        NotificationRecipient recipient = notification.getRecipients().get(action.getRecipientIndex());

        List<SendDigitalFailureDetails.FailedContact> usedDigitalDomicile = timelineDao.getTimeline( action.getIun() )
                .stream()
                .filter( el -> TimelineElementCategory.SEND_DIGITAL_DOMICILE_FEEDBACK.equals( el.getCategory()))
                .map( el -> SendDigitalFailureDetails.FailedContact.builder()
                        .when( el.getTimestamp())
                        .address( ((SendDigitalFeedbackDetails)el.getDetails()).getAddress() )
                        .build()
                    )
                .sorted( Comparator.comparing( SendDigitalFailureDetails.FailedContact::getWhen ))
                .collect(Collectors.toList());


        this.paperRequestProducer.push( extChnEventUtils.buildSendPaperRequest(
                action,
                notification,
                CommunicationType.RECIEVED_DELIVERY_NOTICE,
                ServiceLevelType.SIMPLE_REGISTERED_LETTER
            ));


        // - Write timeline
        addTimelineElement( action, TimelineElement.builder()
                .category(TimelineElementCategory.SEND_DIGITAL_DOMICILE_FAILURE )
                .details( SendDigitalFailureDetails.builder()
                        .taxId( recipient.getTaxId() )
                        .addresses( usedDigitalDomicile )
                        .build()
                )
                .build()
        );

    }

    @Override
    public ActionType getActionType() {
        return ActionType.PEC_FAIL_SEND_PAPER;
    }

}
