package it.pagopa.pn.deliverypush.actions;
/*
import it.pagopa.pn.api.dto.events.CommunicationType;
import it.pagopa.pn.api.dto.events.PnExtChnPaperEvent;
import it.pagopa.pn.api.dto.events.ServiceLevelType;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.Notification;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipient;
import *;
import it.pagopa.pn.commons.abstractions.MomProducer;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.middleware.timelinedao.TimelineDao;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PecFailSendPaperActionHandler extends AbstractActionHandler {

    public static final ServiceLevelType DIGITAL_FAILURE_PAPER_FALLBACK_SERVICE_LEVEL = ServiceLevelType.SIMPLE_REGISTERED_LETTER;
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
                .filter( el -> selectPecTimeline(el, recipient))
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
                DIGITAL_FAILURE_PAPER_FALLBACK_SERVICE_LEVEL
            ));


        // - Write timeline
        addTimelineElement( action, TimelineElementInternal.timelineInternalBuilder()
                .category(TimelineElementCategory.SEND_DIGITAL_DOMICILE_FAILURE )
                .details( SendDigitalFailureDetails.builder()
                        .taxId( recipient.getTaxId() )
                        .addresses( usedDigitalDomicile )
                        .build()
                )
                .build()
        );

    }

    private boolean selectPecTimeline(TimelineElementInternalel, NotificationRecipient recipient) {
        boolean ok;
        boolean sendPecCategory = TimelineElementCategory.SEND_DIGITAL_DOMICILE_FEEDBACK.equals(el.getCategory());
        if ( sendPecCategory ) {
            SendDigitalFeedbackDetails details = (SendDigitalFeedbackDetails) el.getDetails();
            ok = recipient.getTaxId().equalsIgnoreCase(details.getTaxId());
        }
        else {
            ok = false;
        }
        return ok;
    }

    @Override
    public ActionType getActionType() {
        return ActionType.PEC_FAIL_SEND_PAPER;
    }

}


 */