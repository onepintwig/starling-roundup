# Starling Round Up Challenge


## Getting Started

You need a minimum of Java 22 installed to get started. You can then execute the following in the root directory to
build the round-up service:

``` sh
./mvnw clean install
``` 

This will execute Maven, build a bootable jar.

You can start the service by running the roundup-service-bootable.jar

``` sh
java -jar roundup-service-bootable.jar
``` 

The service will now be running on localhost:8080

## Executing the round-up 

Once the service is running, there are a couple of additional dependencies that must be
handled.

1. Ensure you have an account and an accountUid
2. Create a savings goal on the account and note the savings goal uid

Once these have been made, you can execute the roundup for the account and savings-goal.

``` sh
curl -X 'PUT' \
    -H 'Authorization: Bearer your-token' \
    -H 'accept: application/json' \
    -H 'Content-Type: application/json' \
    -d '{"roundUpWeekStartTimestamp": "2024-09-14T12:34:56.000Z"}'
``` 

If successful, this will return a list of all the round-ups performed. A separate roundup will be performed for each detected currency in the feed-items.

``` json
{
    "savingsGoalTransfers": [
        {
            "transferUid":"e770e935-e039-4d4e-8bbb-27c2f3e8635e",
            "success":true
        }
    ]
}
``` 

## Project Structure

Based off the REST Easy examples. I remember this library was mentioned in the 1st round, so I figured I'd give it a go!

### Client
Contains a reactive client set up for easy interaction with the starling api.
Uses reactor core, because I do still love Scala and I was keen to play  with the Java reactive libraries for concurrency!

### Model
Record classes used to model the Starling API

### Rest
Contains the rest-controller

### Service
Contains the business logic. Chains together the various HTTP requests and reduces down the feed-items into round-up amounts