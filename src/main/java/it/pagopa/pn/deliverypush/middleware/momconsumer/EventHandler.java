package it.pagopa.pn.deliverypush.middleware.momconsumer;


public interface EventHandler<E> {

    void handleEvent(E evt );

    Class<E> getEventJavaClass();
}
