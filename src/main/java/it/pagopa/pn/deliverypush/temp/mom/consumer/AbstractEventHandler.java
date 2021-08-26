package it.pagopa.pn.deliverypush.temp.mom.consumer;

public abstract class AbstractEventHandler<E> implements EventHandler<E> {

    private final Class<E> eventJavaClass;

    protected AbstractEventHandler(Class<E> eventJavaClass) {
        this.eventJavaClass = eventJavaClass;
    }

    @Override
    public Class<E> getEventJavaClass() {
        return eventJavaClass;
    }


}
