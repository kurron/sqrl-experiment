sqrl-experiment
===============

An experimental Groovy implementation of Steve Gibson's SQRL authentication protocol.

Steve Gibson has proposed  [a new mechanism for web authentication](https://www.grc.com/sqrl/sqrl.htm) and this is my 
attempt to implement a non-QR code flavor of that protocol.  I want to preserve the "no keyboard involved" aspect
of the solution and take it closer to what is done with current HTTP authentication mechanisms.  I see two scenarios:
a returning client that has already established an identity with a server and a client that needs to establish a new 
identity with the server.  Let's take the easy one first, established identity.

[client] "Hello, I would like access to this resource."
        
    GET /index.html HTTP/1.1
    Host: somesite.com

[server] "I'm sorry but do we know each other?  If we do, you should be able to encrypt this bit of nonsense for me."

    HTTP/1.1 401 Unauthorized
    WWW-Authenticate: SQRL realm="sqrl.somesite.com", nonce="dcd98b7102dd2f0e8b11d0f600bfb0c093"

[client] "Here is your nonsense that I have encrypted for you and here is the identifier you should know me by."

    GET /index.html HTTP/1.1
    Host: somesite.com
    Authorization: SQRL username="beef012395678",
                        realm="sqrl.somesite.com",
                        nonce="dcd98b7102dd2f0e8b11d0f600bfb0c093",
                        response="6629fae49393a05397450978507c4ef1"

[server] "I found your identifier in our records and I used its public key to decrypt the nonsense.  The nonsense
         matches so you must be who you say you are.  I'll allow access to that resource. I would like to suggest
         that you let me know who you are when making future requests so I'll grant you direct access."

    HTTP/1.1 200 OK
    Content-Type: text/html
    Content-Length: 7984

[client] "Thanks, I'll do that."

    GET /something-else.pdf HTTP/1.1
    Host: somesite.com
    Authorization: SQRL username="beef012395678",  realm="sqrl.somesite.com"

[server] "See, that wasn't so hard was it?"

    HTTP/1.1 200 OK
    Content-Type: application/pdf
    Content-Length: 101019

----

The slightly harder scenario, establishing a new identity.

[client] "Hello, I would like access to this resource."
        
    GET /index.html HTTP/1.1
    Host: somesite.com

[server] "I'm sorry but do we know each other?  If we do, you should be able to encrypt this bit of nonsense for me."

    HTTP/1.1 401 Unauthorized
    WWW-Authenticate: SQRL realm="sqrl.somesite.com", nonce="dcd98b7102dd2f0e8b11d0f600bfb0c093"

[client] "I've looked through my list of identifiers and I don't have one for 'sqrl.somesite.com'.  I would like 
to establish a new identity."

    GET /index.html HTTP/1.1
    Host: somesite.com
    Authorization: SQRL realm="sqrl.somesite.com",
                        nonce="dcd98b7102dd2f0e8b11d0f600bfb0c093",
                        establish-identity="true"

[server] "Ok, I see that you want create a new identity.  Let me redirect you to our account creation endpoint. I'll
give you some additional information to hand over to the new endpoint."

    HTTP/1.1 307 Temporary Redirect
    Location: /new-identity/
    WWW-Authenticate: SQRL realm="sqrl.somesite.com", 
                           nonce="dcd98b7102dd2f0e8b11d0f600bfb0c093"
                           original-location="/index.html"

[client] "Thanks.  I'll prepare a public-private key pair for the 'sqrl.somesite.com' realm and sign the provided
nonsense with the private key.  I'll request that my identity be the hash of the public key and I'll upload the public
key to the endpoint. I'll also provde the original endpoint that I was trying to get to."

    POST /new-identity/ HTTP/1.1
    Location: /index.html
    Authorization: SQRL username="beef012395678",
                        realm="sqrl.somesite.com",
                        nonce="dcd98b7102dd2f0e8b11d0f600bfb0c093",
                        response="6629fae49393a05397450978507c4ef1"
                           
    Content-Type: application/octet-stream

[server] "Ok, I've accepted your public key and regisistered it under the username provided.  The response was
decrypted and it matches the nonsense provided so you must've signed it with the matching private key.  We will now
recognize you in future visits.  Allow me to redirect you to your original destination and you might consider
providing your identifier for all requests to this site."

    HTTP/1.1 307 Temporary Redirect
    Location: /index.html
    
[client] "Thanks for adding me to the system.  I'll make sure to add my identifier to my requests for now own."
    
    GET /index.html HTTP/1.1
    Host: somesite.com
    Authorization: SQRL username="beef012395678",  realm="sqrl.somesite.com"

[server] "See, that wasn't so hard was it?"

    HTTP/1.1 200 OK
    Content-Type: text/html
    Content-Length: 1024

