package com.kaisavx.AircraftController.view

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.format.DateFormat
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CompoundButton
import android.widget.SeekBar
import android.widget.TextView
import com.kaisavx.AircraftController.R
import com.kaisavx.AircraftController.controller.MapMissionController
import com.kaisavx.AircraftController.service.Mission
import com.kaisavx.AircraftController.service.MissionState
import com.kaisavx.AircraftController.util.log
import dji.common.model.LocationCoordinate2D
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_mission.*
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by windless on 2017/9/22.
 */
class MissionPanel : Fragment() {
    private val minSpeed = 1
    private val minSpacing = 10
    private val minAltitude = 30
    private val minShootTimeInterval = 2
    private val minGimbalPitch = -90

    private val latlngGap = 0.00001
    private val altitudeGap = 0.1

    var missionListAdapter: MissionAdapter by Delegates.notNull()

    private val binder = CompositeDisposable()

    var onSpacingChanged: ((Int) -> Unit)? = null
    var onSpeedChanged: ((Int) -> Unit)? = null
    var onAltitudeChanged: ((Int) -> Unit)? = null
    var onGimbalPitchChanged:((Int) -> Unit) ?= null
    var onShootTimeIntervalChanged:((Int) -> Unit)? = null
    var onAltitudeOverall:((Boolean) -> Unit)?=null
    var onSpeedOverall:((Boolean) -> Unit)?=null
    var onGimbalOverall:((Boolean) -> Unit)?=null
    var onShootTimeIntervalOverall:((Boolean) -> Unit)?=null
    var onATHChanged:((Boolean) -> Unit)?=null

    var onItemShootTimeIntervalChanged:((Int) -> Unit)?=null
    var onItemSpeedChanged:((Int) -> Unit)?=null
    var onItemAltitudeChanged:((Int) -> Unit)?=null
    var onItemGimbalPitchChanged:((Int) -> Unit)?=null
    var onItemLatLngChanged:((LocationCoordinate2D) -> Unit)?=null


    var onCreateMission: ((MissionPanel, MapMissionController.ControlType) -> Unit)? = null
    var onSelectMission: ((MissionPanel, Mission) -> Unit)? = null
    var onDeleteMission: ((MissionPanel) -> Unit)? = null

    var onUploadPressed: (() -> Unit)? = null
    var onStartPressed: (() -> Unit)? = null
    var onPausePressed: (() -> Unit)? = null
    var onCancelPressed: (() -> Unit)? = null
    var onResumePressed: (() -> Unit)? = null
    var onBackPressed: (() -> Unit)? = null

    private var missionType: MapMissionController.ControlType = MapMissionController.ControlType.Area

