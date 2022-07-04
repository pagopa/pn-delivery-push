package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.mandate.MandateDtoInt;
import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.service.MandateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
public class AuthUtils {
    private final MandateService mandateService;

    public AuthUtils(MandateService mandateService) {
        this.mandateService = mandateService;
    }

    public void checkAuthorization(NotificationInt notification, String senderRecipientId, String mandateId) {
        String paId = notification.getSender().getPaId();
        String iun = notification.getIun();

        log.info("Start Check authorization - iun={} senderRecipientId={} paId={} mandateId={}", iun, senderRecipientId, paId, mandateId);

        if( StringUtils.hasText( mandateId ) ){
            checkMandate(notification, senderRecipientId, mandateId, paId, iun);
        } else {
            checkPaAndRecipients(notification, senderRecipientId, paId, iun);
        }

        log.info("End Check authorization - iun={} senderRecipientId={} paId={} mandateId={}", iun, senderRecipientId, paId, mandateId);
    }

    private void checkMandate(NotificationInt notification, String senderRecipientId, String mandateId, String paId, String iun) {
        //È presente il mandateId viene verificata la delega
        log.debug("Start Check mandate - iun={} senderRecipientIdcxId={} paId={} mandateId={}", iun, senderRecipientId, paId, mandateId);

        List<MandateDtoInt> listMandateIdDto = mandateService.listMandatesByDelegate(senderRecipientId, mandateId);

        if( !listMandateIdDto.isEmpty() && listMandateIdDto.get(0) != null ){
            log.debug("Mandate is present - iun={} senderRecipientId={} paId={} mandateId={}", iun, senderRecipientId, paId, mandateId);
            verifyMandateData(notification, senderRecipientId, mandateId, paId, iun, listMandateIdDto.get(0));

        }else {
            String message = String.format("Unable to find valid mandate - iun=%s delegate=%s with mandateId=%s", iun, senderRecipientId, mandateId);
            handleError(message);
        }
    }

    private void verifyMandateData(NotificationInt notification, String senderRecipientId, String mandateId, String paId, String iun, MandateDtoInt mandateDtoInt) {
        Instant startMandateDate = mandateDtoInt.getDateFrom();

        //Viene verificato che la notifica contenente i legalFacts che si vogliono visualizzare non sia stata create in una data precedente all'inizio mandato
        if( notification.getSentAt().isBefore(startMandateDate) ){
            String message = String.format("Unable to find valid mandate, notification creation date=%s is not before start mandate mate=%s " +
                    "- iun=%s delegate=%s with mandateId=%s", notification.getSentAt(), startMandateDate, iun, senderRecipientId, mandateId);
            handleError(message);

        } else {
            //Viene verificato se la PaId della notifica è nella lista delle pa visibili per quel mandato
            //Se la visilibity mandate è vuota significa che non ci sono limiti di visualizzazione
            
            if( !mandateDtoInt.getVisibilityIds().isEmpty() ) {
                boolean isPaIdInVisibilityPa = mandateDtoInt.getVisibilityIds().stream().anyMatch(
                        paId::equals
                );

                if( !isPaIdInVisibilityPa ){
                    String message = String.format("Unable to find valid mandate, paNotificationId=%s is not in visibility pa id for mandate" +
                            "- iun=%s delegate=%s with mandateId=%s", paId, iun, senderRecipientId, mandateId);
                    handleError(message);
                }
            }

            log.info("Request is from delegate, authorization is valid");
        }
    }

    private void checkPaAndRecipients(NotificationInt notification, String senderRecipientId, String paId, String iun) {
        log.debug("Start Check request is from PA or from recipients - iun={} senderRecipientId={} paId={}", iun, senderRecipientId, paId);
        
        boolean isRequestFromPa = paId.equals(senderRecipientId);
        
        //Viene verificato se la richiesta proviene dalla Pa indicata nella notifica
        if( !isRequestFromPa ){
            log.debug("Request is not from notification Pa - iun={} senderRecipientId={} paId={}", iun, senderRecipientId, paId);
            checkRecipients(notification, senderRecipientId);
        }else {
            log.info("Request is from PA, authorization is valid");
        }
    }

    private void checkRecipients(NotificationInt notification, String senderRecipientId) {
        //La richiesta non proviene dalla PA va quindi verificato se proviene da uno dei destinatari della notifica
         
        boolean isRequestFromRecipient = notification.getRecipients().stream().anyMatch(
                recipient -> recipient.getInternalId().equals(senderRecipientId)
        );
        
        if( !isRequestFromRecipient ){
            String message = String.format("User haven't authorization to get required legal facts - iun=%s user=%s", notification.getIun(), senderRecipientId);
            handleError(message);
        }else {
            log.info("Request is from recipient, authorization is valid");
        }
    }

    private void handleError(String message) {
        log.warn(message);
        throw new PnNotFoundException(message);
    }
}
