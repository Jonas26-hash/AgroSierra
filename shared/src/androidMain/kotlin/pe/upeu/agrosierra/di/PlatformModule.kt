package pe.upeu.agrosierra.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.dsl.module
import pe.upeu.agrosierra.BaseLocalAndroid
import pe.upeu.agrosierra.BaseLocalAndroidDb
import pe.upeu.agrosierra.data.local.AlmacenSeguro

actual fun platformModule() = module {
    single { AlmacenSeguro(get()) }
    single<HttpClientEngine> { OkHttp.create() }
    single { BaseLocalAndroid(get()).crearBaseLocal() }
    single { get<BaseLocalAndroidDb>().base }
}
