package com.kaisavx.AircraftController.activity

import android.app.ProgressDialog
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.os.Environment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.OrientationHelper
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.kaisavx.AircraftController.R
import com.kaisavx.AircraftController.mamager.DJIManager
import com.kaisavx.AircraftController.util.logMethod
import dji.common.camera.SettingsDefinitions
import dji.common.error.DJICameraError
import dji.common.error.DJIError
import dji.common.util.CommonCallbacks
import dji.sdk.camera.VideoFeeder
import dji.sdk.codec.DJICodecManager
import dji.sdk.media.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_media.*
import java.io.File
import java.util.*

class MediaActivity : BaseActivity() {

    var loadingDialog: ProgressDialog? = null
    var downloadDialog: ProgressDialog? = null

    private val fileAdapter = FileListAdapter()
    val mediaFileList = ArrayList<MediaFile>()

    private var lastClickViewIndex = -1
    private var currentProgress = -1
    private var lastClickView: View? = null

    private var destDir = File(Environment.getExternalStorageDirectory().path + "/Aircraft/")

    private val djiManager by lazy {
        DJIManager(this)
    }

    private var codecManager: DJICodecManager? = null

    private var currentFileListState: MediaManager.FileListState = MediaManager.FileListState.UNKNOWN

    private val btnClickListener = object : View.OnClickListener {
        override fun onClick(v: View?) {
            when (v) {
                btnDelete -> {
                    imageView.setImageBitmap(null)
                    deleteFileByIndex(lastClickViewIndex)
                }

                btnDownload -> {
                    downloadFileByIndex(lastClickViewIndex)
                }

                btnStatus -> {
                    if (pointing_drawer_sd.isOpened()) {
                        pointing_drawer_sd.animateClose()
                    } else {
                        pointing_drawer_sd.animateOpen()
                    }
                }
                btnPlay -> {

                    playStateSubject.value?.playbackStatus?.let {
                        if (it == MediaFile.VideoPlaybackStatus.PAUSED) {
                            val mediaManager = djiManager.getMediaManagerInstance()
                            if (mediaManager != null) {
                                mediaManager.resume { error ->
                                    if (null != error) {
                                        setResultToToast("Resume Video Failed" + error.description)
                                    } else {

                                    }
                                }
                            } else {
                                errorSubject.onNext(resources.getString(R.string.error_aircraft_disconnect))
                            }
                        } else {
                            playVideo()
                        }
                    }

                }
                btnPause -> {
                    val mediaManager = djiManager.getMediaManagerInstance()

                    if (mediaManager != null) {
                        mediaManager.pause { error ->
                            if (null != error) {
                                setResultToToast("Pause Video Failed" + error.description)
                            }
                        }
                    } else {
                        errorSubject.onNext(resources.getString(R.string.error_aircraft_disconnect))
                    }

                }
            }
        }
    }

    private val updateFileListStateListener = object : MediaManager.FileListStateListener {
        override fun onFileListStateChange(state: MediaManager.FileListState) {
            currentFileListState = state
        }
    }

    private val updatedVideoPlaybackStateListener = object : MediaManager.VideoPlaybackStateListener {
        override fun onUpdate(state: MediaManager.VideoPlaybackState) {
            updateStatusTextView(state)
            playStateSubject.onNext(state)
            playbackStatusSubject.onNext(state.playbackStatus)
        }
    }

    private val taskCallback = FetchMediaTask.Callback { file, option, error ->
        if (null == error) {
            if (option == FetchMediaTaskContent.PREVIEW) {
                runOnUiThread { fileAdapter.notifyDataSetChanged() }
            }
            if (option == FetchMediaTaskContent.THUMBNAIL) {
                runOnUiThread { fileAdapter.notifyDataSetChanged() }
            }
        } else {
            setResultToToast("Fetch Media Task Failed ${error.description}")
            //DJILog.e(TAG, "Fetch Media Task Failed" + error.description)
        }
    }

