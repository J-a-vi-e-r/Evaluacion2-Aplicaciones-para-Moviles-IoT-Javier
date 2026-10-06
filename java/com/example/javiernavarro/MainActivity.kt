package com.example.javiernavarro

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etRut = findViewById<EditText>(R.id.etRut)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvRegistrarse = findViewById<TextView>(R.id.tvRegistrarse)

        btnLogin.setOnClickListener {
            val rutInput = etRut.text.toString().trim()
            val passInput = etPassword.text.toString()

            // 1. Validar que los campos no estén vacíos
            if (rutInput.isEmpty() || passInput.isEmpty()) {
                Toast.makeText(this, "Por favor, llene todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. Validar formato de RUT chileno (Ej: 12345678-9 o 1234567-K)
            val rutRegex = Regex("^\\d{7,8}-[\\dKk]$")
            if (!rutRegex.matches(rutInput)) {
                Toast.makeText(this, "Formato de RUT inválido (Use: 12345678-9)", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val passwordSegura = hashPassword(passInput)

            Toast.makeText(this, "Verificando en AWS...", Toast.LENGTH_SHORT).show()

            // 3. Consultar al servidor PHP en segundo plano
            executor.execute {
                try {
                    val url = URL("http://192.168.137.237/login.php")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                    val postData = "rut=$rutInput&password=$passwordSegura"
                    val outputStream = conn.outputStream
                    outputStream.write(postData.toByteArray())
                    outputStream.flush()
                    outputStream.close()

                    val responseCode = conn.responseCode
                    val reader = BufferedReader(InputStreamReader(if (responseCode == 200) conn.inputStream else conn.errorStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    val json = JSONObject(response.toString())
                    val estado = json.optString("estado")
                    val mensaje = json.optString("mensaje")

                    runOnUiThread {
                        if (estado == "ok") {
                            Toast.makeText(this, "¡Bienvenido al Invernadero!", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, DashboardActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this, "Error de conexión al iniciar sesión", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        tvRegistrarse.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
        }
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}