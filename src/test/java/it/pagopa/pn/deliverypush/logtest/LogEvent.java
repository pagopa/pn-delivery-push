package it.pagopa.pn.deliverypush.logtest;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public class LogEvent {
    private LogLevel logLevel;
    private String message;
    private String classPath;
}
