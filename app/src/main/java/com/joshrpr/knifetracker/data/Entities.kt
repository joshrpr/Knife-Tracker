package com.joshrpr.knifetracker.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "knives")
data class Knife(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val maker: String = "",
    val steel: String = "",
    /** Preferred sharpening angle in degrees per side, if the owner has one. */
    val targetAngle: Float? = null,
    val notes: String = "",
    /** Absolute path to a photo in app-private storage. */
    val photoPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "sharpenings",
    foreignKeys = [
        ForeignKey(
            entity = Knife::class,
            parentColumns = ["id"],
            childColumns = ["knifeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("knifeId")],
)
data class Sharpening(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val knifeId: Long,
    /** Angle in degrees per side. */
    val angle: Float,
    /** Stones, grits, or system used, e.g. "1000/6000 whetstone". */
    val method: String = "",
    val notes: String = "",
    val sharpenedAt: Long = System.currentTimeMillis(),
)

/** A knife plus its most recent sharpening, for the list screen. */
data class KnifeSummary(
    @Embedded val knife: Knife,
    @ColumnInfo(name = "lastAngle") val lastAngle: Float?,
    @ColumnInfo(name = "lastSharpenedAt") val lastSharpenedAt: Long?,
)
