package it.pagopa.pn.deliverypush.temp.mom.consumer;


public interface EventHandler<E> {

    void handleEvent(E evt );

    Class<E> getEventJavaClass();
}
