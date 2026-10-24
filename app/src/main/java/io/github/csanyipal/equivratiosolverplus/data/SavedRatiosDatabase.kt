/*
 * Copyright 2026 Csányi Pál
 *
 * This file is part of EquivRatioSolverPlus.
 *
 * EquivRatioSolverPlus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 */

package io.github.csanyipal.equivratiosolverplus.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class SavedRatio(
    val name: String,
    val inverse: Boolean,
    val values: List<String>,
    val calculatedIndex: Int,
    val yComments: String,
    val xComments: String,
    val operationVariableIndex: Int,
    val operationIndex: Int,
    val operationOperand: String,
    val operationResult: Double?,
    val operationNote: String
)

class SavedRatiosDatabase(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(database: SQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_NAME TEXT PRIMARY KEY NOT NULL,
                $COLUMN_INVERSE INTEGER NOT NULL,
                $COLUMN_Y1 TEXT NOT NULL,
                $COLUMN_X1 TEXT NOT NULL,
                $COLUMN_Y2 TEXT NOT NULL,
                $COLUMN_X2 TEXT NOT NULL,
                $COLUMN_CALCULATED_INDEX INTEGER NOT NULL,
                $COLUMN_Y_COMMENTS TEXT,
                $COLUMN_X_COMMENTS TEXT,
                $COLUMN_OPERATION_VARIABLE INTEGER NOT NULL,
                $COLUMN_OPERATION_INDEX INTEGER NOT NULL,
                $COLUMN_OPERATION_OPERAND TEXT,
                $COLUMN_OPERATION_RESULT REAL,
                $COLUMN_OPERATION_NOTE TEXT
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun insert(ratio: SavedRatio) {
        require(ratio.values.size == 4)
        require(ratio.calculatedIndex in 0..3)
        require(ratio.operationVariableIndex in 0..3)
        require(ratio.operationIndex in 0..3)
        writableDatabase.insertOrThrow(
            TABLE_NAME,
            null,
            ContentValues().apply {
                put(COLUMN_NAME, ratio.name)
                put(COLUMN_INVERSE, if (ratio.inverse) 1 else 0)
                put(COLUMN_Y1, ratio.values[0])
                put(COLUMN_X1, ratio.values[1])
                put(COLUMN_Y2, ratio.values[2])
                put(COLUMN_X2, ratio.values[3])
                put(COLUMN_CALCULATED_INDEX, ratio.calculatedIndex)
                putOptional(COLUMN_Y_COMMENTS, ratio.yComments)
                putOptional(COLUMN_X_COMMENTS, ratio.xComments)
                put(COLUMN_OPERATION_VARIABLE, ratio.operationVariableIndex)
                put(COLUMN_OPERATION_INDEX, ratio.operationIndex)
                putOptional(COLUMN_OPERATION_OPERAND, ratio.operationOperand)
                ratio.operationResult?.let { put(COLUMN_OPERATION_RESULT, it) }
                    ?: putNull(COLUMN_OPERATION_RESULT)
                putOptional(COLUMN_OPERATION_NOTE, ratio.operationNote)
            }
        )
    }

    fun getAll(): List<SavedRatio> {
        val result = mutableListOf<SavedRatio>()
        readableDatabase.query(
            TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_NAME COLLATE NOCASE ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result += SavedRatio(
                    name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                    inverse = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_INVERSE)) != 0,
                    values = listOf(
                        COLUMN_Y1,
                        COLUMN_X1,
                        COLUMN_Y2,
                        COLUMN_X2
                    ).map { cursor.getString(cursor.getColumnIndexOrThrow(it)) },
                    calculatedIndex = cursor.getInt(
                        cursor.getColumnIndexOrThrow(COLUMN_CALCULATED_INDEX)
                    ),
                    yComments = cursor.getNullableString(COLUMN_Y_COMMENTS).orEmpty(),
                    xComments = cursor.getNullableString(COLUMN_X_COMMENTS).orEmpty(),
                    operationVariableIndex = cursor.getInt(
                        cursor.getColumnIndexOrThrow(COLUMN_OPERATION_VARIABLE)
                    ),
                    operationIndex = cursor.getInt(
                        cursor.getColumnIndexOrThrow(COLUMN_OPERATION_INDEX)
                    ),
                    operationOperand = cursor.getNullableString(COLUMN_OPERATION_OPERAND).orEmpty(),
                    operationResult = cursor.getNullableString(COLUMN_OPERATION_RESULT)?.toDoubleOrNull(),
                    operationNote = cursor.getNullableString(COLUMN_OPERATION_NOTE).orEmpty()
                )
            }
        }
        return result
    }

    private fun ContentValues.putOptional(key: String, value: String) {
        if (value.isEmpty()) putNull(key) else put(key, value)
    }

    private fun android.database.Cursor.getNullableString(column: String): String? {
        val index = getColumnIndexOrThrow(column)
        return if (isNull(index)) null else getString(index)
    }

    private companion object {
        const val DATABASE_NAME = "saved_ratios.db"
        const val DATABASE_VERSION = 1
        const val TABLE_NAME = "saved_ratios"
        const val COLUMN_NAME = "name"
        const val COLUMN_INVERSE = "inverse"
        const val COLUMN_Y1 = "y1"
        const val COLUMN_X1 = "x1"
        const val COLUMN_Y2 = "y2"
        const val COLUMN_X2 = "x2"
        const val COLUMN_CALCULATED_INDEX = "calculated_index"
        const val COLUMN_Y_COMMENTS = "y_comments"
        const val COLUMN_X_COMMENTS = "x_comments"
        const val COLUMN_OPERATION_VARIABLE = "operation_variable"
        const val COLUMN_OPERATION_INDEX = "operation_index"
        const val COLUMN_OPERATION_OPERAND = "operation_operand"
        const val COLUMN_OPERATION_RESULT = "operation_result"
        const val COLUMN_OPERATION_NOTE = "operation_note"
    }
}
