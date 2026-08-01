# GentleInk test EPUBs

Sample ebooks for verifying the Calibre plugin (or Android app) filters profanity correctly without breaking innocent words.

## Download

| File | Description |
|---|---|
| [`gentleink-filter-test.epub`](gentleink-filter-test.epub) | Structured checklist — compound words, safe context, and profane usage in labeled chapters |
| [`gentleink-filter-test-story.epub`](gentleink-filter-test-story.epub) | **Recommended.** Short novella (*The Brass Lantern*) with natural dialogue; profanity is **highlighted in yellow** so you can spot what should change after cleaning |

From a GitHub release, both files are also attached as `gentleink-filter-test*.epub`.

Raw links (replace `main` with a release tag if you prefer a pinned version):

- https://github.com/drewnicorn-git/gentleink/raw/main/calibre-plugin/test-fixtures/gentleink-filter-test.epub
- https://github.com/drewnicorn-git/gentleink/raw/main/calibre-plugin/test-fixtures/gentleink-filter-test-story.epub

## How to test in Calibre

1. **Add books** → select one of the EPUBs above.
2. Select the book → click **GentleInk Clean**.
3. Open the cleaned book and verify:
   - **Should change:** highlighted (story) or labeled profane (checklist) words — e.g. `hell`, `shit`, profane `ass`.
   - **Should stay:** `bass`, `assassin`, `classic`, `Scunthorpe`, donkey `ass`, rooster `cock`, `suck air`, `he'll`, `don't`, etc.

Substitute mode (Family profile) typically yields `heck`, `poop`, `butt` for the profane hits.

## Regenerate locally

```powershell
powershell -ExecutionPolicy Bypass -File build_test_epub.ps1
powershell -ExecutionPolicy Bypass -File build_story_test_epub.ps1
```

Python (if installed): `python build_test_epub.py`
