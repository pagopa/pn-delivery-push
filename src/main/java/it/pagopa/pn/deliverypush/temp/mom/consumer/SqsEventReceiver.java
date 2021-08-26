package it.pagopa.pn.deliverypush.temp.mom.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.pagopa.pn.api.dto.events.IEventType;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.time.Duration;
import java.util.*;

@Slf4j
public class SqsEventReceiver implements EventReceiver {

    private final SqsClient sqs;
    private final String queueUrl;

    private final Map<String, EventHandler<?>> handlers;
    private final Map<String, ObjectReader> jsonParsers;


    public SqsEventReceiver( SqsClient sqs, String queueName, ObjectMapper objMapper,
                                   List<EventHandler<?>> handlers, List<IEventType> eventsTypes ) {
        this.sqs = sqs;
        this.queueUrl = getQueueUrl( sqs, queueName );

        this.handlers = new HashMap<>();
        this.jsonParsers = new HashMap<>();

        fillHandlersAndParsers( objMapper, handlers, eventsTypes );
    }

    private void fillHandlersAndParsers(ObjectMapper objMapper, List<EventHandler<?>> handlers, List<IEventType> eventsTypes) {

        if( handlers.size() != eventsTypes.size() ) {
            throw new PnInternalException("One handler for each eventType is necessary");
        }

        Set<Class<?>> usedClasses = new HashSet<>();

        for( int idx = 0; idx < handlers.size(); idx++ ) {
            EventHandler<?> handler = handlers.get( idx );
            IEventType evtType = eventsTypes.get( idx );

            Class<?> eventJavaClass = handler.getEventJavaClass();

            if( ! eventJavaClass.equals( evtType.getEventJavaClass() )) {
                throw new PnInternalException("Event type end handler java classes must be the same");
            }
            if( usedClasses.contains( eventJavaClass )) {
                throw new PnInternalException("Class " + eventJavaClass + " already used");
            }
            else {
                usedClasses.add( eventJavaClass );
            }

            String eventName = evtType.name();
            ObjectReader reader = objMapper.readerFor( eventJavaClass );

            this.handlers.put( eventName, handler );
            this.jsonParsers.put( eventName, reader );
        }
    }

    private static String getQueueUrl(SqsClient sqsClient, String queueName) {
        return sqsClient.getQueueUrl( GetQueueUrlRequest.builder()
                .queueName( queueName )
                .build()
            ).queueUrl();
    }

    @Override
    public void poll(Duration maxPollTime) {
        long maxPollSecondsLong = maxPollTime.toSeconds();
        int maxPollSeconds = (maxPollSecondsLong < 120 ? (int) maxPollSecondsLong : 120 );

        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl( queueUrl )
                .waitTimeSeconds( maxPollSeconds )
                .maxNumberOfMessages( 10 )
                .build();

        for( Message awsMessage: sqs.receiveMessage(receiveRequest).messages() ) {
            String evtType = computeEventType( awsMessage );

            Object msg = parseMessage( evtType, awsMessage.body() );
            callHandler(evtType, msg);
            deleteMessage( awsMessage );
        }

    }

    @SuppressWarnings("unchecked")
    private void callHandler(String evtType, Object msg) {
        EventHandler<Object> handler = (EventHandler<Object>) handlers.get(evtType);
        handler.handleEvent( handler.getEventJavaClass().cast(msg) );
    }

    private String computeEventType(Message awsMessage) {
        String msgText = awsMessage.body();
        return  msgText.replaceFirst(".*\"eventType\":\"([^\"]+)\".*", "$1");
    }

    private <T> T parseMessage(String evtType, String body) {
        try {
            return jsonParsers.get( evtType ).readValue( body );
        } catch (JsonProcessingException exc) {
            throw new PnInternalException( "Parsing event", exc );
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
