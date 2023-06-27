package com.aftertime.mapper

import com.aftertime.dto.UserPatch
import com.aftertime.dto.UserPost
import com.aftertime.dto.UserResponse
import com.aftertime.entity.User
import com.aftertime.entity.UserData
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy
import org.mapstruct.factory.Mappers

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface UserMapper {
    companion object {
        val instance: UserMapper = Mappers.getMapper(UserMapper::class.java)
    }

    fun userPostToUser(requestBody: UserPost): User
    fun userPatchToUserData(requestBody: UserPatch): UserData
    fun userToUserResponse(user: User): UserResponse
    fun userListToUserResponseList(userList: List<User>): List<UserResponse>
}