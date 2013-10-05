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
import java.security.Provider
import java.security.SecureRandom
import java.security.Security
import java.security.Signature

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

            byte[] digest = generateDomainDigest('localhost')
            KeyPair pair = generateKeyPair()
            storePrivateKey(pair, map, digest)
            byte[] signature = signChallenge(pair, challenge)
            String asString = new String(signature)
            HttpHeaders headers = new HttpHeaders()
            headers.add( 'Authorization', 'signature:signature' )  // must be in a base64 encoded string
            ResponseEntity<String> resource = template.exchange( new URI('http://localhost:8080/sqrl'), HttpMethod.POST, new HttpEntity<byte[]>( pair.public.encoded, headers ), String )

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

    private static byte[] generateDomainDigest(String domain) {
// create an HMAC of the domain
        Mac hmac = Mac.getInstance('HmacSHA256')
        hmac.init(generateSecretHmacKey())
        byte[] digest = hmac.doFinal(domain.bytes)
        digest
    }

    private static SecretKeySpec generateSecretHmacKey() {
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
