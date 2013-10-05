package org.kurron.sqrl.server

import groovy.util.logging.Slf4j
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.util.UriComponentsBuilder

/**
 * Spring MVC controller that understands the SQRL authentication protocol.
 */
@Slf4j
@Controller
class SqrlController {

    // instead of hashing against the domain name, we'll hash against the realm which should remain constant over time
    // or the association between client and server will be broken.
    static final String REALM = UUID.randomUUID().toString()

    @RequestMapping( value = '/sqrl', method = RequestMethod.GET )
    ResponseEntity<String>  bob( @RequestHeader( value = 'Authorization', required = false ) String authenticationString ) {
        log.info( 'Authorization header = {}', authenticationString )
        HttpHeaders headers = new HttpHeaders()
        HttpStatus status = HttpStatus.OK
        if ( authenticationString ) {
        }
        else {
            log.info( 'Authorization was not sent.  Issuing challenge.' )
            headers.add( 'WWW-Authenticate', "Basic realm=${REALM}" )
            status = HttpStatus.UNAUTHORIZED
        }
        ResponseEntity<String> response = new ResponseEntity<>( 'body', headers, status )
        response
    }
}
