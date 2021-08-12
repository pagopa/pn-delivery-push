package it.pagopa.pn.commons.mom.sqs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.mom.MomConsumer;
import it.pagopa.pn.commons.mom.MomProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
public class GenericSqsMOM<T> implements MomProducer<T>, MomConsumer<T> {

    private final String queueName;
    private final Class<T> bodyClass;
    private final ObjectMapper objMapper;

    private final SqsAsyncClient sqs;

    private final String queueUrl;


    public GenericSqsMOM(SqsAsyncClient sqs, ObjectMapper objMapper, Class<T> bodyClass, String queueName) {
        this.queueName = queueName;
        this.bodyClass = bodyClass;
        this.sqs = sqs;
        this.objMapper = objMapper;
        log.info("Using queue {} ", queueName);
        queueUrl = getQueueUrl( sqs );
        log.info("Using queue {} wth url {}", queueName, queueUrl);
    }

    private String getQueueUrl(SqsAsyncClient sqs) {
        GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
                .queueName(queueName)
                .build();

        try {
            return sqs.getQueueUrl(getQueueRequest).get().queueUrl();
        } catch (InterruptedException | ExecutionException exc) {
            throw new IllegalStateException( exc ); // FIXME Definre trattazione eccezioni
        }
    }

    @Override
    public synchronized CompletableFuture<List<T>> poll(Duration maxPollTime) {
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .build();

        return sqs
                .receiveMessage(receiveRequest)
                .thenApply( (sqsMessages) ->
                        sqsMessages
                                .messages()
                                .stream()
                                .map( awsMsg -> {
                                    T evt = parseJson( awsMsg.body() );
                                    deleteMessage( awsMsg );
                                    return evt;
                                } )
                                .collect(Collectors.toList())
            );
    }

    private void deleteMessage(Message awsMsg) {
        DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle( awsMsg.receiptHandle())
                .build();
        try {
            sqs.deleteMessage(deleteMessageRequest).get();
        } catch (ExecutionException | InterruptedException exc) {
            throw new IllegalStateException( exc ); // FIXME Definre trattazione eccezioni
        }
    }

    private T parseJson(String body) {
        try {
            return objMapper.readValue( body, bodyClass );
        } catch (JsonProcessingException exc) {
            throw new IllegalStateException( exc ); // FIXME Definre trattazione eccezioni
        }
    }

    @Override
    public synchronized CompletableFuture<Void> push(T msg) {
        String jsonMessage = objToJson_handleExceptions(msg);

        SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody( jsonMessage )
                .build();

        return sqs.sendMessage(sendMsgRequest).thenApply( (r) -> null);
    }

    private String objToJson_handleExceptions(T msg) {
        try {
            return objMapper.writeValueAsString(msg);
        } catch (JsonProcessingException exc) {
            throw new IllegalStateException( exc ); // FIXME Definre trattazione eccezioni
        }
    }
}
