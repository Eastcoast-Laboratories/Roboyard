package roboyard.ui.compose

import org.jetbrains.compose.resources.DrawableResource
import roboyard.composeapp.generated.resources.Res
import roboyard.composeapp.generated.resources.icon_10_gear
import roboyard.composeapp.generated.resources.icon_11_chart
import roboyard.composeapp.generated.resources.icon_12_bars
import roboyard.composeapp.generated.resources.icon_13_hourglass
import roboyard.composeapp.generated.resources.icon_14_checkmark
import roboyard.composeapp.generated.resources.icon_15_lightbulb
import roboyard.composeapp.generated.resources.icon_16_compass
import roboyard.composeapp.generated.resources.icon_18_wrench
import roboyard.composeapp.generated.resources.icon_1_lightning
import roboyard.composeapp.generated.resources.icon_24_robot_gold
import roboyard.composeapp.generated.resources.icon_26_shield_red
import roboyard.composeapp.generated.resources.icon_27_star
import roboyard.composeapp.generated.resources.icon_29_brain_green
import roboyard.composeapp.generated.resources.icon_2_heart
import roboyard.composeapp.generated.resources.icon_32_infinity
import roboyard.composeapp.generated.resources.icon_33_target_blue
import roboyard.composeapp.generated.resources.icon_35_door
import roboyard.composeapp.generated.resources.icon_36_ring
import roboyard.composeapp.generated.resources.icon_39_stars
import roboyard.composeapp.generated.resources.icon_3_robot
import roboyard.composeapp.generated.resources.icon_40_planet
import roboyard.composeapp.generated.resources.icon_41_crown
import roboyard.composeapp.generated.resources.icon_42_laurel
import roboyard.composeapp.generated.resources.icon_43_trophy_gold
import roboyard.composeapp.generated.resources.icon_45_diamond_blue
import roboyard.composeapp.generated.resources.icon_46_flame
import roboyard.composeapp.generated.resources.icon_48_stars_gold
import roboyard.composeapp.generated.resources.icon_49_armor
import roboyard.composeapp.generated.resources.icon_4_power
import roboyard.composeapp.generated.resources.icon_50_folders
import roboyard.composeapp.generated.resources.icon_51_spiral
import roboyard.composeapp.generated.resources.icon_52_network
import roboyard.composeapp.generated.resources.icon_53_ring_blue
import roboyard.composeapp.generated.resources.icon_54_cube
import roboyard.composeapp.generated.resources.icon_55_cone
import roboyard.composeapp.generated.resources.icon_5_monitor
import roboyard.composeapp.generated.resources.icon_8_target
import roboyard.composeapp.generated.resources.icon_9_trophy

/**
 * Maps achievement icon names (as used in AchievementDefinitions / Android drawable names)
 * to Compose drawable resources.
 */
fun achievementIconResource(name: String?): DrawableResource? = when (name) {
        "icon_10_gear" -> Res.drawable.icon_10_gear
        "icon_11_chart" -> Res.drawable.icon_11_chart
        "icon_12_bars" -> Res.drawable.icon_12_bars
        "icon_13_hourglass" -> Res.drawable.icon_13_hourglass
        "icon_14_checkmark" -> Res.drawable.icon_14_checkmark
        "icon_15_lightbulb" -> Res.drawable.icon_15_lightbulb
        "icon_16_compass" -> Res.drawable.icon_16_compass
        "icon_18_wrench" -> Res.drawable.icon_18_wrench
        "icon_1_lightning" -> Res.drawable.icon_1_lightning
        "icon_24_robot_gold" -> Res.drawable.icon_24_robot_gold
        "icon_26_shield_red" -> Res.drawable.icon_26_shield_red
        "icon_27_star" -> Res.drawable.icon_27_star
        "icon_29_brain_green" -> Res.drawable.icon_29_brain_green
        "icon_2_heart" -> Res.drawable.icon_2_heart
        "icon_32_infinity" -> Res.drawable.icon_32_infinity
        "icon_33_target_blue" -> Res.drawable.icon_33_target_blue
        "icon_35_door" -> Res.drawable.icon_35_door
        "icon_36_ring" -> Res.drawable.icon_36_ring
        "icon_39_stars" -> Res.drawable.icon_39_stars
        "icon_3_robot" -> Res.drawable.icon_3_robot
        "icon_40_planet" -> Res.drawable.icon_40_planet
        "icon_41_crown" -> Res.drawable.icon_41_crown
        "icon_42_laurel" -> Res.drawable.icon_42_laurel
        "icon_43_trophy_gold" -> Res.drawable.icon_43_trophy_gold
        "icon_45_diamond_blue" -> Res.drawable.icon_45_diamond_blue
        "icon_46_flame" -> Res.drawable.icon_46_flame
        "icon_48_stars_gold" -> Res.drawable.icon_48_stars_gold
        "icon_49_armor" -> Res.drawable.icon_49_armor
        "icon_4_power" -> Res.drawable.icon_4_power
        "icon_50_folders" -> Res.drawable.icon_50_folders
        "icon_51_spiral" -> Res.drawable.icon_51_spiral
        "icon_52_network" -> Res.drawable.icon_52_network
        "icon_53_ring_blue" -> Res.drawable.icon_53_ring_blue
        "icon_54_cube" -> Res.drawable.icon_54_cube
        "icon_55_cone" -> Res.drawable.icon_55_cone
        "icon_5_monitor" -> Res.drawable.icon_5_monitor
        "icon_8_target" -> Res.drawable.icon_8_target
        "icon_9_trophy" -> Res.drawable.icon_9_trophy
    else -> null
}
