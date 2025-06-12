package com.example.m2tmdbapp2025

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.m2tmdbapp2025.databinding.ActivitySensorDemoBinding

class SensorDemoActivity : AppCompatActivity() , OnSensorItemClickListener, SensorEventListener {
    private val LOGTAG = SensorDemoActivity::class.simpleName
    private val SAMPLING_INTERVAL_US = 1000000 // one second
    lateinit var binding: ActivitySensorDemoBinding
    lateinit var sensorListAdapter: SensorListAdapter
    lateinit var sensorManager: SensorManager
    private val sensors: ArrayList<Sensor> = ArrayList()
    var currentSensor: Sensor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySensorDemoBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // init sensor list recycler view
        binding.sensorListRv.setHasFixedSize(true)
        binding.sensorListRv.layoutManager = LinearLayoutManager(this)
        sensorListAdapter = SensorListAdapter(sensors, this)
        binding.sensorListRv.adapter = sensorListAdapter

        // get device's available sensors list
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensors.addAll(sensorManager.getSensorList(Sensor.TYPE_ALL))
        sensorListAdapter.notifyDataSetChanged()
        onSensorItemClicked(0)
    }

    override fun onSensorItemClicked(position: Int) {
        Log.i(LOGTAG, "clicked at sensor position=$position")
        // un register current sensor listener
        unRegisterSensorListener()
        // get selected sensor
        currentSensor = sensors[position]
        // register selected sensor listener
        Log.i(LOGTAG,"now listening to '${currentSensor?.name}'")
        registerSensorListener()

    }

    override fun onSensorChanged(event: SensorEvent) {
        Log.i(LOGTAG, "onSensorChanged(${event.sensor.name})")
        val values = StringBuilder()
        val n = event.values.size
        for (i in 0 until n) {
            values.append("v[").append(i).append("]=").append(event.values[i])
                .append(if (n - i == 1) '.' else ", ")
        }
        binding.arrayValuesTv.text=values.toString()
        binding.sensorDemoView.setCaption(currentSensor!!.name)
        binding.sensorDemoView.setSensorEvent(event)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        Log.i(LOGTAG, "onAccuracyChanged($accuracy)")
    }

    private fun registerSensorListener() {
        currentSensor?.let {
            sensorManager.registerListener(
                this,
                it,
                SAMPLING_INTERVAL_US
            )
            //onSensorItemClicked(binding.sensorListRv.adapter.)
        }
    }

    private fun unRegisterSensorListener() {
        currentSensor?.let {
            sensorManager.unregisterListener(this)
        }
    }

    // activity life cycle management:
    // unregister sensors listener to save power
    override fun onPause() {
        super.onPause()
        unRegisterSensorListener()
    }

    override fun onResume() {
        super.onResume()
        registerSensorListener()
    }

    override fun onDestroy() {
        super.onDestroy()
        registerSensorListener()
    }
}