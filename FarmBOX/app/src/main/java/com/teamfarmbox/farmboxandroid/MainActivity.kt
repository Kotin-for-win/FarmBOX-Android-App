package com.teamfarmbox.farmboxandroid

// FarmBOX Android App - Android App to Control FarmBOX Devices
// Copyright (C) 2022 Michael Reeves

// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.perf.ktx.performance
import com.google.firebase.perf.metrics.Trace
import com.teamfarmbox.farmboxandroid.databinding.*

public lateinit var conid2: String
public var RedirectFromDash = false
public var AddingSecondBox = false
public var RedirectForNo = 1
public var WebViewForHelium = false
public var WebViewQueued = false

class FarmboxConnectionIdException(message: String) : Exception(message)

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPreference =  getSharedPreferences("FarmBOX", Context.MODE_PRIVATE)
        val setup =  sharedPreference.getBoolean("setup", false)
        if (setup == true) {
            add_conid(sharedPreference.getString("fb1-conid", null))
            add_conid(sharedPreference.getString("fb2-conid", null))
            val serviceIntent = Intent(
                this,
                AINotificationService::class.java
            )
            startService(serviceIntent)
            Log.d("MICHAELDEBUG", "Setup already done")
            val intent = Intent (this, DashboardActivity::class.java)
            Firebase.crashlytics.log("About to pass intent for DashboardActivity!")
            startActivity(intent)
        } else {
                // Create the NotificationChannel, but only on API 26+ because
                // the NotificationChannel class is new and not in the support library
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val name = getString(R.string.channel_title)
                    val descriptionText = getString(R.string.channel_description)
                    val importance = NotificationManager.IMPORTANCE_DEFAULT
                    val channel = NotificationChannel("fb-ai", name, importance).apply {
                        description = descriptionText
                    }
                    // Register the channel with the system
                    val notificationManager: NotificationManager =
                        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.createNotificationChannel(channel)
                }
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val start_adventure = binding.button2
        Firebase.crashlytics.log("MainActivity Started!")
        start_adventure.setOnClickListener {
            val intent = Intent (this, AuthActivity::class.java)
            Firebase.crashlytics.log("About to pass intent for AuthActivity!")
            startActivity(intent)
        }
    }
}

class AuthActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Firebase.crashlytics.log("AuthActivity Started! About to call FirebaseApp.initalizeApp(this)")
        FirebaseApp.initializeApp(this)
        Firebase.crashlytics.log("Firebase init done!")
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth
        val next = binding.button4
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val SIT_EXIST = Firebase.performance.newTrace("existing_user_sign_in_time")
            SIT_EXIST.start()
            Firebase.crashlytics.log("User already signed in.")
            updateUI(true, binding, null, SIT_EXIST)
        } else {
            val SIT_NEW = Firebase.performance.newTrace("new_user_sign_in_time")
            SIT_NEW.start()
            Firebase.crashlytics.log("New account needed.")
            signInAnonymously(binding, SIT_NEW)
        }


    }

    private fun signInAnonymously(binding2: ActivityAuthBinding, traceToSend: Trace) {
        // [START signin_anonymously]
        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInAnonymously:success")
                    val user = auth.currentUser
                    updateUI(true, binding2, null, traceToSend)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInAnonymously:failure", task.exception)
                    Toast.makeText(
                        baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                    updateUI(false, binding2, task.exception.toString(), traceToSend)
                }
            }
        // [END signin_anonymously]
    }

    fun updateUI(worked: Boolean, binding3: ActivityAuthBinding, exception: String?, toStop: Trace) {
        val user = FirebaseAuth.getInstance().currentUser
        if (worked == true) {
            var uid = "null"
            user?.let {
                uid = user.uid.toString()
            }
            val ad = "All done!"
            val asc = "Account Setup Complete!"
            binding3.textView3.text = ad
            binding3.textView5.text = asc
            binding3.button4.visibility = VISIBLE
            binding3.textView4.text = uid
            toStop.stop()
            val intent = Intent (this, PairActivity::class.java)
            Firebase.crashlytics.log("About to pass intent for PairActivity!")
            startActivity(intent)
        }
        else {
            val ohnoes = "Oh Noes! Something went wrong."
            binding3.textView4.text = exception
            binding3.textView3.text = "Failure!"
            binding3.textView5.text = ohnoes
            toStop.stop()

        }

    }
}

class PairActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPairBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Firebase.crashlytics.log("PairActivity Started!")
        binding = ActivityPairBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val button3 = binding.button3

        button3.setOnClickListener {
            Firebase.crashlytics.log("button3 Pressed!")
            Log.v("MichaelDEBUG","Button3 Pressed")
            val conidEditText: EditText = binding.conid
            val conid = conidEditText.text?.toString()
            if (conid == null) {
                conid2 = "null"
            } else {
                conid2 = conid
            }
            Log.v("MichaelDEBUG-CONID", conid2)
            val getTrace = Firebase.performance.newTrace("check_if_exisits")
            getTrace.start()
            val db = Firebase.firestore
            val docRef = db.collection("farmboxes").document(conid2)
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document.data == null) {
                        val fal = "Failure"
                        binding.textView2.text = fal
                        Snackbar.make(
                            button3,
                            "It doesn't look like you entered a valid Connection ID.",
                            Snackbar.LENGTH_LONG
                        )
                            .show()

                    } else {
                        Log.d(TAG, "DocumentSnapshot data: ${document.data}")
                        val suc: String = "Success!"
                        binding.textView2.text = suc
                        //val map = document.getData()
                        val map = document.data
                        val init = map!!.get("init")
                        if (init == 0.toLong()) {
                            Snackbar.make(button3, "That ID is valid, but it doesn't look like your FarmBOX has connected to the internet yet.", Snackbar.LENGTH_LONG)
                                .show()
                        }
                        else if (init == 1.toLong()){
                        //    Snackbar.make(button3, "You're all set for the next step!", Snackbar.LENGTH_LONG) .show()
                            val intent = Intent (this, EnterConnectionActivity::class.java)
                            Firebase.crashlytics.log("About to pass intent for EnterConnectionActivity!")
                            startActivity(intent)
                        }
                        else if (init == 1.5.toDouble()) {
                            val intent = Intent (this, EnterConnectionActivity::class.java)
                            Firebase.crashlytics.log("About to pass intent for EnterConnectionActivity!")
                            startActivity(intent)
                        }
                        else if (init == null){
                            Snackbar.make(button3, "Life is null.", Snackbar.LENGTH_LONG)
                                .show()
                        }
                        else if (init == 2.toLong()) {
                            val intent = Intent (this, EnterConnectionActivity::class.java)
                            Firebase.crashlytics.log("About to pass intent for EnterConnectionActivity!")
                            startActivity(intent)
                        }
                        else {
                            Snackbar.make(button3, "Strange things are happening. Here's some debug info:" + init.toString() + init::class.simpleName, Snackbar.LENGTH_LONG).show()

                        }


                    }
                }
                .addOnFailureListener { exception ->
                    Log.d(TAG, "get failed with ", exception)
                    Snackbar.make(button3, "Looks like something went wrong on our end. Please make sure you are connected to the internet.", Snackbar.LENGTH_LONG)
                        .show()
                    Log.e("FIRESTORE_FAIL", exception.toString())

                }
            getTrace.stop()
                        }

                    }
                }
class EnterConnectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEnterConnectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEnterConnectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.pword.visibility = INVISIBLE
        binding.ssid.visibility = INVISIBLE
        binding.button12.visibility = INVISIBLE
        val enetusb = binding.button11
        Firebase.crashlytics.log("EnterConnection Activity Started!")
        Snackbar.make(binding.textView7, "We found your FarmBOX connected to the Internet!", Snackbar.LENGTH_LONG)
            .show()
        enetusb.setOnClickListener {
            if (RedirectFromDash == false) {
                val intent = Intent(this, ChoosePreferencesActivity::class.java)
                Firebase.crashlytics.log("About to pass intent for ChoosePreferenceActivity!")
                startActivity(intent)
            } else {
                val intent = Intent(this, DashboardActivity::class.java)
                Firebase.crashlytics.log("About to pass intent for DashboardActivity!")
                startActivity(intent)
            }
        }
        val wifi = binding.button5
        wifi.setOnClickListener {
            binding.textView8.text = "Please enter your WiFi details."
            binding.pword.visibility = VISIBLE
            binding.ssid.visibility = VISIBLE
            binding.button12.visibility = VISIBLE
            wifi.visibility = INVISIBLE
            enetusb.visibility = INVISIBLE
            binding.gas.visibility = INVISIBLE
            if (RedirectFromDash == true) {
                var db = Firebase.firestore
                val docRef = db.collection("farmboxes").document(conid2)
                docRef.get()
                    .addOnSuccessListener { document ->
                        val map = document.data
                        val pre_ssid = map?.get("wifi-ssid")
                        val pre_pword = map?.get("wifi-pword")
                        binding.ssid.setText(pre_ssid?.toString())
                        binding.pword.setText(pre_pword?.toString())
                    }
            }
        }
        binding.button12.setOnClickListener {

            val db = Firebase.firestore
            val docRef = db.collection("farmboxes").document(conid2)
            docRef.get()
                .addOnSuccessListener { document ->
                    try {
                        val map = document.data
                        val pre_init = map?.get("init")
                        val pre_length = map?.get("length")
                        val pre_bt = map?.get("bt")
                        val pre_wt = map?.get("wt")
                        val pre_interval = map?.get("interval")
                        val pre_plant_type = map?.get("plant-type")
                        /*  "interval" to interval,
                        "length" to length,
                        "bt" to bt,
                        "wt" to wt,
                        "init" to 2 */
                        val ssid_value = binding.ssid.text?.toString()
                        if (ssid_value == null) {
                            throw NullPointerException("WiFi Setup: SSID EditText is null")
                        }
                        val wifi_pword = binding.pword.text?.toString()
                        if (wifi_pword == null) {
                            throw NullPointerException("WiFi Setup: pword EditText is null")
                        }
                        val hashMaptoWrite = hashMapOf(
                            "wifi" to true,
                            "wifi-ssid" to ssid_value,
                            "wifi-pword" to wifi_pword,
                            "init" to pre_init,
                            "bt" to pre_bt,
                            "wt" to pre_wt,
                            "interval" to pre_interval,
                            "length" to pre_length,
                            "plant-type" to pre_plant_type,
                        )
                        db.collection("farmboxes").document(conid2)
                            .set(hashMaptoWrite)
                        if (RedirectFromDash == false) {
                            val intent = Intent(this, ChoosePreferencesActivity::class.java)
                            Firebase.crashlytics.log("About to pass intent for ChoosePreferenceActivity!")
                            startActivity(intent)
                        } else {
                            val intent = Intent(this, DashboardActivity::class.java)
                            Firebase.crashlytics.log("About to pass intent for DashboardActivity!")
                            startActivity(intent)
                        }
                    } catch (e: NullPointerException) {
                        Snackbar.make(binding.button12, "Please enter a value in each box.", Snackbar.LENGTH_LONG)
                    }
                }

        }
        binding.gas.setOnClickListener {
            if (RedirectFromDash == false) {
                WebViewQueued = true
                WebViewForHelium = true
                val db = Firebase.firestore
                val docRef = db.collection("farmboxes").document(conid2)
                docRef.get()
                    .addOnSuccessListener { document ->
                        val map = document.data
                        val pre_init = map?.get("init")
                        val pre_length = map?.get("length")
                        val pre_bt = map?.get("bt")
                        val pre_wt = map?.get("wt")
                        val pre_interval = map?.get("interval")
                        val pre_plant_type = map?.get("plant-type")
                        val pre_ssid_value = map?.get("wifi-ssid")
                        val pre_wifi_pword = map?.get("wifi-pword")
                        /*  "interval" to interval,
                        "length" to length,
                        "bt" to bt,
                        "wt" to wt,
                        "init" to 2 */
                        val hashMaptoWrite = hashMapOf(
                            "wifi" to false,
                            "wifi-ssid" to pre_ssid_value,
                            "wifi-pword" to pre_wifi_pword,
                            "init" to pre_init,
                            "bt" to pre_bt,
                            "wt" to pre_wt,
                            "interval" to pre_interval,
                            "length" to pre_length,
                            "plant-type" to pre_plant_type
                        )
                        db.collection("farmboxes").document(conid2)
                            .set(hashMaptoWrite)
                        val intent = Intent(this, ChoosePreferencesActivity::class.java)
                        Firebase.crashlytics.log("About to pass intent for ChoosePreferenceActivity!")
                        startActivity(intent)
                    }
            } else {
                WebViewForHelium = true
                val db = Firebase.firestore
                val docRef = db.collection("farmboxes").document(conid2)
                docRef.get()
                    .addOnSuccessListener { document ->
                        val map = document.data
                        val pre_init = map?.get("init")
                        val pre_length = map?.get("length")
                        val pre_bt = map?.get("bt")
                        val pre_wt = map?.get("wt")
                        val pre_interval = map?.get("interval")
                        val pre_plant_type = map?.get("plant-type")
                        val pre_ssid_value = map?.get("wifi-ssid")
                        val pre_wifi_pword = map?.get("wifi-pword")
                        /*  "interval" to interval,
                            "length" to length,
                            "bt" to bt,
                            "wt" to wt,
                            "init" to 2 */
                        val hashMaptoWrite = hashMapOf(
                            "wifi" to false,
                            "wifi-ssid" to pre_ssid_value,
                            "wifi-pword" to pre_wifi_pword,
                            "init" to pre_init,
                            "bt" to pre_bt,
                            "wt" to pre_wt,
                            "interval" to pre_interval,
                            "length" to pre_length,
                            "plant-type" to pre_plant_type
                        )
                        db.collection("farmboxes").document(conid2)
                            .set(hashMaptoWrite)
                        val intent = Intent(this, WebViewActivity::class.java)
                        Firebase.crashlytics.log("About to pass intent for WebViewActivity!")
                        startActivity(intent)
                    }
            }
        }
    }
}

class ChoosePreferencesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChoosePreferencesBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChoosePreferencesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Firebase.crashlytics.log("ChoosePreferencesActivity Started!")
        if (RedirectFromDash == true) {
            val getTrace = Firebase.performance.newTrace("edit_settings_fetch")
            getTrace.start()
            val db = Firebase.firestore
            val docRef = db.collection("farmboxes").document(conid2)
            docRef.get()
                .addOnSuccessListener { document ->
                    val map = document.data
                    binding.editTextTextPersonName.setText(map!!.get("wt").toString())
                    binding.editTextTextPersonName2.setText(map.get("bt").toString())
                    binding.editTextTextPersonName3.setText(map.get("interval").toString())
                    binding.editTextTextPersonName5.setText(map.get("length").toString())
                    binding.editTextTextPersonName4.setText(
                        getSharedPreferences(
                            "FarmBOX",
                            Context.MODE_PRIVATE
                        ).getString("fb"+ RedirectForNo.toString()+"-nick", "Nickname")
                    )
                }
            getTrace.stop()
        }
        binding.button6.setOnClickListener {
            try {

                val wt = binding.editTextTextPersonName.text?.toString()?.toInt()
                val bt = binding.editTextTextPersonName2.text?.toString()?.toInt()
                val interval = binding.editTextTextPersonName3.text?.toString()?.toInt()
                val length = binding.editTextTextPersonName5.text?.toString()?.toInt()
                val nick = binding.editTextTextPersonName4.text?.toString()
                if (wt == null) {
                    throw NullPointerException("Null value in EditText wt")
                } else if (bt == null) {
                    throw NullPointerException("Null value in EditText bt")
                } else if (interval == null) {
                    throw NullPointerException("Null value in EditText interval")
                } else if (length == null) {
                    throw NullPointerException("Null value in EditText length")
                } else if (nick == null) {
                    throw NullPointerException("NUll value in EditText nick")
                } else {
                    val db = Firebase.firestore
                    val docRef = db.collection("farmboxes").document(conid2)
                    docRef.get()
                        .addOnSuccessListener { document ->

                            val map = document.data
                            val pre_wifi = map?.get("wifi")
                            val pre_wifi_ssid = map?.get("wifi-ssid")
                            val pre_wifi_pword = map?.get("wifi-pword")
                            val pre_plant_type = map?.get("plant-type")
                            val hashMaptoWrite = hashMapOf(
                                "interval" to interval,
                                "length" to length,
                                "bt" to bt,
                                "wt" to wt,
                                "init" to 2,
                                "wifi" to pre_wifi,
                                "wifi-ssid" to pre_wifi_ssid,
                                "wifi-pword" to pre_wifi_pword,
                                "plant-type" to pre_plant_type
                            )
                            db.collection("farmboxes").document(conid2)
                                .set(hashMaptoWrite)
                            Log.d("MICHAELDEBUG", "lol we are at line 296")
                            val sharedPreference = getSharedPreferences("FarmBOX", Context.MODE_PRIVATE)
                            var editor = sharedPreference.edit()
                            if (AddingSecondBox == true) {
                                editor.putString("fb2-conid", conid2)
                                editor.putString("fb2-nick", nick)
                                editor.putBoolean("ownFb2", true)
                            } else {
                                editor.putString("fb1-nick", nick)
                                editor.putString("fb1-conid", conid2)
                            }
                            editor.putBoolean("setup", true)
                            editor.apply()
                            commitSharedPreferencesChanges(editor) {}
                            if (RedirectFromDash == false) {
                                val intent = Intent(this, AiSetupActivity::class.java)
                                Firebase.crashlytics.log("About to pass intent for AiSetupActivity!")
                                startActivity(intent)
                            }
                            else {
                                val intent = Intent(this, DashboardActivity::class.java)
                                Firebase.crashlytics.log("About to pass intent for DashboardActivity!")
                                startActivity(intent)
                            }

                        }

                }
            } catch (e: NumberFormatException) {
                Snackbar.make(
                    binding.button6,
                    "Please enter valid numbers in non-nickname fields.",
                    Snackbar.LENGTH_LONG
                )
            } catch (e: NullPointerException) {
                Snackbar.make(
                    binding.button6,
                    "Please enter a value in each box.",
                    Snackbar.LENGTH_LONG
                )
            }
        }
    }
}

    class DashboardActivity : AppCompatActivity() {

        private lateinit var binding: ActivityDashboardBinding

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            val sharedPreference = getSharedPreferences("FarmBOX", Context.MODE_PRIVATE)

            binding = ActivityDashboardBinding.inflate(layoutInflater)
            setContentView(binding.root)
            val fb1_nick = sharedPreference.getString("fb1-nick", "FarmBOX 1")
            binding.textView11.text = fb1_nick
            val fb2_own = sharedPreference.getBoolean("ownFb2", false)
            if (fb2_own == false) {
                binding.button10.text = "Add"
                binding.cam2.visibility = INVISIBLE
                binding.conn2.visibility = INVISIBLE
                binding.ai2.visibility = INVISIBLE
            } else {
                binding.textView12.text = sharedPreference.getString("fb2-nick", "FarmBOX 2")
            }
            binding.button7.setOnClickListener {
                RedirectFromDash = true
                RedirectForNo = 1
                conid2 = sharedPreference.getString("fb1-conid", "ERROR").toString()
                if (conid2 == "ERROR") {
                    throw FarmboxConnectionIdException("Occured during Loading dashboard")
                }
                val intent = Intent(this, ChoosePreferencesActivity::class.java)
                Firebase.crashlytics.log("About to pass intent for ChoosePrefActivity!")
                startActivity(intent)
            }
            binding.button10.setOnClickListener {
                if (fb2_own == false) {
                    RedirectFromDash = false
                    AddingSecondBox = true
                    val intent = Intent(this, PairActivity::class.java)
                    Firebase.crashlytics.log("About to pass intent for ChoosePrefActivity!")
                    startActivity(intent)
                } else {
                    RedirectFromDash = true
                    RedirectForNo = 2
                    conid2 = sharedPreference.getString("fb2-conid", "ERROR").toString()
                    if (conid2 == "ERROR") {
                        throw FarmboxConnectionIdException("Occured during Loading dashboard")
                    }
                    val intent = Intent(this, ChoosePreferencesActivity::class.java)
                    Firebase.crashlytics.log("About to pass intent for ChoosePrefActivity!")
                    startActivity(intent)
                }
            }
            binding.conn1.setOnClickListener {
                RedirectFromDash = true
                conid2 = sharedPreference.getString("fb1-conid", "ERROR").toString()
                if (conid2 == "ERROR") {
                    throw FarmboxConnectionIdException("Occured during Loading dashboard")
                }
                val intent = Intent(this, EnterConnectionActivity::class.java)
                Firebase.crashlytics.log("About to pass intent for EnterConActivity!")
                startActivity(intent)
            }
            binding.conn2.setOnClickListener {
                RedirectFromDash = true
                conid2 = sharedPreference.getString("fb2-conid", "ERROR").toString()
                if (conid2 == "ERROR") {
                    throw FarmboxConnectionIdException("Occured during Loading dashboard")
                }
                val intent = Intent(this, EnterConnectionActivity::class.java)
                Firebase.crashlytics.log("About to pass intent for EnterConActivity!")
                startActivity(intent)
            }
            binding.ai1.setOnClickListener {
                RedirectFromDash = true
                conid2 = sharedPreference.getString("fb1-conid", "ERROR").toString()
                if (conid2 == "ERROR") {
                    throw FarmboxConnectionIdException("Occured during Loading dashboard")
                }
                val intent = Intent(this, AiSetupActivity::class.java)
                Firebase.crashlytics.log("About to pass intent for AiSetupActivity!")
                startActivity(intent)
            }
            binding.ai2.setOnClickListener {
                RedirectFromDash = true
                conid2 = sharedPreference.getString("fb2-conid", "ERROR").toString()
                if (conid2 == "ERROR") {
                    throw FarmboxConnectionIdException("Occured during Loading dashboard")
                }
                val intent = Intent(this, AiSetupActivity::class.java)
                Firebase.crashlytics.log("About to pass intent for AiSetupActivity!")
                startActivity(intent)
            }
            binding.cam1.setOnClickListener {
                WebViewForHelium = false
                conid2 = sharedPreference.getString("fb1-conid", "ERROR").toString()
                if (conid2 == "ERROR") {
                    throw FarmboxConnectionIdException("Occured during Loading dashboard")
                }
                val intent = Intent(this, WebViewActivity::class.java)
                Firebase.crashlytics.log("About to pass intent for WebViewActivity!")
                startActivity(intent)
            }
            binding.cam2.setOnClickListener {
                WebViewForHelium = false
                conid2 = sharedPreference.getString("fb2-conid", "ERROR").toString()
                if (conid2 == "ERROR") {
                    throw FarmboxConnectionIdException("Occured during Loading dashboard")
                }
                val intent = Intent(this, WebViewActivity::class.java)
                Firebase.crashlytics.log("About to pass intent for WebViewActivity!")
                startActivity(intent)
            }
        }
    }


class AiSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAiSetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAiSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.customB2.visibility = INVISIBLE
        binding.input.visibility = INVISIBLE
        binding.tomatoB.setOnClickListener {
            val db = Firebase.firestore
            val docRef = db.collection("farmboxes").document(conid2)
            docRef.get()
                .addOnSuccessListener { document ->
                    val data = document.data
                    val pre_init = data?.get("init")
                    val pre_length = data?.get("length")
                    val pre_bt = data?.get("bt")
                    val pre_wt = data?.get("wt")
                    val pre_wifi = data?.get("wifi")
                    val pre_wifi_ssid = data?.get("wifi-ssid")
                    val pre_wifi_pword = data?.get("wifi-pword")
                    val pre_interval = data?.get("interval")
                    val hashMaptoWrite = hashMapOf(
                        "wifi" to pre_wifi,
                        "wifi-ssid" to pre_wifi_ssid,
                        "wifi-pword" to pre_wifi_pword,
                        "init" to pre_init,
                        "bt" to pre_bt,
                        "wt" to pre_wt,
                        "interval" to pre_interval,
                        "length" to pre_length,
                        "plant-type" to "tomato-generic"
                    )
                    db.collection("farmboxes").document(conid2)
                        .set(hashMaptoWrite)
                    if (WebViewQueued == true) {
                        val intent = Intent(this, WebViewActivity::class.java)
                        Firebase.crashlytics.log("About to pass intent for WebViewActivity!")
                        startActivity(intent)
                    } else {
                        val intent = Intent(this, DashboardActivity::class.java)
                        Firebase.crashlytics.log("About to pass intent for DashboardActivity!")
                        startActivity(intent)
                    }
                }

        }
        binding.customB.setOnClickListener {
            binding.textView13.text = "Please enter the dir name of your custom model"
            binding.textView14.visibility = INVISIBLE
            binding.tomatoB.visibility = INVISIBLE
            binding.customB.visibility = INVISIBLE
            binding.customB2.visibility = VISIBLE
            binding.input.visibility = VISIBLE


            binding.customB2.setOnClickListener {
                try {
                    val dirname = binding.input.text?.toString()
                    if (dirname == null) {
                        throw NullPointerException()
                    }
                    val db = Firebase.firestore
                    val docRef = db.collection("farmboxes").document(conid2)
                    docRef.get()
                        .addOnSuccessListener { document ->
                            val data = document.data
                            val pre_init = data?.get("init")
                            val pre_length = data?.get("length")
                            val pre_bt = data?.get("bt")
                            val pre_wt = data?.get("wt")
                            val pre_wifi = data?.get("wifi")
                            val pre_wifi_ssid = data?.get("wifi-ssid")
                            val pre_wifi_pword = data?.get("wifi-pword")
                            val pre_interval = data?.get("interval")
                            val hashMaptoWrite = hashMapOf(
                                "wifi" to pre_wifi,
                                "wifi-ssid" to pre_wifi_ssid,
                                "wifi-pword" to pre_wifi_pword,
                                "init" to pre_init,
                                "bt" to pre_bt,
                                "wt" to pre_wt,
                                "interval" to pre_interval,
                                "length" to pre_length,
                                "plant-type" to dirname
                            )
                            db.collection("farmboxes").document(conid2)
                                .set(hashMaptoWrite)
                            if (WebViewQueued == true) {
                                val intent = Intent(this, WebViewActivity::class.java)
                                Firebase.crashlytics.log("About to pass intent for WebViewActivity!")
                                startActivity(intent)
                            } else {
                                val intent = Intent(this, DashboardActivity::class.java)
                                Firebase.crashlytics.log("About to pass intent for DashboardActivity!")
                                startActivity(intent)
                            }
                        }
                } catch (e: NullPointerException) {
                    Snackbar.make(binding.customB2, "Please enter a value in each box.", Snackbar.LENGTH_LONG)

                }

            }
        }
    }
}

class WebViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWebViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val webview = binding.WebView
        webview.webViewClient = WebViewClient()
        webview.settings.javaScriptEnabled = true
        if (WebViewForHelium == false) {
            webview.loadUrl(conid2 + ".local/stream.jpg")
        }
        else {
            webview.loadUrl("https://www.helium.com/console")
        }
        binding.button8.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            Firebase.crashlytics.log("About to pass intent for DashboardActivity!")
            startActivity(intent)
        }
    }
}

