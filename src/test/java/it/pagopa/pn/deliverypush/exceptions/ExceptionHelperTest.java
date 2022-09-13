package it.pagopa.pn.deliverypush.exceptions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Method;

class ExceptionHelperTest {

    private ExceptionHelper exceptionHelper;

    @BeforeEach
    public void setup() {
        //exceptionHelper = new ExceptionHelper();
    }

    @Test
    // TODO improve test coverage for this test
    void getHttpStatusFromException() {
        
        // pass HttpStatus.BAD_REQUEST
        // pass HttpStatus.INTERNAL_SERVER_ERROR - per PnInternalException
        HttpStatus httpStatus = ExceptionHelper.getHttpStatusFromException(Mockito.any(Throwable.class));

        try (MockedStatic<ExceptionHelper> exceptionHelper = Mockito.mockStatic(ExceptionHelper.class, invocation -> {
            Method method = invocation.getMethod();

            if (!method.getName().isEmpty()) {
                return invocation.callRealMethod();
            } else {
                return invocation.getMock();
            }

        })) {
            exceptionHelper.when(this::getHttpStatusFromException).thenReturn(httpStatus);
            HttpStatus stat = ExceptionHelper.getHttpStatusFromException(Mockito.any());
            Assertions.assertNotNull(stat);
        }
    }

    @Test
    void handleException() {
    }
}