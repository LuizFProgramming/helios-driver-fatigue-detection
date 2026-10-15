package br.unicid.helios.app.adaptadores.inercial

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import br.unicid.helios.app.portas.FonteInercial
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.math.sqrt

/**
 * Acelerômetro linear do aparelho (o Android já desconta a gravidade).
 * Aparelho fixo no painel = aceleração do carro. Sem o sensor, o fluxo fica vazio
 * e o sistema funciona só com a câmera.
 */
class AcelerometroAndroid(context: Context) : FonteInercial {
    private val gerenciador = context.getSystemService(SensorManager::class.java)

    override fun amostras(): Flow<Pair<Long, Double>> {
        val sensor = gerenciador.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) ?: return emptyFlow()
        return callbackFlow {
            val ouvinte = object : SensorEventListener {
                override fun onSensorChanged(e: SensorEvent) {
                    val (x, y, z) = Triple(e.values[0], e.values[1], e.values[2])
                    // event.timestamp usa a mesma base de SystemClock.elapsedRealtimeNanos.
                    trySend(e.timestamp / NANOS_POR_MS to sqrt((x * x + y * y + z * z).toDouble()))
                }

                override fun onAccuracyChanged(s: Sensor, precisao: Int) = Unit
            }
            gerenciador.registerListener(ouvinte, sensor, SensorManager.SENSOR_DELAY_GAME)
            awaitClose { gerenciador.unregisterListener(ouvinte) }
        }
    }

    private companion object {
        const val NANOS_POR_MS = 1_000_000L
    }
}
