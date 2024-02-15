//importo funzione 'versioning' dal modulo situato nel percorso relativo
const { versioning } = require("./src/app/eventHandler.js");

//esporto l'handler
exports.handler = async (event, context) => {
    return versioning(event, context);
}