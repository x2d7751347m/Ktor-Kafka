package com.x2d7751347m.mapper

import com.x2d7751347m.dto.UserPatch
import com.x2d7751347m.dto.UserPost
import com.x2d7751347m.dto.UserResponse
import com.x2d7751347m.entity.User
import com.x2d7751347m.entity.UserData
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