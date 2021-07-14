package com.example.covidtracker2.adapter

import android.graphics.RectF
import com.example.covidtracker2.Metric
import com.example.covidtracker2.TimeScale
import com.example.covidtracker2.data.CovidData
import com.robinhood.spark.SparkAdapter

class CovidSparkAdapter(private val dailyData: List<CovidData>)  : SparkAdapter() {

    var metric = Metric.POSITIVE
    var daysAgo = TimeScale.MAX

    override fun getCount(): Int = dailyData.size

    override fun getItem(index: Int): Any = dailyData.get(index)

    override fun getY(index: Int): Float {
        val chosenDayData = dailyData[index]

        return when(metric){
            Metric.POSITIVE -> chosenDayData.positiveIncrease.toFloat()
            Metric.NEGATIVE -> chosenDayData.negativeIncrease.toFloat()
            Metric.DEATH -> chosenDayData.deathIncrease.toFloat()
        }
    }

    override fun getDataBounds(): RectF {
        val bounds =  super.getDataBounds()

        if(daysAgo!=TimeScale.MAX)
        {
            bounds.left = count - daysAgo.numDays.toFloat()
        }
        return bounds
    }


}