    private val videoDataCallback = object : VideoFeeder.VideoDataListener {
        override fun onReceive(videoBuffer: ByteArray?, size: Int) {
            codecManager?.sendDataToDecoder(videoBuffer, size)
        }
    }

    private val errorSubject: PublishSubject<String> = PublishSubject.create()
    private val playStateSubject: BehaviorSubject<MediaManager.VideoPlaybackState> = BehaviorSubject.create()
    private val playbackStatusSubject: BehaviorSubject<MediaFile.VideoPlaybackStatus> = BehaviorSubject.create()

    private val disposable = CompositeDisposable()

    private var backFlag = false

    //region Life-cycle
    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        super.onCreate(savedInstanceState)
        logMethod(this)
        supportActionBar?.hide()

        setContentView(R.layout.activity_media)
        initView()
        initDisposable()
    }

    override fun onStart() {
        super.onStart()
        logMethod(this)
    }

    override fun onResume() {
        super.onResume()
        logMethod(this)
        initMediaManager()
        backFlag=false
    }

    override fun onPause() {
        super.onPause()
        logMethod(this)
        fpvWidget.visibility = View.GONE
    }

    override fun onStop() {
        super.onStop()
        logMethod(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        logMethod(this)


        codecManager?.cleanSurface()
        codecManager?.destroyCodec()
        codecManager = null

        djiManager.getMediaManagerInstance()?.let {
            it.stop(null)
            it.removeFileListStateCallback(updateFileListStateListener)
            it.removeMediaUpdatedVideoPlaybackStateListener(updatedVideoPlaybackStateListener)
            it.exitMediaDownloading()

            it.scheduler?.removeAllTasks()
        }
    }

    override fun onBackPressed() {
        logMethod(this)

        val playbackStatus = playbackStatusSubject.value

        if (playbackStatus != null) {
            when (playbackStatus) {
                MediaFile.VideoPlaybackStatus.PLAYING,
                MediaFile.VideoPlaybackStatus.PAUSED -> {
                    val mediaManager = djiManager.getMediaManagerInstance()
                    if (mediaManager != null) {
                        mediaManager.stop {

                            it?.let { error ->
                                setResultToToast("Stop Video Failed" + error.description)
                            }
                            finish()
                        }
                    } else {
                        finish()
                    }
                }
                else -> {
                    finish()
                }
            }
        } else {
            finish()
        }

    }

    //init
    private fun initView() {
        filelistView.layoutManager = LinearLayoutManager(this, OrientationHelper.VERTICAL, false)

        filelistView.setAdapter(fileAdapter)

        //Init Loading Dialog
        loadingDialog = ProgressDialog(this)
        loadingDialog?.setMessage("Please wait")
        loadingDialog?.setCanceledOnTouchOutside(false)
        loadingDialog?.setCancelable(false)

        //Init Download Dialog
        downloadDialog = ProgressDialog(this)
        downloadDialog?.setTitle("Downloading file")
        downloadDialog?.setIcon(android.R.drawable.ic_dialog_info)
        downloadDialog?.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        downloadDialog?.setCanceledOnTouchOutside(false)
        downloadDialog?.setCancelable(true)
        downloadDialog?.setOnCancelListener {
            djiManager.getMediaManagerInstance()?.exitMediaDownloading()
        }

        btnDelete.setOnClickListener(btnClickListener)
        btnDownload.setOnClickListener(btnClickListener)
        btnStatus.setOnClickListener(btnClickListener)

        btnPlay.setOnClickListener(btnClickListener)

        btnPause.setOnClickListener(btnClickListener)

        VideoFeeder.getInstance().primaryVideoFeed.addVideoDataListener(videoDataCallback)

        fpvWidget.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
                logMethod(this)
                if (codecManager == null) {
                    codecManager = DJICodecManager(applicationContext, surface, width, height)
                }
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {

            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                logMethod(this)

                codecManager?.cleanSurface()
                codecManager?.destroyCodec()
                codecManager = null

                return false
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                playStateSubject.value?.playbackStatus?.let {
                    if (it != MediaFile.VideoPlaybackStatus.PAUSED &&
                            it != MediaFile.VideoPlaybackStatus.PLAYING) {
                        return
                    }
                }

                val mediaManager = djiManager.getMediaManagerInstance()
                if (mediaManager != null) {
                    mediaManager.pause { error ->
                        if (null != error) {
                            setResultToToast("Pause Video Failed" + error.description)
                        }
                    }
                } else {
                    errorSubject.onNext(resources.getString(R.string.error_aircraft_disconnect))
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                playStateSubject.value?.playbackStatus?.let {
                    if (it != MediaFile.VideoPlaybackStatus.PAUSED &&
                            it != MediaFile.VideoPlaybackStatus.PLAYING) {
                        return
                    }
                }
                val mediaManager = djiManager.getMediaManagerInstance()
                if (mediaManager != null) {
                    mediaManager.moveToPosition(seekBar.progress.toFloat(), { error ->
                        if (null != error) {
                            setResultToToast("Move to video position failed" + error.description)
                        } else {
                            mediaManager.resume { e ->
                                if (null != e) {
                                    setResultToToast("Resume Video Failed" + e.description)
                                }
                            }
                        }
                    })
                } else {
                    errorSubject.onNext(resources.getString(R.string.error_aircraft_disconnect))
                }
            }
        })
    }

    private fun initDisposable() {
        disposable.add(playStateSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    it?.let { state ->

                        it.playingMediaFile?.let {
                            val max = it.durationInSeconds.toInt()
                            seekBar.max = max
                            val maxH = max / 3600
                            val maxM = max / 60 % 60
                            val maxS = max % 60
                            if (maxH == 0) {
                                textMax.text = String.format("%02d:%02d", maxM, maxS)
                            } else {
                                textMax.text = String.format("%d:%02d:%02d", maxH, maxM, maxS)
                            }
                        }

                        val pos = state.playingPosition.toInt()
                        seekBar.progress = pos
                        val pH = pos / 3600
                        val pM = pos / 60 % 60
                        val pS = pos % 60
                        if (pH == 0) {
                            textTime.text = String.format("%02d:%02d", pM, pS)
                        } else {
                            textTime.text = String.format("%d:%02d:%02d", pH, pM, pS)
                        }

                    }
                })

        disposable.add(playbackStatusSubject
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    when (it) {
                        MediaFile.VideoPlaybackStatus.PLAYING -> {
                            btnPlay.visibility = View.GONE
                            btnPause.visibility = View.VISIBLE
                        }
                        else -> {
                            btnPlay.visibility = View.VISIBLE
                            btnPause.visibility = View.GONE
                        }
                    }
                })


        disposable.add(errorSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                })
    }

    private fun initMediaManager() {
        if (djiManager.getProductInstance() == null) {
            mediaFileList.clear()
            fileAdapter.notifyDataSetChanged()

        } else {
            djiManager.getCameraInstance()?.let { camera ->
                if (camera.isMediaDownloadModeSupported) {
                    val mediaManager = camera.mediaManager
                    mediaManager?.let {
                        it.addUpdateFileListStateListener(updateFileListStateListener)

                        it.addMediaUpdatedVideoPlaybackStateListener(updatedVideoPlaybackStateListener)

                        camera.setMode(SettingsDefinitions.CameraMode.MEDIA_DOWNLOAD, {
                            if (it == null) {
                                getFileList()
                            } else {
                                setResultToToast("Set cameraMode failed:${it.description}")
                            }
                        })

                        it.scheduler.resume {
                            if (it != null) {
                                setResultToToast("scheduler resume:${it.description}")
                            }
                        }

                    }
                } else {
                    setResultToToast("Media Download Mode not Supported")
                }
            }
        }
    }

    private fun getFileList() {
        showLoadDialog()

        val mediaManager = djiManager.getMediaManagerInstance()

        if (mediaManager != null) {
            mediaManager.let { mm ->
                if ((currentFileListState == MediaManager.FileListState.SYNCING) || (currentFileListState == MediaManager.FileListState.DELETING)) {
                    setResultToToast("getFileList failed!")
                } else {
                    mm.refreshFileListOfStorageLocation(SettingsDefinitions.StorageLocation.SDCARD, {
                        hidLoadDialog()
                        if (it == null) {
                            if (currentFileListState != MediaManager.FileListState.INCOMPLETE) {
                                mediaFileList.clear()
                                lastClickViewIndex = -1
                                lastClickView = null
                            }
                            mediaFileList += mm.sdCardFileListSnapshot

                            Collections.sort(mediaFileList, object : Comparator<MediaFile> {
                                override fun compare(lhs: MediaFile, rhs: MediaFile): Int {
                                    if (lhs.getTimeCreated() < rhs.getTimeCreated()) {
                                        return 1
                                    } else if (lhs.getTimeCreated() > rhs.getTimeCreated()) {
                                        return -1
                                    }
                                    return 0
                                }
                            })

                            runOnUiThread {
                                fileAdapter.notifyDataSetChanged()
                            }

                        } else {
                            setResultToToast("get file list failed:${it.description}")
                        }
                    })
                }
            }
        } else {
            hidLoadDialog()
            errorSubject.onNext(resources.getString(R.string.error_aircraft_disconnect))
        }

    }

    private fun updateStatusTextView(videoPlaybackState: MediaManager.VideoPlaybackState?) {
        val pushInfo = StringBuffer()

        addLineToSB(pushInfo, "Video Playback State", null)
        if (videoPlaybackState != null) {
            if (videoPlaybackState.playingMediaFile != null) {
                addLineToSB(pushInfo, "media index", videoPlaybackState.playingMediaFile.index)
                addLineToSB(pushInfo, "media size", videoPlaybackState.playingMediaFile.fileSize)
                addLineToSB(pushInfo,
                        "media duration",
                        videoPlaybackState.playingMediaFile.durationInSeconds)
                addLineToSB(pushInfo, "media created date", videoPlaybackState.playingMediaFile.dateCreated)
                addLineToSB(pushInfo,
                        "media orientation",
                        videoPlaybackState.playingMediaFile.videoOrientation)
            } else {
                addLineToSB(pushInfo, "media index", "None")
            }
            addLineToSB(pushInfo, "media current position", videoPlaybackState.playingPosition)
            addLineToSB(pushInfo, "media current status", videoPlaybackState.playbackStatus)
            addLineToSB(pushInfo, "media cached percentage", videoPlaybackState.cachedPercentage)
            addLineToSB(pushInfo, "media cached position", videoPlaybackState.cachedPosition)
            pushInfo.append("\n")
            setResultToText(pushInfo.toString())
        }
    }

    private fun addLineToSB(sb: StringBuffer?, name: String?, value: Any?) {
        if (sb == null) return
        sb.append(if (name == null || "" == name) "" else "$name: ").append(if (value == null) "" else value.toString() + "").append("\n")
    }

    private fun getThumbnailByMedia(media: MediaFile) {
        val task = FetchMediaTask(media, FetchMediaTaskContent.THUMBNAIL, taskCallback)
        djiManager.getMediaManagerInstance()?.scheduler?.moveTaskToEnd(task)
    }

    private fun getPreviewByMedia(media: MediaFile) {
        progressBar.visibility = View.VISIBLE
        val task = FetchMediaTask(media, FetchMediaTaskContent.PREVIEW, { mediaFile, fetchMediaTaskContent, djiError ->
            runOnUiThread {
                progressBar.visibility = View.INVISIBLE
            }

            if (djiError == null) {
                if (fetchMediaTaskContent == FetchMediaTaskContent.PREVIEW) {
                    runOnUiThread {
                        imageView.setVisibility(View.VISIBLE)
                        imageView.setImageBitmap(mediaFile.preview)
                    }
                }
            } else {
                setResultToToast("Fetch Media Task Failed ${djiError.description}")
            }
        })

        val mediaManager = djiManager.getMediaManagerInstance()
        if (mediaManager != null) {
            mediaManager.scheduler?.moveTaskToEnd(task)
        } else {
            progressBar.visibility = View.INVISIBLE
        }

    }

    //function
    private fun downloadFileByIndex(index: Int) {

        if (index < 0) {
            currentProgress = -1
            return
        }
        if (mediaFileList[index].mediaType == MediaFile.MediaType.PANORAMA || mediaFileList[index].mediaType == MediaFile.MediaType.SHALLOW_FOCUS) {
            return
        }

        mediaFileList[index].fetchFileData(destDir, null, object : DownloadListener<String> {
            override fun onFailure(error: DJIError) {
                hideDownloadDialog()
                setResultToToast("Download File Failed" + error.description)
                currentProgress = -1
            }

            override fun onProgress(total: Long, current: Long) {}

            override fun onRateUpdate(total: Long, current: Long, persize: Long) {
                val tmpProgress = (1.0 * current / total * 100).toInt()
                if (tmpProgress != currentProgress) {
                    downloadDialog?.setProgress(tmpProgress)
                    currentProgress = tmpProgress
                }
            }

            override fun onStart() {
                currentProgress = -1
                showDownloadDialog()
            }

            override fun onSuccess(filePath: String) {
                hideDownloadDialog()
                setResultToToast("Download File Success:$filePath")
                currentProgress = -1
            }
        })
    }

    private fun deleteFileByIndex(index: Int) {
        if (index < 0) return
        val fileToDelete = ArrayList<MediaFile>()
        if (index < 0) {
            Toast.makeText(this, "delete:$index", Toast.LENGTH_SHORT).show()
        }
        if (mediaFileList.size > index) {
            fileToDelete.add(mediaFileList[index])
            val mediaManager = djiManager.getMediaManagerInstance()
            if (mediaManager != null) {
                mediaManager.deleteFiles(fileToDelete, object : CommonCallbacks.CompletionCallbackWithTwoParam<List<MediaFile>, DJICameraError> {
                    override fun onSuccess(x: List<MediaFile>, y: DJICameraError?) {

                        runOnUiThread {
                            val file = mediaFileList.removeAt(index)

                            //Reset select view
                            lastClickViewIndex = -1
                            lastClickView?.isSelected = false
                            lastClickView = null

                            //Update recyclerView
                            fileAdapter.notifyItemRemoved(index)
                        }
                    }

                    override fun onFailure(error: DJIError) {
                        setResultToToast("Delete file failed")
                    }
                })
            } else {
                errorSubject.onNext(resources.getString(R.string.error_aircraft_disconnect))
            }

        }
    }

    private fun playVideo() {

        val lp = fpvWidget.layoutParams
        lp.height = fpvWidget.width * 9 / 16
        fpvWidget.layoutParams = lp

        val selectedMediaFile = mediaFileList[lastClickViewIndex]
        if (selectedMediaFile.mediaType == MediaFile.MediaType.MOV || selectedMediaFile.mediaType == MediaFile.MediaType.MP4) {


            val mediaManager = djiManager.getMediaManagerInstance()
            if (mediaManager != null) {
                mediaManager.playVideoMediaFile(selectedMediaFile, { error ->
                    if (null != error) {
                        setResultToToast("Play Video Failed" + error.description)
                    } else {
                        runOnUiThread {
                            fpvWidget.visibility = View.VISIBLE
                        }
                    }
                })
            } else {
                errorSubject.onNext(resources.getString(R.string.error_aircraft_disconnect))
            }

        }
    }

    private fun stopVideo() {
        djiManager.getMediaManagerInstance()?.stop {
            it?.let { error ->
                setResultToToast("Stop Video Failed" + error.description)
            }
        }
    }

    private fun showLoadDialog() {
        runOnUiThread {
            loadingDialog?.show()
        }
    }

    private fun hidLoadDialog() {
        runOnUiThread {
            loadingDialog?.dismiss()
        }
    }

    private fun showDownloadDialog() {
        runOnUiThread {
            downloadDialog?.let {
                it.incrementProgressBy(-it.progress)
            }
            downloadDialog?.show()
        }
    }

    private fun hideDownloadDialog() {
        runOnUiThread {
            downloadDialog?.dismiss()
        }
    }

    private fun setResultToToast(result: String) {
        runOnUiThread {
            Toast.makeText(this, result, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setResultToText(string: String) {
        runOnUiThread {
            pointing_push_tv.setText(string)
        }
    }

    private inner class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var thumbnail_img: ImageView
        internal var file_name: TextView
        internal var file_type: TextView
        internal var file_size: TextView
        internal var file_time: TextView

        init {
            this.thumbnail_img = itemView.findViewById(R.id.filethumbnail) as ImageView
            this.file_name = itemView.findViewById(R.id.filename) as TextView
            this.file_type = itemView.findViewById(R.id.filetype) as TextView
            this.file_size = itemView.findViewById(R.id.fileSize) as TextView
            this.file_time = itemView.findViewById(R.id.filetime) as TextView
        }
    }

    private inner class FileListAdapter : RecyclerView.Adapter<ItemHolder>() {

        override fun getItemCount(): Int {
            return mediaFileList.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_media_info, parent, false)
            return ItemHolder(view)
        }

        override fun onBindViewHolder(mItemHolder: ItemHolder, index: Int) {

            val mediaFile = mediaFileList[index]
            if (mediaFile != null) {
                if (mediaFile.thumbnail == null) {
                    getThumbnailByMedia(mediaFile)
                }

                if (mediaFile.getMediaType() != MediaFile.MediaType.MOV && mediaFile.getMediaType() != MediaFile.MediaType.MP4) {
                    mItemHolder.file_time.visibility = View.GONE
                } else {
                    mItemHolder.file_time.visibility = View.VISIBLE
                    mItemHolder.file_time.setText("${mediaFile.getDurationInSeconds()} s")
                }
                mItemHolder.file_name.setText(mediaFile.getFileName())
                mItemHolder.file_type.setText(mediaFile.getMediaType().name)
                mItemHolder.file_size.setText("${mediaFile.getFileSize()} Bytes")
                mItemHolder.thumbnail_img.setImageBitmap(mediaFile.getThumbnail())
                /*
                mItemHolder.thumbnail_img.setOnClickListener { v ->
                    val selectedMedia = v.getTag() as MediaFile
                    val previewImage = selectedMedia.preview

                }
                */
                mItemHolder.thumbnail_img.tag = mediaFile
                mItemHolder.itemView.tag = index

                //log(this , "index:$index")
                if (lastClickViewIndex == index) {
                    mItemHolder.itemView.isSelected = true
                } else {
                    mItemHolder.itemView.isSelected = false
                }
                mItemHolder.itemView.setOnClickListener { v ->
                    val lastIndex = lastClickViewIndex
                    lastClickViewIndex = v.tag as Int
                    progressBar.visibility = View.INVISIBLE
                    btnControl.visibility = View.VISIBLE

                    if (lastClickView != null) {
                        lastClickView?.setSelected(false)
                    }
                    v.setSelected(true)
                    lastClickView = v

                    val mf = mediaFileList[lastClickViewIndex]

                    playbackStatusSubject.value?.let {
                        when (it) {
                            MediaFile.VideoPlaybackStatus.PLAYING,
                            MediaFile.VideoPlaybackStatus.PAUSED -> {
                                if (lastIndex != lastClickViewIndex) {
                                    stopVideo()
                                }
                            }
                            else -> {

                            }
                        }
                    }

                    fpvWidget.visibility = View.INVISIBLE
                    if (mf.mediaType == MediaFile.MediaType.MOV || mf.mediaType == MediaFile.MediaType.MP4) {

                        layoutTool.visibility = View.VISIBLE
                        imageView.visibility = View.INVISIBLE

                    } else {
                        layoutTool.visibility = View.INVISIBLE
                        imageView.visibility = View.VISIBLE

                        val previewImage = mf.preview
                        imageView.setImageBitmap(previewImage)

                        if (previewImage == null) {
                            getPreviewByMedia(mf)
                        }
                    }

                }

            }
        }
    }

}