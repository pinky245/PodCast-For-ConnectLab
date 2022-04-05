package com.crm.connectlabpodcast

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.crm.connectlabpodcast.adapter.RecordListAdapter
import com.crm.connectlabpodcast.data.AppDatabase
import com.crm.connectlabpodcast.data.ListViewModel
import com.crm.connectlabpodcast.data.RecordRepository
import com.crm.connectlabpodcast.databinding.ActivityMainBinding
import com.crm.connectlabpodcast.record.RecordActivity
import com.crm.connectlabpodcast.record.RecyclerViewOnClickListener
import com.crm.connectlabpodcast.utils.RecordState
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), RecyclerViewOnClickListener {

    private lateinit var binding: ActivityMainBinding
    private val permList: Array<String> = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private val viewModel: ListViewModel by viewModels()

    private lateinit var mainHandler: Handler

    companion object {
        private const val PERMISSION_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.imageRecord.setOnClickListener{
            if (!checkForPermission(Manifest.permission.RECORD_AUDIO)) {
                askPermission(permList)

            }else{
                val myIntent = Intent(this, RecordActivity::class.java)
                startActivity(myIntent)
            }
            if (!checkForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                askPermission(permList)
            }

        }

        val recordAdapter = RecordListAdapter(this)
        viewModel.initPlayer(RecordRepository(AppDatabase(this@MainActivity).recordDao()))
        viewModel.list.observe(this, {
            recordAdapter.submitList(it)
        })

        mainHandler = Handler(Looper.getMainLooper())



        binding.apply {
            val layoutM = LinearLayoutManager(this@MainActivity)
            recyclerView.apply {
                layoutManager = layoutM
                adapter = recordAdapter
            }
            layoutPlayer.playPause.setOnClickListener {
                if (isLoopOn()) {
                    // play again without click action
                    playAgain()
                } else {
                    if (isShuffleOn()) {
                        playNextRandom()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "No Track Selected/Shuffle OFF",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    binding.layoutPlayer.next.setOnClickListener {
                        // check for shuffle here
                        checkShuffleAndPlayNEXT()
                    }
                }
            }
        }

        // observe the seek through mediaPlayer current position
        viewModel.progress.observe(this, { progress ->
            binding.layoutPlayer.seek.progress = progress
        })

        // disable client touch on seek bar
        binding.layoutPlayer.seek.setOnTouchListener { v, event -> true }

        // observe record play state
        viewModel.recordState.observe(this, { state ->
            when (state) {
                is RecordState.Playing -> {
                    // pause here
                    changeButtonIcon(true)
                    setRecordTitle()
                    binding.layoutPlayer.playPause.setOnClickListener {
                        viewModel.pauseRecord()
                        enableSeek(false)
                    }
                    binding.layoutPlayer.previous.setOnClickListener {
                        viewModel.playAgain()
                        // pass true here as we go from Playing to playing again
                        enableSeek(true)
                    }
                }
                is RecordState.Pause -> {
                    // play
                    changeButtonIcon(false)
                    binding.layoutPlayer.playPause.setOnClickListener {
                        viewModel.resumeRecord()
                        enableSeek(true)
                    }
                }
                is RecordState.End -> {
                    // end
                    enableSeek(false)
                    changeButtonIcon(false)
                    binding.layoutPlayer.seek.progress = 0

                    if (isLoopOn()) {
                        // play again without click action
                        playAgain()
                    } else {
                        binding.layoutPlayer.playPause.setOnClickListener {
                            if (isLoopOn()) {
                                // play again without click action
                                playAgain()
                            } else {
                                checkShuffleAndPlayNEXT()
                            }
                        }
                        // next should play, a record (random/or in sequence),
                        // but if loop is ON the new played record will loop
                        binding.layoutPlayer.next.setOnClickListener {
                            // check for shuffle here
                            checkShuffleAndPlayNEXT()
                        }
                        binding.layoutPlayer.previous.setOnClickListener {
                            // check for shuffle here
                            checkShuffleAndPlayPrevious()
                        }
                    }
                }
            }
        })
        binding.layoutPlayer.shuffle.setOnClickListener {
            changeButtonIconShuffle()
        }
        binding.layoutPlayer.loop.setOnClickListener {
            changeButtonIconLoop()
        }

        // next should play, a record (random/or in sequence),
        // but if loop is ON the new played record will loop
        binding.layoutPlayer.next.setOnClickListener {
            // check for shuffle here
            checkShuffleAndPlayNEXT()
        }
        binding.layoutPlayer.previous.setOnClickListener {
            checkShuffleAndPlayPrevious()

        }
    }

    override fun onItemClick(position: Int) {


        viewModel.list.value?.let {
            val filePath = it[position].filePath
            lifecycleScope.launch {
                playWithFilePath(filePath, position)
            }
        }
    }

    private fun playWithFilePath(filePath: String, position: Int) {
        viewModel.playRecord(filePath, position)
        binding.layoutPlayer.seek.max = viewModel.recordDuration
        enableSeek(true)
    }

    private fun playNext() {
        viewModel.playNext()
        binding.layoutPlayer.seek.max = viewModel.recordDuration
        enableSeek(true)
    }

    private fun playNextRandom() {
        viewModel.playRandom()
        binding.layoutPlayer.seek.max = viewModel.recordDuration
        enableSeek(true)
    }

    private fun playAgain() {
        viewModel.playAgain()
        binding.layoutPlayer.seek.max = viewModel.recordDuration
        enableSeek(true)
    }

    private fun playPrevious() {
        viewModel.playPrevious()
        binding.layoutPlayer.seek.max = viewModel.recordDuration
        enableSeek(true)
    }

    private fun checkShuffleAndPlayPrevious() {
        if (isShuffleOn()) {
            playNextRandom()
        } else {
            playPrevious()
        }
    }

    private fun checkShuffleAndPlayNEXT() {
        if (isShuffleOn()) {
            playNextRandom()
        } else {
            playNext()
        }
    }

    private fun setRecordTitle() {
        binding.layoutPlayer.recordLabel.text = viewModel.getTitle()
    }

    // could use Coroutine dispatcher here
    private fun enableSeek(bool: Boolean) {
        val runner = object : Runnable {
            override fun run() {
                viewModel.getLiveProgress()
                mainHandler.postDelayed(this, 1000)
            }
        }
        if (bool) {
            mainHandler.post(runner)
        } else {
            mainHandler.removeCallbacksAndMessages(null)
        }
    }

    private fun changeButtonIcon(bool: Boolean) {
        binding.layoutPlayer.playPause.isActivated = bool
    }

    private fun changeButtonIconShuffle() {
        binding.layoutPlayer.shuffle.isActivated = !binding.layoutPlayer.shuffle.isActivated
    }

    private fun changeButtonIconLoop() {
        binding.layoutPlayer.loop.isActivated = !binding.layoutPlayer.loop.isActivated
    }

    private fun isShuffleOn(): Boolean = binding.layoutPlayer.shuffle.isActivated
    private fun isLoopOn(): Boolean = binding.layoutPlayer.loop.isActivated


    private fun checkForPermission(permission: String): Boolean {
        ActivityCompat.checkSelfPermission(this, permission)
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun onPermissionResult(granted: Boolean) {
        if (granted) {
            val myIntent = Intent(this, RecordActivity::class.java)
            startActivity(myIntent)

        } else {
            // again ask for permission
//            Toast.makeText(this, "App Won't Work without Permissions!", Toast.LENGTH_SHORT).show()
            askPermission(permList)
        }
    }

    private fun askPermission(permissions: Array<String>) {
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty()) {
            val bool = grantResults[0] == PackageManager.PERMISSION_GRANTED
            onPermissionResult(bool)
            val bool1 = grantResults[1] == PackageManager.PERMISSION_GRANTED
            onPermissionResult(bool1)
        }
    }

}