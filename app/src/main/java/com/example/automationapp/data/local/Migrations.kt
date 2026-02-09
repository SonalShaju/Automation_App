package com.example.automationapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Migration from version 1 to version 2
// Adds last_executed_at column to automation_rules table
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE automation_rules ADD COLUMN last_executed_at INTEGER"
        )
    }
}

// Migration from version 2 to version 3
// Adds conditions table for AND logic support
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create the conditions table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS conditions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                rule_id INTEGER NOT NULL,
                type TEXT NOT NULL,
                parameters TEXT NOT NULL,
                is_active INTEGER NOT NULL DEFAULT 1,
                created_at INTEGER NOT NULL,
                FOREIGN KEY (rule_id) REFERENCES automation_rules(id) ON DELETE CASCADE ON UPDATE CASCADE
            )
        """.trimIndent())

        // Create indexes for conditions table
        database.execSQL("CREATE INDEX IF NOT EXISTS index_conditions_rule_id ON conditions(rule_id)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_conditions_type ON conditions(type)")
    }
}

// Migration from version 3 to version 4
// Adds favorite_locations table for quick location selection
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create the favorite_locations table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS favorite_locations (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                radius INTEGER NOT NULL DEFAULT 100,
                created_at INTEGER NOT NULL
            )
        """.trimIndent())

        // Create unique index on name
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_favorite_locations_name ON favorite_locations(name)")
    }
}

