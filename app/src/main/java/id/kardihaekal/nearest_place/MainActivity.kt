package id.kardihaekal.nearest_place

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class MainActivity : AppCompatActivity(), LocationListener {
    var mGoogleMap: GoogleMap? = null
    var pBar: ProgressBar? = null
    var mLatitude = 0.0
    var mLongitude = 0.0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val spinnerCari = findViewById<Spinner>(R.id.spnCari)
        pBar = findViewById(R.id.pBar)
        val fragment =
            supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        fragment!!.getMapAsync { googleMap ->
            mGoogleMap = googleMap
            initMap()
        }
        val myAdapter = ArrayAdapter(
            this@MainActivity,
            android.R.layout.simple_list_item_1, resources.getStringArray(R.array.cari_tempat)
        )
        myAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCari.adapter = myAdapter
        spinnerCari.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View,
                position: Int,
                l: Long
            ) { //daftar pilihan spinner
                var xType = ""
                if (position == 1) xType = "mosque" else if (position == 2) xType =
                    "restaurant" else if (position == 3) xType =
                    "atm" else if (position == 4) xType = "bank" else if (position == 5) xType =
                    "school" else if (position == 6) xType =
                    "hospital" else if (position == 7) xType =
                    "laundry" else if (position == 8) xType =
                    "university" else if (position == 9) xType =
                    "post_office" else if (position == 10) xType = "police"
                if (position != 0) { //place API
                    val sb =
                        "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                                "location=" + mLatitude + "," + mLongitude +
                                "&radius=5000" +
                                "&types=" + xType +
                                "&sensor=true" +
                                "&key=" + resources.getString(R.string.google_maps_key)
                    startProg()
                    PlacesTask().execute(sb)
                }
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }
    }

    private fun initMap() { //cek permission
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                115
            )
            return
        }
        if (mGoogleMap != null) {
            startProg()
            mGoogleMap!!.isMyLocationEnabled = true
            val locationManager =
                getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val criteria = Criteria()
            val provider = locationManager.getBestProvider(criteria, true)
            val location =
                locationManager.getLastKnownLocation(provider)
            if (location != null) {
                onLocationChanged(location)
            } else stopProg()
            locationManager.requestLocationUpdates(provider, 20000, 0f, this)
        }
    }

    override fun onLocationChanged(location: Location) {
        mLatitude = location.latitude
        mLongitude = location.longitude
        val latLng = LatLng(mLatitude, mLongitude)
        /*Circle circle = mGoogleMap.addCircle(new CircleOptions()
                .center(new LatLng(mLatitude, mLongitude))
                .radius(500)
                .strokeWidth(6)
                .strokeColor(0xffff0000)
                .fillColor(0x55ff0000));*/mGoogleMap!!.moveCamera(
            CameraUpdateFactory.newLatLng(
                latLng
            )
        )
        mGoogleMap!!.animateCamera(CameraUpdateFactory.zoomTo(12f))
        stopProg()
    }

    private fun stopProg() {
        pBar!!.visibility = View.GONE
    }

    private fun startProg() {
        pBar!!.visibility = View.VISIBLE
    }

    @SuppressLint("StaticFieldLeak")
    private inner class PlacesTask :
        AsyncTask<String?, Int?, String?>() {
        protected override fun doInBackground(vararg url: String?): String? {
            var data: String? = null
            try {
                data = downloadUrl(url[0].toString())
            } catch (e: Exception) {
                stopProg()
                e.printStackTrace()
            }
            return data
        }

        override fun onPostExecute(result: String?) {
            ParserTask().execute(result)
        }
    }

    private fun downloadUrl(strUrl: String): String {
        var data = ""
        val iStream: InputStream
        val urlConnection: HttpURLConnection
        try {
            val url = URL(strUrl)
            urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.connect()
            iStream = urlConnection.inputStream
            val br =
                BufferedReader(InputStreamReader(iStream))
            val sb = StringBuilder()
            var line: String?
            while (br.readLine().also { line = it } != null) {
                sb.append(line)
            }
            data = sb.toString()
            br.close()
            iStream.close()
            urlConnection.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return data
    }

    @SuppressLint("StaticFieldLeak")
    private inner class ParserTask :
        AsyncTask<String?, Int?, List<HashMap<String, String>>?>() {
        var jObject: JSONObject? = null
        protected override fun doInBackground(vararg jsonData: String?): List<HashMap<String, String>>? {
            var places: List<HashMap<String, String>>? =
                null
            val parserPlace = ParserPlace()
            try {
                jObject = JSONObject(jsonData[0])
                places = parserPlace.parse(jObject!!)
            } catch (e: Exception) {
                stopProg()
                e.printStackTrace()
            }
            return places
        }

        //untuk menampilkan jumlah lokasi terdekat
        override fun onPostExecute(list: List<HashMap<String, String>>?) {
            mGoogleMap!!.clear()
            for (i in list!!.indices) {
                val markerOptions = MarkerOptions()
                val hmPlace = list[i]
                val pinDrop =
                    BitmapDescriptorFactory.fromResource(R.drawable.ic_pin)
                val lat = hmPlace["lat"]!!.toDouble()
                val lng = hmPlace["lng"]!!.toDouble()
                val nama = hmPlace["place_name"]
                val namaJln = hmPlace["vicinity"]
                val latLng = LatLng(lat, lng)
                markerOptions.icon(pinDrop)
                markerOptions.position(latLng)
                markerOptions.title("$nama : $namaJln")
                mGoogleMap!!.addMarker(markerOptions)
            }
            stopProg()
        }
    }

    override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {}
    override fun onProviderEnabled(s: String) {}
    override fun onProviderDisabled(s: String) {}
}