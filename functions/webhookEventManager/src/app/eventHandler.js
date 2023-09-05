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
  }

  
  const processedItems = await mapEvents(cdcEvents);
  if (processedItems.length == 0) {
    console.log("No events to persist");
    return {
      batchItemFailures: [],
    };
  }


  console.log(`Items to persist`, processedItems);

  if (processedItems.length > 0){
    await sendMessages(processedItems);
  }else{
    console.log('Nothing to send');
  }
           
};

async function sendMessages(messages) {
  try{
    
      console.log('Proceeding to send ' + messages.length + ' messages to ' + QUEUE_URL);
      const input = {
        Entries: messages.splice(0,10), 
        QueueUrl: QUEUE_URL
      }

      console.log('Sending batch message: %j', input);

      const command = new SendMessageBatchCommand(input);
      const response = await sqs.send(command);
      console.log('Sent message response: %j', response);
      if (response.Failed && response.Failed.length > 0)
      {
        console.log("error sending some message totalErrors:" + response.Failed.length);
        throw new Error("Failed to send some messages");
      }

      if (messages.length > 0)
      {
        console.log('There are ' + messages.length + ' messages to send');
        await sendMessages(messages);
      }

  }catch(exc){
      console.log("error sending message", exc)
      throw exc;
  }

};
