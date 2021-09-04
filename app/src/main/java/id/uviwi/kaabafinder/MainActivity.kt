package id.uviwi.kaabafinder

import android.Manifest
import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.View.INVISIBLE
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import id.uviwi.kaabafinder.databinding.ActivityMainBinding
import id.uviwi.kaabafinder.utils.Compass
import id.uviwi.kaabafinder.utils.Compass.CompassListener
import id.uviwi.kaabafinder.utils.Consts
import id.uviwi.kaabafinder.utils.GPSTracker
import id.uviwi.kaabafinder.utils.Helper
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin


class MainActivity : AppCompatActivity() {
    private lateinit var _binding: ActivityMainBinding
    private val compass by lazy { Compass(this) }
    private val gps by lazy { GPSTracker(this) }

    // helper
    private lateinit var helper: Helper
    private var currentAzimuth = 0f
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(_binding.root)
        prefs = getSharedPreferences("", MODE_PRIVATE)
        helper = Helper(prefs)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        _binding.ivArrow.visibility = INVISIBLE
        _binding.ivArrow.visibility = View.GONE
        _binding.cardGps.setOnClickListener {
            fetchGps()
        }
        setupCompass()
    }

    override fun onStart() {
        super.onStart()
        compass.start()
    }

    override fun onPause() {
        super.onPause()
        compass.stop()
    }

    override fun onResume() {
        super.onResume()
        compass.start()
    }

    override fun onStop() {
        super.onStop()
        compass.stop()
    }

    private fun setupCompass() {
        val permissionGranted: Boolean = helper.getBoolean(Consts.PERMISSION_GRANTED)
        if (permissionGranted) {
            getBearing()
        } else {
            _binding.tvDegree.text = resources.getString(R.string.msg_permission_not_granted_yet)
            _binding.tvLocation.text = resources.getString(R.string.msg_permission_not_granted_yet)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    1
                )
            }
        }
        val cl = CompassListener { azimuth -> // adjustArrow(azimuth);
            adjustImageDial(azimuth)
            adjustArrowKaaba(azimuth)
        }
        compass.setListener(cl)
    }

    private fun adjustImageDial(azimuth: Float) {
        val an: Animation = RotateAnimation(
            -currentAzimuth, -azimuth,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
            0.5f
        )
        currentAzimuth = azimuth
        an.duration = 500
        an.repeatCount = 0
        an.fillAfter = true
        _binding.ivDial.startAnimation(an)
    }

    private fun adjustArrowKaaba(azimuth: Float) {
        val kaabaDegree = helper.getFloat(Consts.KAABA_DEGREE)
        val an: Animation = RotateAnimation(
            -currentAzimuth + kaabaDegree, -azimuth,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
            0.5f
        )
        currentAzimuth = azimuth
        an.duration = 500
        an.repeatCount = 0
        an.fillAfter = true
        _binding.ivArrow.startAnimation(an)
        if (kaabaDegree > 0) {
            _binding.ivArrow.visibility = View.VISIBLE
        } else {
            _binding.ivArrow.visibility = INVISIBLE
            _binding.ivArrow.visibility = View.GONE
        }
    }

    @SuppressLint("MissingPermission")
    fun getBearing() {
        // Get the location manager
        val kaabaDegree = helper.getFloat(Consts.KAABA_DEGREE)
        if (kaabaDegree > 0.0001) {
            val yourLocation =
                getString(R.string.your_location) + getString(R.string.using_last_location)
            _binding.tvLocation.text = yourLocation
            val degreeFromNorth =
                getString(R.string.qibla_direction) + String.format("%.2f", kaabaDegree) + getString(R.string.degree_from_north)
            _binding.tvDegree.text = degreeFromNorth
            _binding.ivGps.setImageResource(R.drawable.gps_on)
            _binding.ivArrow.visibility = View.VISIBLE
        } else {
            fetchGps()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {

                if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    // permission was granted, yay! Do the
                    helper.saveBoolean(Consts.PERMISSION_GRANTED, true)
                    _binding.tvDegree.text = resources.getString(R.string.msg_permission_granted)
                    _binding.tvLocation.text = resources.getString(R.string.msg_permission_granted)
                    _binding.ivArrow.visibility = INVISIBLE
                    _binding.ivArrow.visibility = View.GONE
                } else {
                    Toast.makeText(
                        applicationContext,
                        resources.getString(R.string.toast_permission_required),
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
                return
            }
        }
    }

    private fun fetchGps() {
        if (gps.canGetLocation()) {
            val deviceLatitude = gps.latitude
            val deviceLongitude = gps.longitude
            val textLocation =
                getString(R.string.your_location) + "Lat : $deviceLatitude Long : $deviceLongitude"
            _binding.tvLocation.text = textLocation
            if (deviceLatitude < 0.001 && deviceLongitude < 0.001) {
                _binding.ivArrow.visibility = INVISIBLE
                _binding.ivArrow.visibility = View.GONE
                _binding.tvDegree.text = resources.getString(R.string.location_not_ready)
                _binding.tvLocation.text = resources.getString(R.string.location_not_ready)
                _binding.ivGps.setImageResource(R.drawable.gps_off)
            } else {
                _binding.ivGps.setImageResource(R.drawable.gps_on)
                val kaabaLongitude = Consts.KAABA_LONGITUDE
                val kaabaLatitude = Math.toRadians(Consts.KAABA_LATITUDE)
                val deviceLatitudeRadian = Math.toRadians(deviceLatitude)
                val longDiff = Math.toRadians(kaabaLongitude - deviceLongitude)
                val y = sin(longDiff) * cos(kaabaLatitude)
                val x = cos(deviceLatitudeRadian) * sin(kaabaLatitude) - sin(deviceLatitudeRadian) * cos(
                    kaabaLatitude
                ) * cos(longDiff)
                val result = (Math.toDegrees(atan2(y, x)) + 360) % 360
                val resultToFloat = result.toFloat()
                helper.saveFloat(Consts.KAABA_DEGREE, resultToFloat)
                val degreeText =
                    resources.getString(R.string.qibla_direction) + " ${String.format("%.2f", resultToFloat)} " + resources.getString(
                        R.string.degree_from_north
                    )
                _binding.tvDegree.text = degreeText
                Toast.makeText(applicationContext, degreeText, Toast.LENGTH_LONG).show()
                _binding.ivArrow.visibility = View.VISIBLE
            }
        } else {
            // can't get location
            // GPS or Network is not enabled
            // Ask user to enable GPS/network in settings
            gps.showSettingsAlert()

            _binding.ivArrow.visibility = INVISIBLE
            _binding.ivArrow.visibility = View.GONE
            _binding.tvDegree.text = resources.getString(R.string.pls_enable_location)
            _binding.tvLocation.text = resources.getString(R.string.pls_enable_location)
            _binding.ivGps.setImageResource(R.drawable.gps_off)
        }
    }
}