# finagled-batch

Concept:  You can batch requests to a single endpoint and asynchrously reduce these to a single response.

## Get Started

```BASH
git clone this_repo
cd finagled-batch

# Build the JAR and writes out run.sh
./quick_start.sh

# Runs the API batching endpoint on port 8081
./run.sh


# Sends a request to facebook.com and a twitter.com test endpoint
curl -v -H "Accept: application/json" -H "Content-type: application/json" -X POST -d '[{"url": "http://facebook.com/index.php", "method": "GET", "headers": {"Accept-Language": "en_us"}, "body": ""}, {"url": "https://api.twitter.com/1/help/test.json", "method": "GET", "headers":{}, "body": ""}]' localhost:8081
```

## How it works

This is similar concept to Facebook's API batching:  https://developers.facebook.com/docs/reference/api/batch

You can send off multiple requests as a single JSON payload and receive a JSON array of responses to each request.

```JSON
[
        {
                "url": "http:\/\/facebook.com\/index.php",
                "method": "GET",
                "headers": {
                        "Accept-Language": "en_us"
                },
                "body": ""
        },
        {
                "url": "https:\/\/api.twitter.com\/1\/help\/test.json",
                "method": "GET",
                "headers": {},
                "body": ""
        }
]
```
