package it.pagopa.pn.deliverypush.logtest;

import lombok.ToString;

import java.util.List;

@ToString
public class NotExpectedLogException extends RuntimeException {
    private final List<LogEvent> listLog;
    
    public NotExpectedLogException(String message, List<LogEvent> list){
        super(message);
        listLog = list;
    }

}
