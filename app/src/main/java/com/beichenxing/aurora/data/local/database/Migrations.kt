package com.beichenxing.aurora.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE characters ADD COLUMN raw_json TEXT NOT NULL DEFAULT ''"
        )
        database.execSQL(
            "ALTER TABLE characters ADD COLUMN spec TEXT NOT NULL DEFAULT ''"
        )
        database.execSQL(
            "ALTER TABLE characters ADD COLUMN spec_version TEXT NOT NULL DEFAULT ''"
        )
    }
}