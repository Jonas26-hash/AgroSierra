package pe.upeu.agrosierra.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(
    val username: String,
    val password: String
)

@Serializable
data class LoginResponseDto(
    val token: String,
    val usuario: String,
    val rol: String = "",
    val privilegios: Set<String> = emptySet()
)
