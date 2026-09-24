# run-jtd gotchas

Read when a Driver or preview step misbehaves. Each item cost a past session a rerun.

- **Drive the window in-process only.** Launching `target/jTD.jar` and screenshotting it from
  another process fails: `SetForegroundWindow` silently no-ops for a background process, so the
  capture shows whatever window is on top (possibly something sensitive), and a non-DPI-aware
  process can capture the wrong region. `Driver` owns the `JFrame`, raises it itself and takes
  its bounds from the live frame.
- **Level-select cards ignore Robot clicks.** They are `JPanel`s with a mouse listener, not
  buttons; a Robot click at their centre did not register. Use `level <n>`.
- **`list`'s `visible=` is `Component.isVisible()`, not "on screen".** The content pane is a
  `CardLayout`; the hidden card's components still report `visible=true`.
- **`list` indices shift.** Selecting a tower makes its upgrade buttons visible and a second
  wave-preview row appears once a wave has started. Filter (`list panelenemy`) right before use.
- **Leaks end the game.** Starting lives are small (Curly Path: 5). A script that starts real
  waves without towers, or lets spawned mobs walk, hits game over and then sits there. Run
  `setlives 1000` first, and reach late content with `spawn <id>`, `key n` or `key x`.
- **`spawn` places a mob at the raw path start**, which some levels put one cell off the board so
  a moving mob walks in. A zero-speed mob (a Warden egg) spawned that way stays off screen; reach
  it with `spawn warden1`, `waitfor ticks 40`, `kill`, which drops the egg where the Warden died.
- **A frozen mob cannot cast.** Seekers next to a Warden egg keep it frozen, so it never hatches.
  Test abilities with no freezing tower in range.
- **The inspector keeps a hatched egg's last stats** with no status line, so the panel looking
  unchanged after a hatch does not mean the hatch failed - check `enemies` or a screenshot.
- **Selecting picks the nearest valid enemy.** Clicking an invisible mob with another a few pixels
  away selects the other one; confirm with `text`.
- **The Info pane scrolls.** A long stat block opens scrolled, hiding the header and the
  `Killed`/`Leaked` line in a screenshot. Assert with `text <n>`.
- **Wave-preview slots are square, as tall as the row** (about 30 px at the default size), and
  `hover` picks by x only; hover at 15, 45, 75 for the first three slots of that row.
