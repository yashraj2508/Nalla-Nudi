package com.example.nallanudi.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "terms",
        indices = {
                @Index("englishTerm"),
                @Index("subject"),
                @Index("isSaved")
        }
)
public class Term {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String englishTerm;
    public String kannadaTerm;
    public String subject;
    public String pronunciation;
    public String explanationKannada;
    public String exampleKannada;
    public boolean isSaved;

    public Term() {
    }

    @Ignore
    public Term(
            String englishTerm,
            String kannadaTerm,
            String subject,
            String pronunciation,
            String explanationKannada,
            String exampleKannada
    ) {
        this.englishTerm = englishTerm;
        this.kannadaTerm = kannadaTerm;
        this.subject = subject;
        this.pronunciation = pronunciation;
        this.explanationKannada = explanationKannada;
        this.exampleKannada = exampleKannada;
        this.isSaved = false;
    }
}
