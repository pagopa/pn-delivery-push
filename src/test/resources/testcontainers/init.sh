## Quando viene aggiornato questo file, aggiornare anche il commitId presente nel file initsh-for-testcontainer-sh

echo "### CREATE QUEUES ###"

queues="local-delivery-push-safestorage-inputs local-delivery-push-actions local-ext-channels-inputs local-ext-channels-outputs local-delivery-push-actions-done local-ext-channels-elab-res local-address-manager-to-delivery-push"

for qn in  $( echo $queues | tr " " "\n" ) ; do

    echo creating queue $qn ...

    aws --profile default --region us-east-1 --endpoint-url http://localstack:4566 \
        sqs create-queue \
        --attributes '{"DelaySeconds":"2"}' \
        --queue-name $qn


done

echo "### CREATE QUEUES ###"

queues="local-national-registries-gateway"

for qn in  $( echo $queues | tr " " "\n" ) ; do

    echo creating queue $qn ...

    aws --profile default --region us-east-1 --endpoint-url http://localstack:4566 \
        sqs create-queue \
        --attributes '{"DelaySeconds":"2"}' \
        --queue-name $qn


done

echo " - Create pn-delivery-push TABLES"

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name Timelines \
    --attribute-definitions \
        AttributeName=iun,AttributeType=S \
        AttributeName=timelineElementId,AttributeType=S \
    --key-schema \
        AttributeName=iun,KeyType=HASH \
        AttributeName=timelineElementId,KeyType=RANGE \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5


aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name PaperNotificationFailed \
    --attribute-definitions \
        AttributeName=recipientId,AttributeType=S \
        AttributeName=iun,AttributeType=S \
    --key-schema \
        AttributeName=recipientId,KeyType=HASH \
        AttributeName=iun,KeyType=RANGE \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5


aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name Action \
    --attribute-definitions \
        AttributeName=actionId,AttributeType=S \
    --key-schema \
        AttributeName=actionId,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5


aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name FutureAction \
    --attribute-definitions \
        AttributeName=timeSlot,AttributeType=S \
        AttributeName=actionId,AttributeType=S \
    --key-schema \
        AttributeName=timeSlot,KeyType=HASH \
        AttributeName=actionId,KeyType=RANGE \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5


aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name LastPollForFutureAction \
    --attribute-definitions \
        AttributeName=lastPoolKey,AttributeType=N \
    --key-schema \
        AttributeName=lastPoolKey,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5


aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name PnDeliveryPushShedLock \
    --attribute-definitions \
        AttributeName=_id,AttributeType=S \
    --key-schema \
        AttributeName=_id,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name DocumentCreationRequest \
    --attribute-definitions \
        AttributeName=key,AttributeType=S \
    --key-schema \
        AttributeName=key,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5


aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name TimelinesCounters \
    --attribute-definitions \
        AttributeName=timelineElementId,AttributeType=S \
    --key-schema \
        AttributeName=timelineElementId,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5

echo "Initialization terminated"
