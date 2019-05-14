package com.kaisavx.AircraftController.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.kaisavx.AircraftController.R
import kotlinx.android.synthetic.main.activity_beginner.*

/**
 * Created by Abner on 2017/6/12.
 */

open class Menu(val title: String, val link: String)
class TextMenu(title: String, val textRes: Int): Menu(title, "")

val beginnerMenus = arrayOf(
        Menu("首次飞行视频", "https://www.djivideos.com/video_play/aee7cb24-c3f5-4d5e-9f59-e6ae00e4ef49?autoplay=1&poster=https://cdn-hz.skypixel.com/uploads/cn_files/video/image/8bc8fca5-d097-4a0b-bc10-e85d338f33e6.jpg@!1200"),
        Menu("飞行前的检查和环境选择", "https://www.djivideos.com/video_play/b7bf054b-7974-4055-ac5d-0a714c0d5a96?autoplay=1&poster=https://cdn-usa.skypixel.com/uploads/usa_files/video/image/73bb153d-4a88-4371-b612-4594fcc43fbf.jpeg@!1200"),
        Menu("飞行注意事项", "https://www.djivideos.com/video_play/b922970c-11a6-41be-a882-7441aa2cbbb2?autoplay=1&poster=https://cdn-usa.skypixel.com/uploads/usa_files/video/image/37f404bc-b4ca-40b4-b46c-8e460e736cbc@!1200"),
        Menu("如遇特殊情况怎么办", "https://www.djivideos.com/video_play/f8dd8684-38bb-495e-ad6d-bcfb4134e547?autoplay=1&poster=https://cdn-usa.skypixel.com/uploads/usa_files/video/image/6b69b660-0aea-4b29-bf96-1719f3eca266.jpeg@!1200"),
        Menu("遥控器天线使用注意事项", "https://www.djivideos.com/video_play/b6f48bdb-8e4e-41eb-a43e-38c26a825ab3?autoplay=1&poster=https://cdn-usa.skypixel.com/uploads/usa_files/video/image/7ae59d6b-cda2-45e6-be1c-b327c4b1c176.jpeg@!1200"),
        Menu("电池的使用和保养", "https://www.djivideos.com/video_play/1bb107ce-a33b-4296-a3a8-c929149938e8?autoplay=1&poster=https://cdn-usa.skypixel.com/uploads/usa_files/video/image/c4ac7514-60bd-4da6-873e-18f7097d7e61.jpg@!1200"),
        Menu("安全飞行指引（限飞政策）", "http://www.dji.com/cn/flysafe")
)
val productMenus = arrayOf(
        Menu("飞行器快速入门.pdf", "https://dl.djicdn.com/downloads/m600+pro/cn/Matrice+600+Pro+Quick+Start+Guide+v1.0.pdf"),
        Menu("飞行器用户手册.pdf", "https://dl.djicdn.com/downloads/m600+pro/M600+Pro+User+Manual+v1.0+CHS.pdf"),
        Menu("飞行器免责声明和安全操作指引.pdf", "https://dl.djicdn.com/downloads/m600+pro/cn/Matrice+600+Pro+Disclaimer+and+Safety+Guidelines+v1.0.pdf"),
        Menu("维护保养手册.pdf", "https://dl.djicdn.com/downloads/m600/cn/M600_Maintenance_Manual_V1.0_cn_20160622.pdf"),
        Menu("调参软件及其他文件下载", "http://www.dji.com/cn/matrice600-pro/info#downloads"),
        Menu("采集器说明", "")
)
val bookMenus = arrayOf(
        Menu("飞行器十大误操作", "https://www.djivideos.com/video_play/3847424c-c27f-43dd-9f07-131d49c5ca33?autoplay=1&poster=https://cdn-hz.skypixel.com/uploads/cn_files/video/image/27e9fbad-1e18-4f7a-9d77-211da0025951@!1200"),
        Menu("如何进行固件升级", "https://djistatic.com/academy/faq/?id=168&language=cn"),
        Menu("飞行环境要求", "https://djistatic.com/academy/faq/?id=163&language=cn"),
        Menu("低温使用电池时注意事项", "https://djistatic.com/academy/faq/?id=109&language=cn"),
        TextMenu("飞行前注意事项", R.string.book_notice_before_flying),
        TextMenu("什么时候需要校准指南针", R.string.book_compass_calibration1),
        TextMenu("为什么起飞前校准过指南针，起飞后还提示指南针干扰", R.string.book_compass_calibration2),
        TextMenu("视觉定位系统对环境的要求", R.string.book_vision_system),
        TextMenu("为什么使用『内八』无法启动电机", R.string.book_stick),
        Menu("DJI GO App 如何使用姿态球和地图", "https://www.djivideos.com/video_play/f0a638b7-a0e7-48c4-a8eb-909a0467378a?autoplay=1&poster=https://cdn-usa.skypixel.com/uploads/usa_files/video/image/1de81cf6-c365-4386-823b-a88677fa48fb.jpeg@!1200")
)

class MenusActivity : BaseActivity() {
    companion object {
        val MENU_TYPE_BEGINNER = 0
        val MENU_TYPE_PRODUCT = 1
        val MENU_TYPE_BOOK = 2

        fun intent(context: Context, menuType: Int, title: String): Intent {
            val intent = Intent(context, MenusActivity::class.java)
            intent.putExtra("type", menuType)
            intent.putExtra("title", title)
            return intent
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_beginner)

        title = intent.getStringExtra("title")

        val menus = when (intent.getIntExtra("type", -1)) {
            MENU_TYPE_BEGINNER -> beginnerMenus
            MENU_TYPE_PRODUCT -> productMenus
            MENU_TYPE_BOOK -> bookMenus
            else -> arrayOf()
        }

        val adapter = Adapter(this, menus)
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, i, _ ->
            val menu = adapter.menus[i]
            if (menu is TextMenu) {
                startActivity(TextActivity.intent(this, menu.title, menu.textRes))
            } else if (!menu.link.isEmpty()) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(menu.link)))
            }
        }
    }

    class Adapter(val context: Context, val menus: Array<Menu>) : BaseAdapter() {
        override fun getView(position: Int, view: View?, parent: ViewGroup?): View {
            val titleView: TextView
            if (view == null) {
                titleView = LayoutInflater.from(context).inflate(R.layout.item_menu_item, parent, false) as TextView
            } else {
                titleView = view as TextView
            }

            titleView.text = menus[position].title
            return titleView
        }

        override fun getItem(position: Int): Any {
            return menus[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getCount(): Int {
            return menus.size
        }

    }

}
