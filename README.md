# Nalla-Nudi

Nalla-Nudi is an offline Android bridge-dictionary for Kannada-medium students who are moving into English technical subjects. Students can search English scientific, mathematical, and commerce terms, read simple Kannada explanations, hear English pronunciation through Android Text-To-Speech, and save difficult words for flashcard revision.

## What Is Included

- Native Android app in Java.
- Room database stored fully on-device.
- First-run glossary seed with Science, Math, and Commerce terms.
- Indexed search by English term, Kannada term, explanation, and subject.
- Subject filters: All, Science, Math, Commerce.
- My List for saved difficult words.
- Flashcard revision mode for saved words.
- Word of the Day.
- Android Text-To-Speech voice guide for English pronunciation.
- No internet permission, so the app works offline after installation.

## Project Structure

```text
NallaNudi/
  app/
    src/main/
      AndroidManifest.xml
      java/com/example/nallanudi/
        MainActivity.java
        data/
          NallaNudiDatabase.java
          SeedData.java
          Term.java
          TermDao.java
      res/values/
        strings.xml
        styles.xml
  build.gradle
  settings.gradle
```

## Run In Android Studio

1. Install Android Studio.
2. Open Android Studio and choose `Open`.
3. Select this project folder: `/home/akaza/Yash`.
4. Wait for Gradle Sync to finish.
5. If Android Studio asks to install missing SDK packages, accept it. The project uses:
   - `compileSdk 35`
   - `minSdk 23`
   - Android Gradle Plugin `8.7.3`
   - Room `2.6.1`
6. In `Settings > Build, Execution, Deployment > Build Tools > Gradle`, choose the embedded Android Studio JDK if prompted.
7. Create or start an emulator with Android 6.0/API 23 or newer, or connect a physical Android phone with USB debugging enabled.
8. Press `Run`.
9. Search terms such as `Gravity`, `Photosynthesis`, `Trigonometry`, or `Asset`.
10. Tap `Save` on difficult terms, open `My List`, then use `Cards` for flashcard revision.
11. Tap `Hear` or `Hear Pronunciation` to test the voice guide.

## If Gradle Wrapper Is Requested

This repository is intentionally small and does not include a generated `gradlew` wrapper jar. Android Studio can open and sync the Gradle project directly. If your Android Studio setup specifically requires a wrapper, open the project once, then run this from the project root after Gradle is available:

```bash
gradle wrapper --gradle-version 8.10.2
```

Then reopen or sync the project.

## Offline Behavior

The app requests no network permission. Glossary data is inserted into Room on first launch from `SeedData.java`, and saved words remain in the local `nalla_nudi.db` database.

## Performance Notes

Search is designed to stay under 200ms for the included glossary:

- The Room table has indexes on `englishTerm`, `subject`, and `isSaved`.
- Queries run on a background executor.
- Results are limited to 80 rows.
- The UI stays responsive while search work happens off the main thread.

For a larger production glossary, move the seed into a prepackaged Room database asset or import from CSV during a build step.
