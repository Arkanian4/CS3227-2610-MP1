# User Guide

## Setup

Install JDK 21 or later. From the project root, run:

```powershell
.\gradlew.bat run
```

Run the tests with `./gradlew.bat test`.

The application opens maximized for desktop tournament use. Pool and Direct Elimination bouts are
selectable immediately; resizing the window is not required before recording a result.

## Tournament Home

Tournament Home lists ongoing tournaments before completed tournaments. Ongoing tournaments are
ordered by their latest saved change and show quiet `Updated …` metadata; recent changes use a
relative time while older changes show a concise date. Completed tournaments are ordered by their
completion time and instead show `Completed …`. Opening, viewing, or navigating through a
tournament does not change either time. While Tournament Home is open, relative update labels
refresh approximately once a minute without changing tournament data. Choose **Open** to continue
an ongoing tournament or **View Results** to inspect a completed one.

Each tournament row has a compact **⋯** menu containing the secondary **Delete** action. Deleting requires confirmation and
permanently removes that tournament, including its fencers, pool results, Direct Elimination
results, and final standings. It cannot be undone. Deleting one tournament does not change any
other tournament.

Tournament names must be non-blank and unique. Any name error is shown directly below the name
field, while the entered text remains available for correction.

Choose **+ New Tournament**, enter a name, and choose **Create**. Names are trimmed and unique
without regard to letter case. For example, `Club Open` and ` club open ` cannot both be created.

When a tournament is open, use the connected stage tracker below the header to move between
available competition stages. Completed stages show filled teal circles, the current competition
stage uses a larger filled teal circle with an accent ring, and unavailable later stages remain muted hollow
circles. The header shows the current
tournament and phase; **Tournament Home** remains available in the sidebar.

At the bottom of the sidebar, choose a circular swatch under **Colour theme** to switch between
Emerald & Teal, Deep Navy & Electric Blue, Royal Purple & Violet, and Ice Blue & Cerulean. The
choice applies immediately and is remembered when the application is reopened. Tournament
results and win/loss colours are unaffected.

Use the compact **Light/Dark** switch above the colour swatches to set the neutral appearance
independently. The selected appearance changes immediately and is remembered when the app is
reopened.

## Setup

**Setup** combines registration and initial seeding in one workspace. Add fencers by display name
using the compact **Fencer name** entry row; pressing Enter adds the name too. Names must be
non-blank and unique within the tournament. A successful addition appears at the bottom of the
single numbered **Seed order** list.

Drag a row using its handle to insert it at a new position. For a longer roster, hold the dragged
row near the top or bottom of the seed list to scroll it automatically. Seed numbers update immediately. Use
the compact **×** action at the right of a row to remove that fencer after confirmation; the remaining
rows are renumbered automatically. Name errors appear next to the entry field without clearing
the text that needs correction.

With at least two fencers, choose a maximum pool size and select **Generate pools**. The displayed
order is used directly to distribute fencers across pools and is retained as the final-standings
tie-breaker. There is no separate confirmation or registration-to-seeding step.

After pools have been generated, **Setup** remains available from the stage tracker for inspection,
but its roster, seed order, pool-size, and generation controls are read-only. Choose **Edit setup**
only when the roster or seed order needs correction. After confirmation, the app keeps the displayed
roster and seed order but discards generated pools and every dependent result, then returns the
tournament to editable Setup so fresh pools can be generated. Simply viewing Setup does not change
the tournament.

## Pools and Pool Result

Choose a maximum pool size of 5, 6, 7, or 8 and select **Generate pools**. This is a hard upper
limit: no generated pool can contain more fencers than the selected maximum. Pools are balanced
round-robin groups labelled `POOL #1`, `POOL #2`, and so on.

The **Pools** tab shows each generated pool as a separate matrix, so organisers can scan progress
across all pools without changing pages. The board automatically uses as many side-by-side columns
as the visible width can accommodate while keeping every matrix readable, then wraps additional
pools to later rows. It recalculates when the window is resized.
In a score matrix, `V5` is a five-touch win for the row
fencer, a non-prefixed score is a loss, grey diagonal cells are self-matchups, and blank cells are
uncompleted bouts. Select an unfinished cell in any pool, enter scores in the associated result
area, and choose **Record result**. Press **Escape** or click outside the selected matchup/result
area to dismiss score entry without changing a score.

Pool bouts are to 5: scores cannot be negative or tied, one score must be 5, and neither score can
exceed 5. Select a completed bout and choose **Edit result** to correct it. If a DE tableau already
exists, confirmation is required because the dependent tableau and results will be reset.

Score-entry validation appears directly beneath the active Pool or Direct Elimination result form.
Entered scores remain visible for correction, and an invalid score field receives a subtle red outline.

After every pool bout is complete, **Pool Result** displays overall place, wins, bouts fenced, win
ratio, touches scored/received, indicator, and advancement status. Placing uses win ratio,
indicator, touches scored, then original seed. The top 16 fencers, or all fencers if fewer than 16
compete, advance to direct elimination.

## Direct Elimination and final results

Choose **Generate direct elimination** from Pool Result. The seeded tableau expands to the next
power of two; higher seeds receive automatic byes. A DE score may use any non-negative,
non-tied winning score up to 15 (for example, 10--7 or 5--4); the higher score wins and no score
may exceed 15.

Completed non-bye DE bouts can be corrected with **Edit result**. Scores are prefilled. Choose
**Save changes** or **Cancel**. If a changed winner would invalidate a completed later bout, the
app asks for confirmation and clears only the affected downstream path.

Any pending DE bout becomes selectable as soon as both competitors are known. Other unfinished
bouts elsewhere in an earlier round do not prevent that matchup from being fenced; an
`Awaiting opponent` slot remains unavailable.

When the final is recorded, **Final Results** lists final place and fencer. The champion is first,
the final loser second, and same-round eliminations use Pool Result place as the tie-breaker.

## Saving and opening tournaments

Successful changes, including deleting a tournament, are automatically saved in `tournaments/`; no normal Save action is required.
Saved tournaments are restored to Tournament Home when the app starts. The header **Open File** action
imports a tournament JSON file into this local autosaved collection. If autosaving fails, the app
reports the error while retaining the in-memory tournament for the current session.
