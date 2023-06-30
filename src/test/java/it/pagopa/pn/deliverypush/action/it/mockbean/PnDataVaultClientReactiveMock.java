package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault.model.BaseRecipientDto;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault.model.NotificationRecipientAddressesDto;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.datavault.PnDataVaultClientReactive;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class PnDataVaultClientReactiveMock implements PnDataVaultClientReactive {
    private ConcurrentMap<String, BaseRecipientDto> confidentialMap;
    private ConcurrentMap<String, NotificationRecipientAddressesDto> normalizedAddress;
    
    public void clear() {
        this.confidentialMap = new ConcurrentHashMap<>();
        this.normalizedAddress = new ConcurrentHashMap<>();
    }
    
    public void insertBaseRecipientDto(BaseRecipientDto dto){
        confidentialMap.put(dto.getInternalId(), dto);
    }
    
    @Override
    public Flux<BaseRecipientDto> getRecipientsDenominationByInternalId(List<String> listInternalId) {
        return Flux.fromStream(listInternalId.stream()
                .filter( internalId -> confidentialMap.get(internalId) != null)
                .map(internalId -> confidentialMap.get(internalId)));
    }

    @Override
    public Mono<Void> updateNotificationAddressesByIun(String iun, Boolean normalized, List<NotificationRecipientAddressesDto> list) {
        return Mono.fromRunnable( () -> {
            int recIndex = 0;
            for (NotificationRecipientAddressesDto recNormAddress : list ){
                String key = getKey(iun, recIndex);
                normalizedAddress.put(key, recNormAddress);
                log.info("[TEST] normalized address isert is {}", recNormAddress);
                recIndex ++;
            } 
        }).flatMap( res-> Mono.empty());
    }

    @NotNull
    private static String getKey(String iun, int recIndex) {
        return iun + "_" +recIndex;
    }

    public NotificationRecipientAddressesDto getAddressFromRecipientIndex(String iun, int rexIndex){
        String key = getKey(iun, rexIndex);
        return normalizedAddress.get(key);
    }
}