    private val seekbarChangeListener = object:SeekBar.OnSeekBarChangeListener{
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            when (seekBar) {
                seekbarSpacing -> {
                    val spacing = progress + minSpacing
                    textSpacing.text = activity?.getString(R.string.mission_spacing, spacing.toString())
                    onSpacingChanged?.invoke(spacing)
                }

                seekbarAltitude -> {
                    val altitude = progress + minAltitude
                    textAltitude.text = activity?.getString(R.string.mission_altitude, altitude.toString())
                    onAltitudeChanged?.invoke(altitude)
                }

                seekbarSpeed -> {
                    val speed = progress + minSpeed
                    textSpeed.text = activity?.getString(R.string.mission_speed, speed.toString())
                    onSpeedChanged?.invoke(speed)
                }

                seekbarGimbalPitch -> {
                    val gimbalPitch = progress + minGimbalPitch
                    textGimbalPitch.text = activity?.getString(R.string.mission_gimbalPitch,gimbalPitch.toString())
                    onGimbalPitchChanged?.invoke(gimbalPitch)
                }

                seekbarShootTimeInterval->{
                    val shootTimeInterval = progress + minShootTimeInterval

                    textShootTimeInterval.text = activity?.getString(R.string.mission_timeInterval , shootTimeInterval.toString())
                    onShootTimeIntervalChanged?.invoke(shootTimeInterval)
                }

                seekbarItemGimbalPitch -> {
                    val gimbalPitch = progress + minGimbalPitch
                    textItemGimbalPitch.text = activity?.getString(R.string.mission_gimbalPitch,gimbalPitch.toString())
                    onItemGimbalPitchChanged?.invoke(gimbalPitch)
                }

                seekbarItemShootTimeInterval ->{
                    val shootTimeInterval = progress + minShootTimeInterval

                    textItemShootTimeInterval.text = activity?.getString(R.string.mission_timeInterval , shootTimeInterval.toString())
                    onItemShootTimeIntervalChanged?.invoke(shootTimeInterval)
                }

                seekbarItemSpeed -> {
                    val speed = progress + minSpeed
                    textItemSpeed.text = activity?.getString(R.string.mission_speed, speed.toString())
                    onItemSpeedChanged?.invoke(speed)
                }

                seekbarItemAltitude ->{
                    val altitude = progress + minAltitude
                    textItemAltitude.text = activity?.getString(R.string.mission_altitude, altitude.toString())
                    onItemAltitudeChanged?.invoke(altitude)
                }


            }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {

        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {

        }
    }

    private val btnClickListener = object :View.OnClickListener{
        override fun onClick(btn: View?) {
            when(btn){
                btnLatSub ->{
                    val lat = editLat.text.toString().toDouble() - latlngGap
                    val lng = editLng.text.toString().toDouble()
                    editLat.setText("$lat")
                    onItemLatLngChanged?.invoke(LocationCoordinate2D(lat,lng))
                }
                btnLatAdd ->{
                    val lat = editLat.text.toString().toDouble() + latlngGap
                    val lng = editLng.text.toString().toDouble()
                    editLat.setText("$lat")
                    onItemLatLngChanged?.invoke(LocationCoordinate2D(lat,lng))
                }
                btnLngSub ->{
                    val lat = editLat.text.toString().toDouble()
                    val lng = editLng.text.toString().toDouble() - latlngGap
                    editLng.setText("$lng")

                    onItemLatLngChanged?.invoke(LocationCoordinate2D(lat,lng))
                }
                btnLngAdd->{
                    val lat = editLat.text.toString().toDouble()
                    val lng = editLng.text.toString().toDouble() + latlngGap
                    editLng.setText("$lng")

                    onItemLatLngChanged?.invoke(LocationCoordinate2D(lat,lng))
                }
            }
        }
    }

    private val editorActionListener = object : TextView.OnEditorActionListener {
        override fun onEditorAction(textView: TextView?, actionId: Int, event: KeyEvent?): Boolean {
            when(textView){
                editLat ->{
                    val lat = editLat.text.toString().toDouble()
                    val lng = editLng.text.toString().toDouble()
                    onItemLatLngChanged?.invoke(LocationCoordinate2D(lat,lng))
                }
                editLng -> {
                    val lat = editLat.text.toString().toDouble()
                    val lng = editLng.text.toString().toDouble()
                    onItemLatLngChanged?.invoke(LocationCoordinate2D(lat,lng))
                }
            }
            return false
        }
    }

    private val switchCheckedListener = object:CompoundButton.OnCheckedChangeListener{
        override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
            when(buttonView){
                switchAltitude ->{
                    if(isChecked){
                        linearAltitude.visibility = View.VISIBLE

                        seekbarItemAltitude.progress = seekbarAltitude.progress
                        seekbarItemAltitude.isEnabled = false
                    }else{
                        linearAltitude.visibility = View.GONE
                        seekbarItemAltitude.isEnabled = true
                    }
                    onAltitudeOverall?.invoke(isChecked)
                }
                switchSpeed -> {
                    if (isChecked){
                        linearSpeed.visibility = View.VISIBLE
                        seekbarItemSpeed.progress = seekbarSpeed.progress
                        seekbarItemSpeed.isEnabled = false
                    }else{
                        linearSpeed.visibility = View.GONE
                        seekbarItemSpeed.isEnabled = true
                    }
                    onSpeedOverall?.invoke(isChecked)
                }
                switchGimbalPitch -> {
                    if(isChecked){
                        linearGimbalPitch.visibility = View.VISIBLE
                        seekbarItemGimbalPitch.progress = seekbarGimbalPitch.progress
                        seekbarItemGimbalPitch.isEnabled = false

                    }else{
                        linearGimbalPitch.visibility = View.GONE
                        seekbarItemGimbalPitch.isEnabled = true
                    }
                    onGimbalOverall?.invoke(isChecked)
                }
                switchShootTimeInterval -> {
                    if(isChecked){
                        linearShootTimeInterval.visibility = View.VISIBLE
                        seekbarItemShootTimeInterval.progress = seekbarItemShootTimeInterval.progress
                        seekbarItemShootTimeInterval.isEnabled = false
                    }else{
                        linearShootTimeInterval.visibility = View.GONE
                        seekbarItemShootTimeInterval.isEnabled = true
                    }
                }
                switchATH ->{
                    onATHChanged?.invoke(isChecked)
                }

            }
        }
    }
    //region Life-cycle
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return View.inflate(activity , R.layout.fragment_mission, null)
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        init()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binder.clear()
    }

    private fun init(){

        btnBack.setOnClickListener {
            if(linearItem.visibility == View.VISIBLE){
                showDetail()
            }else if(linearOverall.visibility == View.VISIBLE) {
                showList()
            }
            onBackPressed?.invoke()
        }
        btnAdd.setOnClickListener {
            showMissionType()
        }
        btnDelete.setOnClickListener {
            showList()
            onDeleteMission?.invoke(this)
        }

        btnUpload.setOnClickListener {
            onUploadPressed?.invoke()
        }
        btnStart.setOnClickListener {
            onStartPressed?.invoke()
        }
        btnCancel.setOnClickListener {
            onCancelPressed?.invoke()
        }
        btnPause.setOnClickListener {
            onPausePressed?.invoke()
        }
        btnResume.setOnClickListener {
            onResumePressed?.invoke()
        }
        btnSure.setOnClickListener {
            showDetail()
            onBackPressed?.invoke()
        }

        missionListAdapter = MissionAdapter(activity)
        missionListAdapter.onSelectMission = { mission ->
            onSelectMission?.invoke(this, mission)

            missionType = if (mission.type == 0) {
                MapMissionController.ControlType.Area
            } else {
                MapMissionController.ControlType.Path
            }
            showDetail()
        }
        listViewMission.adapter = missionListAdapter

        seekbarSpacing.setOnSeekBarChangeListener(seekbarChangeListener)
        seekbarAltitude.setOnSeekBarChangeListener(seekbarChangeListener)
        seekbarSpeed.setOnSeekBarChangeListener(seekbarChangeListener)
        seekbarGimbalPitch.setOnSeekBarChangeListener(seekbarChangeListener)
        seekbarShootTimeInterval.setOnSeekBarChangeListener(seekbarChangeListener)

        seekbarItemSpeed.setOnSeekBarChangeListener(seekbarChangeListener)
        seekbarItemShootTimeInterval.setOnSeekBarChangeListener(seekbarChangeListener)
        seekbarItemAltitude.setOnSeekBarChangeListener(seekbarChangeListener)
        seekbarItemGimbalPitch.setOnSeekBarChangeListener(seekbarChangeListener)

        btnLatSub.setOnClickListener(btnClickListener)
        btnLatAdd.setOnClickListener(btnClickListener)
        btnLngSub.setOnClickListener(btnClickListener)
        btnLngAdd.setOnClickListener(btnClickListener)

        switchAltitude.setOnCheckedChangeListener(switchCheckedListener)
        switchSpeed.setOnCheckedChangeListener(switchCheckedListener)
        switchGimbalPitch.setOnCheckedChangeListener(switchCheckedListener)
        switchShootTimeInterval.setOnCheckedChangeListener(switchCheckedListener)
        switchATH.setOnCheckedChangeListener(switchCheckedListener)


        editLat.setOnEditorActionListener(editorActionListener)
        editLng.setOnEditorActionListener(editorActionListener)

    }

    fun setMissions(missions: List<Mission>) {
        missionListAdapter.missions = missions
        missionListAdapter.notifyDataSetChanged()
    }

    fun updateUI() {
        missionListAdapter.notifyDataSetChanged()
    }

    fun setDistance(distance: Int) {
        textDistance.text = activity?.getString(R.string.mission_distance, distance.toString())
    }

    fun setMissionState(state: MissionState) {
        seekbarSpeed.isEnabled = state == MissionState.None
        seekbarSpacing.isEnabled = state == MissionState.None && missionType == MapMissionController.ControlType.Area
        seekbarAltitude.isEnabled = state == MissionState.None

//        btnDelete.isEnabled = state == MissionState.None || state == MissionState.ReadyToExecuting
//        btnBack.isEnabled = state == MissionState.None || state == MissionState.ReadyToExecuting

        btnUpload.isEnabled = state != MissionState.Uploading
        if (state == MissionState.Uploading) {
            btnUpload.text = "正在上传飞行任务"
            btnUpload.isEnabled = false
            progressUpload.visibility = View.VISIBLE
        } else {
            btnUpload.text = "开始上传飞行任务"
            btnUpload.isEnabled = true
            progressUpload.visibility = View.GONE
        }

        when (state) {
            MissionState.None -> {
                btnUpload.visibility = View.VISIBLE
                btnStart.visibility = View.GONE
                btnPause.visibility = View.GONE
                btnCancel.visibility = View.GONE
                btnResume.visibility = View.GONE
            }
            MissionState.ReadyToExecuting -> {
                btnUpload.visibility = View.GONE
                btnStart.visibility = View.VISIBLE
                btnPause.visibility = View.GONE
                btnCancel.visibility = View.GONE
                btnResume.visibility = View.GONE
            }
            MissionState.Uploading -> {
                btnUpload.visibility = View.VISIBLE
                btnStart.visibility = View.GONE
                btnPause.visibility = View.GONE
                btnCancel.visibility = View.GONE
                btnResume.visibility = View.GONE
            }
            MissionState.Executing -> {
                btnUpload.visibility = View.GONE
                btnStart.visibility = View.GONE
                btnPause.visibility = View.VISIBLE
                btnCancel.visibility = View.GONE
                btnResume.visibility = View.GONE
            }
            MissionState.Paused -> {
                btnUpload.visibility = View.GONE
                btnStart.visibility = View.GONE
                btnPause.visibility = View.GONE
                btnCancel.visibility = View.GONE
                btnResume.visibility = View.VISIBLE

                btnDelete.isEnabled = true
                btnBack.isEnabled = true
            }
            else -> {
            }
        }
    }

    fun setTime(time: Int) {
        var seconds = time
        val hours = seconds / 60 / 60
        seconds -= hours * 60 * 60
        val minutes = seconds / 60
        seconds -= minutes * 60

        var str = ""
        if (hours > 0) {
            str += "$hours 小时"
        }
        if (minutes > 0 || str.isNotEmpty()) {
            str += "$minutes 分"
        }
        if (seconds > 0 || str.isNotEmpty()) {
            str += "$seconds 秒"
        }

        textTime.text = activity?.getString(R.string.mission_time, str)
    }

    private fun showList() {
        textTitle.visibility = View.VISIBLE
        btnBack.visibility = View.GONE
        btnAdd.visibility = View.VISIBLE
        btnDelete.visibility = View.GONE
        listViewMission.visibility = View.VISIBLE
        linearOverall.visibility = View.GONE
        linearItem.visibility = View.GONE

    }

    fun showDetail() {
        textTitle.visibility = View.GONE
        btnBack.visibility = View.VISIBLE
        btnAdd.visibility = View.GONE
        btnDelete.visibility = View.VISIBLE
        listViewMission.visibility = View.GONE
        linearOverall.visibility = View.VISIBLE
        linearItem.visibility = View.GONE
        seekbarSpacing.isEnabled = missionType != MapMissionController.ControlType.Path

        if(missionType == MapMissionController.ControlType.Path){
            textSpacing.visibility = View.GONE
            linearSpacing.visibility = View.GONE

        }else{
            textSpacing.visibility = View.VISIBLE
            linearSpacing.visibility = View.VISIBLE
            linearSpeed.visibility = View.VISIBLE
            linearAltitude.visibility = View.VISIBLE
            linearGimbalPitch.visibility = View.VISIBLE
            linearShootTimeInterval.visibility = View.VISIBLE
        }
    }

    fun showItem(){
        textTitle.visibility = View.GONE
        btnBack.visibility = View.VISIBLE
        btnAdd.visibility = View.GONE
        btnDelete.visibility = View.VISIBLE
        listViewMission.visibility = View.GONE
        linearOverall.visibility = View.GONE
        linearItem.visibility = View.VISIBLE
    }

    private fun showMissionType() {
        AlertDialog.Builder(activity)
                .setItems(R.array.mission_types, { dialog, i ->
                    dialog.dismiss()
                    when (i) {
                        0 -> {
                            if(onCreateMission == null){
                                log(this , "onCreateMission is null")
                            }
                            onCreateMission?.invoke(this, MapMissionController.ControlType.Area)
                            missionType = MapMissionController.ControlType.Area

                        }
                        1 -> {
                            onCreateMission?.invoke(this, MapMissionController.ControlType.Path)
                            missionType = MapMissionController.ControlType.Path


                        }
                    }
                    showDetail()
                })
                .show()
    }

    inner class MissionAdapter(val context: Context?) : BaseAdapter() {

        var missions = listOf<Mission>()

        var onSelectMission: ((Mission) -> Unit)? = null

        override fun getView(i: Int, view: View?, parent: ViewGroup): View {
            val mission = missions[i]

            val contentView: View = view ?: LayoutInflater.from(context).inflate(R.layout.item_mission, parent, false)
            contentView.tag = contentView.tag ?: MissionViewHolder(contentView)

            val holder = contentView.tag as MissionViewHolder
            holder.title.text = mission.address
            holder.subtitle.text = getFormatTime(mission.createdAt)
            holder.click.tag = mission
            holder.click.setOnClickListener(missionClick)

            return contentView
        }

        private fun getFormatTime(time: Long): String {
            val date = Date(time)
            return DateFormat.format("yyyy.MM.dd", date).toString()
        }

        override fun getItem(i: Int): Any = missions[i]

        override fun getItemId(i: Int): Long = i.toLong()

        override fun getCount(): Int = missions.size

        private val missionClick = View.OnClickListener { view ->
            val mission = view.tag
            if (mission is Mission) {
                onSelectMission?.invoke(mission)
            }
        }
    }

    inner class MissionViewHolder(val view: View) {
        val title: TextView by lazy { view.findViewById<TextView>(R.id.title) }
        val subtitle: TextView by lazy { view.findViewById<TextView>(R.id.subtitle) }
        val click: View by lazy { view.findViewById<View>(R.id.click) }
    }


}

