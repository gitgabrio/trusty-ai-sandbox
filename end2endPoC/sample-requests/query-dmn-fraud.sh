curl --location --request POST 'http://localhost:1337/fraud-scoring' --header 'Content-Type: application/json' -d "{  \"Transactions\" : [{\"tRiskScore\" : 1, \"tCardType\" : \"Debit\", \"tAuthCode\" : \"Authorized\", \"tLocation\" : \"Local\"}]}"
