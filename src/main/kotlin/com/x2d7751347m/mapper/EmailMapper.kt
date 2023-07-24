package com.x2d7751347m.mapper

import com.x2d7751347m.dto.*
import com.x2d7751347m.entity.Email
import com.x2d7751347m.entity.EmailData
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy
import org.mapstruct.factory.Mappers

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface EmailMapper {
    companion object {
        val instance: EmailMapper = Mappers.getMapper(EmailMapper::class.java)
    }

    fun emailPostToEmail(requestBody: EmailPost): Email
    fun emailUserPostToEmail(requestBody: EmailUserPost): Email
    fun emailPatchToEmailData(requestBody: EmailPatch): EmailData
    fun emailUserPatchToEmailData(requestBody: EmailUserPatch): EmailData
    fun emailToEmailResponse(email: Email): EmailResponse
    fun emailListToEmailResponseList(emailList: List<Email>): List<EmailResponse>
}