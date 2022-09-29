[< Back](../README.md)
---

## UAL Offenders

Unlawfully large offenders must be retained according to policy. *For more information
see [here](https://www.cps.gov.uk/legal-guidance/unlawfully-large-after-recall)*


In order to accurately identify and retain UAL offenders, the Public Protection Unit Database (PPUD) team publish a public quarterly report.

This report can be uploaded using the following endpoint: 

URL `/ual/report`
Method `PUT`
File format:  `text/csv`
---
File csv example format:

| NOMS_ID     | PRISON_NUMBER |  CRO_PNC      | FIRST_NAMES |   FAMILY_NAME | DOB         | LICENSE_REVOKE_DATE | INDEX_OFFENCE_DESCRIPTION
| ----------- | -----------   | -----------   | ----------- |  -----------  | ----------- | -----------         | ----------- |
---
## UAL matching Logic

The logic matches an offender to the UAL data extracted from the UAL report in the following order:

1. Offender number
2. Offender booking No 
3. PNC 
4. CRO

If a match is found with any of the above, two algorithms are run against the offenders names to ensure they are above a
93% similarity. If they match, we retain the offender for UAL reasons.The two algorithms used are `LevienshtienDistance` &
`JaroWinkler` . The threshold of 93% allows for a slight miss-spelling, providing other information is matched. If a middle
name exists it is also considered.