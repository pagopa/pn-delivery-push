
cat docs/openapi/api-internal-b2b-webhook-v1.yaml \
    | grep -v "# NO EXTERNAL" \
    | grep -v "# TO BE IMPLEMENTED" \
    | sed -e '/# ONLY EXTERNAL/s/^#//' \
    > docs/openapi/api-external-b2b-webhook-v1.yaml
