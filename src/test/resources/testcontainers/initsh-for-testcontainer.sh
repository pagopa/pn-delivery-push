echo "### CREATE DELIVERY-PUSH COMPONENT ###"

bash <(curl -s https://raw.githubusercontent.com/pagopa/pn-delivery-push/e1646bf00880ab7f1fdcbc3ba98e9213d4abe41a/src/test/resources/testcontainers/init.sh)

## La creazione delle queue local-delivery-push-inputs.fifo è già presente nel file init.sh di delivery. Viene duplicata su deliveryPush per solo per test-container

echo "### CREATE QUEUES FIFO ###"

queues_fifo="local-delivery-push-inputs.fifo"

for qn in  $( echo $queues_fifo | tr " " "\n" ) ; do

    echo creating queue fifo $qn ...

    aws --profile default --region us-east-1 --endpoint-url http://localstack:4566 \
        sqs create-queue \
        --attributes '{"DelaySeconds":"2","FifoQueue": "true","ContentBasedDeduplication": "true"}' \
        --queue-name $qn



done


