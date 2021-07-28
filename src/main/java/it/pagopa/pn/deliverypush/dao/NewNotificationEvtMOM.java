package it.pagopa.pn.deliverypush.dao;

import it.pagopa.pn.commons.mom.MomConsumer;
import it.pagopa.pn.commons.mom.MomProducer;
import it.pagopa.pn.deliverypush.events.NewNotificationEvt;

public interface NewNotificationEvtMOM extends MomProducer<NewNotificationEvt>, MomConsumer<NewNotificationEvt> {

}
