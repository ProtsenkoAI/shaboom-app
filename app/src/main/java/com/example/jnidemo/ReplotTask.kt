package com.example.jnidemo

import android.content.res.Resources
import android.util.Log
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import java.util.*
import kotlin.collections.ArrayDeque
import kotlin.collections.ArrayList


class ReplotTask(private var inputManager: InputManager,
                 private var chart: LineChart,
                 private var resources: Resources
) : TimerTask() {
    // TODO: fill pitches with start value if needed for proper plotting
    private var pitches = ArrayDeque<Float>()
    private val nPoints = 200
    private val userPitchNPoints = 60
    private var minY = 80.0f
    private var maxY = 220.0f

    override fun run() {
        val tsStart = System.currentTimeMillis()
        inputManager.getPitches(pitches)

        while (pitches.size > userPitchNPoints) {
            pitches.removeFirst()
        }

        val dataSets = makeDataSetParts(pitches)

        val lineData = LineData(dataSets as List<ILineDataSet>?)
        chart.data = lineData

        // styling
        val xAxis = chart.xAxis
        val leftAxis = chart.axisLeft
        val rightAxis = chart.axisRight

        leftAxis.axisMinimum = minY
        leftAxis.axisMaximum = maxY

        chart.legend.isEnabled = false
        chart.description.isEnabled = false
        xAxis.isEnabled = false
        leftAxis.isEnabled = false
        rightAxis.isEnabled = false

        xAxis.axisMinimum = 0.0f
        xAxis.axisMaximum = nPoints.toFloat()

        chart.invalidate()
        val tsEnd = System.currentTimeMillis()
        Log.i("time needed in ms", (tsEnd - tsStart).toString())
    }

    private fun makeDataSetParts(pitches: ArrayDeque<Float>): ArrayList<ILineDataSet> {
        val datasets = ArrayList<ILineDataSet>()

        // TODO: (maybe) reuse old entries
        var entries = mutableListOf<Entry>()
        for ((idx, value) in pitches.withIndex()) {
            if (value != -1.0f) { // -1 is filling value when there's no detected pitch
                entries.add(Entry(idx.toFloat(), value))
            } else {
                if (entries.size > 0) {
                    datasets.add(createStyledDataSet(entries))
                    entries = mutableListOf<Entry>()
                }
            }
        }
        if (entries.size > 0) {
            datasets.add(createStyledDataSet(entries))
        }
        return datasets
    }

    private fun createStyledDataSet(entries: MutableList<Entry>): LineDataSet {
        val dataSet = LineDataSet(entries, "Label")
        dataSet.color = resources.getColor(R.color.design_default_color_primary)

        dataSet.fillAlpha = 0
        dataSet.lineWidth = 3.0f
        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(false)
        return dataSet
    }
}
