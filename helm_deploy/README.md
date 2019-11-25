
### Example deploy command
```
helm --namespace prison-data-compliance-dev  --tiller-namespace prison-data-compliance-dev upgrade dps-data-compliance ./dps-data-compliance/ --install --values=values-dev.yaml --values=example-secrets.yaml
```

### Rolling back a release
Find the revision number for the deployment you want to roll back:
```
helm --tiller-namespace prison-data-compliance-dev history dps-data-compliance -o yaml
```
(note, each revision has a description which has the app version and circleci build URL)

Rollback
```
helm --tiller-namespace prison-data-compliance-dev rollback dps-data-compliance [INSERT REVISION NUMBER HERE] --wait
```

### Helm init

```
helm init --tiller-namespace prison-data-compliance-dev --service-account tiller --history-max 200
```

### Setup Lets Encrypt cert

Ensure the certificate definition exists in the cloud-platform-environments repo under the relevant namespaces folder

e.g.
```
cloud-platform-environments/namespaces/live-1.cloud-platform.service.justice.gov.uk/[INSERT NAMESPACE NAME]/05-certificate.yaml
```
