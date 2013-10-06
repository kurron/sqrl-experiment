package org.kurron.sqrl.client

import groovy.util.logging.Slf4j
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.ResponseErrorHandler
import org.springframework.web.client.RestTemplate

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.Signature

/**
 * An example of the SQRL protocol as seen from the client side.
 */
@Slf4j
class SqrlClient  implements ResponseErrorHandler {
    private static final SecureRandom generator = new SecureRandom()

    public static void main(String[] args) {
        def hmacToPrivateKeyMap = [:]
        RestTemplate template = new RestTemplate()
        template.errorHandler = new SqrlClient()
        String domain = 'localhost'
        URI endpoint = new URI( 'http', null, domain, 8080, '/index.html', null, null )
        ResponseEntity<String> entity = template.getForEntity(endpoint, String)

        if ( entity.statusCode == HttpStatus.UNAUTHORIZED ) {
            String challenge = entity.headers.getFirst('WWW-Authenticate')
            log.info( 'Challenge {} received.', challenge )

            if ( challenge.contains( 'SQRL ') ) {
                log.info( 'SQRL challenge has been issued.')
                def challengeMap = [:]
                challenge.minus( 'SQRL ' ).tokenize( ',' ).collect {
                    def tokens = it.tokenize( '=' )
                    challengeMap[tokens.first().trim()] = tokens.last().trim()
                }

                byte[] digest = createRealmHMAC( challengeMap['realm'] )
                if ( hmacToPrivateKeyMap.containsKey( digest ) ) {
                    log.info( 'SRL realm located.  We have an established identity with the site.')
                }
                else {
                    log.info( 'SRL realm not found.  Requesting to establish an identity with the site.')
                    HttpHeaders identityHeaders = new HttpHeaders()
                    identityHeaders.add( 'Authorization', "SQRL realm=${challengeMap['realm']}, nonce=${challengeMap['nonce']}, establish-identity=true" )
                    ResponseEntity<String> newIdentityResponse = template.exchange( endpoint, HttpMethod.GET, new HttpEntity<byte[]>( identityHeaders ), String )
                    println()
                }

/*
                KeyPair pair = generateKeyPair()
                storePrivateKey(pair, map, digest)
                byte[] signature = signChallenge(pair, challenge)

                HttpHeaders headers = new HttpHeaders()
                // the public key is the identity and is alway s sent -- use base64 encoding
                //
                headers.add( 'Authorization', 'signature:signature' )  // must be in a base64 encoded string
                ResponseEntity<String> resource = template.exchange(endpoint, HttpMethod.POST, new HttpEntity<byte[]>( pair.public.encoded, headers ), String )
*/
            }

            println()
        }
    }

    private static void storePrivateKey(KeyPair pair, Map map, byte[] digest) {
// store the private key and index it by the HMAC of the domain
        map[digest] = pair.private
    }

    private static byte[] signChallenge(KeyPair pair, String challenge) {
// digest the challenge
        Signature signer = Signature.getInstance('SHA256withRSA')
        signer.initSign(pair.private, generator)
        signer.update(challenge.bytes)
        byte[] signature = signer.sign()
        signature
    }

    private static KeyPair generateKeyPair() {
// generate a new key pair using the RSA algorithm
        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance('RSA')
        KeyPair pair = keyGenerator.generateKeyPair()
        pair
    }

    private static byte[] createRealmHMAC(String realm) {
        Mac hmac = Mac.getInstance('HmacSHA256')
        hmac.init(loadSecretKey())
        byte[] digest = hmac.doFinal(realm.bytes)
        digest
    }

    /**
     * Normally the key would be loaded from a keystore but we'll make one on the fly instead.
     * @return the client's secret key.
     */
    private static SecretKeySpec loadSecretKey() {
        byte[] secretBytes = new byte[256]
        generator.nextBytes(secretBytes)
        SecretKeySpec secretKey = new SecretKeySpec(secretBytes, 'AES')
        secretKey
    }

    @Override
    boolean hasError(ClientHttpResponse response) throws IOException {
        !HttpStatus.UNAUTHORIZED == response.statusCode // do not consider authorization issues a problem
    }

    @Override
    void handleError(ClientHttpResponse response) throws IOException {
    }
}
