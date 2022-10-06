package it.pagopa.pn.deliverypush.exceptions;

class ExceptionHelperTest {

//    private ExceptionHelper exceptionHelper;
//
//    @BeforeEach
//    public void setup() {
//        //exceptionHelper = new ExceptionHelper();
//    }
//
//    @Test
//    // TODO improve test coverage for this test
//    void getHttpStatusFromException() {
//        
//        // pass HttpStatus.BAD_REQUEST
//        // pass HttpStatus.INTERNAL_SERVER_ERROR - per PnInternalException
//        HttpStatus httpStatus = ExceptionHelper.getHttpStatusFromException(Mockito.any(Throwable.class));
//
//        try (MockedStatic<ExceptionHelper> exceptionHelper = Mockito.mockStatic(ExceptionHelper.class, invocation -> {
//            Method method = invocation.getMethod();
//
//            if (!method.getName().isEmpty()) {
//                return invocation.callRealMethod();
//            } else {
//                return invocation.getMock();
//            }
//
//        })) {
//            exceptionHelper.when(this::getHttpStatusFromException).thenReturn(httpStatus);
//            HttpStatus stat = ExceptionHelper.getHttpStatusFromException(Mockito.any());
//            Assertions.assertNotNull(stat);
//        }
//    }
//
//    @Test
//    void handleException() {
//    }
}