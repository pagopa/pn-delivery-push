package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault.model.AnalogDomicile;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault.model.NotificationRecipientAddressesDto;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.*;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery.PnDeliveryClient;
import it.pagopa.pn.deliverypush.service.mapper.NotificationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Slf4j
public class PnDeliveryClientMock implements PnDeliveryClient {
    private CopyOnWriteArrayList<SentNotificationV21> notifications;

    private final PnDataVaultClientReactiveMock pnDataVaultClientReactiveMock;

    public PnDeliveryClientMock( @Lazy PnDataVaultClientReactiveMock pnDataVaultClientReactiveMock) {
        this.pnDataVaultClientReactiveMock = pnDataVaultClientReactiveMock;
    }

    public void clear() {
        this.notifications = new CopyOnWriteArrayList<>();
    }

    public void addNotification(NotificationInt notification) {
        SentNotificationV21 sentNotification = NotificationMapper.internalToExternal(notification);
        this.notifications.add(sentNotification);
        log.info("ADDED_IUN:" + notification.getIun());
    }
    
    @Override
    public void updateStatus(it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.RequestUpdateStatusDto dto) {
        //Nothing to do
    }

    @Override
    public SentNotificationV21 getSentNotification(String iun) {
        Optional<SentNotificationV21> sentNotificationOpt = notifications.stream().filter(notification -> iun.equals(notification.getIun())).findFirst();
        if(sentNotificationOpt.isPresent()){
            SentNotificationV21 sentNotification = sentNotificationOpt.get();
            List<NotificationRecipientV21> listRecipient = sentNotification.getRecipients();
            
            int recIndex = 0;
            for (NotificationRecipientV21 recipient : listRecipient){
                
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

            return sentNotificationOpt.get();
        }
        throw new RuntimeException("Test error, iun is not presente in getSentNotification IUN:" + iun);
    }

    @Override
    public Map<String, String> getQuickAccessLinkTokensPrivate(String iun) {
      Map<String, String> body = this.notifications.stream()
      .filter(n->n.getIun().equals(iun))
      .map(SentNotificationV21::getRecipients)
      .flatMap(List::stream)
      .collect(Collectors.toMap(NotificationRecipientV21::getInternalId, (n) -> "test"));
      return body;
    }
}
