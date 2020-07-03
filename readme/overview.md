[< Back](../README.md)
---
## Overview

The DPS Data Compliance Service is responsible for periodically
checking NOMIS offender records to ensure they comply with data
protection laws.

The high-level strategy is as follows:

1. Identify offender records due for deletion in line with
the Ministry of Justice Records, Information Management and
Retention Policy.
2. Check to ensure the record has not been manually marked
for retention by prison staff.
3. Ensure that there are no potential duplicate, unmerged
records in the database.
4. Automatically retain records where the data must be legally
retained due to moratoria.
