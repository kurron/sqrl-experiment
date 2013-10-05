package org.kurron.sqrl.server

import groovy.util.logging.Slf4j
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod

/**
 * Spring MVC controller that understands the SQRL authentication protocol.
 */
@Slf4j
@Controller
class SqrlController {
    @RequestMapping( value = '/sqrl', method = RequestMethod.GET )
    HttpEntity<String>  bob( @RequestHeader( value = 'Authorization', required = false ) String authenticationString ) {
        log.info( 'Authorization header = {}', authenticationString )
        HttpHeaders headers = new HttpHeaders()
        headers.add( 'X-Custom', 'Logan' )
        HttpEntity<String> response = new HttpEntity<>( 'body', headers )
        response
    }
}
