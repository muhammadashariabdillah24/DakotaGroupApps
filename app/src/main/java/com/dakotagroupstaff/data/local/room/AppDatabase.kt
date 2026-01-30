package com.dakotagroupstaff.data.local.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dakotagroupstaff.data.local.dao.*
import com.dakotagroupstaff.data.local.entity.*

/**
 * Dakota Group Staff App Database
 * Following pattern from MyQuranApp (Android-Expert)
 * 
 * Contains 6 cache tables for performance optimization:
 * 1. employee_bio_cache - Employee bio data (7 days cache)
 * 2. agent_locations_cache - Agent locations for GPS validation (30 days cache) - WITH MD5 CODE
 * 3. leave_balance_cache - Leave balance data (1 day cache)
 * 4. attendance_history_cache - Attendance history (1 day cache, 3 months data) - WITH KETERANGAN
 * 5. leave_details_cache - Leave request history (1 day cache)
 * 6. delivery_list_cache - Delivery list for drivers (1 day cache, 14 days data)
 * 7. recent_menus - Recently opened menus history (persistent)
 * 
 * VERSION 2: Added md5_code field to AgentLocationEntity for backend validation
 * VERSION 3: Added keterangan field to AttendanceHistoryEntity for calendar color logic
 * VERSION 4: Added foto_base64, ttd_base64, latitude, longitude to DeliveryListEntity for local BTT drafts
 * VERSION 5: Added RecentMenuEntity for "Riwayat Menu" feature
 */
@Database(
    entities = [
        EmployeeBioEntity::class,
        AgentLocationEntity::class,
        LeaveBalanceEntity::class,
        AttendanceHistoryEntity::class,
        LeaveDetailsEntity::class,
        DeliveryListEntity::class,
        RecentMenuEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun employeeBioDao(): EmployeeBioDao
    abstract fun agentLocationDao(): AgentLocationDao
    abstract fun leaveBalanceDao(): LeaveBalanceDao
    abstract fun attendanceHistoryDao(): AttendanceHistoryDao
    abstract fun leaveDetailsDao(): LeaveDetailsDao
    abstract fun deliveryListDao(): DeliveryListDao
    abstract fun recentMenuDao(): RecentMenuDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE attendance_history_cache ADD COLUMN keterangan TEXT NOT NULL DEFAULT ''"
                )
            }
        }
        
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE delivery_list_cache ADD COLUMN foto_base64 TEXT")
                database.execSQL("ALTER TABLE delivery_list_cache ADD COLUMN ttd_base64 TEXT")
                database.execSQL("ALTER TABLE delivery_list_cache ADD COLUMN latitude TEXT")
                database.execSQL("ALTER TABLE delivery_list_cache ADD COLUMN longitude TEXT")
            }
        }
        
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `recent_menus` (`menu_id` TEXT NOT NULL, `menu_name` TEXT NOT NULL, `icon_res_id` INTEGER NOT NULL, `activity_class` TEXT NOT NULL, `last_opened` INTEGER NOT NULL, PRIMARY KEY(`menu_id`))"
                )
            }
        }
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dakota_group_staff_database"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
