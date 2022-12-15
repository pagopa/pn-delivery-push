package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel;

import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@AllArgsConstructor
@EqualsAndHashCode
public class PaperChannelSendRequest {
    private final NotificationInt notificationInt;
    private final NotificationRecipientInt recipientInt;
    private final PhysicalAddressInt receiverAddress;
    private final String requestId;
    private final String productType;
    private final List<String> attachments;
    private final PhysicalAddressInt arAddress;
    private final PhysicalAddressInt senderAddress;

}
