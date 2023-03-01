
cat docs/openapi/api-internal-b2b-webhook.yaml \
    | grep -v "# NO EXTERNAL" \
    | grep -v "# NOT YET IMPLEMENTED" \
    | sed -e '/# ONLY EXTERNAL/s/^#//' \
    > docs/openapi/api-external-b2b-webhook.yaml

redocly bundle docs/openapi/api-external-b2b-webhook.yaml --output docs/openapi/api-external-b2b-webhook-bundle.yaml

#spectral lint -r https://italia.github.io/api-oas-checker/spectral-security.yml docs/openapi/api-external-b2b-webhook-bundle.yaml
spectral lint -r https://italia.github.io/api-oas-checker/spectral.yml docs/openapi/api-external-b2b-webhook-bundle.yaml

# single files checks
#spectral lint -r https://italia.github.io/api-oas-checker/spectral-security.yml docs/openapi/api-internal-b2b-webhook.yaml
#spectral lint -r https://italia.github.io/api-oas-checker/spectral-security.yml docs/openapi/api-internal-pn-delivery-push.yaml
