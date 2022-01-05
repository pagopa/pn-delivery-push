package it.pagopa.pn.deliverypush.actions;

import it.pagopa.pn.api.dto.events.*;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ExtChnEventUtils {

    private final PnDeliveryPushConfigs cfg;

    public ExtChnEventUtils(PnDeliveryPushConfigs cfg) {
        this.cfg = cfg;
    }
    
    public PnExtChnEmailEvent buildSendEmailRequest(
            Action action,
            Notification notification,
            NotificationRecipient recipient,
            int courtesyAddressIdx,
            DigitalAddress emailAddress
    ) {
        final String accessUrl = getAccessUrl(recipient);
        return PnExtChnEmailEvent.builder()
                .header(StandardEventHeader.builder()
                        .iun(action.getIun())
                        .eventId(action.getActionId() + "_" + courtesyAddressIdx)
                        .eventType(EventType.SEND_COURTESY_EMAIL.name())
                        .publisher(EventPublisher.DELIVERY_PUSH.name())
                        .createdAt(Instant.now())
                        .build()
                )
                .payload(PnExtChnEmailEventPayload.builder()
                        .iun(notification.getIun())
                        .senderId(notification.getSender().getPaId())
                        .senderDenomination(notification.getSender().getPaId())
                        .senderEmailAddress("Not required")
                        .recipientDenomination(recipient.getDenomination())
                        .recipientTaxId(recipient.getTaxId())
                        .emailAddress(emailAddress.getAddress())
                        .shipmentDate(notification.getSentAt())
                        .accessUrl(accessUrl)
                        .build()
                )
                .build();
    }

    private String getAccessUrl(NotificationRecipient recipient) {
        return String.format(cfg.getWebapp().getDirectAccessUrlTemplate(), recipient.getToken());
    }

	/*public PnExtChnPaperEvent buildSendPaperRequest (
			Action action,
			Notification notification,
			CommunicationType communicationType
			) {
		return buildSendPaperRequest(action, notification, communicationType, notification.getPhysicalCommunicationType());
	}*/

    public PnExtChnPaperEvent buildSendPaperRequest(
            Action action,
            Notification notification,
            CommunicationType communicationType,
            ServiceLevelType serviceLevelType
    ) {
        return buildSendPaperRequest(action, notification, communicationType, serviceLevelType, null);
    }

    public PnExtChnPaperEvent buildSendPaperRequest(
            Action action,
            Notification notification,
            CommunicationType communicationType,
            ServiceLevelType serviceLevelType,
            PhysicalAddress address) {
        return buildSendPaperRequest(action, notification, communicationType, serviceLevelType, false, address);
    }


    public PnExtChnPaperEvent buildSendPaperRequest(
            Action action,
            Notification notification,
            CommunicationType communicationType,
            ServiceLevelType serviceLevelType,
            boolean investigation,
            PhysicalAddress address
    ) {
        NotificationRecipient recipient = notification.getRecipients().get(action.getRecipientIndex());
        PhysicalAddress usedAddress = address != null ? address : recipient.getPhysicalAddress();
        final String accessUrl = getAccessUrl(recipient);

        return PnExtChnPaperEvent.builder()
                .header(StandardEventHeader.builder()
                        .iun(action.getIun())
                        .eventId(action.getActionId())
                        .eventType(EventType.SEND_PAPER_REQUEST.name())
                        .publisher(EventPublisher.DELIVERY_PUSH.name())
                        .createdAt(Instant.now())
                        .build()
                )
                .payload(PnExtChnPaperEventPayload.builder()
                        .iun(action.getIun())
                        .requestCorrelationId(action.getActionId())
                        .destinationAddress(usedAddress)
                        .recipientDenomination(recipient.getDenomination())
                        .communicationType(communicationType)
                        .serviceLevel(serviceLevelType)
                        .senderDenomination(notification.getSender().getPaId())
                        .investigation(investigation)
                        .accessUrl(accessUrl)
                        .build()
                )
                .build();
    }


    public PnExtChnPecEvent buildSendPecRequest(Action action, Notification notification,
                                                NotificationRecipient recipient, DigitalAddress address) {
        final String accessUrl = getAccessUrl(recipient);
        return PnExtChnPecEvent.builder()
                .header(StandardEventHeader.builder()
                        .iun(action.getIun())
                        .eventId(action.getActionId())
                        .eventType(EventType.SEND_PEC_REQUEST.name())
                        .publisher(EventPublisher.DELIVERY_PUSH.name())
                        .createdAt(Instant.now())
                        .build()
                )
                .payload(PnExtChnPecEventPayload.builder()
                        .iun(notification.getIun())
                        .requestCorrelationId(action.getActionId())
                        .recipientTaxId(recipient.getTaxId())
                        .recipientDenomination(recipient.getDenomination())
                        .senderId(notification.getSender().getPaId())
                        .senderDenomination(notification.getSender().getPaId())
                        .senderPecAddress("Not required")
                        .pecAddress(address.getAddress())
                        .shipmentDate(notification.getSentAt())
                        .accessUrl(accessUrl)
                        .build()
                )
                .build();
    }
}
