package pe.upeu.agrosierra.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.isSuccess
import pe.upeu.agrosierra.data.local.AlmacenSeguro
import pe.upeu.agrosierra.data.remote.MapeoErrores
import pe.upeu.agrosierra.data.remote.dto.LoginRequestDto
import pe.upeu.agrosierra.data.remote.dto.LoginResponseDto
import pe.upeu.agrosierra.domain.model.Sesion

class AuthRepositoryImpl(
    private val cliente: HttpClient,
    private val almacenSeguro: AlmacenSeguro
) {
    suspend fun login(username: String, password: String): Sesion {
        val response = cliente.post("api/v1/auth/login") {
            setBody(LoginRequestDto(username, password))
        }
        if (!response.status.isSuccess()) {
            throw MapeoErrores.mapear(response)
        }
        val body = response.body<LoginResponseDto>()
        almacenSeguro.guardarToken(body.token)
        almacenSeguro.guardarSesion(body.usuario, body.rol, body.privilegios)
        return Sesion(
            token = body.token,
            usuario = body.usuario,
            rol = body.rol,
            privilegios = body.privilegios
        )
    }

    suspend fun cerrarSesion() {
        almacenSeguro.borrarToken()
        almacenSeguro.borrarSesion()
    }

    suspend fun sesionGuardada(): Sesion? {
        val token = almacenSeguro.obtenerToken() ?: return null
        val sesion = almacenSeguro.obtenerSesion() ?: return null
        return Sesion(token, sesion.first ?: "", sesion.second ?: "", sesion.third)
    }
}
