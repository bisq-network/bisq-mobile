package network.bisq.mobile.presentation.di

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ObjCClass
import kotlinx.cinterop.getOriginalKotlinClass
import network.bisq.mobile.client.di.clientModule
import network.bisq.mobile.domain.di.domainModule
import network.bisq.mobile.domain.di.iosClientModule
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.Qualifier

/**
 * Helper for iOS koin injection
 */
class DependenciesProviderHelper {

    fun initKoin() {
        // Guard against multiple initializations
        if (_koin != null) {
            println("KMP: Koin already initialized, skipping")
            return
        }

        try {
            println("KMP: Initializing Koin...")
            val instance = startKoin {
                modules(
                    listOf(
                        domainModule,
                        serviceModule,
                        presentationModule,
                        clientModule,
                        iosClientModule,
                        iosPresentationModule,
                    )
                )
            }

            _koin = instance.koin
            println("KMP: Koin initialized successfully")
        } catch (e: Exception) {
            println("KMP: Error initializing Koin: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    companion object {
        private var _koin: Koin? = null
        val koin: Koin
            get() = _koin ?: error("Koin not initialized. Call initKoin() first.")
    }

}

@OptIn(BetaInteropApi::class)
fun Koin.get(objCClass: ObjCClass): Any {
    println("KMP: get() called with objCClass: $objCClass")
    return try {
        println("KMP: Getting original Kotlin class...")
        val kClazz = getOriginalKotlinClass(objCClass)
        println("KMP: Original Kotlin class: $kClazz")
        if (kClazz == null) {
            throw IllegalStateException("Could not get original Kotlin class for $objCClass")
        }
        println("KMP: Resolving class: ${kClazz.simpleName}")
        val result: Any = get(kClazz, null, null)
        println("KMP: Successfully resolved: ${kClazz.simpleName}")
        result
    } catch (e: Exception) {
        println("KMP: ERROR resolving dependency: ${e.message}")
        println("KMP: Exception type: ${e::class.simpleName}")
        e.printStackTrace()
        throw e
    }
}

@OptIn(BetaInteropApi::class)
fun Koin.get(objCClass: ObjCClass, qualifier: Qualifier?, parameter: Any): Any {
    val kClazz = getOriginalKotlinClass(objCClass)!!
    return get(kClazz, qualifier) { parametersOf(parameter) }
}
