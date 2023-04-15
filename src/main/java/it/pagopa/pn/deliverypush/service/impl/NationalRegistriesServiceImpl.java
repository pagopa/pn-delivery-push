package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.utils.LogUtils;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.nationalregistries.CheckTaxIdOKInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.ContactPhaseInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.DeliveryModeInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.publicregistry.NationalRegistriesClient;
import it.pagopa.pn.deliverypush.service.NationalRegistriesService;
import it.pagopa.pn.deliverypush.service.utils.PublicRegistryUtils;
import it.pagopa.pn.nationalregistries.generated.openapi.clients.nationalregistries.model.CheckTaxIdOK;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NationalRegistriesServiceImpl implements NationalRegistriesService {
    private final PublicRegistryUtils publicRegistryUtils;
    private final NationalRegistriesClient nationalRegistriesClient;
    private final NotificationUtils notificationUtils;

    public NationalRegistriesServiceImpl(PublicRegistryUtils publicRegistryUtils,
                                         NationalRegistriesClient nationalRegistriesClient,
                                         NotificationUtils notificationUtils) {
        this.publicRegistryUtils = publicRegistryUtils;
        this.nationalRegistriesClient = nationalRegistriesClient;
        this.notificationUtils = notificationUtils;
    }

    /**
     * Send get request to public registry for get digital address
     **/
    @Override
    public void sendRequestForGetDigitalGeneralAddress(NotificationInt notification, 
                                                       Integer recIndex,
                                                       ContactPhaseInt contactPhase, 
                                                       int sentAttemptMade,
                                                       String relatedFeedbackTimelineId) {

        String correlationId = publicRegistryUtils.generateCorrelationId(notification.getIun(), recIndex, contactPhase, sentAttemptMade, DeliveryModeInt.DIGITAL);
        log.debug("Start Async Request for get general address, correlationId={} - iun={} id={}", correlationId, notification.getIun(), recIndex);

        NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification,recIndex);

        nationalRegistriesClient.sendRequestForGetDigitalAddress(recipient.getTaxId(), recipient.getRecipientType().getValue(), correlationId);
        publicRegistryUtils.addPublicRegistryCallToTimeline(
                notification,
                recIndex,
                contactPhase,
                sentAttemptMade,
                correlationId, 
                DeliveryModeInt.DIGITAL, 
                relatedFeedbackTimelineId);

        log.debug("End sendRequestForGetAddress correlationId={} - iun={} id={}", correlationId, notification.getIun(), recIndex);
    }

    @Override
    public CheckTaxIdOKInt checkTaxId(String taxId) {
        log.info("Start checkTaxId for taxId={}", LogUtils.maskTaxId(taxId));

        CheckTaxIdOK response = nationalRegistriesClient.checkTaxId(taxId);

        return CheckTaxIdOKInt.builder()
                .taxId(taxId)
                .isValid(response.getIsValid())
                .errorCode(response.getErrorCode() != null ? response.getErrorCode().getValue() : null )
                .build();
    }

}
