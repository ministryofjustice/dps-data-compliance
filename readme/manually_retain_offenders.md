 [< Back](../README.md)
---

## Manually retaining offenders

This can be done via the UI by searching for the user on global search here:

https://digital-dev.prison.service.justice.gov.uk/global-search

You then click onto a specific prisoner and select the option to retain here:

https://digital-dev.prison.service.justice.gov.uk/prisoner/:noms_number

Alternatively, to run an automated script, update  the csv `offender_retentions.csv` with the
offenders required

Once updated, run the bash script and provide a valid auth token

```
sh ./manually_retain_offenders.sh TOKEN="$provide_token_here"
```
