package org.kurron.sqrl.client

import groovy.util.logging.Slf4j
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.ResponseErrorHandler
import org.springframework.web.client.RestOperations
import org.springframework.web.client.RestTemplate

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * An example of the SQRL protocol as seen from the client side.
 */
@Slf4j
class SqrlClient  implements ResponseErrorHandler {
    private static final SecureRandom generator = new SecureRandom()

    public static void main(String[] args) {
        def map = [:]
        RestTemplate template = new RestTemplate()
        template.errorHandler = new SqrlClient()
        ResponseEntity<String> entity = template.getForEntity(new URI('http://localhost:8080/sqrl'), String)
        if ( entity.statusCode == HttpStatus.UNAUTHORIZED ) {
            String challenge = entity.headers.getFirst('X-SQRL-CHALLENGE')
            log.info( 'Challenge {} received.', challenge )

            // create an HMAC of the domain
            Mac hmac = Mac.getInstance('HmacSHA256')
            byte[] secretBytes = new byte[256]
            generator.nextBytes( secretBytes )
            hmac.init( new SecretKeySpec( secretBytes, 'AES' ) )
            byte[] digest = hmac.doFinal( 'localhost'.bytes )

            // generate a new key pair
            KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance('DiffieHellman')
            KeyPair pair = keyGenerator.generateKeyPair()

            // store the private key and index it by the HMAC of the domain
            map[digest] = pair.private

            // digest the challenge
            MessageDigest digester = MessageDigest.getInstance('SHA-256')
            byte[] challengeDigest = digester.digest(challenge.bytes)


            println()
        }
    }

    @Override
    boolean hasError(ClientHttpResponse response) throws IOException {
        !HttpStatus.UNAUTHORIZED == response.statusCode // do not consider authorization issues a problem
    }

    @Override
    void handleError(ClientHttpResponse response) throws IOException {
    }
}
