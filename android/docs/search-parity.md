# Search and link parity evidence

Historical measurements from the Android port, retained from the former agent
testing document. They were not rerun during this documentation reorganization;
the pair counts describe that captured corpus, not the current test total.
Current executable coverage lives in `SearchMatcherTest`, `LinkTextParserTest`
and `HttpsUrlsTest`.

## How the collation rules were established

Not by reading a specification. A probe ran `CompareInfo.Compare` under
`IgnoreCase | IgnoreNonSpace` over **every BMP code point** and over each
boundary case, and the implementation follows what came back:

- Two Unicode categories are ignorable in full (`Format`, `EnclosingMark`) and
  one nearly so (`Control`, except the six that collate as whitespace).
  `NonSpacingMark` is *not* — 439 of 1066 carry weight — so dropping the
  category is slightly over-broad, and correct for every Latin-script language
  the launcher ships.
- The middle dot of a Catalan `l·l` is a **contraction**, not an ignorable
  character: `l·l` equals `ll` while `a·b` does not equal `ab`. Its condition is
  the **left neighbour alone** — swept over every ordered pair of a 39-character
  alphabet, the dot is dropped in 39 of 1521, exactly those preceded by `l` or
  `L`, whatever follows and even at the end of the string. Two wrong rules are
  easy to reach from here and both were: dropping the dot unconditionally
  (`a·b` would match `ab`), and requiring an `l` on both sides, which silently
  blanks the FAQ list on the keystroke where a Catalan user has typed `instal·`.
  The neighbour is read literally, not through the folding — an accent between
  the `l` and the dot blocks the contraction on the desktop too.
- A match extends over a trailing character only when it belongs to the same
  grapheme — combining marks and the two joiners, not a soft hyphen, a
  byte-order mark, a word joiner or a control.

The result was then checked end to end rather than assumed. A differential ran
both implementations — .NET's `CompareInfo` and the **compiled** Kotlin class,
not a re-implementation of it — over **328,051 (text, term) pairs** in three
corpora, all drawn from the eight catalogues, the FAQ entries and both captured
CDN payloads. Zero disagreements in all three.

| Corpus | Pairs | Matching |
| --- | --- | --- |
| Terms derived from each text | 64,736 | 64,736 |
| Cross product, so most pairs are negatives | 208,962 | 2,047 |
| **Every prefix of every word**, plus a sweep of both neighbours of a middle dot | 54,353 | 46,161 |

The third corpus is there because the first two could not have caught the
both-sides bug: no shipped string contains `l` + dot + non-`l`, so only a
half-typed search term produces one. A corpus built from whole words is not a
corpus of what a keystroke filter is handed.

The same was done for `LinkTextParser` over 912 texts: zero disagreements on the
shipped corpus, and three on deliberately awkward synthetic URLs, all of them in
the link *target* rather than in whether the text is a link, and all three
pinned by `HttpsUrlsTest`.

## Scope

These measurements explain why the implementation includes particular Unicode
and partial-word rules. They do not prove equality over all Unicode, new payloads
or later .NET/Android runtime versions. Re-run differential checks for a change
to collation behavior. A JVM regex test also does not prove ICU compatibility
on a device.
