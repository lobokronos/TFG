package com.example.proyectofinal

/**
 * Completada
 */
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import com.example.proyectofinal.databinding.ActivityProfileBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : BaseActivity(), View.OnClickListener {
    private lateinit var binding: ActivityProfileBinding //Guarda el binding
    private lateinit var db: FirebaseFirestore // Variable para realizar la conexion a Firestore
    private lateinit var auth: FirebaseAuth //Variable para generar la instancia de Authentication
    private var user: FirebaseUser? = null //Variable para guardar el usuario autenticado
    private lateinit var name: String //Variable para guardar el nombre del usuario
    private lateinit var surname: String //Variable para guardar el apellido del usuario
    private lateinit var section: String //Variable para guardar la sección del usuario
    private lateinit var email: String //Variable para guardar el email del usuario
    private lateinit var numEmple: String //Variable para guardar el número de empleado del usuario
    private lateinit var rol: String //Variable para guardar el rol del usuario


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfileBinding.inflate(layoutInflater)

        val navDrawer = findViewById<FrameLayout>(R.id.content_frame)
        navDrawer.addView(binding.root)

        variables() // Método para inicializar las variables al iniciar la pantalla
        checkUserVerification() //Método para ocultar items según si el ususario está verificado o no
        actions() // Método que guarda las acciones de los botones
        allData() // Método que recoge todos los datos del usuario, y que llama al otro método showData() para mostrarlos
    }

    /**
     * Método que oculta o muestra elementos según si el usuario está verificado o no
     *
     * Si el usuario no está verificado, deshabilitará el botón de "editar mail" para evitar errores, ocultará
     * el icono de verificación y mostrará un mensaje de advertencia indicando que se debe verificar el
     * correo antes de poder actualizarlo, junto con un boton para enviar un mail de verificación. Si está
     * verificado, mostrará el icono de verficación y el texto cambiará a "usuario verificado. Ademas
     * habilitará el botón de editar para que, ahora si, el usuario pueda modificar su mail.
     */
    private fun checkUserVerification() {
        if (user?.isEmailVerified == true) {
            binding.includeOldEmailCardView.verifidedText.setText("Email verificado")
            binding.includeOldEmailCardView.verifidedText.setTextColor(Color.GREEN)
            binding.includeOldEmailCardView.btnVerify.visibility = View.GONE
            //binding.includeOldEmailCardView.btnEditMail.isEnabled = true
        } else {
            //binding.includeOldEmailCardView.btnEditMail.isEnabled = false
            binding.includeOldEmailCardView.btnVerify.visibility = View.VISIBLE
            binding.includeOldEmailCardView.verifiedIMG.visibility = View.GONE
        }
    }

    /**
     * Método que contiene las acciones de los botones
     */
    private fun actions() {
        binding.includeOldEmailCardView.btnResetProfilePass.setOnClickListener(this)
        //binding.includeOldEmailCardView.btnEditMail.setOnClickListener(this)
        binding.includerResetEmailCardView.btnResetAccept.setOnClickListener(this)
        binding.includerResetEmailCardView.btnCanelReset.setOnClickListener(this)
        binding.includeOldEmailCardView.btnVerify.setOnClickListener(this)
    }

    /**
     * Método para inicializar variables
     */
    private fun variables() {
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        user = auth.currentUser
        Log.d("CorreoSesionIniciada", "Email actual: ${user?.email}")

    }

    /**
     * Método para mostrar los datos recogidos de la base de datos en el perfil del usuario
     */
    private fun showData() {
        binding.profileRecoveredName.text = "${name} ${surname}"
        binding.profileNumText.text = numEmple
        binding.profileRecoveredSection.text = section
        binding.profileRecoveredJob.text = rol
        binding.includeOldEmailCardView.profileRecoveredEmail.text = email
    }

    /**
     * Método que hace una llamada a la colección del usuario en la base de datos para recoger sus datos
     */
    private fun allData() {
        val uid = user?.uid
        db.collection("users").whereEqualTo("uid", uid).get().addOnSuccessListener { result ->
            if (!result.isEmpty) {
                val resDoc = result.documents[0]
                name = resDoc.getString("nombre").toString()
                surname = resDoc.getString("apellidos").toString()
                email = resDoc.getString("email").toString()
                rol = resDoc.getString("rol").toString()
                val docNum = resDoc.getLong("numEmple")
                numEmple = docNum.toString()
                section = resDoc.getString("seccion").toString()
                showData() // Llamada al método que mostrará los datos en las lineas correspondientes del perfil

            } else { // Si no se encuentra el documento...
                name = "No hay nombre para mostrar"
                surname = "No hay apellidos para mostrar"
                email =
                    "No hay email para mostrar"  // Mostramos el mensaje de error en todos los campos
                rol = "No hay puesto para mostrar"
                numEmple = "No hay número de empleado para mostrar"
                section = "No hay sección para mostrar"
                showData() //Llamamos al metodo que muestra los datos para mostrar el error en cada campo
            }
        }.addOnFailureListener { e ->
            snackBar(binding.root, "Error al acceder a la base de datos: ${e.message}")
        }
    }


    /**
     * Método que reoge las funciones de los botones
     */
    override fun onClick(v: View?) {
        when (v?.id) {

            binding.includeOldEmailCardView.btnResetProfilePass.id -> {
                //Cuando se pulsa, el usuario es redirigido a la pantalla de restablecimiento de contraseña
                startActivity(Intent(applicationContext, ResetPassActivity::class.java))
            }
                    /**
                     * Botón para mandar el email de verificación
                     */
                    binding.includeOldEmailCardView.btnVerify.id->{
                        //Al pulsarlo se envía un email de verificación para que el usuario verifique su cuenta.
                        user?.sendEmailVerification()?.addOnCompleteListener { verify ->
                            if (verify.isSuccessful) { // Si sale bien...
                                snackBar(
                                    binding.root,
                                    "Correo de verificación enviado. Revisa tu bandeja de entrada"
                                )
                            } else { // Si no sale bien...
                                snackBar(binding.root, "Error al enviar el correo de verificación")
                            }
                        }
                    }
                }
            }
    /**
     **
     * Botón para editar el mail

    binding.includeOldEmailCardView.btnEditMail.id -> {
    // Cuando es pulsado, oculta la cardview actual y muestra la de restablecimiento de correo
    binding.cardOldMail.visibility = View.GONE
    binding.cardNewMail.visibility = View.VISIBLE
    }

    /**
     * Botón de "cancelar editado"
    */
    binding.includerResetEmailCardView.btnCanelReset.id -> {
    // cuando es pulsado, oculta la cardview actual y vuelve a mostrar la de verificación
    binding.cardOldMail.visibility = View.VISIBLE
    binding.cardNewMail.visibility = View.GONE
    }*/
    /**
     * Botón para restablecer la contraseña
     */
        }

        /**
         * Método para mostrar Snackbars
         */
        private fun snackBar(view: View, message: String) {
            Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
        }
