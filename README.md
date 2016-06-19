# webpieces
A project containing all the web pieces (WITH apis) to create a web server (and an actual web server, and actual http proxy and http client).  This webserver is also made to be extremely Test Driven for web app developers such that tests can be written that will test all your filters, controllers, views, redirects and everything all together in one.  Don't write brittle low layer tests and instead write high layer tests that are less brittle then their fine grained counter parts

This project is in process of implementing HTTP 2.0 as well.  This project is essentially pieces that can be used to build any http related software and full stacks as well.  The http proxy will be very minimable but is for testing purposes of the http parser such that we can put it in the middle of chrome and firefox for integration testing.

Some HTTP/2 features (we are actively working this)
 * better pipelining of requests fixing head of line blocking problem
 * Server push - sending responses before requests even come based on the first page requests (pre-emptively send what you know they will need)
 * Data compression of HTTP headers
 * Multiplexing multiple requests over TCP connection

Pieces with HTTP/2 support
 * async-http parser - feel free to use with any nio library that you like
 * embeddablehttpproxy - a proxy with http 2 support
 * embeddablewebserver - a webserver with http 2 support
 * httpclient - An http client with http 2 support

"Composition over inheritance" is a well documented ideal.  Generally speaking, after years of development a developer comes to understand why composition is preferred over inheritance.  It is generally more flexible to changing requirements.  In this regard, I also believe "libraries over frameworks" is much of the same and there are many frameworks like netty, http servers, etc. that I believe you could actually do as a library that would be more composable(ie. embeddablewebserver and embeddablehttpproxy are BOTH libraries not frameworks!!!!!).  Basically, webpieces is trying to follow the 'libraries over frameworks' idiom.  Creating a main method is easy, and with webpieces, you have so much more control.....lastly, you can swap ANY piece in these libraries by just bindingin a different piece via one line of java code.  ie. These are very hackable libraries to support many different needs

channelmanager - a very thin layer on nio for speed
asyncserver - a thin wrapper on channelmanager to create a one call tcp server

httpparser - an asynchronous http parser than can accept partial payloads (ie. nio payloads don't have full message)
httpclient - http client built on above core components
httpproxy - build on asyncserver and http client

NOTE: There is a Recorder and Playback that if you wire in, you can record things that are going wrong and use the Playback to play it back into your system.  We use this for http parser and SSL Engine so that we can have an automated test suite against very real test cases.

TODO: 
* AsyncServer - timeout incoming server connection if client sends no data in X seconds
* AsyncServer - timeout server connection if time between data is more than X seconds
* xxxx - make sure we close the connection on a write failure or read failure
* httpparser - limit the payload size of an http request (if it has header after header after head, we should close the connection)
* ChannelManager should offer up a timeout on the writes, the connection is closed (or a wrapper of some sort) so we don't all have to implement this - this is half done....a write() now checks the write at the begin of queue and if hasn't written, it will timeout (The other half is a timer checking all queues every 'timeout' seconds or something like that or the selector could fire and check itself)
* httpproxy - AsyncServer has an overload mode that we should use when we are at a certain amount of outstanding requests(maybe?)
* httpproxy - keep-alive connections should be timed out at some point albeit this is a demo anyways but we could build it into more
* httpclient - timeout the request/response cycle
* SessionExecutor - should we limit the queue size per channel such that we backpressure a channel when the queue size reaches a certain limit? or at least make it configurable?  This helps if client holds up incomingData thread to backpressure just the channels that need it
* httpparser(then httpclient) - if Content-Length > X, simulate http chunking so large files can be streamed through the system...and if < X just return entire response with body where X is configurable
* Need to go back and write more api level tests to beef up the test suite
* httpproxy - test out the caching of httpSocket in httpproxy further to make sure we understand the corner cases
* need to verify host/port is being put in hello ssl packet of http client to verify it works like browsers for SNI servername(not usually needed but we may need it for testing later)
* httprouter - need to compile with variable names -g:vars
* httprouter - tie method param count to path param count unless @Loose is used (we should do this earlier before more and more violations happen...it's easier to loosen constraints later than tighten them up)
* CRUD - create re-usable CRUD routes in a scoped re-usable routerModule vs. global POST route as well?
* Stats - Need a library to record stats(for graphing) that can record 99 percentile latency(not just average) per controller method as well as stats for many other things as well
* Management - Need to do more than just integrate with JMX but also tie it to a datastore interface that is pluggable such that as JMX properties are changed, they are written into the database so changes persist (ie. no need for property files anymore except for initial db connection)
* PRG pattern vs. "POST request comes in, path not found, so send back 404 with rendered page".  Currently in this special instance, we violate PRG and go with 404 back to use with the page.  We NEED to test this though and find if this breaks the browser back button and if it does make it more usable for people using every website written on this webserver.  Every other instance, we force apps into PRG so their users have a GREAT experience with the website

* ALPN is next!!!! 
