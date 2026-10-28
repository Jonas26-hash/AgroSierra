package pe.upeu.agrosierra.di

import org.koin.core.module.Module
import org.koin.dsl.module
import pe.upeu.agrosierra.data.remote.crearClienteHttp
import pe.upeu.agrosierra.data.local.AlmacenSeguro
import pe.upeu.agrosierra.data.repository.AgroRepositoryImpl
import pe.upeu.agrosierra.domain.repository.AgroRepository
import pe.upeu.agrosierra.data.repository.AuthRepositoryImpl

fun appModule(): List<Module> = listOf(
    platformModule(),
    module {
        single { crearClienteHttp(leerToken = { get<AlmacenSeguro>().obtenerToken() }) }
        single { AuthRepositoryImpl(get(), get()) }
        single { AgroRepositoryImpl(get(), get()) }
        single<AgroRepository> { get<AgroRepositoryImpl>() }
    }
)
