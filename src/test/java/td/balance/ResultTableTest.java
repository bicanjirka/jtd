package td.balance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResultTableTest {

    @TempDir
    Path folder;

    private ResultTable table(String name, String... lines) throws IOException {
        Path file = this.folder.resolve(name + ".tsv");
        Files.write(file, List.of(lines), StandardCharsets.UTF_8);
        return ResultTable.load(file);
    }

    @Test
    void aMarginThatDroppedOnEverySeedIsMarkedAsMoved() throws IOException {
        ResultTable before = this.table("before", "L\tcompetent\tmargin\t200\t210\t190\t205");
        ResultTable after = this.table("after", "L\tcompetent\tmargin\t150\t160\t140\t155");

        String comparison = before.compareWith(after, "before");

        assertThat(comparison).contains("* competent margin").contains("-50.00");
    }

    @Test
    void aMarginThatMovedWithinTheSeedsNoiseIsShownUnmarked() throws IOException {
        ResultTable before = this.table("before", "L\tcompetent\tmargin\t200\t100\t300\t150");
        ResultTable after = this.table("after", "L\tcompetent\tmargin\t210\t90\t280\t170");

        String comparison = before.compareWith(after, "before");

        assertThat(comparison).contains("  competent margin").doesNotContain("* competent margin");
    }

    @Test
    void aLabNumberIsListedOnlyOnceItMovesMoreThanAFewPercent() throws IOException {
        ResultTable before = this.table("before", "L\tlab\tnode.SNIPER.a\t1.50", "L\tlab\tnode.SNIPER.b\t1.50");
        ResultTable after = this.table("after", "L\tlab\tnode.SNIPER.a\t1.51", "L\tlab\tnode.SNIPER.b\t1.80");

        String comparison = before.compareWith(after, "before");

        assertThat(comparison).contains("node.SNIPER.b").doesNotContain("node.SNIPER.a");
    }

    @Test
    void aSavedTableLoadsBackTheSame() throws IOException {
        ResultTable original = this.table("original", "L\tnaive\twon\t1.0\t0.0", "L\tnaive\tmargin\t80.0\t90.0");

        ResultTable reloaded = ResultTable.load(original.save(this.folder.resolve("copy.tsv")));

        assertThat(reloaded.compareWith(original, "copy")).contains("nothing else moved")
                .doesNotContain("* naive");
    }
}
