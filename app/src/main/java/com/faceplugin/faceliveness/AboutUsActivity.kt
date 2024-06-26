package com.faceplugin.faceliveness

import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.faceplugin.facerecognition.ContactUsActivity
import com.google.android.material.bottomsheet.BottomSheetDialog


class AboutUsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_new)

        findViewById<androidx.cardview.widget.CardView>(R.id.btn_contact).setOnClickListener {
            val bottomSheet = ContactUsActivity()
            bottomSheet.show(supportFragmentManager, "ContactUsActivity")
        }


    }
}