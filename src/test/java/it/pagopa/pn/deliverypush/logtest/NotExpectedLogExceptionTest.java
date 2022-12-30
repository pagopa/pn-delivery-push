package it.pagopa.pn.deliverypush.logtest;

import lombok.ToString;

import java.util.List;

@ToString
public class NotExpectedLogExceptionTest extends RuntimeException {
    private final List<LogEventTest> listLog;
    
    public NotExpectedLogExceptionTest(String message, List<LogEventTest> list){
        super(message);
        listLog = list;
    }

}
