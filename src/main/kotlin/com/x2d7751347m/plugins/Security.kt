package com.x2d7751347m.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.TokenExpiredException
import com.auth0.jwt.interfaces.DecodedJWT
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.typesafe.config.ConfigFactory
import com.x2d7751347m.dto.GlobalDto
import com.x2d7751347m.entity.UserRole
import com.x2d7751347m.entity.validateLoginForm
import com.x2d7751347m.repository.UserRepository
import io.github.smiley4.ktorswaggerui.dsl.post
import io.github.smiley4.ktorswaggerui.dsl.route
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.config.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import org.mindrot.jbcrypt.BCrypt
import java.util.*


fun Application.configureSecurity() {
//    val redirects = mutableMapOf<String, String>()
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
            val userRepository = UserRepository()
            realm = jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtIssuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("id").asString() != "" && userRepository.fetchUser(
                        credential.payload.getClaim("id").asLong()
                    ) != null && !(this.request.path()
                        .contains("/admins") && UserRole.valueOf(
                        credential.payload.getClaim("role").asString()
                    ) != UserRole.ADMIN)
                ) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { defaultScheme, realm ->
                if (!this.call.request.headers.contains("Upgrade"))
                    call.respond(HttpStatusCode.Unauthorized, "Token is not valid or has expired")
            }
        }
    }
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
}


