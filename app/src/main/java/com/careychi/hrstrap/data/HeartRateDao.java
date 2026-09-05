package com.careychi.hrstrap.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface HeartRateDao {
    @Insert long insertSession(RecordingSession session);
    @Insert void insertSample(HeartRateSample sample);
    @Update void updateSession(RecordingSession session);

    @Query("SELECT * FROM recording_session ORDER BY startTimeMs DESC")
    List<RecordingSession> getSessions();

    @Query("SELECT * FROM recording_session WHERE id = :id LIMIT 1")
    RecordingSession getSession(long id);

    @Query("SELECT * FROM heart_rate_sample WHERE sessionId = :sessionId ORDER BY timestampMs ASC")
    List<HeartRateSample> getSamples(long sessionId);

    @Query("SELECT * FROM heart_rate_sample WHERE sessionId = :sessionId AND timestampMs >= :fromMs ORDER BY timestampMs ASC")
    List<HeartRateSample> getSamplesSince(long sessionId, long fromMs);
}
