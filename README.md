sqrl-experiment
===============

An experimental Groovy implementation of Steve Gibson's SQRL authentication protocol.

Steve Gibson has proposed  [a new mechanism for web authentication](https://www.grc.com/sqrl/sqrl.htm) and this is my 
attempt to implement a non-QR code flavor of that protocol.  I want to preserve the "no keyboard involved" aspect
of the solution and take it closer to what is done with current HTTP authentication mechanisms.  I see two scenarios:
a returning client that has already established and identity with a server and a client that needs to establish an 
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

The steps go something like this:

* client accesses a SQRL enabled site
* server looks for the Authorization header, does not find it and issues a 401 Unauthorized response, filling in 
  the WWW-Authenticate header with with challange string (a nonce), 
  eg A1B2C3D4E5F6G7010101010
* the client sees the 401 and understands that it must authenticate with the site
* the client creates an HMAC of the domain name being accessed, eg www.mysite.com, and uses that as a key into a lookup 
  table of sites that the client has already established an identity with.  Lets assume the client doesn't find a 
  match and wishes to establish a new identity.
* the client generates a new asymetric key pair and stores the private key locally in a lookup table keyed by the HMAC
  value.
* the client creates a digital signature by creating a digest of the domain and challenge, 
  eg www.mysite.com?A1B2C3D4E5F6G7010101010, and signing that digest using the private key
* the client places the signature into the Authorization header and issues a POST to the end point sending the public key
  in the body of the post, eg Authorization 'signature':'signature'.
* the server reacts to the POST by using the sent over key to decrypt the signature in the Authorization header.
  Once decrypted, the server computes a digest using the same algorithm that the client used, eg SHA-256 digest of
  www.mysite.com?A1B2C3D4E5F6G7010101010.  If the computed results match the sent over results, the server will
  store the key as the user's identity.  The key is to be indexed by a digest of the key.  From now on, the user's
  identify is the digest of the site-specific public key.
* Any subsequent returns to the site that requires authentication will follow the standard HTTP Basic Authentication
  mechanism. 401 Unauthorized and WWW-Authenticate with a new nonce from the server.  The client sends back
  an Authorization header that contains a digest of the nonce that has been ecrypted with the site-specific
  private key (signature) as well as a digest of the public key, eg. Authorization 'public key digest':'signature'.  
  The server uses the digest of the public key to look up the public key in its identity table, decrypts the signature
  and performs the same digest calculation on the nonce that the client did.  If the provided and calculated values
  match, the the identity is confirmed and the user is authenticated.
