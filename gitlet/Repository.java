package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/** Repository for Gitlet, the tiny stupid version-control system.
 *  @author Cheng Zhu
 */
public class Repository {

    /**
     * master branch.
     */
    private Branch master;

    /**
     * SHA-1 length.
     */
    private final int _length = 40;

    /**
     * master branch get.
     * @return master branch.
     */
    public Branch getMaster() {
        return master;
    }

    public void init() throws IOException {
        File cwd = new File(System.getProperty("user.dir"));
        File git = new File(".gitlet");
        if (git.exists()) {
            System.out.println("A Gitlet version-control system"
                    + " already exists in the current directory.");
            System.exit(0);
        }
        git.mkdir();
        File stage = new File(".gitlet/stage");
        stage.mkdir();
        File branches = new File(".gitlet/branches");
        branches.mkdir();
        Commit initial = new Commit("initial commit", null,
                null, true);
        master = new Branch("master", initial);
        File masters = new File(".gitlet/branches/master");
        File heads = new File(".gitlet/head");
        masters.createNewFile();
        heads.createNewFile();
        Utils.writeObject(masters, master);
        Utils.writeObject(heads, master);
    }

    /** private helper method to get the BRANCH that HEAD is pointing to.
     * @return get head branch.
     */
    private Branch gethead() {
        Branch b = Utils.readObject(new File(".gitlet/head"),
                Branch.class);
        return b;
    }

    public void add(String filename) throws IOException {
        File f = new File(filename);
        if (!f.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }
        File inremove = new File(".gitlet/stage/remove/" + filename);
        if (inremove.exists()) {
            inremove.delete();
            System.exit(0);
        }
        File stage = new File(".gitlet/stage/add/");
        if (!stage.exists()) {
            stage.mkdir();
        }
        byte[] blobText = Utils.readContents(f);
        Blob b = new Blob(filename, blobText);
        File oneBlob = new File(".gitlet/blobFolder/"
                + b.getHashcode());
        oneBlob.createNewFile();
        Utils.writeObject(oneBlob, b);
        Commit head = gethead().getCommit();
        File stagedfile = new File(".gitlet/stage/add/" + filename);
        if (stagedfile.exists()) {
            stagedfile.delete();
        }
        if (!head.getMap().containsValue(b.getHashcode())) {
            stagedfile.createNewFile();
            Utils.writeObject(stagedfile, b);
        }
    }

    public void remove(String filename) throws IOException {
        File stagedfile = new File(".gitlet/stage/add/" + filename);
        boolean stageexist = stagedfile.exists();
        if (stageexist) {
            stagedfile.delete();
        }
        Commit head = gethead().getCommit();
        boolean incurrentcommit = head.getMap().containsKey(filename);
        if (incurrentcommit) {
            File stage = new File(".gitlet/stage/remove/");
            if (!stage.exists()) {
                stage.mkdir();
            }
            File stageremove = new File(".gitlet/stage/remove/"
                    + filename);
            stageremove.createNewFile();
            File f = new File(filename);
            if (f.exists()) {
                f.delete();
            }
        }
        if (!stageexist && !incurrentcommit) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
    }

    public void commit(String arg, Commit parent2) throws IOException {
        File dir = new File(".gitlet/stage/add/");
        File[] directoryListing = dir.listFiles();
        File rm = new File(".gitlet/stage/remove/");
        File[] rms = rm.listFiles();
        if (directoryListing == null && rms == null) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        Commit old = gethead().getCommit();
        String two = null;
        if (parent2 != null) {
            two = parent2.getHashcode();
        }
        Commit commit = new Commit(arg, old.getHashcode(), two, false);
        File branchpointer = new File(".gitlet/branches/"
                + gethead().getName());
        Branch novel = new Branch(gethead().getName(), commit);
        File headpointer = new File(".gitlet/head");
        Utils.writeObject(branchpointer, novel);
        Utils.writeObject(headpointer, novel);
    }