/**
 * Botón de restablecimiento de Email, se quita porque se ha descubierto un fallo bastante grande, hay que repasarlo
 * para implementarlo antes del lanzamiento
 *
 *
 * binding.includerResetEmailCardView.btnResetAccept.id -> {
//Primero recogemos los Strings de los editText (correo actual, contraseña y el nuevo)
val oldMail = binding.includerResetEmailCardView.cardEditOldEmail.text.toString()
val pass = binding.includerResetEmailCardView.cardEditPass.text.toString()
val newMail = binding.includerResetEmailCardView.cardEditNewMail.text.toString()
if (oldMail.isEmpty()) { //Si no se ha introducido el correo viejo salta un error
snackBar(binding.root, "Por seguridad debes introducir tu correo actual")
} else if (oldMail != user?.email) { //Si el correo viejo no coincide con el del usuario, salta un error
snackBar(
binding.root,
"El correo actual no coincide con el de la sesión iniciada"
)
} else if (pass.isEmpty()) { // Si la contraseña no ha sido introducida, salta un error
snackBar(binding.root, "Por seguridad debes introducir tu contraseña")
} else if (newMail.isEmpty()) { // Si el nuevo correo no ha sido introducido salta un error
snackBar(binding.root, "Introduce tu nuevo correo por favor")
} else { // Si todos los campos estan correctos...
db.collection("usuarios registrados").whereEqualTo("email", newMail).get()
.addOnSuccessListener { query -> // Se hace una busqueda en la base de datos para saber si el correo ya existe
Log.d("VERIFICAR_CORREO", "Correo introducido: $email - Resultados encontrados: ${query.size()}")
if (!query.isEmpty) { // Si existe, se le impide al usuario continuar hasta que introduzca uno que no exista
snackBar(
binding.root,
"Este correo ya existe, por favor introduce otro"
)
} else {
val token = EmailAuthProvider.getCredential(
oldMail,
pass
) //Recogemos el correo y la contraseña del usuario y la guardamos en una variable a modo de token o "llave"
val currentUser = user
Log.d("CorreoEnSesion", user?.email.toString())
currentUser?.reauthenticate(token)
?.addOnCompleteListener { reAuth -> // Aqui utilizamos la "llave" para comprobar si el usuario y contraseña introducidos coinciden con el usuario actual. Si coinciden...
if (reAuth.isSuccessful) {//si coinciden...
if (currentUser.isEmailVerified) {//Y si el usuario ya está verificado...
/*Se ha sustituido el método "updateEmail(String)" por verifyBeforeUpdateEmail ya que el otro estaba deprecado.
El antiguo método daba error ya que el funcionamiento de Authentication ha cambiado. Ahora se necesita verificar el
email antes de ser modificado. Ahora se puede actualizar el mail, pero hasta que el usuario no verifique su cuenta
a traves del mail de verificación, no podra hacer login.
*/
currentUser.verifyBeforeUpdateEmail(newMail)
.addOnCompleteListener { verify -> //Actualizamos el email con el newMail
if (verify.isSuccessful) {//Si la actualización es correcta...
val insertMail =
hashMapOf("mail" to newMail) //creamos un hashmap para introducir los datos en Firebase
db.collection("users")
.document(numEmple)
.set(insertMail)
.addOnCompleteListener { set -> //Hacemos el set para cambiar los datos en la BBDD
if (set.isSuccessful) { // Avisamos de que ha salido bien
snackBar(
binding.root,
"Email actualizado correctamente"
)
} else { //Si no, avisamos del error
snackBar(
binding.root,
"no se ha podido ingresar el nuevo mail en la base de datos"
)
}
}
.addOnFailureListener { e -> //Si da fallo al guardar los datos, avisamos
snackBar(
binding.root,
"Error al guardar en la base de datos: ${e.message}"
)
}
} else {
snackBar(
binding.root,
"Error al actualizar el mail"
)
}
}
.addOnFailureListener { e ->// Se ha dejado este Log para ver el fallo que daba al intentar actualizar el nuevo mail
snackBar(
binding.root,
"Fallo real: ${e.message}"
)
Log.e(
"FIREBASE_ERROR",
"updateEmail failed",
e
)
}
} else {
snackBar(binding.root, "Debes verificar el correo")
}
} else {
snackBar(
binding.root,
"Contraseña incorrecta o main incorrecto"
)
}
}
}
}.addOnFailureListener {
snackBar(binding.root, "Error al comprobar si el correo existe")
}
}
}*/