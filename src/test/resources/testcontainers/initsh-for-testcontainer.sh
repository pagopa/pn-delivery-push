echo "### CREATE DELIVERY COMPONENT ###"
bash <(curl -s https://raw.githubusercontent.com/pagopa/pn-delivery-push/0f344b3453996b638f7125790098aa4bd8e94487/src/test/resources/testcontainers/init.sh)

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