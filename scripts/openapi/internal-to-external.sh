
cat docs/openapi/api-internal-b2b-webhook-v1.yaml \
    | grep -v "# NO EXTERNAL" \
    | grep -v "# NOT YET IMPLEMENTED" \
    | sed -e '/# ONLY EXTERNAL/s/^#//' \
    > docs/openapi/api-external-b2b-webhook-v1.yaml

swagger-cli bundle -o docs/openapi/api-external-b2b-webhook-bundle.yaml -t yaml docs/openapi/api-external-b2b-webhook-v1.yaml

spectral lint -r https://italia.github.io/api-oas-checker/spectral.yml docs/openapi/api-external-b2b-webhook-bundle.yaml



cat docs/openapi/schemas-pn-timeline-v1.yaml \
    | grep -v "# NO EXTERNAL" \
    | grep -v "# NOT YET IMPLEMENTED" \
    | sed -e '/# ONLY EXTERNAL/s/^#//' \
    > docs/openapi/schemas-pn-timeline-external-v1.yaml
