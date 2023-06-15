package com.aftertime.plugins

import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.util.*
import io.ktor.server.response.*
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureSecurity() {
    
    // Please read the jwt property from the config file if you are using EngineMain
    val jwtAudience = "jwt-audience"
    val jwtDomain = "https://jwt-provider-domain/"
    val jwtRealm = "ktor sample app"
    val jwtSecret = "secret"
    authentication {
        jwt {
            realm = jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtDomain)
                    .build()
            )
            validate { credential ->
                if (credential.payload.audience.contains(jwtAudience)) JWTPrincipal(credential.payload) else null
            }
        }
    }
    authentication {
        val myRealm = "MyRealm"
        val usersInMyRealmToHA1: Map<String, ByteArray> = mapOf(
            // pass="test", HA1=MD5("test:MyRealm:pass")="fb12475e62dedc5c2744d98eb73b8877"
            "test" to hex("fb12475e62dedc5c2744d98eb73b8877")
        )
    
        digest("myDigestAuth") {
            digestProvider { userName, realm ->
                usersInMyRealmToHA1[userName]
            }
        }
    }
    authentication {
        basic(name = "myauth1") {
            realm = "Ktor Server"
            validate { credentials ->
                if (credentials.name == credentials.password) {
                    UserIdPrincipal(credentials.name)
                } else {
                    null
                }
            }
        }
    
        form(name = "myauth2") {
            userParamName = "user"
            passwordParamName = "password"
            challenge {
                /**/
            }
        }
    }
    routing {
        authenticate("myDigestAuth") {
            get("/protected/route/digest") {
                val principal = call.principal<UserIdPrincipal>()!!
                call.respondText("Hello ${principal.name}")
            }
        }
        authenticate("myauth1") {
            get("/protected/route/basic") {
                val principal = call.principal<UserIdPrincipal>()!!
                call.respondText("Hello ${principal.name}")
            }
        }
        authenticate("myauth2") {
            get("/protected/route/form") {
                val principal = call.principal<UserIdPrincipal>()!!
                call.respondText("Hello ${principal.name}")
            }
        }
    }
}
