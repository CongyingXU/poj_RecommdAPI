Hello World Client Demo using JavaScript 
========================================

The client demo demonstrates the use of (non-browser)
JavaScript to call a CXF server.

The client side makes call using JAX-WS APIs. It uses the Mozilla Rhino library 
to read the JavaScript file and run it.


Building and running the demo using maven
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located)

Using either UNIX or Windows:

  mvn install
  mvn -Pserver
  mvn -Pclient



JavaScript client output
-----------------------------



The client will show this output:
invoke sayHi().   return Bonjour
invoke greetMe(String).   return Hello Jeff

The same time, the server will give this output:
     [java] Executing operation sayHi

     [java] Executing operation greetMe
     [java] Message received: Jeff

