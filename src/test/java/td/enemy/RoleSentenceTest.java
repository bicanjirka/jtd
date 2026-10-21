package td.enemy;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoleSentenceTest {

    @Test
    void everyBuiltInEnemyHasOneRoleSentenceAtEveryRank() {
        EnemyCatalog catalog = EnemyCatalog.builtIn();
        List<String> problems = new ArrayList<>();

        for (String id : catalog.ids()) {
            String first = catalog.get(id, Rank.GRUNT).description();
            for (Rank rank : Rank.values()) {
                String description = catalog.get(id, rank).description();
                if (description.isEmpty() || !description.endsWith(".") || !description.equals(first)) {
                    problems.add(id + " at " + rank + ": \"" + description + "\"");
                }
            }
        }

        assertThat(problems).isEmpty();
    }
}
