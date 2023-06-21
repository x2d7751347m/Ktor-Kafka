package com.aftertime.plugins

import com.aftertime.Entity.validateLoginForm
import com.aftertime.Service.Service
import com.aftertime.dto.GlobalDto
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.github.smiley4.ktorswaggerui.dsl.post
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.mindrot.jbcrypt.BCrypt
import java.util.*

fun Application.configureSecurity() {
    val service = Service()
    val redirects = mutableMapOf<String, String>()
//    authentication {
//        oauth("auth-oauth-google") {
//            urlProvider = { "http://localhost:8080/callback" }
//            providerLookup = {
//                OAuthServerSettings.OAuth2ServerSettings(
//                    name = "google",
//                    authorizeUrl = "https://accounts.google.com/o/oauth2/auth",
//                    accessTokenUrl = "https://accounts.google.com/o/oauth2/token",
//                    requestMethod = HttpMethod.Post,
//                    clientId = System.getenv("GOOGLE_CLIENT_ID"),
//                    clientSecret = System.getenv("GOOGLE_CLIENT_SECRET"),
//                    defaultScopes = listOf("https://www.googleapis.com/auth/userinfo.profile")
//                )
//            }
//            client = HttpClient(Apache)
//        }
//    }
    // Please read the jwt property from the config file if you are using EngineMain
    val jwtSecret = environment.config.property("jwt.secret").getString()
    val jwtIssuer = environment.config.property("jwt.issuer").getString()
    val jwtAudience = environment.config.property("jwt.audience").getString()
    val jwtRealm = environment.config.property("jwt.realm").getString()
    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtIssuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("username").asString() != "") {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { defaultScheme, realm ->
                call.respond(HttpStatusCode.Unauthorized, "Token is not valid or has expired")
            }
        }
    }
//    authentication {
//        jwt {
//            realm = jwtRealm
//            verifier(
//                JWT
//                    .require(Algorithm.HMAC256(jwtSecret))
//                    .withAudience(jwtAudience)
//                    .withIssuer(jwtIssuer)
//                    .build()
//            )
//            validate { credential ->
//                if (credential.payload.audience.contains(jwtAudience)) JWTPrincipal(credential.payload) else null
//            }
//        }
//    }
//    authentication {
//        val myRealm = "MyRealm"
//        val usersInMyRealmToHA1: Map<String, ByteArray> = mapOf(
//            // pass="test", HA1=MD5("test:MyRealm:pass")="fb12475e62dedc5c2744d98eb73b8877"
//            "test" to hex("fb12475e62dedc5c2744d98eb73b8877")
//        )
//
//        digest("myDigestAuth") {
//            digestProvider { userName, realm ->
//                usersInMyRealmToHA1[userName]
//            }
//        }
//    }
//    authentication {
//        basic(name = "myauth1") {
//            realm = "Ktor Server"
//            validate { credentials ->
//                if (credentials.name == credentials.password) {
//                    UserIdPrincipal(credentials.name)
//                } else {
//                    null
//                }
//            }
//        }
//
//        form(name = "myauth2") {
//            userParamName = "user"
//            passwordParamName = "password"
//            challenge {
//                /**/
//            }
//        }
//    }

    routing {
        post("/login", {
            description = "login"
            request {
//                pathParameter<String>("operation") {
//                    description = "the math operation to perform. Either 'add' or 'sub'"
//                    example = "add"
//                }
                body<GlobalDto.LoginForm> {
                    example("First", GlobalDto.LoginForm(username = "nickname", password = "Password12!")) {
                        description = "nickname"
                    }
                }
            }
            response {
                HttpStatusCode.OK to {
                    description = "Successful Request"
                    header<String>(HttpHeaders.Authorization)
                }
            }
        }) {
            val loginForm = call.receive<GlobalDto.LoginForm>()
            val user = service.findUserByUsername(loginForm.username) ?: throw NotFoundException("user not found")
            validateLoginForm(loginForm).errors.let {
                if (it.isNotEmpty()) throw ValidationExceptions(it)
            }
            // Check username and password
            service.findUserByUsername(loginForm.username)?.run {
                if (!BCrypt.checkpw(loginForm.password, password)) throw ValidationException("It does not match")
            }
            val token = JWT.create()
                .withAudience(jwtAudience)
                .withIssuer(jwtIssuer)
                .withClaim("id", user.id)
                .withExpiresAt(Date(System.currentTimeMillis() + 60000))
                .sign(Algorithm.HMAC256(jwtSecret))
            call.response.headers.append(HttpHeaders.Authorization, token)
            call.response.status(HttpStatusCode.OK)
        }
        authenticate("auth-jwt") {
            get("/hello") {
                val principal = call.principal<JWTPrincipal>()
                val username = principal!!.payload.getClaim("username").asString()
                val expiresAt = principal.expiresAt?.time?.minus(System.currentTimeMillis())
                call.respondText("Hello, $username! Token is expired at $expiresAt ms.")
            }
        }
    }
    // configure routes
