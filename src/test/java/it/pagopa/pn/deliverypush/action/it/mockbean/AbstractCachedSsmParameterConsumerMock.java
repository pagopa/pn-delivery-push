package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.commons.abstractions.ParameterConsumer;

import java.util.Optional;

public class AbstractCachedSsmParameterConsumerMock implements ParameterConsumer {
    @Override
    public <T> Optional<T> getParameterValue(String parameterName, Class<T> clazz) {
        return Optional.empty();
    }
}
