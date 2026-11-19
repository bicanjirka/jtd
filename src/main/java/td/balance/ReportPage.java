package td.balance;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * What a balance stage found, as tables: stored as tab-separated text for the next stage to read, and as one
 * self-contained HTML page for a person. Every stage of the pipeline writes through this, so the pages look alike
 * and each stored sheet is read the same way.
 */
final class ReportPage {

    static final Path FOLDER = Path.of("target", "balance");
    /** Three times off the reference is as deep as a heat cell's colour goes. */
    private static final double HEAT_SPAN = Math.log(3);

    private ReportPage() {
    }

    /**
     * One table of a page.
     *
     * @param note what its columns mean, shown above it
     * @param heat the columns whose numbers are multiples of a reference: 1 is plain, more is warm, less is cool
     */
    record Table(String title, String note, List<String> header, List<List<String>> rows, Set<String> heat) {

        /** A table with no note and no heat columns yet. */
        static Table of(String title, List<String> header, List<List<String>> rows) {
            return new Table(title, "", header, rows, Set.of());
        }

        Table noted(String text) {
            return new Table(this.title, text, this.header, this.rows, this.heat);
        }

        /** The rows that name {@code tower}: a cell that is its type, or that starts with it, as a finding does. */
        Table onlyOf(String tower) {
            List<List<String>> kept = this.rows.stream().filter(row -> row.stream()
                    .anyMatch(cell -> cell.equals(tower) || cell.startsWith(tower + " "))).toList();
            return new Table(this.title, this.note, this.header, kept, this.heat);
        }

        Table heating(String... columns) {
            return new Table(this.title, this.note, this.header, this.rows, Set.of(columns));
        }

        String tsv() {
            StringBuilder text = new StringBuilder(String.join("\t", this.header)).append('\n');
            this.rows.forEach(row -> text.append(String.join("\t", row)).append('\n'));
            return text.toString();
        }
    }

    /** Stores {@code table} as {@code target/balance/<name>.tsv}. */
    static void store(String name, Table table) {
        write(FOLDER.resolve(name + ".tsv"), table.tsv());
    }

    /** Reads a stored sheet back: its header first, then its rows. */
    static List<List<String>> load(String name) {
        try {
            return Files.readAllLines(FOLDER.resolve(name + ".tsv"), StandardCharsets.UTF_8).stream()
                    .filter(line -> !line.isBlank())
                    .map(line -> List.of(line.split("\t", -1)))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("No stored sheet " + name + ": run the stage that writes it first", e);
        }
    }

    /** Writes {@code target/balance/<name>.html}. */
    static void publish(String name, String title, String intro, List<Table> tables) {
        StringBuilder html = new StringBuilder("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\"><title>")
                .append(escape(title)).append("</title><style>").append(STYLE).append("</style></head><body><h1>")
                .append(escape(title)).append("</h1><p class=\"intro\">").append(intro).append("</p>");
        for (Table table : tables) {
            html.append("<h2>").append(escape(table.title())).append("</h2><p class=\"note\">").append(table.note())
                    .append("</p><div class=\"scroll\"><table><thead><tr>");
            table.header().forEach(column -> html.append("<th>").append(escape(column)).append("</th>"));
            html.append("</tr></thead><tbody>");
            for (List<String> row : table.rows()) {
                html.append("<tr>");
                for (int i = 0; i < row.size(); i++) {
                    String column = i < table.header().size() ? table.header().get(i) : "";
                    html.append(cell(row.get(i), table.heat().contains(column)));
                }
                html.append("</tr>");
            }
            html.append("</tbody></table></div>");
        }
        html.append("<script>").append(SORT).append("</script></body></html>");
        write(FOLDER.resolve(name + ".html"), html.toString());
    }

    private static String cell(String text, boolean heat) {
        String upper = text.toUpperCase(Locale.ROOT);
        if (upper.contains("BREACH") || upper.contains("DOMINANT") || upper.contains("FAIL")) {
            return "<td class=\"bad\">" + escape(text) + "</td>";
        }
        if (upper.startsWith("OK")) {
            return "<td class=\"good\">" + escape(text) + "</td>";
        }
        if (upper.startsWith("WARN") || upper.contains("UNMEASURED") || upper.contains("ESTIMATE")) {
            return "<td class=\"warn\">" + escape(text) + "</td>";
        }
        if (!heat) {
            return (isNumber(text) ? "<td class=\"num\">" : "<td>") + escape(text) + "</td>";
        }
        if ("immune".equals(text)) {
            return "<td class=\"num\" style=\"background:hsla(0,75%,45%,0.6)\">immune</td>";
        }
        if (!isNumber(text) || Double.parseDouble(text) <= 0) {
            return "<td class=\"num\">" + escape(text) + "</td>";
        }
        double depth = Math.max(-1, Math.min(1, Math.log(Double.parseDouble(text)) / HEAT_SPAN));
        return String.format(Locale.ROOT, "<td class=\"num\" style=\"background:hsla(%d,75%%,45%%,%.2f)\">%s</td>",
                depth > 0 ? 8 : 210, Math.abs(depth) * 0.6, escape(text));
    }

    private static boolean isNumber(String text) {
        try {
            Double.parseDouble(text);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    static String number(double value) {
        if (Double.isNaN(value)) {
            return "-";
        }
        return Double.isInfinite(value) ? "immune" : String.format(Locale.ROOT, "%.2f", value);
    }

    static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static void write(Path file, String text) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static final String STYLE = """
            :root{--bg:#fbfaf7;--fg:#1f2328;--dim:#667085;--line:#d9d6cf;--head:#f0ede6}
            @media(prefers-color-scheme:dark){:root{--bg:#16181c;--fg:#e6e3dc;--dim:#98a2b3;--line:#33373e;--head:#22252b}}
            body{background:var(--bg);color:var(--fg);font:14px/1.45 system-ui,sans-serif;margin:0;padding:24px 16px 64px;
            max-width:1500px;margin-inline:auto}
            h1{font-size:24px;margin:0 0 8px}h2{font-size:17px;margin:32px 0 4px}
            .intro{max-width:95ch}.note{color:var(--dim);max-width:110ch;margin:0 0 8px}
            .scroll{overflow-x:auto;border:1px solid var(--line);border-radius:6px}
            table{border-collapse:collapse;width:100%;font-variant-numeric:tabular-nums;font-size:13px}
            th{background:var(--head);position:sticky;top:0;cursor:pointer;text-align:left;white-space:nowrap}
            th,td{padding:3px 8px;border-bottom:1px solid var(--line)}td.num{text-align:right}
            td.bad{background:hsla(0,75%,45%,.45);font-weight:600}td.good{background:hsla(140,55%,40%,.25)}
            td.warn{background:hsla(42,90%,50%,.35)}tbody tr:hover{outline:1px solid var(--dim)}
            """;

    private static final String SORT = """
            document.querySelectorAll('th').forEach(th=>th.addEventListener('click',()=>{
            const t=th.closest('table'),i=[...th.parentNode.children].indexOf(th),b=t.tBodies[0];
            const d=th.dataset.d=th.dataset.d==='1'?'-1':'1';
            [...b.rows].sort((x,y)=>{const p=x.cells[i].textContent,q=y.cells[i].textContent,m=parseFloat(p),n=parseFloat(q);
            return d*((isNaN(m)||isNaN(n))?p.localeCompare(q):m-n)}).forEach(r=>b.appendChild(r))}));
            """;
}
