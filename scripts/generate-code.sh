#! /bin/bash -e

echo "Per maggiori informazioni vedere: https://github.com/pagopa/pn-codegen"

tag=$(mvn help:evaluate -Dexpression=pagopa.codegen.version -q -DforceStdout)
echo "Tag from pom.xml: ${tag}"
if [[ ! -z $1 ]]; then
    tag=$1
    echo "Tag from command line ${tag}"
fi
#docker run --rm -v $(pwd):/usr/local/app/microsvc --name=pn-codegen ghcr.io/pagopa/pn-codegen:${tag}

#echo $tag
absolute_path_microservice="//c/SviluppoPagopa/pn-delivery-push"
echo ${absolute_path_microservice}
docker run --rm -v ${absolute_path_microservice}:/usr/local/app/microsvc --name=pn-codegen ghcr.io/pagopa/pn-codegen:${tag}

#docker run --rm -v //c/SviluppoPagopa/pn-delivery-push:/usr/local/app/microsvc --name=pn-codegen ghcr.io/pagopa/pn-codegen:v01.00.02
