package it.pagopa.pn.deliverypush.logtest;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConsoleAppenderCustom extends ConsoleAppender<ILoggingEvent> {

    private static List<LogEvent> eventList
            = Collections.synchronizedList(new ArrayList<>());
    private static List<LogEvent> eventListWarning
        = Collections.synchronizedList(new ArrayList<>());

    private static List<LogEvent> allEvents = Collections.synchronizedList(new ArrayList<>());

    @Override
    protected void append(ILoggingEvent event) {
        super.append(event);
        allEvents.add(LogEvent.builder()
            .classPath(event.getLoggerName())
            .message(event.getFormattedMessage())
            .logLevel(LogLevel.valueOf(event.getLevel().levelStr))
            .build());

        if(Level.ERROR.equals(event.getLevel())){
            
            eventList.add(LogEvent.builder()
                    .classPath(event.getLoggerName())
                    .message(event.getMessage())
                    .logLevel(LogLevel.valueOf(event.getLevel().levelStr))
                    .build());
        } else if (Level.WARN.equals(event.getLevel())){

                eventListWarning.add(LogEvent.builder()
                    .classPath(event.getLoggerName())
                    .message(event.getMessage())
                    .logLevel(LogLevel.valueOf(event.getLevel().levelStr))
                    .build());
        }
    }
    
    //Check log ERROR
    public static void checkLogs(){
        checkLogs(null);
    }

    public static void checkLogs(String acceptedError){
        checkLogs(eventList, acceptedError);
    }

    public static void checkWarningLogs(String acceptedError){
        checkLogs(eventListWarning, acceptedError);
    }

    public static void checkAuditLog(String message){
        long count = allEvents.stream().filter( event ->
            event.getMessage().contains(message)
            && event.getClassPath().equals("it.pagopa.pn.commons.log.PnAuditLog")).count();

        if (count == 0) throw new RuntimeException("Expcted AUDIT_LOG not found");
    }

    private static void checkLogs(List<LogEvent> list, String acceptedError){
        if( list != null && ! list.isEmpty() ){
            boolean throwException = true;
            if (acceptedError != null)
            {
                throwException = false;
                for (LogEvent le : list) {
                    if (!le.getMessage().startsWith(acceptedError))
                    {
                        throwException = true;
                        break;
                    }
                }
            }
            if (throwException) {
                log.warn("[TEST] There are log not excpeted. Log list {}", list);
                NotExpectedLogException expectedLogExceptionTest = new NotExpectedLogException("There are problem ", list);
                initializeLog();
                throw expectedLogExceptionTest;
            }
        }
    }

    public static void initializeLog() {
        eventList = Collections.synchronizedList(new ArrayList<>());
        eventListWarning = Collections.synchronizedList(new ArrayList<>());
    }
}