    public void checkout(String filename, boolean head, String commitid) {
        Commit current;
        if (head) {
            current = gethead().getCommit();
        } else {
            commitid = abbr(commitid);
            File com = new File(".gitlet/commitFolder/" + commitid);
            if (!com.exists()) {
                System.out.println("No commit with that id exists.");
                System.exit(0);
            }
            current = Utils.readObject(com, Commit.class);
        }
        if (!current.getMap().containsKey(filename)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        Blob blob = null;
        for (String blobid: current.getMap().values()) {
            blob = Utils.readObject(new File(".gitlet/blobFolder/"
                    + blobid), Blob.class);
            if (blob.getFilename().equals(filename)) {
                break;
            }
        }
        byte[] text = blob.getText();
        File pointer = new File(filename);
        Utils.writeContents(pointer, text);
    }

    public void checkoutbranch(String branch) {
        File cur = new File(".gitlet/branches/" + branch);
        if (!cur.exists()) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }
        Branch novel = Utils.readObject(cur, Branch.class);
        if (novel.getName().equals(gethead().getName())) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }
        resetandcheckouthelper(novel);
    }

    /**
     * private variable.
     */
    private void clearstages() {
        List<String> adds = Utils.plainFilenamesIn(".gitlet/stage/add/");
        if (adds != null) {
            for (String add : adds) {
                File a = new File(".gitlet/stage/add/" + add);
                a.delete();
            }
        }
        List<String> rms = Utils.plainFilenamesIn(".gitlet/stage/remove/");
        if (rms != null) {
            for (String rm : rms) {
                File a = new File(".gitlet/stage/remove/" + rm);
                a.delete();
            }
        }
    }

    public void log() {
        Commit current = gethead().getCommit();
        while (true) {
            printonelog(current);
            String parent = current.getParent();
            if (parent != null) {
                File p = new File(".gitlet/commitFolder/" + parent);
                current = Utils.readObject(p, Commit.class);
            } else {
                break;
            }
        }
    }

    public void globallog() {
        List<String> al = Utils.plainFilenamesIn(".gitlet/commitFolder/");
        for (String log : al) {
            Commit current = Utils.readObject(new File(
                    ".gitlet/commitFolder/" + log), Commit.class);
            printonelog(current);
        }
    }

    /**
     * private variable.
     * @param current
     */
    private void printonelog(Commit current) {
        System.out.println("===");
        System.out.println("commit " + current.getHashcode());
        String[] splitStr = current.getTimestamp().trim().split("\\s+");
        String s = String.format("Date: %s %s %s %s %s -0800", splitStr[0],
                splitStr[1], splitStr[2], splitStr[3], splitStr[5]);
        System.out.println(s);
        System.out.println(current.getMessage() + "\n");
    }

    public void find(String message) {
        List<String> alllogs = Utils.plainFilenamesIn(
                ".gitlet/commitFolder/");
        int i = 0;
        for (String log : alllogs) {
            Commit current = Utils.readObject(new File(
                    ".gitlet/commitFolder/" + log), Commit.class);
            if (current.getMessage().equals(message)) {
                System.out.println(log);
                i++;
            }
        }
        if (i == 0) {
            System.out.println("Found no commit with that message.");
        }
    }

    public void status() {
        System.out.println("=== Branches ===");
        List<String> branches = Utils.plainFilenamesIn(".gitlet/branches/");
        for (String branch : branches) {
            Branch b = Utils.readObject(new File(
                    ".gitlet/branches/" + branch), Branch.class);
            if (b.getName().equals(gethead().getName())) {
                System.out.print("*");
            }
            System.out.println(branch);
        }
        System.out.println("\n" + "=== Staged Files ===");
        List<String> stageadd = Utils.plainFilenamesIn(
                ".gitlet/stage/add/");
        if (stageadd != null) {
            for (String add : stageadd) {
                System.out.println(add);
            }
        }
        System.out.println("\n" + "=== Removed Files ===");
        List<String> stageremove = Utils.plainFilenamesIn(
                ".gitlet/stage/remove/");
        if (stageremove != null) {
            for (String remove : stageremove) {
                System.out.println(remove);
            }
        }
        System.out.println("\n=== Modifications Not Staged For Commit ===");
        System.out.println("\n=== Untracked Files ===");
    }

    public void branch(String branch) throws IOException {
        List<String> branches = Utils.plainFilenamesIn(".gitlet/branches/");
        if (branches.contains(branch)) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        File b = new File(".gitlet/branches/" + branch);
        Branch novel = new Branch(branch, gethead().getCommit());
        b.createNewFile();
        Utils.writeObject(b, novel);
    }

    public void rmbranch(String branch) {
        List<String> branches = Utils.plainFilenamesIn(".gitlet/branches/");
        if (!branches.contains(branch)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        File pointer = new File(".gitlet/branches/" + branch);
        Branch current = Utils.readObject(pointer, Branch.class);
        if (current.getName().equals(gethead().getName())) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        pointer.delete();
    }

    public void reset(String commitid) {
        String com = abbr(commitid);
        File pointer = new File(".gitlet/commitFolder/" + com);
        if (!pointer.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        Commit commit = Utils.readObject(pointer, Commit.class);
        Branch old = Utils.readObject(new File(".gitlet/head"),
                Branch.class);
        File p = new File(".gitlet/branches/" + old.getName());
        Branch novel = new Branch(old.getName(), commit);
        resetandcheckouthelper(novel);
        Utils.writeObject(p, novel);
    }

    /** SHA-1 abbreviation.
     * @param commitid
     * @return full id.
     */
    private String abbr(String commitid) {
        if (commitid.length() != _length) {
            List<String> alllogs = Utils.plainFilenamesIn(
                    ".gitlet/commitFolder/");
            for (String log : alllogs) {
                if (log.startsWith(commitid)) {
                    return log;
                }
            }
        }
        return commitid;
    }

    /**
     * Move the head pointer to the new commit,
     * and the branch point does not change.
     * @param novel
     */
    private void resetandcheckouthelper(Branch novel) {
        Commit head = gethead().getCommit();
        for (String file : novel.getCommit().getMap().keySet()) {
            File cwd = new File(file);
            if (cwd.exists() && !head.getMap().containsKey(file)) {
                System.out.println("There is an untracked file in the way;"
                        + " delete it, or add and commit it first.");
                System.exit(0);
            }
        }
        for (String f : head.getMap().keySet()) {
            if (!novel.getCommit().getMap().containsKey(f)) {
                Utils.restrictedDelete(f);
            }
        }
        for (String f : novel.getCommit().getMap().keySet()) {
            checkout(f, false, novel.getCommit().getHashcode());
        }
        clearstages();
        File h = new File(".gitlet/head");
        Utils.writeObject(h, novel);
    }

    public void merge(String branc) throws IOException {
        Branch current = Utils.readObject(new File(
                ".gitlet/head"), Branch.class);
        File add = new File(".gitlet/stage/add");
        File remove = new File(".gitlet/stage/remove");
        if (Utils.plainFilenamesIn(add).size() > 0) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        if (remove.exists()) {
            if (Utils.plainFilenamesIn(remove).size() > 0) {
                System.out.println("You have uncommitted changes.");
                System.exit(0);
            }
        }
        File b = new File(".gitlet/branches/" + branc);
        if (!b.exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        checkuntracked(current, branc);
        if (current.getName().equals(branc)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
        Branch branch = Utils.readObject(b, Branch.class);
        branchset = new ArrayList<>();
        findallcommitid(branch.getCommit());
        queue = new LinkedList<>();
        checked = new ArrayList<>();
        helper(current, branch, branc);
        commit("Merged " + branch.getName() + " into "
                + current.getName() + ".", branch.getCommit());
    }

    private void helper(Branch current, Branch branch, String branc)
            throws IOException {
        String splitpoint = findsplitpoint(current.getCommit());
        if (current.getCommit().getHashcode().equals(splitpoint)) {
            checkoutbranch(branc);
            System.out.println("Current branch fast-forwarded.");
            System.exit(0);
        } else if (branch.getCommit().getHashcode().equals(splitpoint)) {
            System.out.println(
                    "Given branch is an ancestor of the current branch.");
            System.exit(0);
        }
        Commit split = Utils.readObject(new File(
                ".gitlet/commitFolder/" + splitpoint), Commit.class);
        ArrayList<String> cf = new
                ArrayList<>(current.getCommit().getMap().keySet());
        ArrayList<String> gf = new
                ArrayList<>(branch.getCommit().getMap().keySet());
        ArrayList<String> sf = new
                ArrayList<>(split.getMap().keySet());
        for (String c : cf) {
            String curblob = current.getCommit().getMap().get(c);
            if (!gf.contains(c) && sf.contains(c)) {
                String splitblob = split.getMap().get(c);
                if (!splitblob.equals(curblob)) {
                    conflict(curblob, null);
                }
            } else if (gf.contains(c) && !sf.contains(c)) {
                String givenblob = branch.getCommit().getMap().get(c);
                if (!givenblob.equals(curblob)) {
                    conflict(curblob, givenblob);
                }
            }
            if (!gf.contains(c) && sf.contains(c) && split.getMap()
                    .get(c).equals(current.getCommit().getMap().get(c))) {
                remove(c);
            } else if (gf.contains(c) && sf.contains(c)) {
                String splitblob = split.getMap().get(c);
                String givenblob = branch.getCommit().getMap().get(c);
                if (splitblob.equals(curblob) && !givenblob.equals(splitblob)) {
                    checkout(c, false, branch.getCommit().getHashcode());
                    add(c);
                } else if (!splitblob.equals(curblob) && !givenblob.
                        equals(curblob) && !givenblob.equals(splitblob)) {
                    conflict(curblob, givenblob);
                }
            }
        }
        for (String given : gf) {
            String givenblob = branch.getCommit().getMap().get(given);
            if (!cf.contains(given) && sf.contains(given)) {
                String splitblob = split.getMap().get(given);
                if (!splitblob.equals(givenblob)) {
                    conflict(null, givenblob);
                }
            } else if (!cf.contains(given) && !sf.contains(given)) {
                checkout(given, false, branch.getCommit().getHashcode());
                add(given);
            }
        }
    }

    /** Find the commit id of the split point of CURRENT and BRANCH.
     * @param current
     * @return commit id
     */
    private String findsplitpoint(Commit current) {
        queue.add(current.getHashcode());
        checked.add(current.getHashcode());
        while (queue.size() != 0) {
            String s = queue.poll();
            Commit now = Utils.readObject(new File(
                    ".gitlet/commitFolder/" + s), Commit.class);
            if (branchset.contains(s)) {
                return s;
            }
            if (now.getParent() != null) {
                Commit parent = Utils.readObject(new File(
                        ".gitlet/commitFolder/" + now.getParent()),
                        Commit.class);
                String p = parent.getHashcode();
                if (!checked.contains(p)) {
                    checked.add(p);
                    queue.add(p);
                }
            }
            if (now.getParent2() != null) {
                Commit parent2 = Utils.readObject(new File(
                        ".gitlet/commitFolder/" + now.getParent2()),
                        Commit.class);
                String p2 = parent2.getHashcode();
                if (!checked.contains(p2)) {
                    checked.add(p2);
                    queue.add(p2);
                }
            }
        }
        return null;
    }

    /** Find all the commit ids along the branch, save them to branchset.
     * @param commit
     */
    private void findallcommitid(Commit commit) {
        if (!branchset.contains(commit.getHashcode())) {
            branchset.add(commit.getHashcode());
            if (commit.getParent() != null) {
                Commit parent = Utils.readObject(new File(
                        ".gitlet/commitFolder/" + commit.getParent()),
                        Commit.class);
                findallcommitid(parent);
            }
            if (commit.getParent2() != null) {
                Commit parent2 = Utils.readObject(new File(
                        ".gitlet/commitFolder/" + commit.getParent2()),
                        Commit.class);
                findallcommitid(parent2);
            }
        }
    }

    /**
     * private variable.
     * @param curblobid
     * @param givenblobid
     */
    private void conflict(String curblobid, String givenblobid)
            throws IOException {
        String filename = null;
        String first = "<<<<<<< HEAD\n";
        String second = "";
        String third = "=======\n";
        String fourth = "";
        String fifth = ">>>>>>>\n";
        byte[] sec = null;
        byte[] four = null;
        if (curblobid != null) {
            File cur = new File(".gitlet/blobFolder/" + curblobid);
            Blob current = Utils.readObject(cur, Blob.class);
            filename = current.getFilename();
            sec = current.getText();
        }
        if (givenblobid != null) {
            File giv = new File(".gitlet/blobFolder/" + givenblobid);
            Blob given = Utils.readObject(giv, Blob.class);
            filename = given.getFilename();
            four = given.getText();
        }
        File file = new File(filename);
        if (curblobid == null && givenblobid == null) {
            Utils.writeContents(file, first, second, third, fourth, fifth);
        } else if (curblobid != null && givenblobid != null) {
            Utils.writeContents(file, first, sec, third, four, fifth);
        } else if (curblobid == null && givenblobid != null) {
            Utils.writeContents(file, first, second, third, four, fifth);
        } else {
            Utils.writeContents(file, first, sec, third, fourth, fifth);
        }
        add(filename);
        System.out.println("Encountered a merge conflict.");
    }

    /** if a file in the CWD was not tracked in the current commit but was
     * tracked in the commit-to-merge, then by definition the merge would try
     * to overwrite it in the CWD. In this case there will be an "untracked
     * file in the way" error.
     * @param branch
     * @param current
     */
    private void checkuntracked(Branch current, String branch) {
        File b = new File(".gitlet/branches/" + branch);
        Commit bb = Utils.readObject(b, Branch.class).getCommit();
        for (String filename : bb.getMap().keySet()) {
            File f = new File(filename);
            if (!current.getCommit().getMap().containsKey(filename)
                    && f.exists()) {
                System.out.println("There is an untracked file in the way;"
                        + " delete it, or add and commit it first.");
                System.exit(0);
            }
        }
    }

    /** Contains all the commit ids along the branch.
     */
    private ArrayList<String> branchset;

    /** Queue for finding the split point.
     */
    private LinkedList<String> queue;

    /** Contains all already checked commit ids.
     */
    private ArrayList<String> checked;
}
