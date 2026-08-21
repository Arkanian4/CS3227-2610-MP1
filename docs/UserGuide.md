# User Guide

## Setup

Install JDK 21 or later. From the project root, run:

```powershell
.\gradlew.bat run
```

Run the tests with `./gradlew.bat test`.

## Tournament Home

Tournament Home lists ongoing tournaments before completed tournaments. Choose **Open** to
continue an ongoing tournament or **View Results** to inspect a completed one.

Each tournament row has a compact **⋯** menu containing the secondary **Delete** action. Deleting requires confirmation and
permanently removes that tournament, including its fencers, pool results, Direct Elimination
results, and final standings. It cannot be undone. Deleting one tournament does not change any
other tournament.

Choose **+ New Tournament**, enter a name, and choose **Create**. Names are trimmed and unique
without regard to letter case. For example, `Club Open` and ` club open ` cannot both be created.

When a tournament is open, use the left navigation to move between available competition stages.
Unavailable later stages are muted until the required earlier stage is complete. The header shows
the current tournament and phase; **Tournament Home** remains available in the sidebar.

## Registration and seeding

Add fencers by display name using the compact **Fencer name** entry row. Names must be non-blank
and unique within the tournament. The roster is numbered in registration order; select a row and
choose **Remove selected** to remove that fencer after confirmation.

With at least two fencers, choose **Continue to seeding** at the bottom of the registration
workspace. Drag a fencer to a new position, or use **Move up** and **Move down**, to reorder the
provisional seed list. **Generate pools** locks the order by advancing to the pool stage and uses
it to distribute fencers across pools; it is also the final standings tie-breaker.

## Pools and Pool Result

Choose a maximum pool size of 5, 6, 7, or 8 and select **Generate pools**. Pools are balanced
round-robin groups labelled `POOL #1`, `POOL #2`, and so on.

The **Pools** tab shows each generated pool as a separate matrix, so organisers can scan progress
across all pools without changing pages. Two pools use a compact side-by-side board suitable for
the club's common 8-fencer format; extra pools wrap and scroll instead of making a matrix unreadable.
In a score matrix, `V5` is a five-touch win for the row
fencer, a non-prefixed score is a loss, grey diagonal cells are self-matchups, and blank cells are
uncompleted bouts. Select an unfinished cell in any pool, enter scores in the associated result
area, and choose **Record result**. Press **Escape** or click outside the selected matchup/result
area to dismiss score entry without changing a score.

Pool bouts are to 5: scores cannot be negative or tied, one score must be 5, and neither score can
exceed 5. Select a completed bout and choose **Edit result** to correct it. If a DE tableau already
exists, confirmation is required because the dependent tableau and results will be reset.

After every pool bout is complete, **Pool Result** displays overall place, wins, bouts fenced, win
ratio, touches scored/received, indicator, and advancement status. Placing uses win ratio,
indicator, touches scored, then original seed. The top 16 fencers, or all fencers if fewer than 16
compete, advance to direct elimination.

## Direct Elimination and final results

Choose **Generate direct elimination** from Pool Result. The seeded tableau expands to the next
power of two; higher seeds receive automatic byes. Ready DE bouts are to 15, using the same
non-negative, non-tied, winning-score validation as pools.

Completed non-bye DE bouts can be corrected with **Edit result**. Scores are prefilled. Choose
**Save changes** or **Cancel**. If a changed winner would invalidate a completed later bout, the
app asks for confirmation and clears only the affected downstream path.

When the final is recorded, **Final Results** lists final place and fencer. The champion is first,
the final loser second, and same-round eliminations use Pool Result place as the tie-breaker.

## Saving and opening tournaments

Successful changes, including deleting a tournament, are automatically saved in `tournaments/`; no normal Save action is required.
Saved tournaments are restored to Tournament Home when the app starts. The header **Open File** action
imports a tournament JSON file into this local autosaved collection. If autosaving fails, the app
reports the error while retaining the in-memory tournament for the current session.
