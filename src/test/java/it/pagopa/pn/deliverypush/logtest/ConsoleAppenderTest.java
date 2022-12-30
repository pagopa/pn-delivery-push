package it.pagopa.pn.deliverypush.logtest;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;

import java.util.ArrayList;
import java.util.List;

public class ConsoleAppenderTest extends ConsoleAppender<ILoggingEvent> {

    private static List<LogEventTest> eventList
            = new ArrayList<>();

    @Override
    protected void append(ILoggingEvent event) {
        super.append(event);
        if(Level.WARN.equals(event.getLevel()) ||
                Level.ERROR.equals(event.getLevel())){
            
            eventList.add(LogEventTest.builder()
                    .classPath(event.getLoggerName())
                    .message(event.getMessage())
                    .logLevelTest(LogLevelTest.valueOf(event.getLevel().levelStr))
                    .build());
        }
    }
    
    public static void checkLogs(){
        if( eventList != null && ! eventList.isEmpty() ){
            NotExpectedLogExceptionTest expectedLogExceptionTest = new NotExpectedLogExceptionTest("There are problem ", eventList);
            initializeLog();
            throw expectedLogExceptionTest;
        }
    }

    public static void initializeLog() {
        eventList = new ArrayList<>();
    }
}