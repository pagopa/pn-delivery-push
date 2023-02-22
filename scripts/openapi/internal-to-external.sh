
cat docs/openapi/api-internal-b2b-webhook-v1.yaml \
    | grep -v "# NO EXTERNAL" \
    | grep -v "# NOT YET IMPLEMENTED" \
    | sed -e '/# ONLY EXTERNAL/s/^#//' \
    > docs/openapi/api-external-b2b-webhook-v1.yaml

redocly bundle docs/openapi/api-external-b2b-webhook-v1.yaml --output docs/openapi/api-external-b2b-webhook-bundle.yaml

spectral lint -r https://italia.github.io/api-oas-checker/spectral.yml docs/openapi/api-external-b2b-webhook-bundle.yaml
