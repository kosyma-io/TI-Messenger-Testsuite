# KoSyMa Developer Manual

## Current situation
Gematik only supports maven 3.6.3 with jdk17. Any higher version leads to compilation errors.
It was forecasted to be upgraded to maven 3.8.6 end of 2025 (whereas the latest maven version today, 12. Feb 2024 is already 3.9.9)

## Running the testsuite
To avoid the maven problem, we can run the testsuite from Docker.

```shell
# open terminal 1
cd ti-messenger-client/testtreiber
npm run testtreiber
# 🚀 Server running at: http://localhost:3010
# 🚀 Server running at: http://localhost:3000
# 🚀 Server running at: http://localhost:3030
# 🚀 Server running at: http://localhost:3020

# open terminal 2
cd TI-Messenger-Testsuite
docker build -f ./Dockerfile -t tiger . 
docker run -it --network=host tiger /bin/bash
# inside the docker container make sure you are in /testsuite
mvn verify
```