package com.example.nallanudi;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nallanudi.data.NallaNudiDatabase;
import com.example.nallanudi.data.SeedData;
import com.example.nallanudi.data.Term;
import com.example.nallanudi.data.TermDao;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final String SUBJECT_ALL = "All";
    private static final String SCREEN_SEARCH = "search";
    private static final String SCREEN_MY_LIST = "my_list";
    private static final String SCREEN_FLASHCARDS = "flashcards";

    private final int colorBackground = Color.rgb(244, 247, 246);
    private final int colorSurface = Color.WHITE;
    private final int colorPrimary = Color.rgb(47, 111, 115);
    private final int colorPrimaryDark = Color.rgb(31, 78, 82);
    private final int colorInk = Color.rgb(31, 42, 46);
    private final int colorMuted = Color.rgb(94, 107, 112);
    private final int colorLine = Color.rgb(218, 225, 220);
    private final int colorAccent = Color.rgb(196, 92, 58);

    private NallaNudiDatabase database;
    private ExecutorService ioExecutor;
    private Handler mainHandler;
    private TextToSpeech textToSpeech;

    private LinearLayout contentRoot;
    private LinearLayout bottomNav;
    private LinearLayout resultsContainer;
    private LinearLayout wordOfDayCard;

    private String currentScreen = SCREEN_SEARCH;
    private String selectedSubject = SUBJECT_ALL;
    private String searchQuery = "";
    private int searchVersion = 0;

    private final List<Term> flashcardTerms = new ArrayList<>();
    private int flashcardIndex = 0;
    private boolean flashcardRevealed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainHandler = new Handler(Looper.getMainLooper());
        ioExecutor = Executors.newSingleThreadExecutor();
        database = NallaNudiDatabase.getInstance(this);

        Window window = getWindow();
        window.setStatusBarColor(colorBackground);
        window.setNavigationBarColor(colorBackground);

        setupTextToSpeech();
        buildShell();
        showPreparingState();
        seedGlossaryThenOpen();
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (ioExecutor != null) {
            ioExecutor.shutdownNow();
        }
        super.onDestroy();
    }

    private void setupTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS && textToSpeech != null) {
                textToSpeech.setLanguage(Locale.US);
                textToSpeech.setSpeechRate(0.85f);
            }
        });
    }

    private void buildShell() {
        LinearLayout appRoot = new LinearLayout(this);
        appRoot.setOrientation(LinearLayout.VERTICAL);
        appRoot.setBackgroundColor(colorBackground);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(false);

        contentRoot = new LinearLayout(this);
        contentRoot.setOrientation(LinearLayout.VERTICAL);
        contentRoot.setPadding(dp(18), dp(18), dp(18), dp(16));
        scrollView.addView(contentRoot, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        bottomNav = new LinearLayout(this);
        bottomNav.setOrientation(LinearLayout.HORIZONTAL);
        bottomNav.setPadding(dp(10), dp(8), dp(10), dp(10));
        bottomNav.setGravity(Gravity.CENTER);
        bottomNav.setBackgroundColor(Color.WHITE);

        appRoot.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));
        appRoot.addView(bottomNav, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        setContentView(appRoot);
    }

    private void showPreparingState() {
        contentRoot.removeAllViews();
        contentRoot.addView(label("Nalla-Nudi", 30, colorPrimaryDark, Typeface.BOLD));
        contentRoot.addView(spacer(6));
        contentRoot.addView(label("Preparing the offline glossary...", 16, colorMuted, Typeface.NORMAL));
    }

    private void seedGlossaryThenOpen() {
        ioExecutor.execute(() -> {
            TermDao dao = database.termDao();
            if (dao.count() == 0) {
                dao.insertAll(SeedData.terms());
            }
            mainHandler.post(this::showSearchScreen);
        });
    }

    private void showSearchScreen() {
        currentScreen = SCREEN_SEARCH;
        contentRoot.removeAllViews();

        contentRoot.addView(label("Nalla-Nudi", 30, colorPrimaryDark, Typeface.BOLD));
        contentRoot.addView(label("Technical Kannada bridge dictionary", 15, colorMuted, Typeface.NORMAL));
        contentRoot.addView(spacer(16));

        TextView wordTitle = label("Word of the Day", 18, colorInk, Typeface.BOLD);
        contentRoot.addView(wordTitle);
        contentRoot.addView(spacer(8));

        wordOfDayCard = verticalCard();
        wordOfDayCard.addView(label("Loading...", 15, colorMuted, Typeface.NORMAL));
        contentRoot.addView(wordOfDayCard);
        contentRoot.addView(spacer(16));
        loadWordOfDay();

        EditText searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setText(searchQuery);
        searchInput.setHint("Search: Gravity, Trigonometry, Asset");
        searchInput.setTextSize(16);
        searchInput.setTextColor(colorInk);
        searchInput.setHintTextColor(colorMuted);
        searchInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchInput.setPadding(dp(14), 0, dp(14), 0);
        searchInput.setMinHeight(dp(52));
        searchInput.setBackground(rounded(colorSurface, colorLine, 1, 8));
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString();
                loadSearchResults();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        if (!searchQuery.isEmpty()) {
            searchInput.setSelection(searchQuery.length());
        }
        contentRoot.addView(searchInput, fullWidthWrap());
        contentRoot.addView(spacer(12));

        LinearLayout filters = new LinearLayout(this);
        filters.setOrientation(LinearLayout.HORIZONTAL);
        filters.setGravity(Gravity.CENTER_VERTICAL);
        filters.addView(filterButton(SUBJECT_ALL));
        filters.addView(filterButton("Science"));
        filters.addView(filterButton("Math"));
        filters.addView(filterButton("Commerce"));
        contentRoot.addView(filters, fullWidthWrap());
        contentRoot.addView(spacer(18));

        resultsContainer = new LinearLayout(this);
        resultsContainer.setOrientation(LinearLayout.VERTICAL);
        contentRoot.addView(resultsContainer, fullWidthWrap());

        renderBottomNav();
        loadSearchResults();
    }

    private void showMyListScreen() {
        currentScreen = SCREEN_MY_LIST;
        contentRoot.removeAllViews();

        contentRoot.addView(label("My List", 28, colorPrimaryDark, Typeface.BOLD));
        contentRoot.addView(label("Daily revision words", 15, colorMuted, Typeface.NORMAL));
        contentRoot.addView(spacer(14));

        Button flashcardButton = actionButton("Start Flashcards", true);
        flashcardButton.setOnClickListener(v -> showFlashcardsScreen());
        contentRoot.addView(flashcardButton, fullWidthWrap());
        contentRoot.addView(spacer(14));

        resultsContainer = new LinearLayout(this);
        resultsContainer.setOrientation(LinearLayout.VERTICAL);
        contentRoot.addView(resultsContainer, fullWidthWrap());

        renderBottomNav();
        loadSavedTerms();
    }

    private void showFlashcardsScreen() {
        currentScreen = SCREEN_FLASHCARDS;
        contentRoot.removeAllViews();

        contentRoot.addView(label("Flashcards", 28, colorPrimaryDark, Typeface.BOLD));
        contentRoot.addView(label("Saved words revision", 15, colorMuted, Typeface.NORMAL));
        contentRoot.addView(spacer(16));

        resultsContainer = new LinearLayout(this);
        resultsContainer.setOrientation(LinearLayout.VERTICAL);
        contentRoot.addView(resultsContainer, fullWidthWrap());

        renderBottomNav();
        loadFlashcards();
    }

    private void loadWordOfDay() {
        ioExecutor.execute(() -> {
            TermDao dao = database.termDao();
            int count = dao.count();
            Term word = null;
            if (count > 0) {
                int offset = (Calendar.getInstance().get(Calendar.DAY_OF_YEAR) - 1) % count;
                word = dao.termAtOffset(offset);
            }
            Term finalWord = word;
            mainHandler.post(() -> renderWordOfDay(finalWord));
        });
    }

    private void renderWordOfDay(Term term) {
        if (wordOfDayCard == null) {
            return;
        }
        wordOfDayCard.removeAllViews();
        if (term == null) {
            wordOfDayCard.addView(label("Word of the Day will appear after setup.", 15, colorMuted, Typeface.NORMAL));
            return;
        }

        wordOfDayCard.addView(label(term.englishTerm, 24, colorInk, Typeface.BOLD));
        wordOfDayCard.addView(label(term.pronunciation, 14, colorAccent, Typeface.BOLD));
        wordOfDayCard.addView(spacer(6));
        wordOfDayCard.addView(label(term.kannadaTerm, 19, colorPrimaryDark, Typeface.BOLD));
        wordOfDayCard.addView(label(term.exampleKannada, 15, colorMuted, Typeface.NORMAL));
        wordOfDayCard.addView(spacer(10));

        Button hearButton = actionButton("Hear Pronunciation", false);
        hearButton.setOnClickListener(v -> speak(term.englishTerm));
        wordOfDayCard.addView(hearButton, fullWidthWrap());
    }

    private void loadSearchResults() {
        if (resultsContainer == null) {
            return;
        }
        int version = ++searchVersion;
        String query = searchQuery.trim();
        String subject = selectedSubject;
        resultsContainer.removeAllViews();
        resultsContainer.addView(label("Searching...", 15, colorMuted, Typeface.NORMAL));

        ioExecutor.execute(() -> {
            TermDao dao = database.termDao();
            List<Term> terms = query.isEmpty()
                    ? dao.bySubject(subject)
                    : dao.search(query, subject);
            mainHandler.post(() -> {
                if (version == searchVersion && SCREEN_SEARCH.equals(currentScreen)) {
                    renderTermList(terms, false);
                }
            });
        });
    }

    private void loadSavedTerms() {
        resultsContainer.removeAllViews();
        resultsContainer.addView(label("Loading saved words...", 15, colorMuted, Typeface.NORMAL));
        ioExecutor.execute(() -> {
            List<Term> terms = database.termDao().savedTerms();
            mainHandler.post(() -> {
                if (SCREEN_MY_LIST.equals(currentScreen)) {
                    renderTermList(terms, true);
                }
            });
        });
    }

    private void loadFlashcards() {
        resultsContainer.removeAllViews();
        resultsContainer.addView(label("Loading flashcards...", 15, colorMuted, Typeface.NORMAL));
        ioExecutor.execute(() -> {
            List<Term> terms = database.termDao().savedTerms();
            mainHandler.post(() -> {
                if (!SCREEN_FLASHCARDS.equals(currentScreen)) {
                    return;
                }
                flashcardTerms.clear();
                flashcardTerms.addAll(terms);
                if (flashcardIndex >= flashcardTerms.size()) {
                    flashcardIndex = 0;
                }
                flashcardRevealed = false;
                renderFlashcard();
            });
        });
    }

    private void renderTermList(List<Term> terms, boolean savedOnly) {
        resultsContainer.removeAllViews();
        if (terms.isEmpty()) {
            String emptyMessage = savedOnly
                    ? "No saved words yet. Save terms from Search."
                    : "No matching term found. Try another word or subject.";
            resultsContainer.addView(emptyState(emptyMessage));
            return;
        }

        TextView count = label(terms.size() + " terms", 14, colorMuted, Typeface.BOLD);
        resultsContainer.addView(count);
        resultsContainer.addView(spacer(8));

        for (Term term : terms) {
            resultsContainer.addView(termCard(term, savedOnly));
            resultsContainer.addView(spacer(10));
        }
    }

    private View termCard(Term term, boolean savedOnly) {
        LinearLayout card = verticalCard();

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = label(term.englishTerm, 22, colorInk, Typeface.BOLD);
        topRow.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        topRow.addView(chip(term.subject));
        card.addView(topRow);

        card.addView(label(term.pronunciation, 14, colorAccent, Typeface.BOLD));
        card.addView(spacer(6));
        card.addView(label(term.kannadaTerm, 19, colorPrimaryDark, Typeface.BOLD));
        card.addView(label(term.explanationKannada, 15, colorInk, Typeface.NORMAL));
        card.addView(spacer(6));
        card.addView(label("ಉದಾಹರಣೆ: " + term.exampleKannada, 14, colorMuted, Typeface.NORMAL));
        card.addView(spacer(12));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);

        Button saveButton = actionButton(savedOnly || term.isSaved ? "Remove" : "Save", false);
        saveButton.setOnClickListener(v -> updateSaved(term, !(savedOnly || term.isSaved)));

        Button hearButton = actionButton("Hear", true);
        hearButton.setOnClickListener(v -> speak(term.englishTerm));

        actions.addView(saveButton, weightedActionParams());
        actions.addView(spacerHorizontal(8));
        actions.addView(hearButton, weightedActionParams());
        card.addView(actions, fullWidthWrap());

        return card;
    }

    private void updateSaved(Term term, boolean saved) {
        ioExecutor.execute(() -> {
            database.termDao().setSaved(term.id, saved);
            mainHandler.post(() -> {
                String message = saved ? "Saved to My List" : "Removed from My List";
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                if (SCREEN_SEARCH.equals(currentScreen)) {
                    loadSearchResults();
                } else if (SCREEN_MY_LIST.equals(currentScreen)) {
                    loadSavedTerms();
                } else if (SCREEN_FLASHCARDS.equals(currentScreen)) {
                    loadFlashcards();
                }
            });
        });
    }

    private void renderFlashcard() {
        resultsContainer.removeAllViews();
        if (flashcardTerms.isEmpty()) {
            resultsContainer.addView(emptyState("Save difficult words first, then revise them here."));
            return;
        }

        Term term = flashcardTerms.get(flashcardIndex);
        TextView progress = label(
                (flashcardIndex + 1) + " of " + flashcardTerms.size(),
                14,
                colorMuted,
                Typeface.BOLD
        );
        resultsContainer.addView(progress);
        resultsContainer.addView(spacer(10));

        LinearLayout card = verticalCard();
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setMinimumHeight(dp(260));
        card.setOnClickListener(v -> {
            flashcardRevealed = !flashcardRevealed;
            renderFlashcard();
        });

        card.addView(label(term.englishTerm, 30, colorInk, Typeface.BOLD));
        card.addView(label(term.pronunciation, 15, colorAccent, Typeface.BOLD));
        card.addView(spacer(18));

        if (flashcardRevealed) {
            card.addView(label(term.kannadaTerm, 24, colorPrimaryDark, Typeface.BOLD));
            card.addView(spacer(8));
            card.addView(label(term.explanationKannada, 17, colorInk, Typeface.NORMAL));
            card.addView(spacer(8));
            card.addView(label(term.exampleKannada, 15, colorMuted, Typeface.NORMAL));
        } else {
            TextView reveal = label("Meaning hidden", 16, colorMuted, Typeface.NORMAL);
            reveal.setGravity(Gravity.CENTER);
            card.addView(reveal);
        }

        resultsContainer.addView(card);
        resultsContainer.addView(spacer(14));

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);

        Button previous = actionButton("Previous", false);
        previous.setOnClickListener(v -> moveFlashcard(-1));
        Button reveal = actionButton(flashcardRevealed ? "Hide" : "Reveal", true);
        reveal.setOnClickListener(v -> {
            flashcardRevealed = !flashcardRevealed;
            renderFlashcard();
        });
        Button next = actionButton("Next", false);
        next.setOnClickListener(v -> moveFlashcard(1));

        controls.addView(previous, weightedActionParams());
        controls.addView(spacerHorizontal(8));
        controls.addView(reveal, weightedActionParams());
        controls.addView(spacerHorizontal(8));
        controls.addView(next, weightedActionParams());
        resultsContainer.addView(controls);
        resultsContainer.addView(spacer(10));

        Button hear = actionButton("Hear Pronunciation", true);
        hear.setOnClickListener(v -> speak(term.englishTerm));
        resultsContainer.addView(hear, fullWidthWrap());
    }

    private void moveFlashcard(int direction) {
        if (flashcardTerms.isEmpty()) {
            return;
        }
        flashcardIndex = (flashcardIndex + direction + flashcardTerms.size()) % flashcardTerms.size();
        flashcardRevealed = false;
        renderFlashcard();
    }

    private Button filterButton(String subject) {
        Button button = actionButton(subject, selectedSubject.equals(subject));
        button.setOnClickListener(v -> {
            selectedSubject = subject;
            showSearchScreen();
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(44), 1f);
        params.setMargins(0, 0, dp(7), 0);
        button.setLayoutParams(params);
        return button;
    }

    private void renderBottomNav() {
        bottomNav.removeAllViews();
        bottomNav.addView(navButton("Search", SCREEN_SEARCH), weightedActionParams());
        bottomNav.addView(spacerHorizontal(8));
        bottomNav.addView(navButton("My List", SCREEN_MY_LIST), weightedActionParams());
        bottomNav.addView(spacerHorizontal(8));
        bottomNav.addView(navButton("Cards", SCREEN_FLASHCARDS), weightedActionParams());
    }

    private Button navButton(String label, String screen) {
        Button button = actionButton(label, currentScreen.equals(screen));
        button.setOnClickListener(v -> {
            if (SCREEN_SEARCH.equals(screen)) {
                showSearchScreen();
            } else if (SCREEN_MY_LIST.equals(screen)) {
                showMyListScreen();
            } else {
                showFlashcardsScreen();
            }
        });
        return button;
    }

    private void speak(String text) {
        if (textToSpeech == null) {
            Toast.makeText(this, "Voice guide is starting. Try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "term-" + text);
    }

    private LinearLayout verticalCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackground(rounded(colorSurface, colorLine, 1, 8));
        card.setLayoutParams(fullWidthWrap());
        return card;
    }

    private TextView emptyState(String message) {
        TextView view = label(message, 16, colorMuted, Typeface.NORMAL);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(20), dp(28), dp(20), dp(28));
        view.setBackground(rounded(colorSurface, colorLine, 1, 8));
        return view;
    }

    private TextView chip(String value) {
        TextView chip = label(value, 12, colorPrimaryDark, Typeface.BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(10), dp(4), dp(10), dp(4));
        chip.setBackground(rounded(Color.rgb(229, 242, 239), Color.rgb(183, 215, 207), 1, 8));
        return chip;
    }

    private Button actionButton(String label, boolean primary) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setMinHeight(dp(44));
        button.setGravity(Gravity.CENTER);
        if (primary) {
            button.setTextColor(Color.WHITE);
            button.setBackground(rounded(colorPrimary, colorPrimary, 1, 8));
        } else {
            button.setTextColor(colorPrimaryDark);
            button.setBackground(rounded(Color.WHITE, colorLine, 1, 8));
        }
        return button;
    }

    private TextView label(String text, int sp, int color, int style) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, style);
        view.setLineSpacing(0, 1.08f);
        view.setIncludeFontPadding(true);
        return view;
    }

    private View spacer(int heightDp) {
        View view = new View(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(heightDp)
        ));
        return view;
    }

    private View spacerHorizontal(int widthDp) {
        View view = new View(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(
                dp(widthDp),
                1
        ));
        return view;
    }

    private LinearLayout.LayoutParams fullWidthWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams weightedActionParams() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    private GradientDrawable rounded(int fillColor, int strokeColor, int strokeWidthDp, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(fillColor);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(strokeWidthDp), strokeColor);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
