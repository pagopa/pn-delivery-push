package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.ConfidentialTimelineElementDto;
import it.pagopa.pn.deliverypush.dto.ext.datavault.BaseRecipientDtoInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.ConfidentialTimelineElementDtoInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.externalclient.pnclient.datavault.PnDataVaultClient;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementDetails;
import it.pagopa.pn.deliverypush.service.ConfidentialInformationService;
import it.pagopa.pn.deliverypush.service.mapper.ConfidentialTimelineElementDtoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ConfidentialInformationServiceImpl implements ConfidentialInformationService {
    private final PnDataVaultClient pnDataVaultClient;

    public ConfidentialInformationServiceImpl(PnDataVaultClient pnDataVaultClient) {
        this.pnDataVaultClient = pnDataVaultClient;
    }
    
    @Override
    public void saveTimelineConfidentialInformation(TimelineElementInternal timelineElement) {
        String iun = timelineElement.getIun();
        if (checkPresenceConfidentialInformation(timelineElement)){

            ConfidentialTimelineElementDtoInt dtoInt = getConfidentialDtoFromTimeline(timelineElement);

            ConfidentialTimelineElementDto dtoExt = ConfidentialTimelineElementDtoMapper.internalToExternal(dtoInt);
            ResponseEntity<Void> resp = pnDataVaultClient.updateNotificationTimelineByIunAndTimelineElementId(iun, dtoExt);

            if (resp.getStatusCode().is2xxSuccessful()) {
                log.debug("UpdateNotificationTimelineByIunAndTimelineElementId OK for - iun {} timelineElementId {}", iun, dtoInt.getTimelineElementId());
            } else {
                log.error("UpdateNotificationTimelineByIunAndTimelineElementId Failed for - iun {} timelineElementId {}", iun, dtoInt.getTimelineElementId());
                throw new PnInternalException("UpdateNotificationTimelineByIunAndTimelineElementId Failed for - iun " + iun +" timelineElementId "+ dtoInt.getTimelineElementId());
            }
        }
    }

    private ConfidentialTimelineElementDtoInt getConfidentialDtoFromTimeline(TimelineElementInternal timelineElement) {
        TimelineElementDetails details = timelineElement.getDetails();
        
        ConfidentialTimelineElementDtoInt.ConfidentialTimelineElementDtoIntBuilder dtoIntBuilder = ConfidentialTimelineElementDtoInt.builder()
                .timelineElementId(timelineElement.getElementId())
                .physicalAddress(details.getPhysicalAddress())
                .newPhysicalAddress(details.getNewAddress());

        if (details.getDigitalAddress() != null){
            dtoIntBuilder.digitalAddress(details.getDigitalAddress().getAddress());
        }

        return dtoIntBuilder.build();
    }

    @Override
    public ConfidentialTimelineElementDtoInt getTimelineConfidentialInformation(String iun, String timelineElementId) {
        ResponseEntity<ConfidentialTimelineElementDto> resp = pnDataVaultClient.getNotificationTimelineByIunAndTimelineElementId(iun, timelineElementId);
        if (resp.getStatusCode().is2xxSuccessful()) {
            log.debug("getNotificationTimelineByIunAndTimelineElementId OK for - iun {} timelineElementId {}", iun, timelineElementId);
            ConfidentialTimelineElementDto dtoExt = resp.getBody();

            if (dtoExt != null){
                return ConfidentialTimelineElementDtoMapper.externalToInternal(dtoExt);
            }else {
                log.error("getNotificationTimelineByIunAndTimelineElementId Failed for - iun {} timelineElementId {}", iun, timelineElementId);
                throw new PnInternalException("getNotificationTimelineByIunAndTimelineElementId Failed for - iun " + iun +" timelineElementId "+ timelineElementId);
            }
        } else {
            log.error("getNotificationTimelineByIunAndTimelineElementId Failed for - iun {} timelineElementId {}", iun, timelineElementId);
            throw new PnInternalException("getNotificationTimelineByIunAndTimelineElementId Failed for - iun " + iun +" timelineElementId "+ timelineElementId);
        }
    }
    
    @Override
    public ResponseEntity<List<BaseRecipientDtoInt>> getRecipientDenominationByInternalId(List<String> internalId) {
        //TODO Da eliminare probabilmente
        return null;
    }

    private boolean checkPresenceConfidentialInformation(TimelineElementInternal timelineElementInternal) {
        TimelineElementDetails details = timelineElementInternal.getDetails();
        return details.getNewAddress() != null || details.getDigitalAddress() != null || details.getPhysicalAddress() != null;
    }
}
