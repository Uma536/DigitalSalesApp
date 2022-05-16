package com.digitalsales.velocitypro

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.app.Activity
import android.net.NetworkInfo
import android.os.Bundle
import android.os.StrictMode
import android.speech.RecognizerIntent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import com.google.cloud.translate.Translation
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var noSearchResultsFoundText: TextView
    private val carsList: List<Cars> = carsList()
    private lateinit var editText: AppCompatEditText
    private lateinit var clearQueryImageView: ImageView
    private lateinit var voiceSearchImageView: ImageView
    private var originalText: String? = null
    private var translatedText: String? = null
    private var connected = false
    var translate: Translate? = null
        var language = "en"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.search_list)
        noSearchResultsFoundText = findViewById(R.id.no_search_results_found_text)
        editText = findViewById(R.id.search_edit_text)
        clearQueryImageView = findViewById(R.id.clear_search_query)
        voiceSearchImageView = findViewById(R.id.voice_search_query)

        findViewById<Button>(R.id.translateButton).setOnClickListener {
            if (checkInternetConnection()) {

                //If there is internet connection, get translate service and start translation:
                getTranslateService()
                translate()
            } else {

                //If not, display "no connection" warning:
                findViewById<TextView>(R.id.translatedTv).text = getString(R.string.no_connection)
            }
        }
        voiceSearchImageView.setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,"$language-IN"
                )
            }
            startActivityForResult(intent, SPEECH_REQUEST_CODE)
        }

        attachAdapter(carsList)

        editText.doOnTextChanged { text, _, _, _ ->
            val query = text.toString().toLowerCase(Locale.getDefault())
            filterWithQuery(query)
            toggleImageView(query)
        }

        clearQueryImageView.setOnClickListener {
            editText.setText("")
        }
        languagespinner()
    }

    private fun attachAdapter(list: List<Cars>) {
        val searchAdapter = SearchAdapter(list)
        recyclerView.adapter = searchAdapter
    }


    private fun filterWithQuery(query: String) {
        if (query.isNotEmpty()) {
            val filteredList: List<Cars> = onQueryChanged(query)
            attachAdapter(filteredList)
            toggleRecyclerView(filteredList)
        } else if (query.isEmpty()) {
            attachAdapter(carsList)
        }
    }

    private fun onQueryChanged(filterQuery: String): List<Cars> {
        val filteredList = ArrayList<Cars>()
        for (currentSport in carsList) {
            if (currentSport.title.toLowerCase(Locale.getDefault()).contains(filterQuery)) {
                filteredList.add(currentSport)
            }
        }
        return filteredList
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val spokenText: String? =
                data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).let { results ->
                    results?.get(0)
                }
            // Do something with spokenText
            editText.setText(spokenText)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun toggleRecyclerView(carsList: List<Cars>) {
        if (carsList.isEmpty()) {
            recyclerView.visibility = View.INVISIBLE
            noSearchResultsFoundText.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            noSearchResultsFoundText.visibility = View.INVISIBLE
        }
    }

    private fun toggleImageView(query: String) {
        if (query.isNotEmpty()) {
            clearQueryImageView.visibility = View.VISIBLE
            voiceSearchImageView.visibility = View.INVISIBLE
        } else if (query.isEmpty()) {
            clearQueryImageView.visibility = View.INVISIBLE
            voiceSearchImageView.visibility = View.VISIBLE
        }
    }

    companion object {
        const val SPEECH_REQUEST_CODE = 0
        const val REQUEST_CODE = 1091
    }
        fun languagespinner(){
        val spinner: Spinner = findViewById(R.id.languagre_spinner)
// Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter.createFromResource(
            this,
            R.array.languages_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinner.adapter = adapter
        }
        spinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>?,
                selectedItemView: View?,
                position: Int,
                id: Long
            ) {
                // your code here
                language =  resources.getStringArray(R.array.languages_array)[position]
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {
                // your code here
            }
        })
    }

    fun getTranslateService() {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        try {
            resources.openRawResource(R.raw.credentials).use { `is` ->

                //Get credentials:
                val myCredentials: GoogleCredentials = GoogleCredentials.fromStream(`is`)

                //Set credentials and get translate service:
                val translateOptions: TranslateOptions =
                    TranslateOptions.newBuilder().setCredentials(myCredentials).build()
                translate = translateOptions.getService()
            }
        } catch (ioe: IOException) {
            ioe.printStackTrace()
        }
    }

    fun translate() {

        //Get input text to be translated:
        originalText = editText!!.text.toString()
        val translation: Translation = translate!!.translate(
            originalText,
            Translate.TranslateOption.targetLanguage("$language-IN"),
            Translate.TranslateOption.model ("base")
        )
        translatedText = translation.getTranslatedText()

        //Translated text and original text are set to TextViews:
        findViewById<TextView>(R.id.translatedTv)!!.text = translatedText
    }


    fun checkInternetConnection(): Boolean {

        //Check internet connection:
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        //Means that we are connected to a network (mobile or wi-fi)
        connected =
            connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)!!.state == NetworkInfo.State.CONNECTED ||
                    connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)!!.state == NetworkInfo.State.CONNECTED
        return connected
    }

}