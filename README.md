# finagled-batch

Concept:  You can batch requests to a single endpoint and asynchronously reduce these to a single response.

Thinking aloud:  This seems to be heading very much into the direction of map <-> reduce

## Get Started

```BASH
git clone git://github.com/SamGiles/finagled-batch.git
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

Response

```JSON
[
        {
                "status": 302,
                "headers": {
                        "Location": "https:\/\/173.252.110.27\/index.php",
                        "Connection": "keep-alive",
                        "Content-Length": "0",
                        "X-FB-Debug": "*",
                        "Content-Type": "text\/html; charset=utf-8",
                        "Date": "Thu, 11 Apr 2013 17:34:32 GMT"
                },
                "body": ""
        },
        {
                "status": 200,
                "headers": {
                        "x-transaction": "f855ea94fa382521",
                        "etag": "\"72054d9a6fbdcc7df012e19f32345b65\"",
                        "expires": "Tue, 31 Mar 1981 05:00:00 GMT",
                        "server": "tfe",
                        "cache-control": "no-cache, no-store, must-revalidate, pre-check=0, post-check=0",
                        "x-transaction-mask": "a6183ffa5f8ca943ff1b53b5644ef114f5d00892",
                        "content-length": "4",
                        "last-modified": "Thu, 11 Apr 2013 17:34:32 GMT",
                        "date": "Thu, 11 Apr 2013 17:34:32 GMT",
                        "content-type": "application\/json; charset=utf-8",
                        "status": "200 OK",
                        "pragma": "no-cache",
                        "x-mid": "687960793640b4964f28bd58ff80dc81c1669bdf",
                        "set-cookie": "guest_id=; Domain=.twitter.com; Path=\/; Expires=Sat, 11-Apr-2015 17:34:32 UTC",
                        "x-frame-options": "SAMEORIGIN",
                        "x-runtime": "0.01271",
                        "vary": "Accept-Encoding"
                 },
                 "body": "\"ok\""
        }
]
```
