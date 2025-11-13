package com.example.hikenativeapp.data.local;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

import com.example.hikenativeapp.data.local.dao.HikeDao;
import com.example.hikenativeapp.data.local.dao.ObservationDao;
import com.example.hikenativeapp.data.local.dao.UserDao;
import com.example.hikenativeapp.data.local.entity.Hike;
import com.example.hikenativeapp.data.local.entity.Observation;
import com.example.hikenativeapp.data.local.entity.User;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(
    entities = {User.class, Hike.class, Observation.class},
    version = 10,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "hike_database";
    private static volatile AppDatabase INSTANCE;

    // Define a database write executor
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(4);

    // Abstract methods to get DAOs
    public abstract UserDao userDao();
    public abstract HikeDao hikeDao();
    public abstract ObservationDao observationDao();

    // Singleton pattern to get database instance
    public static AppDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    )
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }

    // Method to close database
    public static void destroyInstance() {
        INSTANCE = null;
    }
}
