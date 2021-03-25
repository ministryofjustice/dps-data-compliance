[< Back](../README.md)
---
## Upload Unlawfully At Large Report 

A new UAL csv report can be uploaded via a http request.

The report must be a csv file with the following columns:

* NOMS_ID
* PRISON_NUMBER
* CRO_PNC
* FIRST_NAMES
* FAMILY_NAME
* DOB
* LICENCE_REVOKE_DATE
* INDEX_OFFENCE_DESCRIPTION


#### Request Specification

* Method: `PUT`
  

* Content-type: `multipart/form-data; boundary={calculated when request is sent}`


* Content-disposition: `form-data; name={name}; filename={filename_with_csv_extention}`


Example cURL request:

```
curl --location --request PUT '{BASE_URL}}/ual/report' \
--form 'file=@"
{PATH_TO_FILE}"
```
