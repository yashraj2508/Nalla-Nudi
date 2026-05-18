package com.example.nallanudi.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TermDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Term> terms);

    @Query("SELECT COUNT(*) FROM terms")
    int count();

    @Query("SELECT * FROM terms " +
            "WHERE (:subject = 'All' OR subject = :subject) " +
            "ORDER BY englishTerm " +
            "LIMIT 80")
    List<Term> bySubject(String subject);

    @Query("SELECT * FROM terms " +
            "WHERE (:subject = 'All' OR subject = :subject) " +
            "AND (englishTerm LIKE '%' || :query || '%' COLLATE NOCASE " +
            "OR kannadaTerm LIKE '%' || :query || '%' " +
            "OR explanationKannada LIKE '%' || :query || '%') " +
            "ORDER BY englishTerm " +
            "LIMIT 80")
    List<Term> search(String query, String subject);

    @Query("UPDATE terms SET isSaved = :saved WHERE id = :id")
    void setSaved(int id, boolean saved);

    @Query("SELECT * FROM terms WHERE isSaved = 1 ORDER BY englishTerm")
    List<Term> savedTerms();

    @Query("SELECT * FROM terms ORDER BY id LIMIT 1 OFFSET :offset")
    Term termAtOffset(int offset);
}
