package org.kurron.sqrl.server

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody

/**
 * Spring MVC controller that understands the SQRL authentication protocol.
 */
@Slf4j
@Controller
class SqrlController {
    @RequestMapping( value = '/sqrl', method = RequestMethod.GET )
    @ResponseBody
    String bob( @RequestHeader( value = 'Authorization', required = false ) String authenticationString ) {
        log.info( 'Authorization header = {}', authenticationString)
        'bob'
    }
}
