package com.x2d7751347m.plugins

import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*
import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import java.util.Date
class Mail {
    companion object {
        const val GOOGLE_SMTP_HOST = "smtp.gmail.com"
        const val ZOHO_SMTP_HOST = "smtp.zoho.in"
    }
    private val props = Properties().apply {
        this["mail.smtp.host"] = GOOGLE_SMTP_HOST
        this["mail.smtp.port"] = "587"
        this["mail.smtp.auth"] = "true"
        this["mail.smtp.starttls.enable"] = "true"
    }
    private val emailUsername = HoconApplicationConfig(ConfigFactory.load()).property("mail.username").getString()
        ?: throw IllegalStateException("EMAIL_USERNAME env should not be null.")
    private val emailPassword = HoconApplicationConfig(ConfigFactory.load()).property("mail.password").getString()
        ?: throw IllegalStateException("EMAIL_PASSWORD env should not be null.")
    private val fromEmail = HoconApplicationConfig(ConfigFactory.load()).propertyOrNull("mail.from")?.getString() ?: emailUsername

    private val session: Session = Session.getInstance(props, object : Authenticator() {
        override fun getPasswordAuthentication(): PasswordAuthentication {
            val username = emailUsername
            val password = emailPassword
            return PasswordAuthentication(username, password.toString())
        }
    })

    suspend fun sendEmail(emailMessage: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val message = MimeMessage(session)
            val from = fromEmail
            message.setFrom(InternetAddress(from))
            message.setRecipients(
                Message.RecipientType.TO,
                "x2d7751347m@gmail.com"
//                "emailMessage.to.lowercase().trim()"
            )
            message.subject = "emailMessage.subject"
            message.sentDate = Date()
            message.setText(emailMessage)
            Transport.send(message)
            true
        } catch (mex: MessagingException) {
            println("send failed, exception: $mex")
            false
        } catch (e: SendFailedException) {
            println("email send failed, exception: $e")
            false
        }
        catch (e: java.net.ConnectException) {
            println("Connection failed: $e")
            false
        }
        catch (e: Exception) {
            e.printStackTrace()
            println("Unhandled exception while send email ${e.javaClass.name} from ${e.javaClass.packageName}")
            false
        }
    }
}