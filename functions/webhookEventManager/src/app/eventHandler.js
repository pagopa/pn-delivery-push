const { extractKinesisData } = require("./lib/kinesis.js");
const { mapEvents } = require("./lib/eventMapper.js");
const { SQSClient, SendMessageBatchCommand } = require("@aws-sdk/client-sqs");

const sqs = new SQSClient({ region: process.env.REGION });
const QUEUE_URL = process.env.QUEUE_URL

exports.handleEvent = async (event) => {

  const cdcEvents = extractKinesisData(event);
  console.log(`Batch size: ${cdcEvents.length} cdc`);

  if (cdcEvents.length == 0) {
    console.log("No events to process");
    return {
      batchItemFailures: [],
    };
  }else{
    let batchItemFailures = [];
    while(cdcEvents.length > 0){
      let currentCdcEvents = cdcEvents.splice(0,10);
      try{
        let processedItems = await mapEvents(currentCdcEvents);
        if (processedItems.length > 0){
          await sendMessages(processedItems);
        }else{
          console.log('No events to persist in current cdcEvents: ',currentCdcEvents);
        }
      }catch(exc){
        console.log('Error in persist current cdcEvents: ', currentCdcEvents);
        batchItemFailures = batchItemFailures.concat(currentCdcEvents.map((i) => {
          return { itemIdentifier: i.kinesisSeqNumber };
        }));
      }
    }
    if(batchItemFailures.length > 0){
      console.log('process finished with error!');
    }
    return {
      batchItemFailures: batchItemFailures,
    };
  }
           
};

async function sendMessages(messages) {
  try{
    
      console.log('Proceeding to send ' + messages.length + ' messages to ' + QUEUE_URL);
      const input = {
        Entries: messages, 
        QueueUrl: QUEUE_URL
      }

      console.log('Sending batch message: %j', input);

      const command = new SendMessageBatchCommand(input);
      const response = await sqs.send(command);
      
      if (response.Failed && response.Failed.length > 0)
      {
        console.log("error sending some message totalErrors:" + response.Failed.length);
        throw new Error("Failed to send some messages");
      }

  }catch(exc){
      console.log("error sending message", exc)
      throw exc;
  }

};
