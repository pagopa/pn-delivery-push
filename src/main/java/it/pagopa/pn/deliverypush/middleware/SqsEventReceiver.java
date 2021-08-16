package it.pagopa.pn.deliverypush.middleware;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.pagopa.pn.api.dto.events.EventType;
import it.pagopa.pn.api.dto.events.NewNotificationEvent;
import it.pagopa.pn.deliverypush.eventhandler.EventHandler;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class SqsEventReceiver implements EventReceiver {

    private final SqsClient sqs;
    private final ObjectMapper objMapper;
    private final Map<EventType, EventHandler> handlers;
    private final String queueName;

    private final Map<EventType, ObjectReader> jsonParsers;
    private final String queueUrl;

    public SqsEventReceiver(SqsClient sqs, ObjectMapper objMapper, List<EventHandler> handlers, String queueName) {
        this.sqs = sqs;
        this.objMapper = objMapper;
        this.handlers = toHandlersMap( handlers );
        this.queueName = queueName;

        this.jsonParsers = prepareParsers( objMapper );
        this.queueUrl = getQueueUrl( sqs, queueName );
    }

    protected Map<EventType, EventHandler> toHandlersMap(List<EventHandler> handlers) {
        Map<EventType, EventHandler> handlersMap = new EnumMap<>( EventType.class );
        for( EventHandler<?> handler: handlers ) {
            handlersMap.put( handler.getEventType(), handler );
        }
        return handlersMap;
    }

    private static String getQueueUrl(SqsClient sqsClient, String queueName) {
        return sqsClient.getQueueUrl( GetQueueUrlRequest.builder()
                .queueName( queueName )
                .build()
            ).queueUrl();
    }

    private static Map<EventType, ObjectReader> prepareParsers( ObjectMapper objMapper ) {
        Map<EventType, ObjectReader> readers = new EnumMap<>( EventType.class );
        readers.put( EventType.NEW_NOTIFICATION, objMapper.readerFor( NewNotificationEvent.class ));

        return readers;
    }

    @Override
    public void poll(Duration maxPollTime) {
        long maxPollSecondsLong = maxPollTime.toSeconds();;
        int maxPollSeconds = (maxPollSecondsLong < 120 ? (int) maxPollSecondsLong : 120 );

        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl( queueUrl )
                .waitTimeSeconds( maxPollSeconds )
                .maxNumberOfMessages( 10 )
                .build();

        for( Message awsMessage: sqs.receiveMessage(receiveRequest).messages() ) {
            EventType evtType = computeEventType( awsMessage );

            Object msg = parseMessage( evtType, awsMessage.body() );
            handlers.get( evtType ).handle( msg );

            deleteMessage( awsMessage );
        }

    }

    private EventType computeEventType(Message awsMessage) {
        String msgText = awsMessage.body();
        String evtTypeString = msgText
                .replaceFirst(".*\"eventType\":\"([^\"]+)\".*", "$1");
        return EventType.valueOf( evtTypeString );
    }

    private Object parseMessage(EventType evtType, String body) {
        try {
            return jsonParsers.get( evtType ).readValue( body );
        } catch (JsonProcessingException exc) {
            throw new IllegalStateException( exc ); // FIXME: gestione eccezioni
        }
    }

    private void deleteMessage(Message awsMsg) {
        DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle( awsMsg.receiptHandle())
                .build();

        sqs.deleteMessage(deleteMessageRequest);
    }

}
