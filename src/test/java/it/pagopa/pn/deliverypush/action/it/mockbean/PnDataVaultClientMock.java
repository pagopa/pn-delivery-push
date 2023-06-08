package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault.model.ConfidentialTimelineElementDto;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.datavault.PnDataVaultClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PnDataVaultClientMock implements PnDataVaultClient {
    ConcurrentMap<String, ConfidentialTimelineElementDto> confidentialMap;
    
    public void clear() {
        this.confidentialMap = new ConcurrentHashMap<>();
    }

    @Override
    public void updateNotificationTimelineByIunAndTimelineElementId(String iun, ConfidentialTimelineElementDto dto) {
        String iunTimelineId = getId(iun, dto.getTimelineElementId());
        if(confidentialMap.get(iunTimelineId) !=null){
            System.out.println("ERRORE ERRORE TIMELINE_ID "+dto.getTimelineElementId());
        }
        confidentialMap.put(iunTimelineId, dto);
    }

    @Override
    public ConfidentialTimelineElementDto getNotificationTimelineByIunAndTimelineElementId(String iun, String timelineElementId) {
        String iunTimelineId = getId(iun, timelineElementId);
        return confidentialMap.get(iunTimelineId);
    }

    @Override
    public List<ConfidentialTimelineElementDto> getNotificationTimelineByIunWithHttpInfo(String iun) {
        List<ConfidentialTimelineElementDto> listConfElement = new ArrayList<>();
        
        for (Map.Entry<String, ConfidentialTimelineElementDto> entry : confidentialMap.entrySet()) {
            if(entry.getKey().startsWith(iun)){
                listConfElement.add(entry.getValue());
            }
        }
        return listConfElement;
    }
    
    private String getId(String iun, String timelineElementId){
        return iun +"_"+ timelineElementId;
    }
}
