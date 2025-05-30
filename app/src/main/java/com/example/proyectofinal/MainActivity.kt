package com.example.proyectofinal

/**
 *Completada
 *
 *
 */
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectofinal.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore


class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var binding: ActivityMainBinding // variable para el binding
    private lateinit var db: FirebaseFirestore //variable para la conexion con Firestore
    private lateinit var auth: FirebaseAuth // Variable que guarda la instancia de Authentication
    private lateinit var googleSignInClient: GoogleSignInClient //Variable para iniciar sesión con Google
    private lateinit var progressDialog: ProgressDialog // Dialogo de progreso que se mosrtará para la acción de Google mientras se carga el procedimiento
    private var firebaseUser: FirebaseUser? = null //Guarga el usuario autenticado (si lo hay)
    private var userRol: String? = null // Guarda el rol del usuario actual
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail().build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Espere por favor")



        variables()
        actions()
    }

    /**
     * Función que recoge las inicializaciones de las variables
     */
    private fun variables() {
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()


    }

    /**
     * Función que recoge las acciones de los elementos
     */
    private fun actions() {
        binding.btnLogin.setOnClickListener(this)
        binding.warningPass.visibility = View.GONE
        binding.btnForgotPassword.setOnClickListener(this)
        //binding.btnGoogle.setOnClickListener(this)
    }

    /**
     * On click de los botones
     */
    override fun onClick(v: View?) {
        when (v?.id) {
            /**
             * Botón de login
             *
             * Este botón llama la función loginUser() siempre y cuando todos los campos estén rellenos.
             */
            binding.btnLogin.id -> { //Lógica del botón login.
                val email =
                    binding.editEmployeeMail.text.toString() //Variable que recoge el texto del email.
                val password =
                    binding.editPass.text.toString() //variable que recoge el texto de la contraseña.

                if (email.isEmpty() || password.isEmpty()) { //Si algun campo está vacío
                    snackBar(binding.root, "Debes rellenar ambos campos")
                } else {
                    loginUser(email, password) // si no...entramos
                }
            }
            /**
             * Botón Olvido su Contraseña
             *
             * Si pulsamos sobre este botón, nos lleva directamente a la activity de restablecimiento
             * de contraseña
             */
            binding.btnForgotPassword.id -> {
                auth.signOut()
                startActivity(Intent(this, ResetPassActivity::class.java))
            }

            /*binding.btnGoogle.id -> {
                startGoogleSignIn()
            }*/
        }
    }

    /*private fun startGoogleSignIn() {
        val googleSignIntent = googleSignInClient.signInIntent
        googleSignInARL.launch(googleSignIntent)
    }

    private val googleSignInARL =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    authGoogleFirebase(account.idToken)
                } catch (e: Exception) {
                    snackBar(binding.root, "Error: ${e.message}")
                }
            } else {

                snackBar(binding.root, "Cancelado")
            }
        }

    private fun authGoogleFirebase(idToken: String?) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnSuccessListener { authResult ->
            val uid = auth.currentUser?.uid
            db.collection("users").whereEqualTo("uid", uid).get().addOnSuccessListener { query ->
                if (!query.isEmpty) {
                    val document = query.documents[0]
                    val rol = document.getString("rol")

                    when (rol) { //Dependiendo del rol, el usuario será redirigido a un calendario u otro.
                        "Jefe de tienda" -> startActivity(
                            Intent(
                                applicationContext,
                                CalendarActivity::class.java
                            )
                        )

                        "Jefe de sección" -> startActivity(
                            Intent(
                                applicationContext,
                                EmployeeCalendarActivity::class.java
                            )
                        )

                        "Empleado" -> startActivity(
                            Intent(
                                applicationContext,
                                EmployeeCalendarActivity::class.java
                            )
                        )
                    }
                } else {
                    auth.signOut()
                    snackBar(binding.root, "Este usuario no está registrado")
                }

            }

        }.addOnFailureListener { e ->
            snackBar(binding.root, "${e.message}")

        }
    }*/

    /**
     * Realiza el inicio de sesión del usuario
     *
     * Esta función realiza el Login cuando se pulsa el boton de "Iniciar sesión". Coge de la base de datos el uid del usuario
     * que logea para acceder a su "login" y su número de empleado. Si su login es FALSE querrá decir que será la primera vez
     * que hace login y se le enviará un email de restablecimiento de contraseña como recomendación ya que, por defecto, la contraseña
     * que se asigna automáticamente al crear usuarios es la misma que la de su numero de empleado. Lo abra o no, podrá volver
     * a meter sus credenciales y logearse. Si su login es TRUE, no mandará el email y entrará automáticamente
     */
    private fun loginUser(email: String, password: String) {
        auth.setLanguageCode("es")  // Establece el idioma en español para el envío del correo de restablecimiento de contraseña
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task -> // se procede a hacer el inicio de sesión con las variables recogidas del usuario
            if (task.isSuccessful) { // Si la tarea se ha llevado a cabo...
                val uid = auth.currentUser?.uid.toString()
                db.collection("users").whereEqualTo("uid", uid).get()
                    .addOnSuccessListener { query -> // Hacemos una consulta a la colección users para localizar la uid del usuario
                        if (!query.isEmpty) { //Si se encuentra el resultado...
                            val document = query.documents[0] // Cogemos el primer resultado encontrado (solo hay uno...)
                            var login = document.getBoolean("login")//Y cogemos los datos necesarios para ingresar al usuario dentro de la app
                            var numEmple = document.getLong("numEmple")!!.toString()
                            var rol = document.getString("rol")
                            userRol=rol // Guardamos el rol que hemos sacado de la base de datos en la variable global
                            if (login == false) { // Si es su primer login...
                                auth.sendPasswordResetEmail(email).addOnCompleteListener { task -> //Enviamos un email de restablecimiento de contraseña
                                    if (task.isSuccessful) { // Si el email se envía...
                                        db.collection("users").document(numEmple).update("login", true)// Cambiamos el valor de login de la coleccion users a "true" para que no vuelva a enviarle el mail en el proximo login
                                        binding.warningPass.visibility = View.VISIBLE // Mostramos el mensjae de recomendación para cambiar la contraseña
                                        Snackbar.make(binding.root, "Email enviado, revisa tu correo.", Snackbar.LENGTH_SHORT).show() // Indicamos que se ha enviado el correo
                                    }
                                }.addOnFailureListener { e -> // Si falla la tarea...mostramos el mensaje correspondiente
                                    snackBar(binding.root, "Error al enviar el mail: ${e.message}.")
                                }
                            } else { // Si no es su primer login...
                                binding.warningPass.visibility = View.GONE // Ocultamos el mensaje de recomendación
                                when (rol) { //Dependiendo del rol, el usuario será redirigido a un calendario u otro.
                                    "Jefe de tienda" -> startActivity(Intent(applicationContext, CalendarActivity::class.java))
                                    "Jefe de sección", "Empleado" -> startActivity(Intent(applicationContext, EmployeeCalendarActivity::class.java))
                                }
                            }
                        } else {
                            snackBar(binding.root, "No existe ningun usuario con ese email.") // Excepción que se muestra cuando no existe el email introducido
                        }
                    }.addOnFailureListener { e ->
                        snackBar(binding.root, "${e.message}.")
                    }
            }
        }.addOnFailureListener {   // Error de credenciales incorrectas
            snackBar(binding.root, "El usuario o contraseña no coinciden.")
        }
    }

    private fun checkSession() { // Funcion para comprobar si la sesion esta activa, la cual si es que si, vuelve a redirigir al usuario a su pantalla
        firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null && userRol !=null) {
            when (userRol) { //Dependiendo del rol, el usuario será redirigido a un calendario u otro.
                "Jefe de tienda" -> startActivity(Intent(applicationContext, CalendarActivity::class.java))

                "Jefe de sección", "Empleado" -> startActivity(Intent(applicationContext, EmployeeCalendarActivity::class.java)
                )
            }
            finish() // Cierra por completo la actividad actual impidiendo que sea accesible al pulsar el botón atras
        }

    }


    private fun snackBar(view: View, message: String) { //Función para simplificar Snackbars
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onStart() { //Sobreescribimos el método onStart paraque cuando se cargue de nuevo esta activity llame al metodo para comprobar sesion activa.
        checkSession()
        super.onStart()
    }
}

