package data.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket

class ESP32Service {

    companion object {
        private const val TAG = "ESP32Service"
        private const val ESP32_IP = "192.168.100.98"
        private const val ESP32_PORT = 8080
        private const val CONNECTION_TIMEOUT = 3000
    }

    suspend fun sendVolume(volume: Int) = withContext(Dispatchers.IO) {
        val volumeClamped = volume.coerceIn(0, 100)
        sendCommand("VOL:$volumeClamped")
    }

    // 🆕 FUNCIÓN FALTANTE: Play/Pause
    suspend fun sendPlayPause(isPlaying: Boolean) = withContext(Dispatchers.IO) {
        val command = if (isPlaying) "PLAY:1" else "PLAY:0"
        sendCommand(command)
    }

    suspend fun sendEqualizerBand(frequency: String, gain: Float) = withContext(Dispatchers.IO) {
        val gainClamped = gain.coerceIn(-15f, 15f)

        // Mapeo ajustado para que coincida con los "if" de tu C++
        val code = when (frequency) {
            "60Hz" -> "EQ60"
            "230Hz" -> "EQ230"
            "910Hz" -> "EQ910"
            "3.6kHz" -> "EQ3600" // Asegúrate de agregar esto al C++
            "14kHz" -> "EQ14000" // Asegúrate de agregar esto al C++
            else -> "EQ60"
        }
        sendCommand("$code:$gainClamped")
    }

    suspend fun sendAllEqualizerBands(bands: Map<String, Float>) = withContext(Dispatchers.IO) {
        bands.forEach { (freq, gain) ->
            sendEqualizerBand(freq, gain)
            kotlinx.coroutines.delay(60) // Un poco más de margen para el simulador
        }
    }

    suspend fun checkConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val socket = Socket()
            socket.connect(java.net.InetSocketAddress(ESP32_IP, ESP32_PORT), CONNECTION_TIMEOUT)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun sendCommand(command: String): Boolean {
        var socket: Socket? = null
        return try {
            socket = Socket()
            socket.connect(java.net.InetSocketAddress(ESP32_IP, ESP32_PORT), CONNECTION_TIMEOUT)

            val writer = OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8)
            writer.write(command)
            writer.flush()

            // Lectura mínima para confirmar recepción
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val response = reader.readLine()

            Log.d(TAG, "📡 Enviado: $command | Servidor: $response")

            writer.close()
            reader.close()
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en comando $command: ${e.message}")
            false
        } finally {
            socket?.close()
        }
    }
}