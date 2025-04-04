package com.example.proyectofinal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.FrameLayout
import android.widget.RadioGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.proyectofinal.databinding.ActivityAddQuitEmployeeBinding
import com.google.android.gms.tasks.Tasks
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AddQuitEmployeeActivity : BaseActivity(), RadioGroup.OnCheckedChangeListener,
    View.OnClickListener, AdapterView.OnItemSelectedListener {
    private var positionSection: Int = 0
    private var positionJob: Int = 0
    private var job: String = ""
    private lateinit var binding: ActivityAddQuitEmployeeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddQuitEmployeeBinding.inflate(layoutInflater)

        val frameContent = findViewById<FrameLayout>(R.id.content_frame)
        frameContent.addView(binding.root)




        binding.radioGroup.setOnCheckedChangeListener(this)
        binding.includeAddEmployee.btnAdd.setOnClickListener(this)
        binding.includeAddEmployee.spinnerSections.onItemSelectedListener = this
        binding.includeAddEmployee.spinnerJob.onItemSelectedListener = this

    }

    override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
        when (checkedId) {
            binding.AddEmployee.id -> {
                binding.addCardView.visibility = View.VISIBLE
                binding.deleteCardView.visibility = View.GONE
            }

            binding.deleteEmployee.id -> {
                binding.addCardView.visibility = View.GONE
                binding.deleteCardView.visibility = View.VISIBLE
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.includeAddEmployee.btnAdd.id -> {
                var name = binding.includeAddEmployee.editName.text.toString()
                var surname = binding.includeAddEmployee.editSurname.text.toString()
                var email = binding.includeAddEmployee.editEmail.text.toString()
                saveData(name, surname, email, positionSection, job)
            }
        }
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        when (p0?.id) {
            binding.includeAddEmployee.spinnerSections.id -> {
                val section = p0.getItemAtPosition(p2).toString()
                positionSection = p2
                if (positionSection == 0) {
                    Snackbar.make(
                        binding.root,
                        "Debes seleccionar una sección",
                        Snackbar.LENGTH_SHORT
                    ).show()
                } else {
                    Snackbar.make(binding.root, "$section selecccionado", Snackbar.LENGTH_SHORT)
                        .show()
                }
            }

            binding.includeAddEmployee.spinnerJob.id -> {
                job = p0.getItemAtPosition(p2).toString()
                positionJob = p2
                if (positionJob == 0) {
                    Snackbar.make(
                        binding.root,
                        "Debes seleccionar una ocupación",
                        Snackbar.LENGTH_SHORT
                    ).show()
                } else {
                    Snackbar.make(binding.root, "$job selecccionado", Snackbar.LENGTH_SHORT).show()
                }
            }
        }

    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

    private fun saveData(
        name: String,
        surname: String,
        email: String,
        positionSection: Int,
        job: String
    ) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").orderBy("numEmple", Query.Direction.DESCENDING).get()
            .addOnSuccessListener { documents ->
                var newEmpNum = 1
                if (!documents.isEmpty) {
                    val lastEmpNum = documents.documents[0].getLong("numEmple") ?: 0
                    newEmpNum = (lastEmpNum + 32).toInt()
                }
                val pass = newEmpNum.toString()
                val employee = hashMapOf(
                    "nombre" to name,
                    "apellidos" to surname,
                    "numEmple" to newEmpNum,
                    "password" to pass,
                    "email" to email,
                    "rol" to job,
                    "seccion" to positionSection
                )
                val employeeNumber = hashMapOf("numEmple" to newEmpNum)

                val insertJob = db.collection("secciones").document(positionSection.toString())
                    .collection("empleados_sala").document(newEmpNum.toString()).set(employeeNumber)
                val insertRol = db.collection("rol").document(job).set(employeeNumber)
                val insertUser = db.collection("users").document(newEmpNum.toString()).set(employee)

                Tasks.whenAllComplete(insertUser, insertRol, insertJob).addOnSuccessListener {
                    val intent= Intent(this,SuccessActivity::class.java)
                    val bundle=Bundle()
                    bundle.putString("name",name)
                    bundle.putString("surname",surname)
                    bundle.putString("numEmp",newEmpNum.toString())
                    bundle.putString("password",pass)
                    intent.putExtra("data",bundle)
                    startActivity(intent)
                }
                    .addOnFailureListener { e ->
                        Snackbar.make(
                            binding.root,
                            "Error al guardar el empleado: ${e.message}",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
            }
    }
}