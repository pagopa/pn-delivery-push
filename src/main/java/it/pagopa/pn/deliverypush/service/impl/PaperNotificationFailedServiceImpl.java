package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.api.dto.notification.failednotification.PaperNotificationFailed;
import it.pagopa.pn.deliverypush.middleware.dao.failednotificationdao.PaperNotificationFailedDao;
import it.pagopa.pn.deliverypush.service.PaperNotificationFailedService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class PaperNotificationFailedServiceImpl implements PaperNotificationFailedService {
    private final PaperNotificationFailedDao paperNotificationFailedDao;

    public PaperNotificationFailedServiceImpl(PaperNotificationFailedDao paperNotificationFailedDao) {
        this.paperNotificationFailedDao = paperNotificationFailedDao;
    }

    @Override
    public void addPaperNotificationFailed(PaperNotificationFailed paperNotificationFailed) {
        //FIXME In attesa della risoluzione della PN-1472 che prevede l'anonimizzazione del taxId, l'inserimento viene commentato
        
        //paperNotificationFailedDao.addPaperNotificationFailed(paperNotificationFailed);
    }

    @Override
    public void deleteNotificationFailed(String recipientId, String iun) {
        //FIXME In attesa della risoluzione della PN-1472 che prevede l'anonimizzazione del taxId, la delete viene commentata
        
        //paperNotificationFailedDao.deleteNotificationFailed(recipientId, iun);
    }

    @Override
    public List<PaperNotificationFailed> getPaperNotificationByRecipientId(String recipientId) {
        log.info( "Retrieve paper notifications failed for recipientId={}", recipientId);
        return new ArrayList<>(paperNotificationFailedDao.getPaperNotificationFailedByRecipientId(recipientId));
    }

}
