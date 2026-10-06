package com.example.javiernavarro

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class DashboardActivity : AppCompatActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val tvTemperatura = findViewById<TextView>(R.id.tvTemperatura)
        val tvHumedad = findViewById<TextView>(R.id.tvHumedad)
        val btnSalir = findViewById<ImageButton>(R.id.btnSalir)

        // Configuramos la tarea repetitiva
        runnable = object : Runnable {
            override fun run() {
                cargarDatosInvernadero(tvTemperatura, tvHumedad)
                handler.postDelayed(this, 3000)
            }
        }

        // Le damos arranque al ciclo por primera vez
        handler.post(runnable)

        // Lógica del botón de salir
        btnSalir.setOnClickListener {
            // 1. Detenemos el ciclo que consulta a la base de datos
            handler.removeCallbacks(runnable)

            // 2. Creamos el puente para volver a la pantalla de Login
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

            // 3. Cerramos el Dashboard para que no quede abierto en el fondo
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }

    private fun cargarDatosInvernadero(tvTemp: TextView, tvHum: TextView) {
        executor.execute {
            try {
                val url = URL("http://192.168.137.237/obtener.php")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 3000
                conn.readTimeout = 3000

                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val json = JSONObject(response.toString())
                val temperatura = json.optString("temperatura", "--")
                val humedad = json.optString("humedad_suelo", "--")

                runOnUiThread {
                    tvTemp.text = "Temperatura: $temperatura °C"
                    tvHum.text = "Humedad del Suelo: $humedad %"
                }

            } catch (e: Exception) {
                // Silencio en caso de microcortes de red
            }
        }
    }
}