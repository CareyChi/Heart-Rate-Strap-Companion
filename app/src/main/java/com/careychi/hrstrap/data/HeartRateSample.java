package com.careychi.hrstrap.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "heart_rate_sample",
        foreignKeys = @ForeignKey(entity = RecordingSession.class, parentColumns = "id", childColumns = "sessionId", onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = {"sessionId", "timestampMs"})}
)
public class HeartRateSample {
    @PrimaryKey(autoGenerate = true) public long id;
    public long sessionId;
    public long timestampMs;
    public int bpm;

    public HeartRateSample(long sessionId, long timestampMs, int bpm) {
        this.sessionId = sessionId;
        this.timestampMs = timestampMs;
        this.bpm = bpm;
    }
}
