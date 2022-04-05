package com.crm.connectlabpodcast.record

import android.content.Context
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.crm.connectlabpodcast.R
import com.crm.connectlabpodcast.data.AppDatabase
import com.crm.connectlabpodcast.data.Record
import com.crm.connectlabpodcast.data.RecordRepository
import com.crm.connectlabpodcast.databinding.ActivityRecordBinding
import com.crm.connectlabpodcast.utils.RecordState

class RecordActivity : AppCompatActivity() {

    private val viewModel: RecordViewModel by viewModels()
    var buttonState = ""

    private lateinit var recordBinding: ActivityRecordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recordBinding = ActivityRecordBinding.inflate(layoutInflater)
        setContentView(recordBinding.root)

        subscribe()

        recordBinding.imageRecord.setOnClickListener{

            if (buttonState == "") {

                val bool = viewModel.validateName(recordBinding.audioNameEt.text.toString())
                try {
                    val imm: InputMethodManager =
                        this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(
                        recordBinding.imageRecord.windowToken,
                        InputMethodManager.RESULT_UNCHANGED_SHOWN
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                if (bool) {
                    showProgress(true)
                    val text = recordBinding.audioNameEt.text.toString()
                    // start recording here and make the file name
                    viewModel.startRecording(text,this@RecordActivity)
                    setText(text)
                } else {
                    // set error to the text input
                    recordBinding.audioNameEt.error = "Please enter file name"
                    recordBinding.audioNameEt.requestFocus()
                }
            }else{
                val text = recordBinding.audioNameEt.text.toString()
                viewModel.stopRecording(text,RecordRepository(AppDatabase(this).recordDao()))
            }
        }

    }

    private fun subscribe() {
        viewModel.recordState.observe(this, { recordState ->
            when (recordState) {
                is RecordState.Recording -> {
                    // recording make the visualizer here or a progress bar
                    // switch the buttons
                    recordBinding.audioNameEt.isEnabled = false
                    buttonState = "Recording"
                    switchButtons()
                }
                is RecordState.Done<Record> -> {
                    // do the done thing
                    buttonState = "Done"

                    // switch the buttons
                    switchButtons()

                    showProgress(false)
                    Toast.makeText(this@RecordActivity, "Saved!", Toast.LENGTH_SHORT).show()
                }
                is RecordState.Error -> {
                    // handle the error
                    showProgress(false)
                }
            }
        })
    }

    private fun switchButtons() {
        if (buttonState == "Recording") {
            recordBinding.imageRecord.setImageResource(R.drawable.ic_baseline_cancel_24)
            recordBinding.imageSave.visibility = View.GONE
            recordBinding.imageUndo.visibility = View.GONE
        } else if (buttonState == "Done") {
            recordBinding.imageRecord.setImageResource(R.drawable.ic_round_microphone)
            recordBinding.imageSave.visibility = View.VISIBLE
            recordBinding.imageUndo.visibility = View.VISIBLE
        }
    }

    private fun showProgress(bool: Boolean) {
        recordBinding.progress.visibility = if (bool) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }
    }

    private fun setText(text: String) {
        val finalText: String ="Your voice is recording :" + " " + text
        val ss = SpannableString(finalText)
        val cyan = ForegroundColorSpan(ContextCompat.getColor(this, R.color.purple_500))
        ss.setSpan(cyan, 25, finalText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        recordBinding.isRecordingTv.text = ss
    }}