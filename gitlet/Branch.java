package gitlet;

import java.io.Serializable;

/** A branch.
 * @author Cheng Zhu
 */
public class Branch implements Serializable {

    /**
     * private variable.
     */
    private String _name;

    /**
     * private variable.
     */
    private Commit _commit;

    public Branch(String name, Commit commit) {
        _commit = commit;
        _name = name;
    }

    public Commit getCommit() {
        return _commit;
    }

    public String getName() {
        return _name;
    }
}
