package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.datavault;

import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.ConfidentialTimelineElementDto;

import java.util.List;

public interface PnDataVaultClient {
    String CLIENT_NAME = "PN-DATA-VAULT";

    String UPDATE_TIMELINE_ELEMENT_CONF_INFORMATION = "UPDATE TIMELINE ELEMENT CONFIDENTIAL INFORMATION";
    String GET_TIMELINE_ELEMENT_CONF_INFORMATION = "GET TIMELINE ELEMENT CONFIDENTIAL INFORMATION";
    String GET_TIMELINE_CONF_INFORMATION = "GET TIMELINE CONFIDENTIAL INFORMATION";

    
    void updateNotificationTimelineByIunAndTimelineElementId(String iun, ConfidentialTimelineElementDto dto);

    ConfidentialTimelineElementDto getNotificationTimelineByIunAndTimelineElementId(String iun, String timelineElementId);

    List<ConfidentialTimelineElementDto> getNotificationTimelineByIunWithHttpInfo(String iun);
    
}
