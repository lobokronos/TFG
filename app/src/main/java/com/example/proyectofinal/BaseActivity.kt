package com.example.proyectofinal

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectofinal.databinding.ActivityBaseBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**Esta clase tipo "Base" contiene el código que da forma al Navigation Drawer y será heredada por cada una de las activities
 * de la app. Contiene la configuración tanto del ToolBar superior, extrayendo los nombres de cada activity en la que se encuentra
 * para el encabezado, y la configuración de cada uno de los items del menú desplegable, los cuales van enlazados a cada una de las
 * activities correspondientes. Esta Base esta en modo "Open" para que se pueda acceder a ella sin problemas desde otra activity
 */

open class BaseActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBaseBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var uid: String
    private lateinit var employeeType: String
    private lateinit var employeeName: String
    private lateinit var numEmple: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBaseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        variables()
        verifyEmployeeType()



        //Esta linea configura la barra de herramientas principal superior
        setSupportActionBar(binding.toolbar)

        /** Aquí se crea el icono del menú, el cual abre o cierra el navigation drawer. Necesita 5 parametros por defecto**/
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


        /**En este código de abajo, escuchamos la opcion seleccionada en el menú desplegable y se ejecuta el código correspondiente
         * dependiendo de la opción que se seleccione(nos llevará a la activity correspondiente o cerrará sesión).
         */
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {

                R.id.menu_home ->
                    startActivity(Intent(this, HomeActivity::class.java)) //Lleva hacia Home


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

                R.id.menu_closeSession -> {
                    auth.signOut()
                    startActivity(Intent(this, MainActivity::class.java))
                }
            }

            binding.drawerLayout.closeDrawers()
            true
        }
    }



        private fun variables() {
            auth = FirebaseAuth.getInstance()
            db = FirebaseFirestore.getInstance()
            uid = auth.currentUser?.uid ?: ""
        }

        private fun verifyEmployeeType() {
            db.collection("users").whereEqualTo("uid", uid).get().addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val document = result.documents[0]
                    val numEmpleLong = document.getLong("numEmple")
                    numEmple = numEmpleLong.toString()

                    db.collection("users").document(numEmple).get()
                        .addOnSuccessListener { document ->
                            employeeType = document.getString("rol") ?: ""
                            employeeName = document.getString("nombre") ?: ""

                            editMenuRolName()
                        }

                }
            }
        }

        private fun selectCalendarActivity() {
            if (employeeType == "Jefe de tienda") { //Si el usuario es Jefe de Tienda...
                startActivity(Intent(
                        this, CalendarActivity::class.java))//Lleva hacia el calendario del Jefe
            } else if(employeeType=="Empleado") {
                startActivity(Intent(this, EmployeeCalendarActivity::class.java)) // Lleva hacia el calendario de usuario
            }
        }

        private fun editMenuRolName() {
            val header = binding.navigationView.getHeaderView(0)
            val textViewName = header.findViewById<TextView>(R.id.headerProfileText)
            textViewName.text = "Bienvenido ${employeeName}"
            if (employeeType != "Jefe de tienda") {
                binding.navigationView.menu.findItem(R.id.menu_addQuit).isVisible = false
            } else {
                binding.navigationView.menu.findItem(R.id.menu_addQuit).isVisible = true
            }
        }

        /** Esta es la función encargada de dar el título correspondiente al header del Toolbar del Navigation Drawer. En ella se guarda
         * el nombre de cada activity en la variable "title", y mediante la condicional "when", si el nombre de la activity corresponde
         * con la activity actual (en la que se encuentre) reutilizará el nombre de values/Strings que le hemos dado a los menús correspondientes
         * convirtiendolo a String y retornandolo.
         */
        private fun activityTitle(): String {
            val title = this::class.java.name
            return when (title) {
                "com.example.proyectofinal.HomeActivity" -> getString(R.string.menu_home)
                "com.example.proyectofinal.CalendarActivity" -> getString(R.string.menu_calendar)
                "com.example.proyectofinal.AddQuitEmployeeActivity" -> getString(R.string.menu_addQuit)
                "com.example.proyectofinal.ProfileActivity" -> getString(R.string.menu_profile)
                else -> "No Title"
            }

        }

    }

