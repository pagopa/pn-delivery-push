## Creazione file .env

Creare il file _.env_ nella root del progetto settando le seguenti variabili d'ambiente:

## Esecuzione build

Il comando di seguito genera uno zip nella directory build contenente tutte e sole le dipendenze necessarie all'ambiente di produzione

```
    npm run-script build
```

## Esecuzione test

Il comando di seguito permette di eseguire tutti i test previsti

```
    npm test
```

## Esecuzione codecoverage

Il comando di seguito permette di eseguire la code coverga dopo l'esecuizione dei test

```
    npm run-script coverage
```

## Esecuzione test, coverage, sonar e build

Il comando di seguito permette di eseguire la routine dei test per poi generare lo zip di build

```
    npm run-script test-build
```

## Handler

L'handler della lambda è presente nel file index.js

```bash
PROFILE=
REGION=
VALUE="*"
aws ssm put-parameter \
    --profile $PROFILE \
    --region $REGION \
    --description "TokenExchange login allowed tax ids" \
    --name "/pn-auth-fleet/allowedLoginTaxids" \
    --value $VALUE \
    --type "String" \
    --overwrite
```
