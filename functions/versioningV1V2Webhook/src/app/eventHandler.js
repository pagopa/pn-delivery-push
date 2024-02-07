
const { generateProblem } = require("./lib/utils");

exports.eventHandler = async (event, context) => {

    try{
        const handlers = [];

        for( let i = 0; i<handlers.length; i++){
            console.log(i);
            if (handlers[i].checkOwnership(event, context)) {
                console.log("check ok")
                try {
                    return handlers[i].handlerEvent(event, context);
                }catch (error){
                    //TODO
                    console.log("risposta negativa: ", error.message);
                    const ret = {
                        statusCode: "?" ,
                        body: error.message
                    };
                    return ret;
                }
            }
        }

        console.log("ERROR ENDPOINT ERRATO");

        const err = {
            statusCode: 502,
            body: JSON.stringify(generateProblem(502, "ENDPOINT ERRATO"))
        };
        return err;
    } catch (e) {
        return {
            //TODO
            statusCode: 500,
            //body: JSON.stringify(utils.generateProblem(500, e.message))
        }
    }
}

