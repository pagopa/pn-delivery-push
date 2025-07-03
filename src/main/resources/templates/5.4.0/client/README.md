Il template pojo.mustache presente in questa folder DEVE essere referenziato solo per la generazione dei client di
pn-timeline-service.

Il template è una copia del template presente sul modulo pn-commons per i client con versione 5.4.0.
L'unica modifica apportata è la rimozione in caso di presenza di un tag discriminator delle annotation @JsonTypeInfo e @JsonSubTypes

Si sceglie di introdurre questo template come soluzione rapida e momentanea, visto che in roadmap è prevista la separazione
del codebase del microservizio che permetterà una gestione diversa dell'integrazione con pn-timeline-service aumentando la
versione del plugin di generazione dei client.