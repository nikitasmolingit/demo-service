package com.itmo.microservices.demo.users.impl.service

import com.google.common.eventbus.EventBus
import com.itmo.microservices.demo.common.exception.NotFoundException
import com.itmo.microservices.demo.users.api.messaging.UserDeletedEvent
import com.itmo.microservices.demo.users.api.service.UserService
import com.itmo.microservices.demo.users.impl.entity.AppUser
import com.itmo.microservices.demo.users.api.model.AppUserModel
import com.itmo.microservices.demo.users.api.model.RegistrationRequest
import com.itmo.microservices.demo.users.impl.repository.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Suppress("UnstableApiUsage")
@Service
class DefaultUserService(private val userRepository: UserRepository,
                         private val passwordEncoder: PasswordEncoder,
                         private val eventBus: EventBus
                         ): UserService {

    override fun findUser(username: String): AppUserModel? = userRepository
            .findByIdOrNull(username)
            ?.let { entityToModel(it) }

    override fun registerUser(request: RegistrationRequest) {
        userRepository.save(requestToEntity(request))
    }

    override fun getAccountData(requester: UserDetails): AppUserModel =
            userRepository.findByIdOrNull(requester.username)?.let { entityToModel(it) } ?:
            throw NotFoundException("User ${requester.username} not found")

    override fun deleteUser(user: UserDetails) {
        runCatching {
            userRepository.deleteById(user.username)
        }.onSuccess {
            eventBus.post(UserDeletedEvent(user.username))
        }.onFailure {
            throw NotFoundException("User ${user.username} not found", it)
        }
    }

    private fun entityToModel(entity: AppUser): AppUserModel = kotlin.runCatching {
        AppUserModel(
                username = entity.username!!,
                name = entity.name!!,
                surname = entity.surname!!,
                email = entity.email!!,
                password = entity.password!!
        )
    }.getOrElse { exception -> throw IllegalStateException("Some of user fields are null", exception) }

    private fun requestToEntity(request: RegistrationRequest): AppUser =
            AppUser(username = request.username,
                    name = request.name,
                    surname = request.surname,
                    email = request.email,
                    password = passwordEncoder.encode(request.password)
            )
}