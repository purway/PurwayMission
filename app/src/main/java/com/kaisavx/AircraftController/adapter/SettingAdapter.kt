package com.kaisavx.AircraftController.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.kaisavx.AircraftController.R
import com.kaisavx.AircraftController.util.log


class SettingAdapter(private val context:Context , private val dataList : ArrayList<HashMap<String,Any>>):BaseAdapter() {

    companion object {
        val ITEM_SEEKBAR = 1
        val ITEM_SWITCH = 2

        val KEY_TYPE ="type"
        val KEY_TITLE = "title"
        val KEY_VALUE = "value"
        val KEY_MIN = "min"
        val KEY_MAX = "max"
        val KEY_SEEK_BAR_CHANGE_LISTENER = "seekBarChangeListener"
        val KEY_SWITCH_CHANGE_LISTENER = "switchChangeListener"

        val KEY_DATA_TEXT ="dataText"
    }

    var inflater: LayoutInflater? = null

    val seekBarChangeListener = object:SeekBar.OnSeekBarChangeListener{
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {

            seekBar?.let {
                it.tag?.let {
                    it as PositionHolder
                    val position = it.position
                    val map = getItem(position) as HashMap<String , Any>

                    val listener = map[KEY_SEEK_BAR_CHANGE_LISTENER] as SeekBar.OnSeekBarChangeListener
                    listener.onStopTrackingTouch(seekBar)
                }

            }
        }
    }
    
    val switchChangeListener = object:CompoundButton.OnCheckedChangeListener{
        override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
            buttonView?.let {
                it.tag?.let {
                    it as PositionHolder
                    val position = it.position
                    val map = getItem(position) as HashMap<String , Any>

                    val listener = map[KEY_SWITCH_CHANGE_LISTENER] as CompoundButton.OnCheckedChangeListener
                    listener.onCheckedChanged(buttonView,isChecked)
                }
            }
        }
    }


    val TYPE_MAX = 3

    init{
        inflater = LayoutInflater.from(context)
    }

    override fun getItemViewType(position: Int): Int {
        val map = dataList[position]

        var type: Int? = map[KEY_TYPE] as Int
        if (type == null) {
            type = 0
        }
        return type

    }

    override fun isEnabled(position: Int): Boolean {
        // TODO Auto-generated method stub
        // return super.isEnabled(position);
        if (dataList == null || position < 0 || position > count) {
            return false
        }
        return true
    }

    override fun getViewTypeCount(): Int {
        // TODO Auto-generated method stub
        return TYPE_MAX
    }

    override fun getItem(position: Int): Any {
        return dataList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        // TODO Auto-generated method stub
        return dataList.size
    }



    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        log(this ,"position$position")
        val map = getItem(position) as HashMap<String, Any>
        val type = getItemViewType(position)

        var holder: Holder?=null
        var view = convertView
        if(view == null){
            holder = Holder()
            //log(this,"${position} ${holder} create:${type}")
            when(type){
                ITEM_SEEKBAR -> {
                    view = inflater?.inflate(R.layout.item_seekbar,null)
                    holder.position = position
                    holder.title = view?.findViewById(R.id.seekbarTitle)
                    holder.seekBar = view?.findViewById(R.id.seekbar)
                    holder.seekBar?.setOnSeekBarChangeListener(seekBarChangeListener)
                    holder.dataText = view?.findViewById(R.id.dataText)
                }
                ITEM_SWITCH -> {
                    view = inflater?.inflate(R.layout.item_switch , null)
                    holder.title = view?.findViewById(R.id.checkboxTitle)
                    holder.switch = view?.findViewById(R.id.switchor)
                    holder.switch?.setOnCheckedChangeListener(switchChangeListener)
                }
            }
            view?.tag = holder
        }else{
            holder = view.tag as Holder
            //if(holder == null || holder.position != position)return View(context)
            //log(this , "holder:${holder}")

        }

        when(type){
            ITEM_SEEKBAR -> {
                holder.title?.setText(map[KEY_TITLE] as String)
                holder.seekBar?.let { seekBar ->
                    val value = map[KEY_VALUE] as Int
                    val min = map[KEY_MIN] as Int
                    val max = map[KEY_MAX] as Int
                    val process = value - min

                    //log(this, "${position} value:${value} min:${min} max:${max} process:${process}")
                    seekBar.setProgress(process)
                    seekBar.max = max - min

                    var tag = seekBar.tag
                    if(tag == null){
                        var seekBarHolder = PositionHolder()
                        seekBarHolder.position = position
                        seekBar.tag =seekBarHolder
                    }else{
                        var seekBarHolder = tag as PositionHolder
                        seekBarHolder.position = position
                    }

                }
                holder.dataText?.setText(map[KEY_DATA_TEXT] as String)
            }

            ITEM_SWITCH -> {
                holder.title?.setText(map[KEY_TITLE] as String)
                holder.switch?.let {switch ->
                    switch.setChecked(map[KEY_VALUE] as Boolean)
                    var tag = switch.tag
                    if(tag == null){
                        var positionHolder = PositionHolder()
                        positionHolder.position = position
                        switch.tag = positionHolder
                    }else{
                        var positionHolder = tag as PositionHolder
                        positionHolder.position = position

                    }
                }
            }

        }

        if(view == null){
            return View(context)
        }else {
            return view
        }
    }

    inner class Holder{
        var position:Int=0
        var title: TextView?=null
        var switch:Switch?=null
        var seekBar:SeekBar?=null
        var dataText:TextView?=null
    }

    inner class PositionHolder{
        var position:Int = 0
    }
    
}