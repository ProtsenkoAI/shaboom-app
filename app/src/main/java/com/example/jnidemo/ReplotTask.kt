package com.example.jnidemo

import android.content.res.Resources
import android.widget.ProgressBar
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
                 private val userPerformanceBar: ProgressBar,
                 private var resources: Resources,
                 private var toneShift: Int // represents how user lowered/ raised original tone

) : TimerTask() {
    private var pitches = ArrayDeque<Float>()
    private var drawnTargetPitches = ArrayDeque<Float>()
    private val drawnFutureTargets = ArrayDeque<Float>()
    private val futureTargets = ArrayDeque<Float>()
    private val nPoints = 200
    private val userPitchNPoints = 60
    private var minY = 80.0f
    private var maxY = 220.0f
    private val rightNoteThresh = 5

    init {
        val paddedTargetPitches = targetPitches

        for (idx in 1..userPitchNPoints) {
            pitches.addLast(-1.0f)
        }

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

        // tone shift is always zero for user pitches and handled by toneShift for target pitch
        val dataSets = makeDataSetParts(pitches, R.color.design_default_color_primary, adjustShift=false)
        val targetDataSets = makeDataSetParts(drawnTargetPitches,
            R.color.design_default_color_secondary)
        val futureTargetDataSets = makeDataSetParts(drawnFutureTargets,
            R.color.design_default_color_secondary_variant, startX = userPitchNPoints)
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

        var metricValue = (100 * calcPitchMatch()).toInt()
        metricValue = clip(metricValue, 10, 100)
        println("metric value $metricValue")
//        val progressColor = ColorUtils.blendARGB(
//            resources.getColor(R.color.red),
//            resources.getColor(R.color.green),
//            metricValue.toFloat() / 100
//        )
//        userPerformanceBar.progressDrawable.setColorFilter(progressColor, PorterDuff.Mode.SRC_IN)
//        userPerformanceBar.progressDrawable.level = metricValue
        userPerformanceBar.progress = metricValue
        chart.invalidate()
    }

    private fun clip(value: Int, minValue: Int, maxValue: Int): Int {
        return if (value < minValue) {
            minValue
        } else if (value > maxValue) {
            maxValue
        } else {
            value
        }
    }

    fun setToneShift(shift: Int) {
        toneShift = shift
    }

    private fun calcPitchMatch(): Float {
        /** in this formula we calculate MAE only on parts where both user and target have pitches,
        // and normalize it with intersection of these periods
        Returns value between 0 and 1 - 0 is best match, 1 is worst
         */
        var cntGoodNotes = 0
        var bothVoicedCount = 0
        var oneButNotAnotherVoicedCount = 0
        var anyVoicedCount = 0
        for (i in 0 until userPitchNPoints) {
            val targPitch = adjustTarget(drawnTargetPitches[i])
            val targVoiced = targPitch != -1.0f
            val userVoiced = pitches[i] != -1.0f
            if (targVoiced || userVoiced) {
                anyVoicedCount += 1
            }
            if (targVoiced && userVoiced) {
                bothVoicedCount += 1
                if (targPitch - pitches[i] <= rightNoteThresh) {
                    cntGoodNotes += 1
                }
                // only one is true
            } else if (targVoiced && !userVoiced || !targVoiced && userVoiced) {
                oneButNotAnotherVoicedCount += 1
            }
        }
        // factor decreasing if user and target pitches greatly overlap
        if (bothVoicedCount == 0 || anyVoicedCount == 0) {
            return 1.0f
        }
        val overlapNormalization = if (anyVoicedCount > 0) {
            ((oneButNotAnotherVoicedCount.toFloat() / anyVoicedCount) - 0.5f) / 3
        } else {
            0.0f
        }

        println("values: ${cntGoodNotes.toFloat() / (bothVoicedCount)} $overlapNormalization")
        return cntGoodNotes.toFloat() / bothVoicedCount - overlapNormalization;
    }

    private fun adjustTarget(pitch: Float): Float {
        return if (pitch == -1.0f) {
            pitch
        } else {
            pitch + toneShift
        }
    }

    private fun makeDataSetParts(pitches: ArrayDeque<Float>, colorId: Int, startX: Int = 0,
    adjustShift: Boolean = true): ArrayList<ILineDataSet> {
        val datasets = ArrayList<ILineDataSet>()
        // TODO: (maybe) reuse old entries for efficiency
        var entries = mutableListOf<Entry>()
        for ((idx, value) in pitches.withIndex()) {
            if (value != -1.0f) { // -1 is filling value when there's no detected pitch
                var adjValue = value
                if (adjustShift) {
                    adjValue = adjustTarget(value)
                }
                entries.add(Entry(idx.toFloat() + startX, adjValue))
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
