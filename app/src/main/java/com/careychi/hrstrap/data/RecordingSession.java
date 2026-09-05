package com.careychi.hrstrap.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "recording_session")
public class RecordingSession {
    @PrimaryKey(autoGenerate = true) public long id;
    public String type;
    public long startTimeMs;
    public long endTimeMs;
    public long durationMs;
    public int maxBpm;
    public int avgBpm;

    public RecordingSession(String type, long startTimeMs, long endTimeMs, long durationMs, int maxBpm, int avgBpm) {
        this.type = type;
        this.startTimeMs = startTimeMs;
        this.endTimeMs = endTimeMs;
        this.durationMs = durationMs;
        this.maxBpm = maxBpm;
        this.avgBpm = avgBpm;
    }
}
