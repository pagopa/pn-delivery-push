package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.ConfidentialTimelineElementDto;
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
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    public Optional<ConfidentialTimelineElementDtoInt> getTimelineElementConfidentialInformation(String iun, String timelineElementId) {
        ResponseEntity<ConfidentialTimelineElementDto> resp = pnDataVaultClient.getNotificationTimelineByIunAndTimelineElementId(iun, timelineElementId);
        
        if (resp.getStatusCode().is2xxSuccessful()) {
            log.debug("getTimelineElementConfidentialInformation OK for - iun {} timelineElementId {}", iun, timelineElementId);
            ConfidentialTimelineElementDto dtoExt = resp.getBody();

            if (dtoExt != null){
                return Optional.of(ConfidentialTimelineElementDtoMapper.externalToInternal(dtoExt));
            }
            
            log.debug("getTimelineElementConfidentialInformation haven't confidential information for - iun {} timelineElementId {}", iun, timelineElementId);
            return Optional.empty();
        } else {
            log.error("getTimelineElementConfidentialInformation Failed for - iun {} timelineElementId {}", iun, timelineElementId);
            throw new PnInternalException("getTimelineElementConfidentialInformation Failed for - iun " + iun +" timelineElementId "+ timelineElementId);
        }
    }

    @Override
    public Optional<Map<String, ConfidentialTimelineElementDtoInt>> getTimelineConfidentialInformation(String iun) {
        ResponseEntity<List<ConfidentialTimelineElementDto>> resp = pnDataVaultClient.getNotificationTimelineByIunWithHttpInfo(iun);
        
        if (resp.getStatusCode().is2xxSuccessful()) {
            log.debug("getTimelineConfidentialInformation OK for - iun {} ", iun);
            List<ConfidentialTimelineElementDto> listDtoExt = resp.getBody();

            if (listDtoExt != null && !listDtoExt.isEmpty()){
                return Optional.of(
                        listDtoExt.stream()
                        .map(ConfidentialTimelineElementDtoMapper::externalToInternal)
                        .collect(Collectors.toMap(ConfidentialTimelineElementDtoInt::getTimelineElementId, Function.identity()))
                    );
            }
            log.debug("getTimelineConfidentialInformation haven't confidential information for - iun {} ", iun);
            return Optional.empty();
        } else {
            log.error("getTimelineConfidentialInformation Failed - iun {} ", iun);
            throw new PnInternalException("getTimelineConfidentialInformation Failed - iun " + iun);
        }
    }

    private boolean checkPresenceConfidentialInformation(TimelineElementInternal timelineElementInternal) {
        TimelineElementDetails details = timelineElementInternal.getDetails();
        return details.getNewAddress() != null || details.getDigitalAddress() != null || details.getPhysicalAddress() != null;
    }
}
