package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.CategorizedAttachmentsResultInt;

import java.util.List;

public interface PaperChannelService {
    void prepareAnalogNotificationForSimpleRegisteredLetter(NotificationInt notification,  Integer recIndex);

    void prepareAnalogNotification(NotificationInt notification, Integer recIndex, int sentAttemptMade);

    String sendSimpleRegisteredLetter(NotificationInt notification, Integer recIndex, String requestId, PhysicalAddressInt receiverAddress, String productType, List<String> replacedF24AttachmentUrls, CategorizedAttachmentsResultInt categorizedAttachmentsResult);

    String sendAnalogNotification(NotificationInt notification, Integer recIndex, int sentAttemptMade, String requestId, PhysicalAddressInt receiverAddress, String productType, List<String> replacedF24AttachmentUrls, CategorizedAttachmentsResultInt categorizedAttachmentsResult);
}
