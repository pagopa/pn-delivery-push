package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.address.DigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine.model.NotificationReceivedDigitalDomicile;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine.model.NotificationReceivedNotification;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine.model.NotificationReceivedRecipient;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

public class TemplatesEngineMapper {

    private TemplatesEngineMapper() {
    }

    public static NotificationReceivedRecipient notificationReceivedNotification(String physicalAddressAndDenomination,
                                                                                 NotificationRecipientInt recipientInt) {
        var digitalDomicile = digitalDomicile(recipientInt);
        return (StringUtils.isBlank(physicalAddressAndDenomination)
                || recipientInt.getDenomination() == null
                || recipientInt.getTaxId() == null
                || digitalDomicile == null)
                ? null :
                new NotificationReceivedRecipient()
                        .physicalAddressAndDenomination(physicalAddressAndDenomination)
                        .denomination(recipientInt.getDenomination())
                        .taxId(recipientInt.getTaxId())
                        .digitalDomicile(digitalDomicile);

    }

    private static NotificationReceivedDigitalDomicile digitalDomicile(NotificationRecipientInt recipientInt) {
        String address = Optional.of(recipientInt).map(NotificationRecipientInt::getDigitalDomicile)
                .map(DigitalAddressInt::getAddress).orElse(null);
        return address != null ? new NotificationReceivedDigitalDomicile().address(address) : null;
    }


}
