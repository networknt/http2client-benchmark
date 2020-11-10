### http2client-benchmark

[Stack Overflow](https://stackoverflow.com/questions/tagged/light-4j) |
[Google Group](https://groups.google.com/forum/#!forum/light-4j) |
[Gitter Chat](https://gitter.im/networknt/light-4j) |
[Subreddit](https://www.reddit.com/r/lightapi/) |
[Youtube Channel](https://www.youtube.com/channel/UCHCRMWJVXw8iB7zKxF55Byw) |
[Documentation](https://doc.networknt.com) |
[Contribution Guide](https://doc.networknt.com/contribute/) |

### httpserver

The project contains an httpserver built with light-4j with two endpoints:

A get endpoint with a query parameter. 

https://localhost:8443/get?name=Steve

A post endpoint with a body of "Steve" with the media type of text/plain. 

https://localhost:8443/post

To start the server.

```
cd ~/networknt/http2client-benchmark/httpserver
mvn clean install exec:exec
```

To test the server with curl command for get request. 

```
curl -k https://localhost:8443/get?name=Steve
```

To test the server with curl command for post request.

```
curl -k https://localhost/post --data-raw 'Steve'
```

### httpclient

HTTPS/2 client contains three implementations: Jdk 11 HttpClient, Okhttp HttpClient and Light-4j Http2Client. All three support TLS and HTTP/2 with async non-blocking calls. If anyone knows other clients, we are open to accept PRs and publish the test results on the same computer. 

The client application accepts a parameter to indicate which client to run the test against the server. The value can be `jdk11`, `okhttp` or `light`. 

For each client that is picked, it will send 10K get requests and 10K post requests to the server and collect the time consumed each. It loops three times, and we discard the first two rounds. Only the third round output is displayed as the JVM needs to warm up. 

Before running the test, we need to build the client. The client is running on the same computer as the server, so that network shouldn't be the bottleneck.

```
cd ~/networknt/http2client-benchmark/httpclient
mvn clean install
cd target
```

#### jdk11

To run the test against jdk11 HttpClient in the httpclient target folder.

```
java -jar httpclient-1.0-SNAPSHOT.jar jdk11
```

The result:

```
duration jdk11 client with 10K get requests = 1198 milliseconds
duration jdk11 client with 10K post requests = 1621 milliseconds
```

#### okhttp

To run the test against okhttp HttpClient in the httpclient target folder.

```
java -jar httpclient-1.0-SNAPSHOT.jar okhttp
```

The result:

```
duration okhttp client with 10K get requests = 835 milliseconds
duration okhttp client with 10K post requests = 1093 milliseconds
```

#### light

To run the test against light-4j Http2Client in the httpclient target folder.

```
java -jar httpclient-1.0-SNAPSHOT.jar light
```

The result:

```
duration light client with 10K get requests = 198 milliseconds
duration light client with 10K post requests = 175 milliseconds
```

### Summary

As you can see, the light-4j Http2Client is significantly faster than the OkHttp HttpClient, and OkHttp HttpClient is faster than the Jdk11 HttpClient. 

Given the high performance of light-4j Http2Client, we recommend our customers using it if performance is critical for service to service invocations in a microservices architecture. We are using it internally for the light-router and light-proxy for the cross-cutting concerns over the network. We also use it for middleware handlers to communicate with the light-platform services or components. 

