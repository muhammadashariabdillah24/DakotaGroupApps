package com.dakotagroupstaff.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for storing recently opened menus
 * Used for the "Riwayat Menu" section on the home screen
 */
@Entity(tableName = "recent_menus")
data class RecentMenuEntity(
    @PrimaryKey
    @ColumnInfo(name = "menu_id")
    val menuId: String,
    
    @ColumnInfo(name = "menu_name")
    val menuName: String,
    
    @ColumnInfo(name = "icon_res_id")
    val iconResId: Int,
    
    @ColumnInfo(name = "activity_class")
    val activityClass: String,
    
    @ColumnInfo(name = "last_opened")
    val lastOpened: Long
)
