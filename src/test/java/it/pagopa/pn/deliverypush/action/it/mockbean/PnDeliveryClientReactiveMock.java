package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault.model.AnalogDomicile;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault.model.NotificationRecipientAddressesDto;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.NotificationPhysicalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.NotificationRecipient;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.SentNotification;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery.PnDeliveryClientReactive;
import it.pagopa.pn.deliverypush.service.mapper.NotificationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Slf4j
public class PnDeliveryClientReactiveMock implements PnDeliveryClientReactive {

    private CopyOnWriteArrayList<SentNotification> notifications;

    private PnDataVaultClientReactiveMock pnDataVaultClientReactiveMock;

    public PnDeliveryClientReactiveMock( @Lazy PnDataVaultClientReactiveMock pnDataVaultClientReactiveMock) {
        this.pnDataVaultClientReactiveMock = pnDataVaultClientReactiveMock;
    }

    public void clear() {
        this.notifications = new CopyOnWriteArrayList<>();
    }

    public void addNotification(NotificationInt notification) {
        SentNotification sentNotification = NotificationMapper.internalToExternal(notification);
        this.notifications.add(sentNotification);
    }


    @Override
    public Mono<SentNotification> getSentNotification(String iun) {
        Optional<SentNotification> sentNotificationOpt = notifications.stream().filter(notification -> iun.equals(notification.getIun())).findFirst();
        if(sentNotificationOpt.isPresent()){
            SentNotification sentNotification = sentNotificationOpt.get();
            List<NotificationRecipient> listRecipient = sentNotification.getRecipients();

            int recIndex = 0;
            for (NotificationRecipient recipient : listRecipient){

                NotificationRecipientAddressesDto recipientAddressesDto = pnDataVaultClientReactiveMock.getAddressFromRecipientIndex(iun, recIndex);

                if(recipientAddressesDto != null){
                    final AnalogDomicile normalizedAddress = recipientAddressesDto.getPhysicalAddress();

                    if(normalizedAddress != null){
                        NotificationPhysicalAddress physicalAddress = new NotificationPhysicalAddress()
                                .address(normalizedAddress.getAddress())
                                .addressDetails(normalizedAddress.getAddressDetails())
                                .zip(normalizedAddress.getCap())
                                .at(normalizedAddress.getAt())
                                .municipality(normalizedAddress.getMunicipality())
                                .foreignState(normalizedAddress.getState())
                                .municipalityDetails(normalizedAddress.getMunicipalityDetails())
                                .province(normalizedAddress.getProvince());

                        recipient.setPhysicalAddress(physicalAddress);
                    }
                }

                log.info("[TEST] recipient address returned is {}", recipient.getPhysicalAddress());

                recIndex ++;
            }

            return Mono.just(sentNotificationOpt.get());
        }
        return Mono.error(new RuntimeException("Test error, iun is not presente in getSentNotification"));
    }

    @Override
    public Mono<Void> updateStatus(String iun, NotificationStatusInt notificationStatusInt, Instant updateStatusTimestamp) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> removeAllNotificationCostsByIun(String iun) {
        return Mono.empty();
    }
}
