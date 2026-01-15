# How to Update/Synchronize Translations

This guide outlines the process for managing translations in Bisq Mobile using the Transifex platform and the Transifex Client (`tx`)[^1].

## Overview

Bisq Mobile uses a custom i18n system where `.properties` files are compiled into Kotlin code at build time. Translation strings are stored in:

```
shared/domain/src/commonMain/resources/mobile/
```

### Currently Supported Languages (14 total)

| Code | Language |
|------|----------|
| `en` | English (default) |
| `af-ZA` | Afrikaans |
| `cs` | Czech |
| `de` | German |
| `es` | Spanish |
| `fr` | French |
| `hi` | Hindi |
| `id` | Indonesian |
| `it` | Italian |
| `pcm` | Nigerian Pidgin |
| `pt-BR` | Brazilian Portuguese |
| `ru` | Russian |
| `tr` | Turkish |
| `vi` | Vietnamese |

### Resource Bundle Files (15 files per language)

- `account.properties`
- `application.properties`
- `authorized_role.properties`
- `bisq_easy.properties`
- `chat.properties`
- `default.properties`
- `mobile.properties` (mobile-specific strings)
- `network.properties`
- `payment_method.properties`
- `reputation.properties`
- `settings.properties`
- `support.properties`
- `trade_apps.properties`
- `user.properties`
- `wallet.properties`

## One-Time Setup

