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

    @RequestMapping( value = '/index.html', method = RequestMethod.GET )
    ResponseEntity<String>  bob( @RequestHeader( value = 'Authorization', required = false ) String authenticationString ) {
        log.info( 'Authorization header = {}', authenticationString )
        HttpHeaders headers = new HttpHeaders()
        HttpStatus status = HttpStatus.OK
        if ( authenticationString ) {
            if ( authenticationString.contains( 'SQRL ') ) {
                log.info( 'SQRL authorization requested.' )
                def authorizationMap = [:]
                authenticationString.minus( 'SQRL ' ).tokenize( ',' ).collect {
                    def tokens = it.tokenize( '=' )
                    authorizationMap[tokens.first().trim()] = tokens.last().trim()
                }
                if ( authorizationMap.containsKey( 'establish-identity' ) ) {
                    log.info( 'New SQRL identity requested. Directing caller to identity creation endpoint.' )
                    status = HttpStatus.TEMPORARY_REDIRECT
                    headers.setLocation( new URI( '/new-identity' ) )
                    headers.add( 'WWW-Authenticate', "SQRL realm=${authorizationMap['realm']}, nonce=${authorizationMap['nonce']}, original-location=/index.html" )
                }
            }
        }
        else {
            log.info( 'Authorization was not sent.  Issuing challenge.' )
            String nonce = UUID.randomUUID().toString()
            headers.add( 'WWW-Authenticate', "SQRL realm=sqrl.somesite.com, nonce=${nonce}"  )
            status = HttpStatus.UNAUTHORIZED
        }
        ResponseEntity<String> response = new ResponseEntity<>( 'body', headers, status )
        response
    }
}
