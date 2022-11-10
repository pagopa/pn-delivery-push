package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.ConfidentialTimelineElementDto;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.datavault.PnDataVaultClient;
import org.springframework.http.ResponseEntity;

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
    public ResponseEntity<Void> updateNotificationTimelineByIunAndTimelineElementId(String iun, ConfidentialTimelineElementDto dto) {
        String iunTimelineId = getId(iun, dto.getTimelineElementId());
        if(confidentialMap.get(iunTimelineId) !=null){
            System.out.println("ERRORE ERRORE TIMELINE_ID "+dto.getTimelineElementId());
        }
        confidentialMap.put(iunTimelineId, dto);
        return ResponseEntity.ok(null);
    }

    @Override
    public ResponseEntity<ConfidentialTimelineElementDto> getNotificationTimelineByIunAndTimelineElementId(String iun, String timelineElementId) {
        String iunTimelineId = getId(iun, timelineElementId);
        return ResponseEntity.ok(confidentialMap.get(iunTimelineId));
    }

    @Override
    public ResponseEntity<List<ConfidentialTimelineElementDto>> getNotificationTimelineByIunWithHttpInfo(String iun) {
        List<ConfidentialTimelineElementDto> listConfElement = new ArrayList<>();
        
        for (Map.Entry<String, ConfidentialTimelineElementDto> entry : confidentialMap.entrySet()) {
            if(entry.getKey().startsWith(iun)){
                listConfElement.add(entry.getValue());
            }
        }
        return ResponseEntity.ok(listConfElement);
    }
    
    private String getId(String iun, String timelineElementId){
        return iun +"_"+ timelineElementId;
    }
}
