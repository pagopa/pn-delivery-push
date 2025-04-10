package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault.model.*;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.NotificationPhysicalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.SentNotificationV25;
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
    private PnDeliveryClientMock pnDeliveryClientMock;
    
    public void clear() {
        this.confidentialMap = new ConcurrentHashMap<>();
        this.normalizedAddress = new ConcurrentHashMap<>();
    }
    
    public void insertBaseRecipientDto(BaseRecipientDto dto){
        confidentialMap.put(dto.getInternalId(), dto);
    }

    public void setPnDeliveryClientMock(PnDeliveryClientMock pnDeliveryClientMock) {
        this.pnDeliveryClientMock = pnDeliveryClientMock;
    }
    
    @Override
    public Flux<BaseRecipientDto> getRecipientsDenominationByInternalId(List<String> listInternalId) {
        return Flux.fromStream(listInternalId.stream()
                .filter( internalId -> confidentialMap.get(internalId) != null)
                .map(internalId -> confidentialMap.get(internalId)));
    }

    @Override
    public Flux<ConfidentialTimelineElementDto> getNotificationTimelines(List<ConfidentialTimelineElementId> confidentialTimelineElementId) {
        return null;
    }

    @Override
    public Mono<Void> updateNotificationAddressesByIun(String iun, Boolean normalized, List<NotificationRecipientAddressesDto> list) {
        if (normalized) {
            return Mono.fromRunnable( () -> {
                int recIndex = 0;
                for (NotificationRecipientAddressesDto recNormAddress : list ){
                    String key = getKey(iun, recIndex);
                    normalizedAddress.put(key, recNormAddress);
                    log.info("[TEST] normalized address isert is {}", recNormAddress);
                    recIndex ++;
                }
            }).flatMap( res-> Mono.empty());
        } else {
            SentNotificationV25 notification = pnDeliveryClientMock.getNotification(iun);
            for (NotificationRecipientAddressesDto recNormAddress : list ){
                log.info("[TEST] normalized address isert is {}", recNormAddress);
                notification.getRecipients().get(recNormAddress.getRecIndex())
                        .setPhysicalAddress(mapToNotificationPhysicalAddress(recNormAddress.getPhysicalAddress()));
            }
            return Mono.empty();
        }
    }

    private NotificationPhysicalAddress mapToNotificationPhysicalAddress(AnalogDomicile dto) {
        NotificationPhysicalAddress notificationPhysicalAddress = new NotificationPhysicalAddress();
        notificationPhysicalAddress.setAt(dto.getAt());
        notificationPhysicalAddress.setAddress(dto.getAddress());
        notificationPhysicalAddress.setProvince(dto.getProvince());
        notificationPhysicalAddress.setMunicipality(dto.getMunicipality());
        return notificationPhysicalAddress;
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