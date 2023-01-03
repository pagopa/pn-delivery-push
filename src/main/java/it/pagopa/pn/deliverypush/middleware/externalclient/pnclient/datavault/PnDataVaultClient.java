package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.datavault;

import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.BaseRecipientDto;
import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.ConfidentialTimelineElementDto;

import java.util.List;

public interface PnDataVaultClient {
    void updateNotificationTimelineByIunAndTimelineElementId(String iun, ConfidentialTimelineElementDto dto);

    ConfidentialTimelineElementDto getNotificationTimelineByIunAndTimelineElementId(String iun, String timelineElementId);

    List<ConfidentialTimelineElementDto> getNotificationTimelineByIunWithHttpInfo(String iun);

    List<BaseRecipientDto> getRecipientDenominationByInternalId(List<String> listInternalId)

}
