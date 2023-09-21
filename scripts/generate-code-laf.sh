#! /bin/bash -e

echo "Per maggiori informazioni vedere: https://github.com/pagopa/pn-codegen"
echo "su questo PC il comando da dare è quello sotto, da lanciare dalla cartella padre pn-delivery-push. Inoltre accertarsi di avere MVN che scarica dal repository"

tag=$(mvn help:evaluate -Dexpression=pagopa.codegen.version -q -DforceStdout)
echo "Tag from pom.xml: ${tag}"
if [[ ! -z $1 ]]; then
    tag=$1
    echo "Tag from command line ${tag}"
fi
#docker run --rm -v $(pwd):/usr/local/app/microsvc --name=pn-codegen ghcr.io/pagopa/pn-codegen:${tag}



#docker run --rm -v "c:\\SviluppoPagopa\\pn-delivery-push":/usr/local/app/microsvc --name=pn-codegen ghcr.io/pagopa/pn-codegen:v01.00.02
docker run --rm -v "c:\\SviluppoPagopa\\pn-delivery-push":/usr/local/app/microsvc --name=pn-codegen ghcr.io/pagopa/pn-codegen:${tag}