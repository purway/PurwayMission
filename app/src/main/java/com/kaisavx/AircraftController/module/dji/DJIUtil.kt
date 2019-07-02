package com.kaisavx.AircraftController.module.dji

import dji.common.product.Model
import dji.sdk.camera.Camera.*
import dji.sdk.remotecontroller.RemoteController.*

object DJIUtil{
    fun getRCDisplayName(displayName:String):String{
        when(displayName){
            DisplayNameMavicPro->{
                return "遥控器 御Pro"
            }
            DisplayNameInspire1->{
                return "遥控器 悟"
            }
            DisplayNameLightbridge2->{
                return "遥控器 Lightbridge 2"
            }
            DisplayNameInspire2->{
                return "遥控器 悟2"
            }
            DisplayNameCendence->{
                return "遥控器 Cendence"
            }
            DisplayNameCendenceSDR->{
                return "遥控器 Cendence SDR"
            }
            DisplayNamePhantom3Professinal->{
                return "遥控器 精灵3 Pro"
            }
            DisplayNamePhantom4Pro->{
                return "遥控器 精灵4 Pro"
            }
            DisplayNamePhantom4Advanced->{
                return "遥控器 精灵4 A"
            }
            DisplayNamePhantom4ProV2->{
                return "遥控器 精灵4 Pro V2.0"
            }
            DisplayNameSpark->{
                return "遥控器 晓"
            }
            DisplayNameMavicAir->{
                return "遥控器 御 Air"
            }
            DisplayNameMavic2->{
                return "遥控器 御2"
            }
            DisplayNameMavic2Enterprise->{
                return "遥控器 御2 行业版"
            }
            DisplayNameDJISmartController->{
                return "遥控器 Smart"
            }
            else->{
                return displayName
            }
        }
    }

    fun getFlightDisplayName(model:Model):String{
        when(model){
            Model.MATRICE_200->{
                return "经纬 M200"
            }
            Model.MATRICE_210->{
                return "经纬 M210"
            }
            Model.MATRICE_210_RTK->{
                return "经纬 M210 RTK"
            }
            Model.MATRICE_PM420->{
                return "经纬 M200 V2"
            }
            Model.MATRICE_PM420PRO->{
                return "经纬 M210 V2"
            }
            Model.MATRICE_PM420PRO_RTK->{
                return "经纬 M210 V2 RTK"
            }
            Model.MATRICE_600->{
                return "经纬 M600"
            }
            Model.MATRICE_600_PRO->{
                return "经纬 M600 Pro"
            }
            else->{
                return model.displayName
            }
        }
    }

    fun getCameraDisplayName(displayName: String):String?{
        when(displayName){
            DisplayNameZ30 ->{
                return "禅思 Z30"
            }

            DisplayNameX4S->{
                return "禅思 X4S"
            }

            DisplayNameX5S->{
                return "禅思 X5S"
            }

            DisplayNameX5->{
                return "禅思 X5"
            }

            DisplayNameX5R->{
                return "禅思 X5R"
            }

            DisplayNameX7->{
                return "禅思 X7"
            }

            DisplayNameXT->{
                return "禅思 XT"
            }
            DisplayNameXT2_IR->{
                return null
            }
            DisplayNameXT2_VL->{
                return "禅思 XT2"
            }
            else->{
                return displayName
            }

        }
    }
}