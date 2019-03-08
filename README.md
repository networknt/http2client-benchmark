### http2client-benchmark

[Stack Overflow](https://stackoverflow.com/questions/tagged/light-4j) |
[Google Group](https://groups.google.com/forum/#!forum/light-4j) |
[Gitter Chat](https://gitter.im/networknt/light-4j) |
[Subreddit](https://www.reddit.com/r/lightapi/) |
[Youtube Channel](https://www.youtube.com/channel/UCHCRMWJVXw8iB7zKxF55Byw) |
[Documentation](https://doc.networknt.com) |
[Contribution Guide](https://doc.networknt.com/contribute/) |

HTTP/2 client raw benchmark against light-4j server with both 10k get requests and 10k post requests. The light-client returns in about 8 seconds on my desktop computer. Trying to compare with OkHttp but got trouble to get TLS works with it. Any help is much appreciated. 

Here is the step to do the test on your computer.

```
cd ~
mkdir networknt
cd networknt
git clone https://github.com/networknt/http2client-benchmark.git
cd http2client-benchmark/httpserver
mvn clean install exec:exec
```

Start another terminal. 

```
cd ~/networknt/http2client-benchmark/lightclient
mvn clean install
mvn exec:exec
mvn exec:exec
mvn exec:exec
mvn exec:exec
mvn exec:exec
```

You can run the exec:exec multiple times to calculate the average total times. 