//    routing {
//        authenticate {
//            // route is in an "authenticate"-block ->  default security scheme will be used (see plugin-config "defaultSecuritySchemeName")
//            get("hello", {
//                // Set the security schemes to be used by this route
//                securitySchemeNames = setOf("MyOtherSecurityScheme", "MySecurityScheme")
//                description = "Protected 'Hello World'-Route"
//                response {
//                    HttpStatusCode.OK to {
//                        description = "Successful Request"
//                        body<String> { description = "the response" }
//                    }
//                    // response for "401 Unauthorized" is automatically added (see plugin-config "defaultUnauthorizedResponse").
//                }
//            }) {
//                call.respondText("Hello World!")
//            }
//        }
//        // route is not in an "authenticate"-block and does not set the `protected` property -> security schemes will be ignored
//        get("hello-unprotected", {
//            // Security scheme will be ignored since the operation is not protected
//            securitySchemeNames = setOf("MyOtherSecurityScheme", "MySecurityScheme")
//            description = "Unprotected 'Hello World'-Route"
//            response {
//                HttpStatusCode.OK to {
//                    description = "Successful Request"
//                    body<String> { description = "the response" }
//                }
//                // no response for "401 Unauthorized" is added
//            }
//        }) {
//            call.respondText("Hello World!")
//        }
//        // route is not in an "authenticate"-block but sets the `protected` property -> security scheme (or default security scheme) will be used
//        get("hello-externally-protected", {
//            // mark the route as protected even though there is no "authenticate"-block (e.g. because the route is protected by an external proxy)
//            protected = true
//            // Set the security scheme to be used by this route
//            securitySchemeName = "MyOtherSecurityScheme"
//            description = "Externally protected 'Hello World'-Route"
//            response {
//                HttpStatusCode.OK to {
//                    description = "Successful Request"
//                    body<String> { description = "the response" }
//                }
//                // response for "401 Unauthorized" is automatically added (see plugin-config "defaultUnauthorizedResponse").
//            }
//        }) {
//            call.respondText("Hello World!")
//        }
//    }
//    routing {
////        authenticate("auth-oauth-google") {
////            get("login") {
////                call.respondRedirect("/callback")
////            }
////
////            get("/callback") {
////                val principal: OAuthAccessTokenResponse.OAuth2? = call.authentication.principal()
////                call.sessions.set(UserSession(principal?.accessToken.toString()))
////                val redirect = redirects[principal!!.state!!]
////                call.respondRedirect(redirect!!)
////            }
////        }
//        authenticate("myDigestAuth") {
//            get("/protected/route/digest") {
//                val principal = call.principal<UserIdPrincipal>()!!
//                call.respondText("Hello ${principal.name}")
//            }
//        }
//        authenticate("myauth1") {
//            get("/protected/route/basic") {
//                val principal = call.principal<UserIdPrincipal>()!!
//                call.respondText("Hello ${principal.name}")
//            }
//        }
//        authenticate("myauth2") {
//            get("/protected/route/form") {
//                val principal = call.principal<UserIdPrincipal>()!!
//                call.respondText("Hello ${principal.name}")
//            }
//        }
//    }
}

class UserSession(accessToken: String)