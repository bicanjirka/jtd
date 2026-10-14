package td.balance;

import td.effect.EffectKind;
import td.level.LevelDefinition;
import td.tower.TowerFactory;
import td.wave.Point;
import td.wave.Vec2;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The report as one self-contained HTML page: per level the verdicts as tiles, then charts for what the text
 * report tabulates (margins against the target band, pressure round by round, where enemies die and towers stand,
 * each tower's best cells, the counter matrix) and the numbers beside them as tables. No script and no external
 * file; hovering a mark shows its value.
 */
final class HtmlReport {

    private static final int BOARD_CELL = 24;
    private static final int SMALL_CELL = 13;
    private static final int STEPS = 6;

    private final StringBuilder html = new StringBuilder();

    private HtmlReport() {
    }

    static String of(List<LevelRun> runs) {
        HtmlReport report = new HtmlReport();
        report.page(runs);
        return report.html.toString();
    }

    private void page(List<LevelRun> runs) {
        this.html.append("""
                <!doctype html>
                <html lang="en"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>jTD balance report</title>
                <style>
                :root{color-scheme:light;--page:#f9f9f7;--surface:#fcfcfb;--ink:#0b0b0b;--ink2:#52514e;--muted:#898781;
                --grid:#e1e0d9;--axis:#c3c2b7;--accent:#2a78d6;--deep:#184f95;--gray:#c3c2b7;--neg:#e34948;--mid:#f0efec;
                --good:#0ca30c;--serious:#ec835a;--critical:#d03b3b;--ring:rgba(11,11,11,.10)}
                @media (prefers-color-scheme:dark){:root:not([data-theme=light]){color-scheme:dark;--page:#0d0d0d;
                --surface:#1a1a19;--ink:#f0efec;--ink2:#c3c2b7;--muted:#898781;--grid:#2c2c2a;--axis:#383835;
                --accent:#3987e5;--deep:#86b6ef;--gray:#52514e;--neg:#e66767;--mid:#383835;--ring:rgba(255,255,255,.10)}}
                :root[data-theme=dark]{color-scheme:dark;--page:#0d0d0d;--surface:#1a1a19;--ink:#f0efec;--ink2:#c3c2b7;
                --muted:#898781;--grid:#2c2c2a;--axis:#383835;--accent:#3987e5;--deep:#86b6ef;--gray:#52514e;--neg:#e66767;
                --mid:#383835;--ring:rgba(255,255,255,.10)}
                body{margin:0;background:var(--page);color:var(--ink);font:14px/1.45 system-ui,-apple-system,"Segoe UI",sans-serif}
                main{max-width:1100px;margin:0 auto;padding:24px 16px 64px}
                h1{font-size:24px;margin:0 0 4px}h2{font-size:20px;margin:40px 0 4px}h3{font-size:15px;margin:24px 0 8px}
                p.note,.note{color:var(--ink2);margin:4px 0 12px}
                .tiles{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:12px;margin:12px 0}
                .tile{background:var(--surface);border:1px solid var(--ring);border-radius:10px;padding:12px 14px}
                .tile .k{color:var(--ink2);font-size:12px;text-transform:uppercase;letter-spacing:.04em}
                .tile .v{font-size:22px;font-weight:600;margin:2px 0}
                .tile .s{color:var(--ink2);font-size:13px}
                .badge{display:inline-flex;gap:6px;align-items:center;font-weight:600}
                .dot{width:10px;height:10px;border-radius:50%;display:inline-block}
                .card{background:var(--surface);border:1px solid var(--ring);border-radius:10px;padding:12px 14px;
                margin:8px 0;overflow-x:auto}
                .row{display:flex;flex-wrap:wrap;gap:16px;align-items:flex-start}
                table{border-collapse:collapse;font-variant-numeric:tabular-nums}
                th,td{padding:4px 8px;text-align:right;border-bottom:1px solid var(--grid);white-space:nowrap}
                th:first-child,td:first-child{text-align:left}
                th{color:var(--ink2);font-weight:600;font-size:12px}
                td.heat{text-align:center;min-width:56px}
                svg text{fill:var(--muted);font-size:11px}
                svg .label{fill:var(--ink2)}
                .legend{display:flex;gap:6px;align-items:center;color:var(--ink2);font-size:12px;margin-top:6px}
                .legend span.sw{width:18px;height:10px;display:inline-block;border-radius:2px}
                </style></head><body><main>
                <h1>jTD balance report</h1>
                <p class="note">Headless plays of every built-in level by a naive player and a competent one, the
                competent one also on random cells and without upgrades. Margins are the highest enemy health, as a
                percentage of the authored one, a player still wins at. Intervals are 95%.</p>
                """);
        for (LevelRun run : runs) {
            this.level(run);
        }
        this.html.append("</main></body></html>\n");
    }

    private void level(LevelRun run) {
        LevelDefinition level = run.survey().level();
        this.html.append("<h2 id=\"").append(level.name().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-"))
                .append("\">").append(escape(level.name())).append("</h2><p class=\"note\">")
                .append(level.startingCredits()).append(" credits, ").append(level.startingLives()).append(" lives, ")
                .append(level.paths().getFirst().waves().size()).append(" rounds, ").append(level.paths().size())
                .append(level.paths().size() == 1 ? " path" : " paths").append("</p>");
        this.tiles(run);
        this.html.append("<h3>Difficulty: how much tougher the enemies could be</h3>");
        this.marginChart(run);
        this.html.append("<h3>Pressure by round</h3><p class=\"note\">How far the furthest enemy got (competent"
                + " player), and the enemy health each round sends.</p><div class=\"row\">");
        double[] closest = run.perRound(BalanceReport.COMPETENT, round -> round.pressure().closest());
        double[] leaks = run.perRound(BalanceReport.COMPETENT, round -> round.pressure().leaked());
        this.roundChart(closest, leaks, 1.0, "furthest enemy, share of the path", true);
        this.roundChart(run.perRound(BalanceReport.COMPETENT, round -> round.pressure().healthIn()), new double[0],
                Double.NaN, "enemy health walking in", false);
        this.html.append("</div>");
        this.timeline(run);
        this.economy(run);
        this.html.append("<h3>Where it happens</h3><div class=\"row\">");
        this.board(run, "where enemies die (competent)", deaths(run), BOARD_CELL);
        this.board(run, "where the competent player builds", builds(run), BOARD_CELL);
        this.html.append("</div>");
        this.html.append("<h3>Each tower's best cells (lab)</h3><p class=\"note\">Damage one tower alone lands in a"
                + " probe round from each cell, each tower on its own scale. Different patterns mean each tower has"
                + " places of its own.</p><div class=\"row\">");
        for (TowerFactory.Type type : TowerLab.damageDealers()) {
            Map<Point, Double> values = new HashMap<>();
            run.survey().buildable().forEach(cell -> values.put(cell, run.lab().cellValue(type, cell)));
            this.board(run, type.displayName(), values, SMALL_CELL);
        }
        this.html.append("</div>");
        this.towerShares(run);
        this.upgradePhases(run);
        this.counters(run);
        this.variety(run);
        this.coverage(run);
    }

    private void coverage(LevelRun run) {
        PlayRecord.EffectUse use = run.effectUse(BalanceReport.COMPETENT);
        this.html.append("<h3>Coverage</h3><p class=\"note\">The share of enemy time each effect was on an enemy in"
                + " the competent player's plays, and what no play of any player ever triggered.</p>"
                + "<div class=\"row\"><div class=\"card\"><table><tr><th>effect</th><th>share of enemy time</th>"
                + "</tr>");
        Arrays.stream(EffectKind.values()).filter(kind -> use.starts().containsKey(kind))
                .sorted(java.util.Comparator.comparingDouble(use::uptime).reversed())
                .forEach(kind -> this.html.append("<tr><td>").append(BalanceReport.name(kind)).append("</td><td>")
                        .append(String.format("%.1f%%", use.uptime(kind) * 100)).append("</td></tr>"));
        this.html.append("</table></div><div class=\"card\"><table><tr><th>never triggered</th><th></th></tr>");
        this.coverageRow("effects", run.effectsNeverStarted().stream().map(BalanceReport::name).toList());
        this.coverageRow("enemy abilities", run.abilitiesNeverUsed());
        run.nodesNeverBought().forEach((type, nodes) -> this.coverageRow(type.displayName() + " nodes", nodes));
        this.html.append("</table></div></div>");
    }

    private void coverageRow(String what, List<String> never) {
        this.html.append("<tr><td>").append(escape(what)).append("</td><td style=\"text-align:left;white-space:normal\">")
                .append(never.isEmpty() ? "none" : escape(String.join(", ", never))).append("</td></tr>");
    }

    private void tiles(LevelRun run) {
        String difficulty = BalanceReport.difficultyVerdict(run);
        String positioning = BalanceReport.mattersVerdict(run.marginRatio(BalanceReport.COMPETENT,
                BalanceReport.RANDOM_CELLS));
        String upgrades = BalanceReport.mattersVerdict(run.marginRatio(BalanceReport.COMPETENT,
                BalanceReport.NO_UPGRADES));
        Estimate margin = run.margin(BalanceReport.COMPETENT);
        this.html.append("<div class=\"tiles\">");
        this.tile("Difficulty", difficulty.startsWith("IN BAND") ? "good"
                        : difficulty.startsWith("TOO HARD") ? "critical" : "serious",
                head(difficulty), String.format("competent holds %.0f%% (%.0f-%.0f); wins %s", margin.value(),
                        margin.low(), margin.high(), run.winRate(BalanceReport.COMPETENT).formatPercent()));
        this.tile("Positioning", positioning.startsWith("MATTERS") ? "good" : "serious", head(positioning),
                String.format("chosen cells %.0f%%, random cells %.0f%%", margin.value(),
                        run.margin(BalanceReport.RANDOM_CELLS).value()));
        this.tile("Upgrades", upgrades.startsWith("MATTERS") ? "good" : "serious", head(upgrades),
                String.format("with %.0f%%, without %.0f%%", margin.value(),
                        run.margin(BalanceReport.NO_UPGRADES).value()));
        Map<TowerFactory.Type, Double> shares = run.damageShares(BalanceReport.COMPETENT);
        TowerFactory.Type top = shares.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey)
                .orElse(TowerFactory.Type.SNIPER);
        double topShare = shares.getOrDefault(top, 0.0);
        this.tile("Top tower", topShare > 0.5 ? "serious" : "good", top.displayName(),
                String.format("lands %.0f%% of the competent player's damage", topShare * 100));
        this.html.append("</div>");
    }

    private static String head(String verdict) {
        int colon = verdict.indexOf(':');
        return colon < 0 ? verdict : verdict.substring(0, colon);
    }

    private void tile(String key, String status, String value, String sub) {
        String icon = switch (status) {
            case "good" -> "&#10003;";
            case "critical" -> "&#10007;";
            default -> "!";
        };
        this.html.append("<div class=\"tile\"><div class=\"k\">").append(key).append("</div><div class=\"v\">")
                .append("<span class=\"badge\"><span class=\"dot\" style=\"background:var(--").append(status)
                .append(")\"></span>").append(icon).append(' ').append(escape(value)).append("</span></div>")
                .append("<div class=\"s\">").append(escape(sub)).append("</div></div>");
    }

    /** One bar per player: its margin with its interval, the competent player in the accent, the band shaded. */
    private void marginChart(LevelRun run) {
        List<String> players = BalanceReport.shownPlayers(run);
        double max = players.stream().mapToDouble(player -> run.margin(player).high()).max().orElse(200);
        max = Math.max(200, Math.ceil(max / 100) * 100);
        int left = 110;
        int width = 620;
        int bar = 18;
        int gap = 10;
        int height = players.size() * (bar + gap) + 28;
        double scale = (width - left - 60) / max;
        StringBuilder svg = new StringBuilder();
        svg.append(String.format("<svg width=\"%d\" height=\"%d\" role=\"img\" aria-label=\"margins by player\">",
                width, height));
        svg.append(String.format("<rect x=\"%.1f\" y=\"0\" width=\"%.1f\" height=\"%d\" fill=\"var(--good)\""
                        + " opacity=\".12\"><title>target band: %.0f-%.0f%%</title></rect>", left + BalanceReport.MARGIN_LOW
                        * scale, (BalanceReport.MARGIN_HIGH - BalanceReport.MARGIN_LOW) * scale, height - 20,
                BalanceReport.MARGIN_LOW, BalanceReport.MARGIN_HIGH));
        for (int tick = 0; tick <= max; tick += 100) {
            double x = left + tick * scale;
            svg.append(String.format("<line x1=\"%.1f\" x2=\"%.1f\" y1=\"0\" y2=\"%d\" stroke=\"var(--grid)\"/>"
                    + "<text x=\"%.1f\" y=\"%d\" text-anchor=\"middle\">%d%%</text>", x, x, height - 20, x,
                    height - 6, tick));
        }
        for (int i = 0; i < players.size(); i++) {
            String player = players.get(i);
            Estimate margin = run.margin(player);
            int y = i * (bar + gap) + 4;
            String fill = player.equals(BalanceReport.COMPETENT) ? "var(--accent)" : "var(--gray)";
            double w = Math.max(2, margin.value() * scale);
            svg.append(String.format("<text class=\"label\" x=\"%d\" y=\"%d\" text-anchor=\"end\">%s</text>",
                    left - 8, y + bar - 5, player));
            svg.append(String.format("<path d=\"M%d,%d h%.1f a4,4 0 0 1 4,4 v%d a4,4 0 0 1 -4,4 h%.1f z\" fill=\"%s\">"
                            + "<title>%s: holds %.0f%% (%.0f-%.0f), wins %s</title></path>", left, y, w - 4, bar - 8,
                    -(w - 4), fill, player, margin.value(), margin.low(), margin.high(),
                    run.winRate(player).formatPercent()));
            svg.append(String.format("<line x1=\"%.1f\" x2=\"%.1f\" y1=\"%d\" y2=\"%d\" stroke=\"var(--ink2)\"/>",
                    left + margin.low() * scale, left + margin.high() * scale, y + bar / 2, y + bar / 2));
            svg.append(String.format("<text class=\"label\" x=\"%.1f\" y=\"%d\">%.0f%%</text>",
                    left + Math.max(w, margin.high() * scale) + 6, y + bar - 5, margin.value()));
        }
        svg.append("</svg>");
        this.html.append("<div class=\"card\">").append(svg).append("<div class=\"legend\"><span class=\"sw\""
                + " style=\"background:var(--good);opacity:.25\"></span>target band for the competent player</div>");
        this.html.append("<table><tr><th>player</th><th>won</th><th>lives lost</th><th>leaks</th><th>holds</th></tr>");
        for (String player : players) {
            this.html.append("<tr><td>").append(player).append("</td><td>")
                    .append(run.winRate(player).formatPercent()).append("</td><td>")
                    .append(run.livesLost(player).format(1)).append("</td><td>").append(run.leaks(player).format(1))
                    .append("</td><td>").append(run.margin(player).format(0)).append("%</td></tr>");
        }
        this.html.append("</table></div>");
    }

    /** A column per round; with {@code leaks}, a round that leaked carries its count above the column. */
    private void roundChart(double[] values, double[] leaks, double fixedMax, String title, boolean share) {
        int rounds = values.length;
        int column = Math.max(14, Math.min(30, 480 / Math.max(1, rounds)));
        int plot = 140;
        int width = rounds * column + 50;
        int height = plot + 50;
        double max = Double.isNaN(fixedMax) ? Arrays.stream(values).filter(v -> !Double.isNaN(v)).max().orElse(1)
                : fixedMax;
        max = max <= 0 ? 1 : max;
        StringBuilder svg = new StringBuilder(String.format("<svg width=\"%d\" height=\"%d\" role=\"img\""
                + " aria-label=\"%s\">", width, height, title));
        for (int g = 0; g <= 4; g++) {
            double y = 16 + plot - plot * g / 4.0;
            String label = share ? String.format("%.0f%%", g * 25.0) : BalanceReport.compact(max * g / 4);
            svg.append(String.format("<line x1=\"40\" x2=\"%d\" y1=\"%.1f\" y2=\"%.1f\" stroke=\"var(--grid)\"/>"
                    + "<text x=\"36\" y=\"%.1f\" text-anchor=\"end\">%s</text>", width, y, y, y + 4, label));
        }
        for (int r = 0; r < rounds; r++) {
            double v = Double.isNaN(values[r]) ? 0 : values[r];
            double h = Math.max(1, plot * v / max);
            double x = 40 + r * column + 2;
            double y = 16 + plot - h;
            boolean leaked = leaks.length > r && leaks[r] > 0.05;
            String fill = leaked ? "var(--critical)" : "var(--accent)";
            svg.append(String.format("<rect x=\"%.1f\" y=\"%.1f\" width=\"%d\" height=\"%.1f\" rx=\"2\" fill=\"%s\">"
                            + "<title>round %d: %s%s</title></rect>", x, y, column - 4, h, fill, r + 1,
                    share ? String.format("%.0f%%", v * 100) : BalanceReport.compact(v),
                    leaked ? String.format(", %.1f leaks", leaks[r]) : ""));
            if (leaked) {
                svg.append(String.format("<text class=\"label\" x=\"%.1f\" y=\"%.1f\" text-anchor=\"middle\">%.0f"
                        + "</text>", x + (column - 4) / 2.0, y - 3, Math.max(1, leaks[r])));
            }
            if (rounds <= 20 || r % 2 == 0) {
                svg.append(String.format("<text x=\"%.1f\" y=\"%d\" text-anchor=\"middle\">%d</text>",
                        x + (column - 4) / 2.0, 16 + plot + 14, r + 1));
            }
        }
        svg.append(String.format("<text x=\"40\" y=\"%d\">%s</text></svg>", height - 4, title));
        this.html.append("<div class=\"card\">").append(svg);
        if (leaks.length > 0) {
            this.html.append("<div class=\"legend\"><span class=\"sw\" style=\"background:var(--critical)\"></span>"
                    + "a round that leaked, with its leaks per play</div>");
        }
        this.html.append("</div>");
    }

    /** The level's board with each cell shaded by {@code values} on one scale and the paths drawn over it. */
    /** Per round, what the competent player had, earned, spent and wanted next, averaged over plays. */
    private void economy(LevelRun run) {
        String player = BalanceReport.COMPETENT;
        double[][] rows = {
                run.perRound(player, round -> round.economy().creditsAtStart()),
                run.perRound(player, round -> round.economy().earned()),
                run.perRound(player, round -> round.economy().spent().towers()),
                run.perRound(player, round -> round.economy().spent().upgrades()),
                run.perRound(player, round -> round.economy().spent().wanted())};
        String[] names = {"idle at the start", "bounty earned", "spent on towers", "spent on upgrades",
                "price of what it wants next"};
        this.html.append("<h3>Economy</h3><p class=\"note\">Per round, averaged over the competent player's plays. ")
                .append(escape(BalanceReport.economyVerdict(rows[1], rows[4]))).append(".</p><div class=\"card\">")
                .append("<table><tr><th>round</th>");
        for (int r = 1; r <= rows[0].length; r++) {
            this.html.append("<th>").append(r).append("</th>");
        }
        this.html.append("</tr>");
        for (int i = 0; i < rows.length; i++) {
            this.html.append("<tr><td>").append(names[i]).append("</td>");
            for (double value : rows[i]) {
                this.html.append("<td>").append(Double.isNaN(value) ? "-" : BalanceReport.compact(value)).append("</td>");
            }
            this.html.append("</tr>");
        }
        this.html.append("</table></div>");
    }

    /** The competent player's first play, tick by tick: enemy health on the board, the furthest enemy, credits. */
    private void timeline(LevelRun run) {
        List<PlayRecord> plays = run.playsOf(BalanceReport.COMPETENT);
        if (plays.isEmpty() || plays.getFirst().watch().timeline().isEmpty()) {
            return;
        }
        List<PlayRecord.Sample> samples = plays.getFirst().watch().timeline();
        this.html.append("<h3>Timeline</h3><p class=\"note\">One play of the competent player (seed ")
                .append(plays.getFirst().play().seed()).append("), sampled every ").append(PlayRecord.Sample.EVERY)
                .append(" ticks; the grid lines are round starts.</p>");
        this.line(samples, sample -> sample.health(), "enemy health on the board", false);
        this.line(samples, PlayRecord.Sample::furthest, "furthest enemy, share of the path", true);
        this.line(samples, PlayRecord.Sample::credits, "credits unspent", false);
    }

    private void line(List<PlayRecord.Sample> samples, java.util.function.ToDoubleFunction<PlayRecord.Sample> value,
                      String title, boolean share) {
        int width = 960;
        int plot = 90;
        double last = samples.getLast().tick();
        double max = share ? 1 : Math.max(1, samples.stream().mapToDouble(value).max().orElse(1));
        StringBuilder svg = new StringBuilder(String.format("<svg width=\"100%%\" viewBox=\"0 0 %d %d\" role=\"img\""
                + " aria-label=\"%s\">", width + 50, plot + 36, title));
        int round = 0;
        for (PlayRecord.Sample sample : samples) {
            if (sample.round() != round) {
                round = sample.round();
                double x = 40 + width * sample.tick() / last;
                svg.append(String.format("<line x1=\"%.1f\" x2=\"%.1f\" y1=\"8\" y2=\"%d\" stroke=\"var(--grid)\"/>",
                        x, x, 8 + plot));
            }
        }
        StringBuilder points = new StringBuilder();
        for (PlayRecord.Sample sample : samples) {
            points.append(String.format("%.1f,%.1f ", 40 + width * sample.tick() / last,
                    8 + plot - plot * value.applyAsDouble(sample) / max));
        }
        svg.append(String.format("<polyline points=\"%s\" fill=\"none\" stroke=\"var(--accent)\""
                + " stroke-width=\"1.5\"/>", points.toString().trim()));
        svg.append(String.format("<text x=\"36\" y=\"14\" text-anchor=\"end\">%s</text>",
                share ? "100%" : BalanceReport.compact(max)));
        svg.append(String.format("<text x=\"40\" y=\"%d\">%s</text></svg>", plot + 30, title));
        this.html.append("<div class=\"card\">").append(svg).append("</div>");
    }

    private void board(LevelRun run, String title, Map<Point, Double> values, int cell) {
        BoardSurvey survey = run.survey();
        LevelDefinition level = survey.level();
        double max = values.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
        int width = level.width() * cell;
        int height = level.height() * cell;
        double pixel = (double) cell / survey.scale();
        StringBuilder svg = new StringBuilder(String.format("<svg width=\"%d\" height=\"%d\" role=\"img\""
                + " aria-label=\"%s\"><rect width=\"%d\" height=\"%d\" fill=\"var(--surface)\"/>", width,
                height + 16, escape(title), width, height));
        for (int x = 0; x < level.width(); x++) {
            for (int y = 0; y < level.height(); y++) {
                double value = values.getOrDefault(new Point(x, y), 0.0);
                double t = max <= 0 ? 0 : value / max;
                svg.append(String.format("<rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" fill=\"%s\""
                                + " stroke=\"var(--surface)\" stroke-width=\"1\"><title>(%d,%d) %s</title></rect>",
                        x * cell, y * cell, cell, cell, sequential(t), x, y, BalanceReport.compact(value)));
            }
        }
        for (int path = 0; path < survey.pathCount(); path++) {
            StringBuilder points = new StringBuilder();
            List<Vec2> samples = survey.samples(path);
            for (int i = 0; i < samples.size(); i += 2) {
                points.append(String.format("%.1f,%.1f ", samples.get(i).x() * pixel, samples.get(i).y() * pixel));
            }
            svg.append("<polyline points=\"").append(points).append("\" fill=\"none\" stroke=\"var(--ink2)\""
                    + " stroke-opacity=\".55\" stroke-width=\"").append(cell > SMALL_CELL ? 2 : 1).append("\"/>");
        }
        svg.append(String.format("<text x=\"0\" y=\"%d\">%s</text></svg>", height + 12, escape(title)));
        this.html.append("<div class=\"card\">").append(svg).append("</div>");
    }

    /** A step of the one-hue ramp, from the surface at 0 to the deep accent at 1. */
    private static String sequential(double t) {
        if (t <= 0) {
            return "var(--surface)";
        }
        int step = (int) Math.ceil(Math.min(1, t) * STEPS);
        return String.format("color-mix(in oklab, var(--deep) %d%%, var(--surface))", 8 + step * 92 / STEPS);
    }

    /** Blue above 1, red below, the neutral midpoint at 1; a factor of 2 or 0 is the full pole. */
    private static String diverging(double factor) {
        double t = Math.max(-1, Math.min(1, factor - 1));
        int step = (int) Math.round(Math.abs(t) * 4) * 25;
        String pole = t >= 0 ? "var(--accent)" : "var(--neg)";
        return step == 0 ? "var(--mid)" : String.format("color-mix(in oklab, %s %d%%, var(--mid))", pole, step);
    }

    private static Map<Point, Double> deaths(LevelRun run) {
        return perCell(run, BalanceReport.COMPETENT, true);
    }

    private static Map<Point, Double> builds(LevelRun run) {
        Map<Point, Double> counts = new HashMap<>();
        List<PlayRecord> plays = run.playsOf(BalanceReport.COMPETENT);
        for (PlayRecord play : plays) {
            play.towers().forEach(tower -> counts.merge(tower.cell(), 1.0 / plays.size(), Double::sum));
        }
        return counts;
    }

    private static Map<Point, Double> perCell(LevelRun run, String player, boolean killed) {
        Map<Point, Double> counts = new HashMap<>();
        int scale = run.survey().scale();
        List<PlayRecord> plays = run.playsOf(player);
        for (PlayRecord play : plays) {
            for (PlayRecord.EnemyEnd end : play.ends()) {
                if (end.leaked() != killed) {
                    Point cell = new Point((int) (end.spot().x() / scale), (int) (end.spot().y() / scale));
                    counts.merge(cell, 1.0 / plays.size(), Double::sum);
                }
            }
        }
        return counts;
    }

    private void towerShares(LevelRun run) {
        Map<TowerFactory.Type, Double> shares = run.damageShares(BalanceReport.COMPETENT);
        Map<TowerFactory.Type, Double> counts = run.towerCounts(BalanceReport.COMPETENT);
        this.html.append("<h3>Who lands the damage (competent)</h3><div class=\"card\"><table><tr><th>tower</th>"
                + "<th>share of damage</th><th></th><th>towers at the end</th></tr>");
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            double share = shares.getOrDefault(type, 0.0);
            this.html.append("<tr><td>").append(type.displayName()).append("</td><td>")
                    .append(String.format("%.0f%%", share * 100)).append("</td><td style=\"text-align:left\">")
                    .append(String.format("<svg width=\"220\" height=\"12\"><rect width=\"%.1f\" height=\"12\""
                            + " rx=\"3\" fill=\"var(--accent)\"/></svg>", Math.max(1, share * 220)))
                    .append("</td><td>").append(String.format("%.1f", counts.getOrDefault(type, 0.0)))
                    .append("</td></tr>");
        }
        this.html.append("</table></div>");
    }

    private void upgradePhases(LevelRun run) {
        this.html.append("<h3>Upgrades by phase</h3><p class=\"note\">The share of what a player spent that went into"
                + " upgrades, in each third of the level's rounds.</p><div class=\"card\"><table><tr><th>player</th>"
                + "<th>early</th><th>middle</th><th>late</th></tr>");
        for (String player : List.of(BalanceReport.COMPETENT, BalanceReport.STRONG)) {
            if (!run.has(player)) {
                continue;
            }
            this.html.append("<tr><td>").append(player).append("</td>");
            for (double share : run.upgradeShareByThird(player)) {
                this.html.append("<td>").append(Double.isNaN(share) ? "-" : String.format("%.0f%%", share * 100))
                        .append("</td>");
            }
            this.html.append("</tr>");
        }
        this.html.append("</table></div>");
    }

    private void counters(LevelRun run) {
        TowerLab lab = run.lab();
        this.html.append("<h3>Counters (lab duels)</h3><p class=\"note\">The share of a wave of each kind a tower"
                + " brings down, against its share of a plain wave: under 1 (red) the kind resists it, over 1"
                + " (blue) it suits it.</p><div class=\"card\"><table><tr><th>kind at rank</th>");
        TowerLab.damageDealers().forEach(type -> this.html.append("<th>").append(type.displayName()).append("</th>"));
        this.html.append("</tr>");
        for (String key : lab.levelRanks()) {
            this.html.append("<tr><td>").append(escape(key)).append("</td>");
            for (TowerFactory.Type type : TowerLab.damageDealers()) {
                double factor = lab.kindFactor(type, key);
                this.html.append("<td class=\"heat\" style=\"background:").append(diverging(factor)).append("\">")
                        .append(String.format("%.2f", factor)).append("</td>");
            }
            this.html.append("</tr>");
        }
        this.html.append("</table></div>");
    }

    private void variety(LevelRun run) {
        if (!run.margins().containsKey(BalanceReport.banned(TowerFactory.Type.SNIPER))) {
            return;
        }
        this.html.append("<h3>Variety</h3><p class=\"note\">The competent player's margin with only one tower type,"
                + " and with one banned.</p><div class=\"card\"><table><tr><th>tower</th><th>alone</th><th>banned</th>"
                + "<th>banned, against the full choice</th></tr>");
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            Estimate ratio = run.marginRatio(BalanceReport.banned(type), BalanceReport.COMPETENT);
            this.html.append("<tr><td>").append(type.displayName()).append("</td><td>")
                    .append(type == TowerFactory.Type.AURA ? "-"
                            : BalanceReport.marginText(run.margin(BalanceReport.alone(type))))
                    .append("</td><td>").append(BalanceReport.marginText(run.margin(BalanceReport.banned(type))))
                    .append("</td><td>")
                    .append(String.format("x%.2f (%.2f-%.2f)", ratio.value(), ratio.low(), ratio.high()))
                    .append("</td></tr>");
        }
        this.html.append("</table></div>");
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
