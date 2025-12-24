package network.bisq.mobile.client.common.di

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ObjCClass
import kotlinx.cinterop.getOriginalKotlinClass
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
            println("Koin already initialized, skipping")
            return
        }

        try {
            val instance =
                startKoin {
                    modules(
                        modules =
                            clientModules +
                                listOf(
                                    iosClientDomainModule,
                                    iosClientPresentationModule,
                                ),
                    )
                }

            _koin = instance.koin
        } catch (e: Exception) {
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
    // println("get() called with objCClass: $objCClass")
    return try {
        val kClazz = getOriginalKotlinClass(objCClass)
        if (kClazz == null) {
            throw IllegalStateException("Could not get original Kotlin class for $objCClass")
        }
        val result: Any = get(kClazz, null, null)
        result
    } catch (e: Exception) {
//        e.printStackTrace()
        throw e
    }
}

@OptIn(BetaInteropApi::class)
fun Koin.get(
    objCClass: ObjCClass,
    qualifier: Qualifier?,
    parameter: Any,
): Any {
    val kClazz = getOriginalKotlinClass(objCClass)!!
    return get(kClazz, qualifier) { parametersOf(parameter) }
}