fun Route.securityRouting() {
    val userRepository = UserRepository()
    val jwtSecret = HoconApplicationConfig(ConfigFactory.load()).propertyOrNull("jwt.secret")!!.getString()
    val jwtIssuer = HoconApplicationConfig(ConfigFactory.load()).propertyOrNull("jwt.issuer")!!.getString()
    val jwtAudience = HoconApplicationConfig(ConfigFactory.load()).propertyOrNull("jwt.audience")!!.getString()
    val jwtRealm = HoconApplicationConfig(ConfigFactory.load()).propertyOrNull("jwt.realm")!!.getString()
    route("/v1/api", {
        tags = listOf("auth")
        response {
            HttpStatusCode.OK to {
                description = "Successful Request"
            }
            HttpStatusCode.BadRequest to {
                description = "Not a valid request"
                body<ExceptionResponse> { description = "the response" }
            }
            HttpStatusCode.InternalServerError to {
                description = "Something unexpected happened"
                body<ExceptionResponse> { description = "the response" }
            }
        }
    }) {
        post("/sign-in", {
            summary = "log-in"
            request {
//                pathParameter<String>("operation") {
//                    description = "the math operation to perform. Either 'add' or 'sub'"
//                    example = "add"
//                }
                body<GlobalDto.LoginForm> {
                    example(
                        "First",
                        GlobalDto.LoginForm(username = "username", password = "Password12!", deviceId = "device1")
                    ) {
                        description = "Enter your username and password and device id"
                    }
                    required = true
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
            validateLoginForm(loginForm).errors.let {
                if (it.isNotEmpty()) throw ValidationExceptions(it)
            }
            val user =
                userRepository.fetchUserByUsername(loginForm.username) ?: throw NotFoundException("user not found")
            // Check username and password
            userRepository.fetchUserByUsername(loginForm.username)?.run {

                // Check that an unencrypted password matches one that has
                // previously been hashed
                if (!BCrypt.checkpw(loginForm.password, password)) throw ValidationException("It does not match")
            }
            val authorizationToken = JWT.create()
                .withAudience(jwtAudience)
                .withIssuer(jwtIssuer)
                .withClaim("id", user.id)
                .withClaim("role", user.userRole.name)
                .withClaim("device-id", loginForm.deviceId)
                .withExpiresAt(Date(System.currentTimeMillis() + 24 * 60 * 60000))
                .sign(Algorithm.HMAC256(jwtSecret))
            val refreshToken = JWT.create()
                .withAudience(jwtAudience)
                .withIssuer(jwtIssuer)
                .withClaim("id", user.id)
                .withClaim("role", user.userRole.name)
                .withClaim("device-id", loginForm.deviceId)
                .withExpiresAt(Date(System.currentTimeMillis() + 7 * 24 * 60 * 60000))
                .sign(Algorithm.HMAC256(jwtSecret))
            call.response.headers.append(HttpHeaders.Authorization, authorizationToken)
            call.response.headers.append("Refresh", refreshToken)
            call.response.status(HttpStatusCode.OK)
        }
        post("/oauth-sign-in", {
            summary = "oauth log-in"
            description = "not implemented"
            request {
//                pathParameter<String>("operation") {
//                    description = "the math operation to perform. Either 'add' or 'sub'"
//                    example = "add"
//                }
                queryParameter<Oauth>("platform") {
                    description = "platform"
                    example = Oauth.GOOGLE
                    required = true
                }
                headerParameter<String>("IdToken")
            }
            response {
                HttpStatusCode.OK to {
                    description = "Successful Request"
                    header<String>(HttpHeaders.Authorization)
                }
            }
        }) {
            when (Oauth.valueOf(call.parameters.getOrFail("platform"))) {
                Oauth.GOOGLE -> {
                    val idTokenString = call.request.headers["IdToken"]!!
                    val transport: HttpTransport = NetHttpTransport()
                    val jsonFactory: JsonFactory = GsonFactory.getDefaultInstance()
                    val verifier = GoogleIdTokenVerifier.Builder(
                        transport,
                        jsonFactory
                    ) // Specify the CLIENT_ID of the app that accesses the backend:
                        .setAudience(listOf<String>("CLIENT_ID")) // Or, if multiple clients access the backend:
                        //.setAudience(Arrays.asList(CLIENT_ID_1, CLIENT_ID_2, CLIENT_ID_3))
                        .build()

// (Receive idTokenString by HTTPS POST)
                    val idToken: GoogleIdToken =
                        verifier.verify(idTokenString) ?: throw BadRequestException("Invalid ID token.")
                    val payload: Payload = idToken.payload

                    // Print user identifier
                    val userId: String = payload.getSubject()
                    println("User ID: $userId")

                    // Get profile information from payload
                    val email: String = payload.getEmail()
                    val emailVerified: Boolean = java.lang.Boolean.valueOf(payload.getEmailVerified())
                    val name = payload.get("name")
                    val pictureUrl = payload.get("picture")
                    val locale = payload.get("locale")
                    val familyName = payload.get("family_name")
                    val givenName = payload.get("given_name")

                    // Use or store profile information
                    // ...
                }

                Oauth.APPLE -> {
                }

                else -> {
                    throw BadRequestException("invalid platform")
                }
            }
        }
    }
    post("/v1/api/token/refresh", {
        tags = listOf("auth")
        summary = "refresh token"
        request {
            headerParameter<String>("Refresh")
        }
        response {
            HttpStatusCode.OK to {
                description = "Successful Request"
                header<String>(HttpHeaders.Authorization)
            }
        }

    }) {

        val refreshToken = call.request.headers["Refresh"]

        val verifier = JWT
            .require(Algorithm.HMAC256(jwtSecret))
            .withAudience(jwtAudience)
            .withIssuer(jwtIssuer)
            .build()
        var decodedJWT: DecodedJWT
        try {
            decodedJWT = verifier.verify(refreshToken)
        } catch (tokenExpiredException: TokenExpiredException) {
            throw ValidationException(tokenExpiredException.localizedMessage)
        }
        val id = decodedJWT.getClaim("id").asLong()
        val user =
            userRepository.fetchUser(id) ?: throw NotFoundException("user not found")
        val token = JWT.create()
            .withAudience(jwtAudience)
            .withIssuer(jwtIssuer)
            .withClaim("id", user.id)
            .withClaim("role", user.userRole.name)
            .withClaim("device-id", decodedJWT.getClaim("device-id").asString())
            .withExpiresAt(Date(System.currentTimeMillis() + 24 * 60 * 60000))
            .sign(Algorithm.HMAC256(jwtSecret))
        call.response.headers.append(HttpHeaders.Authorization, token)
        call.response.status(HttpStatusCode.OK)
//            val expiresAt = principal.expiresAt?.time?.minus(System.currentTimeMillis())
//            call.respondText("Hello, $id! Token is expired at $expiresAt ms.")
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

enum class Oauth {
    GOOGLE, APPLE
}

class UserSession(accessToken: String)