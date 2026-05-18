package com.example.nallanudi.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Term.class}, version = 1, exportSchema = false)
public abstract class NallaNudiDatabase extends RoomDatabase {
    private static volatile NallaNudiDatabase instance;

    public abstract TermDao termDao();

    public static NallaNudiDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (NallaNudiDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    NallaNudiDatabase.class,
                                    "nalla_nudi.db"
                            )
                            .build();
                }
            }
        }
        return instance;
    }
}
