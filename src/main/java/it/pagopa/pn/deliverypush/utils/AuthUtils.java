package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.commons.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.dto.ext.datavault.BaseRecipientDtoInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.mandate.MandateDtoInt;
import it.pagopa.pn.deliverypush.service.ConfidentialInformationService;
import it.pagopa.pn.deliverypush.service.MandateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class AuthUtils {
    private final MandateService mandateService;
    private final ConfidentialInformationService confidentialInformationService;

    public AuthUtils(
            MandateService mandateService,
            ConfidentialInformationService confidentialInformationService) {
        this.mandateService = mandateService;
        this.confidentialInformationService = confidentialInformationService;
    }

    public void checkAuthorization(NotificationInt notification, String senderRecipientId, String mandateId) {
        String paId = notification.getSender().getPaId();
        String iun = notification.getIun();

        log.info("Start Check authorization - iun={} senderRecipientId={} paId={} mandateId={}", iun, senderRecipientId, paId, mandateId);

        if(StringUtils.hasText(mandateId)){
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
        Instant startMandateDate = Instant.parse(mandateDtoInt.getDateFrom());

        //Viene verificato che la notifica contenente i legalFacts che si vogliono visualizzare non sia stata create ina una data precedente all'inizio mandato
        if( notification.getSentAt().isBefore(startMandateDate) ){
            String message = String.format("Unable to find valid mandate, notification creation date=%s is not before start mandate mate=%s " +
                    "- iun=%s delegate=%s with mandateId=%s", notification.getSentAt(), startMandateDate, iun, senderRecipientId, mandateId);
            handleError(message);

        } else {
            //Se la visilibity mandate è vuota significa che non ci sono limiti di visualizzazione
            if( !mandateDtoInt.getVisibilityIds().isEmpty() ) {
                //Viene verificato se la PaId della notifica è nella lista delle pa visibili per quel mandato
                boolean isPaIdInVisibilityPa = mandateDtoInt.getVisibilityIds().stream().anyMatch(
                        paId::equals
                );

                if( !isPaIdInVisibilityPa ){
                    String message = String.format("Unable to find valid mandate, paNotificationId=%s is not in visibility pa id for mandate" +
                            "- iun=%s delegate=%s with mandateId=%s", paId, iun, senderRecipientId, mandateId);
                    handleError(message);
                }
            }
        }
    }

    private void checkPaAndRecipients(NotificationInt notification, String senderRecipientId, String paId, String iun) {
        log.debug("Start Check request is from PA or from recipients - iun={} senderRecipientId={} paId={}", iun, senderRecipientId, paId);
        
        //TODO se senderRecipientId si riferisce alla Pa immagino non arrivi anonimizzato dunque non è necessario richiedere deanonimizzazione a datavault
        boolean isRequestFromPa = paId.equals(senderRecipientId);
        
        //Viene verificato se la richiesta proviene dalla Pa indicata nella notifica
        if( !isRequestFromPa ){
            log.debug("Request is not from notification Pa - iun={} senderRecipientId={} paId={}", iun, senderRecipientId, paId);
            checkRecipients(notification, senderRecipientId, paId, iun);
        }

    }

    private void checkRecipients(NotificationInt notification, String senderRecipientId, String paId, String iun) {
        //La richiesta non proviene dalla PA va quindi verificato se proviene da uno dei destinatari della notifica

        //TODO la lista recipient contiene id non anonimizzati. Dunque per effettuare questo check devo ottenere l'id in chiaro da dataVaul

        log.debug("Recipient verification need to get confidentialInformation - iun={} senderRecipientId={} paId={}", iun, senderRecipientId, paId);
        Optional<Map<String, BaseRecipientDtoInt>> confidentialInfMapOpt =  
                confidentialInformationService.getRecipientDenominationByInternalId(Collections.singletonList(senderRecipientId));

        if( confidentialInfMapOpt.isPresent() ){
            log.debug("Confidential Information is present - iun={} senderRecipientId={} paId={}", iun, senderRecipientId, paId);

            Map<String, BaseRecipientDtoInt> confidentialInfMap = confidentialInfMapOpt.get();
            BaseRecipientDtoInt recipientConfDto = confidentialInfMap.get(senderRecipientId);
            
            boolean isRequestFromRecipient = notification.getRecipients().stream().anyMatch(
                    recipient -> recipient.getTaxId().equals(recipientConfDto.getTaxId())
            );
            
            if( !isRequestFromRecipient ){
                String message = String.format("User haven't authorization to get required legal facts - iun=%s user=%s", notification.getIun(), senderRecipientId);
                handleError(message);
            }
            
        }else {
            String message = String.format("There isn't confidential information stored for recipientId. Can't be authorize request - iun=%s user=%s", notification.getIun(), senderRecipientId);
            handleError(message);
        }
    }

    private void handleError(String message) {
        log.warn(message);
        throw new PnNotFoundException(message);
    }
}
