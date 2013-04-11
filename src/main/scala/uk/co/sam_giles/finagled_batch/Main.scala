package uk.co.sam_giles.finagled_batch;

/*
 
 A Proof of Concept Finagle HTTP batching service.
 
 You can test with the following curl command.
 curl -v -H "Accept: application/json" -H "Content-type: application/json" -X POST -d '[{"url": "http://facebook.com/index.php", "method": "GET", "headers": {"Accept-Language": "en_us"}, "body": ""}, {"url": "https://api.twitter.com/1/help/test.json", "method": "GET", "headers":{}, "body": ""}]' localhost:8081
 
 */

object Main extends App {
println("Scala version: " + scala.util.Properties.versionString);
    Server.start
}
