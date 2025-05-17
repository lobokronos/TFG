package com.example.proyectofinal

/**
 *Completada
 *
 * Cambiado el redirigido de inicio de sesion al calendario directamente.
 * Falta borrar la activity HomeActivity.kt
 */
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectofinal.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var progressDialog: ProgressDialog
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
        binding.btnGoogle.setOnClickListener(this)
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

                if (email.isEmpty()) { //Si el email está vacío, muestra una advertencia.
                    snackBar(binding.root, "Ingrese un email")
                }
                if (password.isEmpty())  //Si la contraseña está vacía, muestra una advertencia.
                    snackBar(binding.root, "Ingrese la contraseña")
                else {  //Si todos los campos están rellenos, se procede a la llamada del método login.
                    loginUser(email, password)
                }
            }
            /**
             * Botón Olvido su Contraseña
             *
             * Si pulsamos sobre este botón, nos lleva directamente a la activity de restablecimiento
             * de contraseña
             */
            binding.btnForgotPassword.id -> {
                startActivity(Intent(this, ResetPassActivity::class.java))
            }

            binding.btnGoogle.id -> {
                startGoogleSignIn()
            }
        }
    }

    private fun startGoogleSignIn() {
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
    }

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
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val uid = auth.currentUser?.uid.toString()
                db.collection("users").whereEqualTo("uid", uid).get()
                    .addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty) {
                            val document = querySnapshot.documents[0]
                            var login = document.getBoolean("login")
                            var numEmple = document.getLong("numEmple")!!.toString()
                            var rol = document.getString("rol")
                            if (login == false) {
                                auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        db.collection("users").document(numEmple)
                                            .update("login", true)
                                        binding.warningPass.visibility = View.VISIBLE
                                        Snackbar.make(
                                            binding.root,
                                            "Email enviado, revisa tu correo.",
                                            Snackbar.LENGTH_SHORT
                                        ).show()
                                    }
                                }.addOnFailureListener { e ->
                                    snackBar(binding.root, "Error al enviar el mail: ${e.message}.")
                                }
                            } else {
                                binding.warningPass.visibility = View.GONE
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
                            }
                        } else {
                            snackBar(binding.root, "No existe ningun usuario con ese email.")
                        }
                    }.addOnFailureListener { e ->
                    snackBar(binding.root, "${e.message}.")
                }
            }
        }.addOnFailureListener { e ->
            snackBar(binding.root, "El usuario o contraseña no coinciden: ${e.message}.")
        }
    }
}

private fun snackBar(view: View, message: String) {
    Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
}

