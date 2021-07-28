package it.pagopa.pn.deliverypush.dao;

import it.pagopa.pn.commons.mom.MomConsumer;
import it.pagopa.pn.commons.mom.MomProducer;
import it.pagopa.pn.deliverypush.events.NewNotificationEvt;
import it.pagopa.pn.deliverypush.events.PecRequest;

public interface PecRequestMOM extends MomProducer<PecRequest>, MomConsumer<PecRequest> {

}
