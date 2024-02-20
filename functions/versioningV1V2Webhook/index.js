//importo funzione 'eventHandler' dal modulo situato nel percorso relativo
const { eventHandler } = require("./src/app/eventHandler.js");

//esporto l'handler
exports.handler = async (event, context) => {
    return eventHandler(event, context);
}