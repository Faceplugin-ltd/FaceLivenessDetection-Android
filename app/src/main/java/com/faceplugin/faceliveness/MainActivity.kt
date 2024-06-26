package com.faceplugin.faceliveness

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.ocp.facesdk.FaceBox
import com.ocp.facesdk.FaceDetectionParam
import com.ocp.facesdk.FaceSDK
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    companion object {
        private val SELECT_PHOTO_REQUEST_CODE = 1
        private val SELECT_ATTRIBUTE_REQUEST_CODE = 2
    }

    private lateinit var dbManager: DBManager
    private lateinit var textWarning: TextView
    private lateinit var personAdapter: PersonAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textWarning = findViewById<TextView>(R.id.textWarning)

        var ret = FaceSDK.setActivation(
            "fj/KH0MjIOG+oMW2YVROoAKW9DZkGeNyZ8KbutGslPtYir3yokTq1t/WMFshNzy/GMGUU+3RcmlR\n" +
            "HfmKaGTKB1Vo9dD7PzcZYsWF07VtLstDf3TCD0ZIF6/MAbeQ1mGn9YtDI9r2Z9ZRxJTsJ+THlGUY\n" +
            "BPbMu4d8vvsDKySO7TQAGS4J73PKZ6JodU9T2Kx8m2IKUWhH6mBLr/QFzEHIgbBV45WEmB/5lYn7\n" +
            "yytTrJreM0taCZrVOcUlCNt+YxBaBW8tULxHAsuosScDDkwKZ7XxNigQCSicMgHA59FS2SKPxMG2\n" +
            "iIVPNK20JOn1AjsXHOUmTUzZGpC0RAf++JDBcg=="
        )

        if (ret == FaceSDK.SDK_SUCCESS) {
            ret = FaceSDK.init(assets)
        }

        if (ret != FaceSDK.SDK_SUCCESS) {
            textWarning.setVisibility(View.VISIBLE)
            if (ret == FaceSDK.SDK_LICENSE_KEY_ERROR) {
                textWarning.setText("Invalid license!")
            } else if (ret == FaceSDK.SDK_LICENSE_APPID_ERROR) {
                textWarning.setText("Invalid error!")
            } else if (ret == FaceSDK.SDK_LICENSE_EXPIRED) {
                textWarning.setText("License expired!")
            } else if (ret == FaceSDK.SDK_NO_ACTIVATED) {
                textWarning.setText("No activated!")
            } else if (ret == FaceSDK.SDK_INIT_ERROR) {
                textWarning.setText("Init error!")
            }
        }

        dbManager = DBManager(this)
        dbManager.loadPerson()

        personAdapter = PersonAdapter(this, DBManager.personList)
        findViewById<Button>(R.id.buttonIdentify).setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java))
        }

        findViewById<Button>(R.id.buttonSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<Button>(R.id.buttonAbout).setOnClickListener {
            startActivity(Intent(this, AboutUsActivity::class.java))
        }
    }


    override fun onResume() {
        super.onResume()

        personAdapter.notifyDataSetChanged()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SELECT_PHOTO_REQUEST_CODE && resultCode == RESULT_OK) {
            try {
                var bitmap: Bitmap = Utils.getCorrectlyOrientedImage(this, data?.data!!)
                var faceBoxes: List<FaceBox>? = FaceSDK.faceDetection(bitmap, null)

                if(faceBoxes.isNullOrEmpty()) {
                    Toast.makeText(this, getString(R.string.no_face_detected), Toast.LENGTH_SHORT).show()
                } else if (faceBoxes.size > 1) {
                    Toast.makeText(this, getString(R.string.multiple_face_detected), Toast.LENGTH_SHORT).show()
                } else {
                    val faceImage = Utils.cropFace(bitmap, faceBoxes[0])
                    val templates = FaceSDK.templateExtraction(bitmap, faceBoxes[0])

                    dbManager.insertPerson("Person" + Random.nextInt(10000, 20000), faceImage, templates)
                    personAdapter.notifyDataSetChanged()
                    Toast.makeText(this, getString(R.string.person_enrolled), Toast.LENGTH_SHORT).show()
                }
            } catch (e: java.lang.Exception) {
                //handle exception
                e.printStackTrace()
            }
        } else if (requestCode == SELECT_ATTRIBUTE_REQUEST_CODE && resultCode == RESULT_OK) {
            try {
                var bitmap: Bitmap = Utils.getCorrectlyOrientedImage(this, data?.data!!)


                val param = FaceDetectionParam()
                param.check_liveness = true
                param.check_liveness_level = SettingsActivity.getLivenessLevel(this)
                param.check_eye_closeness = true
                param.check_face_occlusion = true
                param.check_mouth_opened = true
                param.estimate_age_gender = true
                var faceBoxes: List<FaceBox>? = FaceSDK.faceDetection(bitmap, param)

                if(faceBoxes.isNullOrEmpty()) {
                    Toast.makeText(this, getString(R.string.no_face_detected), Toast.LENGTH_SHORT).show()
                } else if (faceBoxes.size > 1) {
                    Toast.makeText(this, getString(R.string.multiple_face_detected), Toast.LENGTH_SHORT).show()
                } else {
                    val faceImage = Utils.cropFace(bitmap, faceBoxes[0])

                    val intent = Intent(this, AttributeActivity::class.java)
                    intent.putExtra("face_image", faceImage)
                    intent.putExtra("yaw", faceBoxes[0].yaw)
                    intent.putExtra("roll", faceBoxes[0].roll)
                    intent.putExtra("pitch", faceBoxes[0].pitch)
                    intent.putExtra("face_quality", faceBoxes[0].face_quality)
                    intent.putExtra("face_luminance", faceBoxes[0].face_luminance)
                    intent.putExtra("liveness", faceBoxes[0].liveness)
                    intent.putExtra("left_eye_closed", faceBoxes[0].left_eye_closed)
                    intent.putExtra("right_eye_closed", faceBoxes[0].right_eye_closed)
                    intent.putExtra("face_occlusion", faceBoxes[0].face_occlusion)
                    intent.putExtra("mouth_opened", faceBoxes[0].mouth_opened)
                    intent.putExtra("age", faceBoxes[0].age)
                    intent.putExtra("gender", faceBoxes[0].gender)

                    startActivity(intent)
                }
            } catch (e: java.lang.Exception) {
                //handle exception
                e.printStackTrace()
            }
        }
    }
}