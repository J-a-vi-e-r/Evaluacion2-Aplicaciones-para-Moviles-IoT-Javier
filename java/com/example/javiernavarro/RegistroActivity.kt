package com.example.javiernavarro

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors

class RegistroActivity : AppCompatActivity() {
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        val etRegRut = findViewById<EditText>(R.id.etRegRut)
        val etRegPassword = findViewById<EditText>(R.id.etRegPassword)
        val btnRegistrarUsuario = findViewById<Button>(R.id.btnRegistrarUsuario)

        btnRegistrarUsuario.setOnClickListener {
            val rut = etRegRut.text.toString().trim()
            val pass = etRegPassword.text.toString()

            if (rut.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val rutRegex = Regex("^\\d{7,8}-[\\dKk]$")
            if (!rutRegex.matches(rut)) {
                Toast.makeText(this, "RUT inválido. Use formato: 12345678-9", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val passwordSegura = hashPassword(pass)

            Toast.makeText(this, "Conectando con el servidor...", Toast.LENGTH_SHORT).show()

            // Ejecutamos en segundo plano con límites de tiempo seguros
            executor.execute {
                try {
                    val url = URL("http://192.168.137.237/registrar_usuario.php")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"

                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000

                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                    val postData = "rut=$rut&password=$passwordSegura"
                    val outputStream = conn.outputStream
                    outputStream.write(postData.toByteArray())
                    outputStream.flush()
                    outputStream.close()

                    val responseCode = conn.responseCode
                    Log.d("API_DEBUG", "Código de respuesta: $responseCode")

                    val reader = BufferedReader(InputStreamReader(if (responseCode == 200) conn.inputStream else conn.errorStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    Log.d("API_DEBUG", "Respuesta PHP: $response")

                    runOnUiThread {
                        if (responseCode == 200) {
                            Toast.makeText(this, "¡Registrado en AWS con éxito!", Toast.LENGTH_LONG).show()
                            finish()
                        } else {
                            Toast.makeText(this, "Error del servidor (Código: $responseCode)", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("API_DEBUG", "Excepción de red: ${e.message}", e)
                    runOnUiThread {
                        Toast.makeText(this, "Fallo de red: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}