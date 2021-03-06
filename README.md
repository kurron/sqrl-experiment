sqrl-experiment
===============

An experimental Groovy implementation of Steve Gibson's SQRL authentication protocol.

Steve Gibson has proposed  [a new mechanism for web authentication](https://www.grc.com/sqrl/sqrl.htm) and this is my 
attempt to implement a non-QR code flavor of that protocol.  I want to preserve the "no keyboard involved" aspect
of the solution and take it closer to what is done with current HTTP authentication mechanisms.  I see two scenarios:
a returning client that has already established an identity with a server and a client that needs to establish a new 
identity with the server.  Let's take the easy one first, established identity.

<strong>client:</strong> "Hello, I would like access to this resource."
        
    GET /index.html HTTP/1.1
    Host: somesite.com

<strong>server:</strong> "I'm sorry but do we know each other?  If we do, you should be able to encrypt this bit of nonsense for me."

    HTTP/1.1 401 Unauthorized
    WWW-Authenticate: SQRL realm="sqrl.somesite.com", nonce="dcd98b7102dd2f0e8b11d0f600bfb0c093"

<strong>client:</strong> "Here is your nonsense that I have encrypted for you and here is the identifier you should know me by."

    GET /index.html HTTP/1.1
    Host: somesite.com
    Authorization: SQRL username="beef012395678",
                        realm="sqrl.somesite.com",
                        nonce="dcd98b7102dd2f0e8b11d0f600bfb0c093",
                        response="6629fae49393a05397450978507c4ef1"

<strong>server:</strong> "I found your identifier in our records and I used its public key to decrypt the nonsense.  The nonsense
         matches so you must be who you say you are.  I'll allow access to that resource. I would like to suggest
         that you let me know who you are when making future requests so I'll grant you direct access."

    HTTP/1.1 200 OK
    Content-Type: text/html
    Content-Length: 7984

<strong>client:</strong> "Thanks, I'll do that."

    GET /something-else.pdf HTTP/1.1
    Host: somesite.com
    Authorization: SQRL username="beef012395678",  realm="sqrl.somesite.com"

<strong>server:</strong> "See, that wasn't so hard was it?"

    HTTP/1.1 200 OK
    Content-Type: application/pdf
    Content-Length: 101019

----

The slightly harder scenario, establishing a new identity.

<strong>client:</strong> "Hello, I would like access to this resource."
        
    GET /index.html HTTP/1.1
    Host: somesite.com

<strong>server:</strong> "I'm sorry but do we know each other?  If we do, you should be able to encrypt this bit of nonsense for me."

    HTTP/1.1 401 Unauthorized
    WWW-Authenticate: SQRL realm="sqrl.somesite.com", nonce="dcd98b7102dd2f0e8b11d0f600bfb0c093"

<strong>client:</strong> "I've looked through my list of identifiers and I don't have one for 'sqrl.somesite.com'.  I would like 
to establish a new identity."

    GET /index.html HTTP/1.1
    Host: somesite.com
    Authorization: SQRL realm="sqrl.somesite.com",
                        nonce="dcd98b7102dd2f0e8b11d0f600bfb0c093",
                        establish-identity="true"

<strong>server:</strong> "Ok, I see that you want create a new identity.  Let me redirect you to our account creation endpoint. I'll
give you some additional information to hand over to the new endpoint."

    HTTP/1.1 307 Temporary Redirect
    Location: /new-identity/
    WWW-Authenticate: SQRL realm="sqrl.somesite.com", 
                           nonce="dcd98b7102dd2f0e8b11d0f600bfb0c093"
                           original-location="/index.html"

<strong>client:</strong> "Thanks.  I'll prepare a public-private key pair for the 'sqrl.somesite.com' realm and sign the provided
nonsense with the private key.  I'll request that my identity be the HMAC of the nonce and I'll upload the public
key to the endpoint. I'll also provde the original endpoint that I was trying to get to."

    POST /new-identity/ HTTP/1.1
    Location: /index.html
    Authorization: SQRL username="beef012395678",
                        realm="sqrl.somesite.com",
                        nonce="dcd98b7102dd2f0e8b11d0f600bfb0c093",
                        response="6629fae49393a05397450978507c4ef1"
                           
    Content-Type: application/octet-stream

<strong>server:</strong> "Ok, I've accepted your public key and regisistered it under the username provided.  The response was
decrypted and it matches the nonsense provided so you must've signed it with the matching private key.  We will now
recognize you in future visits.  Allow me to redirect you to your original destination and you might consider
providing your identifier for all requests to this site."

    HTTP/1.1 307 Temporary Redirect
    Location: /index.html
    
<strong>client:</strong> "Thanks for adding me to the system.  I'll make sure to add my identifier to my requests for now own."
    
    GET /index.html HTTP/1.1
    Host: somesite.com
    Authorization: SQRL username="beef012395678",  realm="sqrl.somesite.com"

<strong>server:</strong> "See, that wasn't so hard was it?"

    HTTP/1.1 200 OK
    Content-Type: text/html
    Content-Length: 1024

----

As you can see, I've decided to base the key pair on the provided realm instead of the domain name like Steve does.  The 
thinking there is that this allows endpoints to change slightly over time and keep the identity intact. As long as
the realm remains constant, you could move the entire site to another domain and things should still work.

Another deviation is in the use of the HMAC.  Steve appears to want to use the HMAC as a source of randomness when 
generating the key pair.  I haven't been able to figure out how to do that with the Java APIs so my compromise is
to use the HMAC as the identifier instead of the public key. UPDATE: in the latest podcast Steve implies that he
regenerates the key pair for each new authentication session and doesn't keep them around.  That is why the keys 
need to be tied to the HMAC -- so they always come out the same.  Nice idea but I'm not sure the Java Crypto APIs
will allow me to do this.
