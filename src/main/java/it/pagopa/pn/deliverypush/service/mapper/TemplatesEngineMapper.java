package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine.model.*;
import it.pagopa.pn.deliverypush.legalfacts.CustomInstantWriter;

import java.time.Instant;

public class TemplatesEngineMapper {

    private TemplatesEngineMapper() {
    }

    public static NotificationViewedLegalFact notificationViewedLegalFact(String iun,
                                                                          NotificationRecipientInt recipient,
                                                                          DelegateInfoInt delegateInfo,
                                                                          Instant timeStamp,
                                                                          CustomInstantWriter instantWriter) {
        NotificationViewedRecipient notificationViewedRecipient = new NotificationViewedRecipient()
                .denomination(recipient.getDenomination())
                .taxId(recipient.getTaxId());

        return new NotificationViewedLegalFact()
                .recipient(notificationViewedRecipient)
                .iun(iun)
                .delegate(notificationViewedDelegate(delegateInfo))
                .when(instantWriter.instantToDate(timeStamp));
    }

    private static NotificationViewedDelegate notificationViewedDelegate(DelegateInfoInt delegateInfo) {
        return delegateInfo != null ?
                new NotificationViewedDelegate()
                        .denomination(delegateInfo.getDenomination())
                        .taxId(delegateInfo.getTaxId())
                : null;
    }

}