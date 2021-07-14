package com.example.covidtracker2

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.covidtracker2.adapter.CovidSparkAdapter
import com.example.covidtracker2.data.CovidData
import com.example.covidtracker2.databinding.ActivityMainBinding
import com.example.covidtracker2.services.CovidService
import com.google.gson.GsonBuilder
import com.robinhood.ticker.TickerUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private const val BASE_URL = "https://api.covidtracking.com/v1/"
private const val TAG = "MainActivity"
private const val ALL_STATES = "All (NationWide)"


class MainActivity : AppCompatActivity() {
    private lateinit var currentlyShownData: List<CovidData>
    private lateinit var adapter: CovidSparkAdapter
    private lateinit var binding : ActivityMainBinding

//    private lateinit var binding : ActivityMainBind
    private lateinit var perstateDailyData: Map<String, List<CovidData>>
    private lateinit var nationalDailyData: List<CovidData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this,R.layout.activity_main)

        val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        val covidService = retrofit.create(CovidService::class.java)

//        Fetch National Data

        covidService.getNationalData().enqueue(object : Callback<List<CovidData>>{
            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {

                setUpEventListeners()
                val nationalData = response.body()

                if(nationalData != null) {
                    nationalDailyData = nationalData.reversed()
                    updateDisplayWithData(nationalDailyData)
                }
            }
            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG , "onFailure $t" )
            }
        })

        covidService.getStatesData().enqueue(object : Callback<List<CovidData>> {
            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                val statesData = response.body()

                if(statesData!=null){
                    perstateDailyData = statesData.reversed().groupBy { it.state }
                    updateSpinnerWithStateData(perstateDailyData.keys)
                }

            }
            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG,"onFailure $t")
            }
        })
    }

    private fun updateSpinnerWithStateData(stateNames: Set<String>) {

        val stateAbbreviatioinList = stateNames.toMutableList()
        stateAbbreviatioinList.sort()
        stateAbbreviatioinList.add(0, ALL_STATES)

//        Add stateAbbreviationList as data source for the spinner

        binding.spinner.attachDataSource(stateAbbreviatioinList)

        binding.spinner.setOnSpinnerItemSelectedListener { parent, _, position, _ ->
            val selectedState = parent.getItemAtPosition(position) as String
            val selectedData = perstateDailyData[selectedState] ?: nationalDailyData

            updateDisplayWithData(selectedData)
        }
    }

    private fun setUpEventListeners() {

        binding.tickerView.setCharacterLists(TickerUtils.provideNumberList())

//        Add a listener for the user scrubbing on the chart
            binding.sparkView.isScrubEnabled = true
            binding.sparkView.setScrubListener { itemData ->
                if(itemData is CovidData) {
                    updateInfoForDate(itemData)
                }
            }

    //        TODO : Respond to radio button selected events

        binding.radioGroupTimeSelection.setOnCheckedChangeListener{_ , checkId ->

            adapter.daysAgo = when(checkId){
                binding.radioButtonMonth.id -> TimeScale.MONTH
                binding.radioButtonWeek.id -> TimeScale.WEEK
                else -> TimeScale.MAX
            }
            adapter.notifyDataSetChanged()
        }

        binding.radioGroupMetricSelection.setOnCheckedChangeListener { _ , checkedId ->
            when(checkedId) {
                binding.radioButtonNegative.id -> updateDisplayMetric(Metric.NEGATIVE)
                binding.radioButtonDeath.id -> updateDisplayMetric(Metric.DEATH)
                else -> updateDisplayMetric(Metric.POSITIVE)
            }
        }
    }

    private fun updateDisplayMetric(metric: Metric) {

        val colorRes = when(metric){
            Metric.NEGATIVE -> R.color.colorNegative
            Metric.POSITIVE -> R.color.colorPositive
            Metric.DEATH -> R.color.colorDeath
        }

        @ColorInt val colorInt: Int = ContextCompat.getColor(this,colorRes)

        binding.apply {
            sparkView.lineColor = colorInt
            tickerView.setTextColor(colorInt)
        }


//        update the metric on the adapter
        adapter.metric = metric
        adapter.notifyDataSetChanged()

//        Reset the number and date show in bottom text views
        updateInfoForDate(currentlyShownData.last())
    }

    private fun updateDisplayWithData(dailyData: List<CovidData>) {

        currentlyShownData = dailyData

//        create a new SparkAdapter with the data

        adapter = CovidSparkAdapter(dailyData)
        binding.sparkView.adapter = adapter

//        update radio button to select positive cases and max time by default

        binding.radioButtonMax.isChecked = true
        binding.radioButtonPositive.isChecked = true

//        display metric for most recent date

        updateDisplayMetric(Metric.POSITIVE)
    }

    private fun updateInfoForDate(covidData: CovidData) {

        val numCases = when(adapter.metric){
            Metric.POSITIVE -> covidData.positiveIncrease
            Metric.NEGATIVE -> covidData.negativeIncrease
            Metric.DEATH -> covidData.deathIncrease
        }

        binding.tickerView.text = NumberFormat.getInstance().format(numCases)
        val outDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        binding.dateLabelText.text = outDateFormat.format(covidData.dateChecked)


    }
}