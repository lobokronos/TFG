package com.example.proyectofinal

/**
 * Completada
 *
 */

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectofinal.databinding.ActivityBaseBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Descripcion del base Adapter para el Navigation Drawer
 *
 * Esta clase tipo "Base" contiene el código que da forma al Navigation Drawer y mostrará cada una de las activities
 * de la app en ella. Contiene la configuración tanto del ToolBar superior, extrayendo los nombres de cada activity en la que se encuentra
 * para el encabezado, y la configuración de cada uno de los items del menú desplegable, los cuales van enlazados a cada una de las
 * activities correspondientes. Esta Base esta en modo "Open" para que se pueda acceder a ella sin problemas desde otra activity
 */

open class BaseActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBaseBinding // Variable del binding
    private lateinit var auth: FirebaseAuth // Variable que guarda la instancia de Authentication
    private lateinit var db: FirebaseFirestore // Variable para hacer la conexión con Firebase
    private lateinit var uid: String // Variable que guardará el uid del usuario
    private lateinit var employeeType: String // Variable que guarda el tipo de empleado (rol)
    private lateinit var employeeName: String // Variable para guardar el nombre del empleado
    private lateinit var numEmple: String // Variable para guardar el número de empleado

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBaseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        variables()
        verifyEmployeeType() // Función que comprueba el rol del usuario que ha iniciado sesión



        /**
         * Esta linea configura la barra de herramientas principal superior
         **/
        setSupportActionBar(binding.toolbar)

        /**
         *  Aquí se crea el icono del menú, el cual abre o cierra el navigation drawer. Necesita 5 parametros por defecto
         **/
        val toggle = ActionBarDrawerToggle(
            this,   //Contexto de la app
            binding.drawerLayout,   //el layout del Drawer
            binding.toolbar,        //el layout de la barra de herramientas
            R.string.open_drawer,   //Texto al abrir
            R.string.close_drawer   //Texto al cerrar
        )

        //Agregamos la acción de escucha para el botón del menú
        binding.drawerLayout.addDrawerListener(toggle)

        toggle.syncState() //syncStatesincroniza el icono del menu cuando se abre o se cierra, cambiandolo segun su estado.

        //Aquí le damos título personalizado en el toolbar dependiendo de la activity en la que nos encontremos a traves de la
        //función "activityTitle".
        supportActionBar?.title = activityTitle()


        /**
         * Aquí escuchamos la opcion seleccionada del menú
         *
         * En este código de abajo, escuchamos la opcion seleccionada en el menú desplegable y se ejecuta el código correspondiente
         * dependiendo de la opción que se seleccione(nos llevará a la activity correspondiente o cerrará sesión).
         */
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {

                R.id.menu_profile ->
                    startActivity(Intent(this, ProfileActivity::class.java))//Lleva hacia Mi Perfil


                R.id.menu_calendar ->
                    if(employeeType.isNotEmpty()){
                    selectCalendarActivity()
                    }else{
                        Snackbar.make(binding.root,"Cargando datos, por favor espere",Snackbar.LENGTH_SHORT).show()
                    }
                R.id.menu_addQuit -> {
                    startActivity(
                        Intent(
                            this,
                            AddQuitEmployeeActivity::class.java
                        )
                    )//Lleva hacia Agregar/Quitar Empleado
                }
/**
 *
 *  Este item del menú cierra la sesión actual, cortando de raíz el funcionamiento de la activity actual y redirigiendo al usuario a la pantalla de login
 *  */
                R.id.menu_closeSession -> {
                    auth.signOut()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }
            //Esta función cierra automáticamente el menú cuando un item es pulsado
            binding.drawerLayout.closeDrawers()
            true
        }
    }


    /**
     * Función para definir variables
     */
        private fun variables() {
            auth = FirebaseAuth.getInstance()
            db = FirebaseFirestore.getInstance()
            uid = auth.currentUser?.uid ?: ""
        }

    /**
     * Función para averiguar el rol del usuario que se ha logueado
     *
     * Esta función sirve para determinar que tipo de rol tiene el usuario, y así ocultar y redirigir a las activity
     * correspondientes segun su rol establecido.
     */
        private fun verifyEmployeeType() {
        db.collection("users").whereEqualTo("uid", uid).get().addOnSuccessListener { result ->
            if (!result.isEmpty) { // A traves de su uid sacamos su número de empleado.
                val document =
                    result.documents[0] //Cogemos el primer resultado de la consulta (el único).
                val numEmpleLong =
                    document.getLong("numEmple") //Cogemos su número de empleado en Long.
                numEmple = numEmpleLong.toString() // Lo passamos a String para poder trabajarlo.
                employeeType = document.getString("rol") ?: "" //Cogemos tambien el tipo de empleado.
                employeeName = document.getString("nombre") ?: "" // Y el nombre.
                editMenuRolName() // Llamamos a la función que define el menú según el rol.

                }
            }
        }

    /**
     * Función para desviar a la activit Calendar o EmployeeCalendar dependiendo del rol. (falta introducir el jefe de sección).
     */
        private fun selectCalendarActivity() {
            if (employeeType == "Jefe de tienda") { //Si el usuario es Jefe de Tienda...
                startActivity(Intent(this, CalendarActivity::class.java))//Lleva hacia el calendario del Jefe
            } else if(employeeType=="Empleado"|| employeeType == "Jefe de sección") {
                startActivity(Intent(this, EmployeeCalendarActivity::class.java)) // Lleva hacia el calendario de usuario
            }
        }

    /**
     * Función para desvira el menu según el rol del usuario.
     *
     * Esta función Cambia el nombre del menu al del usuario en la cabecera y, dependiendo de si es el
     * jefe de tienda (superusuario) o no, mostrará o no la activity de Añadir o quitar empleados.
     */
        private fun editMenuRolName() {
            val header = binding.navigationView.getHeaderView(0) // Cogemos la única vista de la cabecera del menú.
            val textViewName = header.findViewById<TextView>(R.id.headerProfileText) //Cogemos el textView de dicha vista .
            textViewName.text = "Bienvenido ${employeeName}" //Asignamos el nombre del usuario al TextView.
            if (employeeType != "Jefe de tienda") { // Si es jefe de tienda muestra las activitys.
                binding.navigationView.menu.findItem(R.id.menu_addQuit).isVisible = false
            } else { // Si no las oculta.
                binding.navigationView.menu.findItem(R.id.menu_addQuit).isVisible = true
            }
        }

        /**
         * Función que dá título a cada activity en la cabecera de la pantalla.
         *
         * Esta es la función encargada de dar el título correspondiente al header del Toolbar del Navigation Drawer. En ella se guarda
         * el nombre de cada activity en la variable "title", y mediante la condicional "when", si el nombre de la activity corresponde
         * con la activity actual (en la que se encuentre) reutilizará el nombre de values/Strings que le hemos dado a los menús correspondientes
         * convirtiendolo a String y retornandolo.
         */
        private fun activityTitle(): String {
            val title = this::class.java.name // Cogemos el nombre de la activity en la que nos vayamos a encontrar.
            return when (title) { // dependiendo del nombre que de, extraemos y colocamos el String correspondiente.
                "com.example.proyectofinal.CalendarActivity" -> getString(R.string.menu_calendar)
                "com.example.proyectofinal.EmployeeCalendarActivity" ->getString(R.string.menu_calendar)
                "com.example.proyectofinal.AddQuitEmployeeActivity" -> getString(R.string.menu_addQuit)
                "com.example.proyectofinal.ProfileActivity" -> getString(R.string.menu_profile)
                "com.example.proyectofinal.ProfileActivity" -> "Usuario creado"

                else -> "No Title"
            }

        }

    }

