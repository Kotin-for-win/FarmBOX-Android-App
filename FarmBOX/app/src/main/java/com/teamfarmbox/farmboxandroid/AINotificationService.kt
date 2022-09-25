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

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

var conids = mutableListOf<String>()

public fun add_conid(conidAdd: String?) {
    if (conidAdd != null) {
        conids.add(conidAdd)
    }
}

public fun remove_conid(conidRemove: String) {
    conids.remove(conidRemove)
}
class AINotificationService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Thread {
            val number = conids.size
            var time = 0
            while (number > time) {
                Log.w("BGSERVICE", "time is" + time.toString())
           startListening(conids[time]) { noti, msg ->
               Log.w("BGSERVICE", "we arrived at line 31 with vars " + noti.toString() + msg)
            if (noti) {
                Log.w("BGSERVICE3", "here")
                val intent = Intent(this, DashboardActivity::class.java).apply {
                }
                val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

                val builder = NotificationCompat.Builder(this, "fb-ai")
                    .setSmallIcon(R.drawable.ai_icon)
                    .setContentTitle("AI Results are in!")
                    .setContentText(msg)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                with(NotificationManagerCompat.from(this)) {
                    // notificationId is a unique int for each notification that you must define
                    notify((0..10000).random(), builder.build())
                }

            }
           }
                time += 1
            }

        }.start()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}

fun startListening(conid: String, callback: (shouldSendNotification: Boolean, msg: String?) -> Unit) {
    val db = Firebase.firestore
    val docRef = db.collection("farmboxes").document(conid)
    docRef.addSnapshotListener { snapshot, e ->
        if (snapshot != null && snapshot.exists()) {
            val map = snapshot.data
            val ai_new = map!!.get("new-ai")
            if (ai_new == null) {
                callback(false, null)
                Log.w("BGSERVICE2", "omg we are null")
            } else if (ai_new == false) {
                callback(false, null)
                Log.w("BGSERVICE2", "omg we false")
            } else {
                val ripeness = map.get("ai-ripe")
                val disease = map.get("ai-health")
                Log.w("BGSERVICE2", "stuff: " + ripeness + disease)
                if (ripeness == "no" && disease == "no") {
                    callback(true, "Your plant is not ripe or diseased!")
                } else if (ripeness == "yes") {
                    if (disease == "no") {
                        callback(true, "Your plant is ripe and healthy!")
                    }
                    else {
                        callback(true, "Your plant is ripe but has $disease!")
                    }
                } else {
                    callback(true, "Your plant has $disease, please investigate!")
                }
            }
        } else {
            callback(false, null)
        }
    }

}
