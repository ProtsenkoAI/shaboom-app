package com.example.jnidemo

import android.content.res.Resources
import android.graphics.Color
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
                 targetPitches: Array<Float>,
                 private var chart: LineChart,
                 private var resources: Resources
) : TimerTask() {
    private var pitches = ArrayDeque<Float>()
    private var drawnTargetPitches = ArrayDeque<Float>()
    private val futureTargets = ArrayDeque<Float>()
    private val drawnFutureTargets = ArrayDeque<Float>()
    private val nPoints = 200
    private val userPitchNPoints = 60
    private var minY = 80.0f
    private var maxY = 220.0f

    init {
        val paddedTargetPitches = targetPitches

        for (elem in paddedTargetPitches) {
            futureTargets.addLast(elem)
        }

        for (idx in 1..(nPoints - userPitchNPoints)) {
            drawnFutureTargets.addLast(futureTargets.removeFirst())
        }

        for (idx in 1..userPitchNPoints) {
            drawnTargetPitches.add(-1.0f)
        }
    }

    override fun run() {
        val nbPitchesBefore = pitches.size
        inputManager.getPitches(pitches)
        val nbReceivedPitches = pitches.size - nbPitchesBefore
//        println("nb new pitches, $nbReceivedPitches")

        for (i in 1..nbReceivedPitches) {
            // synchronizing queues of target and user pitches
            if (futureTargets.size == 0) {
                break
            }
            drawnFutureTargets.addLast(futureTargets.removeFirst())
            drawnTargetPitches.addLast(drawnFutureTargets.removeFirst())
        }
//        assert(drawnTargetPitches.size == pitches.size)
        while (pitches.size > userPitchNPoints) {
            pitches.removeFirst()
        }

        while (drawnTargetPitches.size > userPitchNPoints) {
            drawnTargetPitches.removeFirst()
        }

        val dataSets = makeDataSetParts(pitches, R.color.design_default_color_primary)
        val targetDataSets = makeDataSetParts(drawnTargetPitches, R.color.design_default_color_secondary)
        val futureTargetDataSets = makeDataSetParts(drawnFutureTargets, R.color.design_default_color_secondary_variant, startX=userPitchNPoints)
        dataSets += targetDataSets
        dataSets += futureTargetDataSets

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
    }

    private fun makeDataSetParts(pitches: ArrayDeque<Float>, colorId: Int, startX: Int = 0): ArrayList<ILineDataSet> {
        val datasets = ArrayList<ILineDataSet>()

        // TODO: (maybe) reuse old entries
        var entries = mutableListOf<Entry>()
        for ((idx, value) in pitches.withIndex()) {
            if (value != -1.0f) { // -1 is filling value when there's no detected pitch
                entries.add(Entry(idx.toFloat() + startX, value))
            } else {
                if (entries.size > 0) {
                    datasets.add(createStyledDataSet(entries, colorId))
                    entries = mutableListOf<Entry>()
                }
            }
        }
        if (entries.size > 0) {
            datasets.add(createStyledDataSet(entries, colorId))
        }
        return datasets
    }

    private fun createStyledDataSet(entries: MutableList<Entry>, colorId: Int): LineDataSet {
        val dataSet = LineDataSet(entries, "Label")
        dataSet.color = resources.getColor(colorId)

        dataSet.fillAlpha = 0
        dataSet.lineWidth = 3.0f
        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(false)
        dataSet.setDrawHighlightIndicators(false)
        return dataSet
    }
}