1. **Install the Transifex Client**: Follow the instructions on the [official documentation](https://developers.transifex.com/docs/cli).
2. **Get an API token**: Generate an API token[^2] from your Transifex user profile. You must have "Project Maintainer" rights for the Bisq project to push source files and manage resources.

## Use Case 1: Syncing Existing Translations (e.g., Before a Release)

This is the most common task. It pulls the latest completed translations from Transifex into your local repository.

1. Navigate to the project root directory.
2. Pull all translations:
   ```bash
   tx pull -t
   ```
3. Regenerate the Kotlin i18n files:
   ```bash
   ./gradlew generateResourceBundles
   ```
4. Commit the updated `*_<lang>.properties` files and generated Kotlin files to the repository.

## Use Case 2: Adding a New Translatable File

When a new source file (e.g., `new_feature.properties`) is added to the project, it must be properly registered with Transifex and the repository.

### Step 1: Add the New Source File

Place your new `.properties` file in `shared/domain/src/commonMain/resources/mobile/`.

### Step 2: Update Transifex Configuration

Manually add a new resource block to the `.tx/config` file in the project's root directory. Copy an existing block and change the resource name and file paths.

*Example for `new_feature.properties`*:
```ini
[o:bisq:p:bisq-mobile:r:new_feature_properties]
file_filter            = shared/domain/src/commonMain/resources/mobile/new_feature_<lang>.properties
source_file            = shared/domain/src/commonMain/resources/mobile/new_feature.properties
type                   = UNICODEPROPERTIES
minimum_perc           = 0
resource_name          = new_feature.properties
replace_edited_strings = false
keep_translations      = false
```

### Step 3: Update build.gradle.kts

Add the new bundle name to the `bundleNames` list in `shared/domain/build.gradle.kts`:

```kotlin
val bundleNames: List<String> = listOf(
    "default",
    "application",
    // ... existing bundles ...
    "new_feature",  // <-- Add your new bundle here
)
```

### Step 4: Push the New Source File to Transifex

```bash
tx push --source
# Or using shorthand:
tx push -s
```

### Step 5: Pull Translation Files for the New Resource

```bash
# From project root directory, pull all locales for the new resource
tx pull -t --force
```

**Important**:
- The `-t` flag pulls translation files
- The `--force` flag ensures files are created even if they don't exist locally

### Step 6: Commit All Changes

Commit the following to your Git repository:
1. The new source file (e.g., `new_feature.properties`)
2. The updated `.tx/config` file
3. The updated `build.gradle.kts`
4. All the new translation files (e.g., `new_feature_de.properties`, etc.)

## Use Case 3: Adding a New Language/Locale

When you want to add a completely new language to Bisq Mobile (e.g., French, Japanese), you need to update both the codebase and Transifex configuration.

### Step 1: Add Language to Transifex

1. Log in to Transifex at <https://app.transifex.com/>
2. Navigate to the Bisq Mobile project
3. Go to **Languages** settings
4. Click **Add Language** and select the target language
5. Confirm the addition

**Note**: You must have "Project Maintainer" rights to add new languages.

### Step 2: Update I18nSupport.kt

Add the new language code to the `setLanguage()` function in `shared/domain/src/commonMain/kotlin/network/bisq/mobile/i18n/I18nSupport.kt`:

```kotlin
fun setLanguage(languageCode: String = "en") {
    currentLanguage = languageCode
    val bundleMapsByName: Map<String, Map<String, String>> =
        when (languageCode) {
            "en" -> GeneratedResourceBundles_en.bundles
            "af-ZA" -> GeneratedResourceBundles_af_ZA.bundles
            "cs" -> GeneratedResourceBundles_cs.bundles
            "de" -> GeneratedResourceBundles_de.bundles
            "es" -> GeneratedResourceBundles_es.bundles
            "fr" -> GeneratedResourceBundles_fr.bundles  // <-- Add new language
            "it" -> GeneratedResourceBundles_it.bundles
            "pcm" -> GeneratedResourceBundles_pcm.bundles
            "pt-BR" -> GeneratedResourceBundles_pt_BR.bundles
            "ru" -> GeneratedResourceBundles_ru.bundles
            else -> GeneratedResourceBundles_en.bundles
        }
    bundles = bundleMapsByName.values.map { ResourceBundle(it) }
}
```

### Step 3: Update build.gradle.kts

Add the new language code to the `languageCodes` list in `shared/domain/build.gradle.kts`:

```kotlin
val languageCodes = listOf("en", "af_ZA", "cs", "de", "es", "fr", "hi", "id", "it", "pcm", "pt_BR", "ru", "tr", "vi")
//                                                          ^^^ Add new language
```

**Note**: Use underscores (`_`) in the list, not hyphens. The build system handles conversion.

### Step 4: Update the Batch Script (Recommended)

Update `scripts/generate_transifex_batches.py` to include the new locale:

```python
ALL_LOCALES = [
    "af_ZA", "cs", "de", "es", "fr", "hi", "id", "it", "pcm", "pt_BR", "ru", "tr", "vi"
    #                        ^^^ Add new locale
]
```

**Why this step is optional for CI/CD but recommended:**

The CI/CD workflow **dynamically detects** changed locales from git diff - it does not rely on the `ALL_LOCALES` list. When translation files are modified and merged to main, the workflow:

1. Scans changed files to find translation files (e.g., `*_fr.properties`)
2. Extracts locale codes from filenames
3. Passes those locales to the batch script via `--locales "fr,de,..."` flag

This means **CI/CD will work correctly** even without updating `ALL_LOCALES`.

However, updating `ALL_LOCALES` is **recommended** because it enables:
- Manual batch generation: `python scripts/generate_transifex_batches.py` (without `--locales`)
- Statistics and planning: Preview batch configurations for all supported locales
- Priority tier grouping: When using `--priority` flag to prioritize certain locales

### Step 5: Pull Translation Files for New Locale

```bash
# From project root directory
tx pull -t --force -l fr
```

This creates all translation files with English values as placeholders.

### Step 6: Translate the Files

You have several options:

#### Option A: Use AI Translation Tools (Recommended for initial translations)
Use Claude Code or other AI translation services to generate initial translations.

#### Option B: Manual Translation via Transifex
```bash
# Push the source files
tx push -s

# Translators work in the Transifex web interface
# Pull completed translations when ready
tx pull -t -l fr
```

#### Option C: Manual Translation Locally
- Open each `*_fr.properties` file and replace English values with translations
- Preserve all placeholders like `{0}`, `{1}`, etc.
- Maintain multi-line continuation format with `\n\`

### Step 7: Generate Kotlin i18n Files

```bash
./gradlew generateResourceBundles
```

This generates the `GeneratedResourceBundles_fr.kt` file.

### Step 8: Verify and Test

```bash
# Build the project
./gradlew clean build

# Run tests
./gradlew test
```

### Step 9: Commit All Changes

Commit:
1. Updated `I18nSupport.kt` with the new language case
2. Updated `build.gradle.kts` with the new language code
3. All translation files for the new language
4. Generated `GeneratedResourceBundles_XX.kt` file
5. Updated batch script (if modified)

## CI/CD: Automatic Transifex Sync

The GitHub Actions workflow `.github/workflows/sync_transifex.yml` automatically:

1. **Verifies** that `.tx/config` matches the actual source files
2. **Pushes** changed source files to Transifex when merged to main
3. **Pushes** changed translation files using intelligent batching

### Batching System

To prevent Transifex API rate limits, translation uploads are batched:

- **Batch size**: 4 locales per batch
- **Max parallel**: 2 concurrent batches
- **Rate limiting**: 5-second delay between locales within a batch
- **Retry logic**: 3 attempts with exponential backoff

The batching script (`scripts/generate_transifex_batches.py`) can be used to preview batch configurations:

```bash
# Preview batch configuration
python scripts/generate_transifex_batches.py --batch-size 4 --max-parallel 2

# Generate JSON for specific locales
python scripts/generate_transifex_batches.py --locales "de,es,ru" --json
```

## File Naming Conventions

- **Source files**: `<bundle>.properties` (e.g., `mobile.properties`)
- **Translation files**: `<bundle>_<lang>.properties` (e.g., `mobile_de.properties`)
- **Language codes with regions**: Use underscores in filenames (e.g., `mobile_pt_BR.properties`)
- **Language codes in Java/Kotlin**: Use hyphens (e.g., `pt-BR`)

## Troubleshooting

### "GeneratedResourceBundles_XX not found" Error

The Kotlin i18n files need to be regenerated:
```bash
./gradlew generateResourceBundles
```

### Translation Files Not Loading

1. Check that the language code in `I18nSupport.kt` matches exactly
2. Verify the file exists in the resources directory
3. Ensure `build.gradle.kts` includes the language code

### Transifex Push/Pull Failures

1. Verify your TX_TOKEN is set correctly
2. Check that `.tx/config` paths match the actual file locations
3. Run verification: The CI workflow will detect mismatches

## References

[^1]: [Transifex CLI Documentation](https://developers.transifex.com/docs/cli)
[^2]: [Transifex API Authentication](https://developers.transifex.com/reference/api-authentication)
