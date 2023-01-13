package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.BaseRecipientDto;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.datavault.PnDataVaultClientReactive;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PnDataVaultClientReactiveMock implements PnDataVaultClientReactive {
    ConcurrentMap<String, BaseRecipientDto> confidentialMap;
    
    public void clear() {
        this.confidentialMap = new ConcurrentHashMap<>();
    }
    
    public void insertBaseRecipientDto(BaseRecipientDto dto){
        confidentialMap.put(dto.getInternalId(), dto);
    }
    
    @Override
    public Flux<BaseRecipientDto> getRecipientDenominationByInternalId(List<String> listInternalId) {
        return Flux.fromStream(listInternalId.stream()
                .filter( internalId -> confidentialMap.get(internalId) != null)
                .map(internalId -> confidentialMap.get(internalId)));
    }
}
