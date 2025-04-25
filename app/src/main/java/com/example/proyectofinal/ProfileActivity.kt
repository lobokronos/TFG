package com.example.proyectofinal

/**
 * No completado
 *
 * Falta probar a verificar el usuario antes de poder cambiar el mail. Ya que si no sale un error pidiendolo (mirar el logCat)
 */
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.proyectofinal.databinding.ActivityProfileBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : BaseActivity(), View.OnClickListener {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var db:FirebaseFirestore
    private lateinit var auth:FirebaseAuth
    private var user: FirebaseUser?=null
    private lateinit var name:String
    private lateinit var surname:String
    private lateinit var section:String
    private lateinit var email:String
    private lateinit var numEmple:String
    private lateinit var rol:String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding=ActivityProfileBinding.inflate(layoutInflater)

        val navDrawer=findViewById<FrameLayout>(R.id.content_frame)
        navDrawer.addView(binding.root)

        variables()
        actions()
        allData()


    }

    private fun actions() {
        binding.includeOldEmailCardView.btnResetProfilePass.setOnClickListener(this)
        binding.includeOldEmailCardView.btnEditMail.setOnClickListener(this)
        binding.includerResetEmailCardView.btnResetAccept.setOnClickListener(this)
        binding.includerResetEmailCardView.btnCanelReset.setOnClickListener(this)
    }

    private fun variables(){
        db=FirebaseFirestore.getInstance()
        auth= FirebaseAuth.getInstance()
        user=auth.currentUser

    }

    private fun showData() {
        binding.profileRecoveredName.text="${name} ${surname}"
        binding.profileNumText.text=numEmple
        binding.profileRecoveredSection.text=section
        binding.profileRecoveredJob.text=rol
        binding.includeOldEmailCardView.profileRecoveredEmail.text=email
    }

    private fun allData(){
        val uid=user?.uid
        db.collection("users").whereEqualTo("uid",uid).get().addOnSuccessListener { result->
            if(!result.isEmpty){
                val resDoc=result.documents[0]
                name=resDoc.getString("nombre").toString()
                surname=resDoc.getString("apellidos").toString()
                email=resDoc.getString("email").toString()
                rol=resDoc.getString("nombre").toString()
                val docNum=resDoc.getLong("numEmple")
                numEmple=docNum.toString()
                section=resDoc.getString("seccion").toString()
                showData()

            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.includeOldEmailCardView.btnEditMail.id -> {
                binding.cardOldMail.visibility = View.GONE
                binding.cardNewMail.visibility = View.VISIBLE
            }

            binding.includerResetEmailCardView.btnCanelReset.id -> {
                binding.cardOldMail.visibility = View.VISIBLE
                binding.cardNewMail.visibility = View.GONE
            }

            binding.includeOldEmailCardView.btnResetProfilePass.id->{
                startActivity(Intent(applicationContext,ResetPassActivity::class.java))
            }

            binding.includerResetEmailCardView.btnResetAccept.id -> {
                val oldMail = binding.includerResetEmailCardView.cardEditOldEmail.text.toString()
                val pass = binding.includerResetEmailCardView.cardEditPass.text.toString()
                val newMail = binding.includerResetEmailCardView.cardEditNewMail.text.toString()

                val token = EmailAuthProvider.getCredential(oldMail, pass)
                val currentUser = user
                currentUser?.reauthenticate(token)?.addOnCompleteListener { reAuth ->
                    if (reAuth.isSuccessful) {
                        currentUser.updateEmail(newMail).addOnCompleteListener { update ->
                            if (update.isSuccessful) {
                                val insertMail= hashMapOf("mail" to newMail)
                                db.collection("users").document(numEmple).set(insertMail).addOnCompleteListener { set ->
                                    if(set.isSuccessful){
                                        snackBar(binding.root, "Email actualizado correctamente")
                                    }else{
                                        snackBar(binding.root,"no se ha podido ingresar el nuevo mail en la base de datos")
                                    }
                                }.addOnFailureListener{e->
                                    snackBar(binding.root,"${e.message}")
                                }
                            } else {
                                snackBar(binding.root, "Error al actualizar el mail")
                            }
                        }.addOnFailureListener { e ->
                            snackBar(binding.root, "Error de reautenticación: ${e.message}")
                            Log.e("FirebaseError", "Error de reautenticación", e)
                        }
                    }
                }
            }
        }
    }
    }

private fun snackBar(view: View, message: String) {
    Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
}