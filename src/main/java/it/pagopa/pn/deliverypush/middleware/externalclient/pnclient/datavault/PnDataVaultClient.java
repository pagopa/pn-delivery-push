package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.datavault;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault.model.ConfidentialTimelineElementDto;

import java.util.List;

public interface PnDataVaultClient {
    String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_DATA_VAULT;

    String UPDATE_TIMELINE_ELEMENT_CONF_INFORMATION = "UPDATE TIMELINE ELEMENT CONFIDENTIAL INFORMATION";
    String GET_TIMELINE_ELEMENT_CONF_INFORMATION = "GET TIMELINE ELEMENT CONFIDENTIAL INFORMATION";
    String GET_TIMELINE_CONF_INFORMATION = "GET TIMELINE CONFIDENTIAL INFORMATION";

    
    void updateNotificationTimelineByIunAndTimelineElementId(String iun, ConfidentialTimelineElementDto dto);

    ConfidentialTimelineElementDto getNotificationTimelineByIunAndTimelineElementId(String iun, String timelineElementId);

    List<ConfidentialTimelineElementDto> getNotificationTimelineByIunWithHttpInfo(String iun);
    
}
