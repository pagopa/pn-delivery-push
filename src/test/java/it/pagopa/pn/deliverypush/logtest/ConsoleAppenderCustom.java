package it.pagopa.pn.deliverypush.logtest;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ConsoleAppenderCustom extends ConsoleAppender<ILoggingEvent> {

    private static List<LogEvent> eventList
            = new ArrayList<>();

    @Override
    protected void append(ILoggingEvent event) {
        super.append(event);
        if(Level.WARN.equals(event.getLevel()) ||
                Level.ERROR.equals(event.getLevel())){
            
            eventList.add(LogEvent.builder()
                    .classPath(event.getLoggerName())
                    .message(event.getMessage())
                    .logLevel(LogLevel.valueOf(event.getLevel().levelStr))
                    .build());
        }
    }
    
    //Check log ERROR o WARNING
    public static void checkLogs(){
        if( eventList != null && ! eventList.isEmpty() ){
            log.warn("There not excpeted eventList {}", eventList);
            NotExpectedLogException expectedLogExceptionTest = new NotExpectedLogException("There are problem ", eventList);
            initializeLog();
            throw expectedLogExceptionTest;
        }
    }

    public static void initializeLog() {
        eventList = new ArrayList<>();
    }
}