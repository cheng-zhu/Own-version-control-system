package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/** A commit, therefore, will consist of a log message, timestamp, a mapping
 * of file names to blob references, a parent reference, and (for merges) a
 * second parent reference.
 *
 * Including all metadata and references when hashing a commit.
 *
 * Distinguishing somehow between hashes for commits and hashes for blobs.
 * A good way of doing this involves a well-thought out directory structure
 * within the .gitlet directory.
 * @author Cheng Zhu
 */
public class Commit implements Serializable {

    /**
     * private variable.
     */
    private String _message;

    /**
     * private variable.
     */
    private String _timestamp;

    /**
     * private variable.
     */
    private String _parent;

    /**
     * private variable.
     */
    private String _parent2;

    /**
     * private variable.
     */
    private String _hashcode;

    /**
     * private variable.
     */
    private HashMap<String, String> _map = new HashMap<>();

    public Commit(String message, String parent, String parent2, boolean init)
            throws IOException {
        final Date current;
        if (!init) {
            current = new Date();
        } else {
            current = new Date(0);
            File commitFolder = new File(".gitlet/commitFolder");
            File blobFolder = new File(".gitlet/blobFolder");
            commitFolder.mkdir();
            blobFolder.mkdir();
        }
        _timestamp = current.toString();
        _message = message;
        _parent = parent;
        _parent2 = parent2;
        if (parent != null) {
            Commit par = Utils.readObject(new File(
                    ".gitlet/commitFolder/" + parent), Commit.class);
            _map = par.getMap();
        }
        help();
        File dir = new File(".gitlet/stage/add/");
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                _map.put((Utils.readObject(child, Blob.class)).getFilename(),
                        (Utils.readObject(child, Blob.class)).getHashcode());
                child.delete();
            }
        }
        saveCommit();
    }

    /**
     * private method.
     */
    private void help() {
        ArrayList<String> deleteblobs = new ArrayList<>();
        ArrayList<String> addblobs = new ArrayList<>();
        for (String blob : _map.values()) {
            Blob old = Utils.readObject(new File(".gitlet/blobFolder/"
                    + blob), Blob.class);
            File n = new File(".gitlet/stage/add/"
                    + old.getFilename());
            File rv = new File(".gitlet/stage/remove/"
                    + old.getFilename());
            if (n.exists()) {
                deleteblobs.add(blob);
                Blob neew = Utils.readObject(n, Blob.class);
                addblobs.add(neew.getHashcode());
                n.delete();
            }
            if (rv.exists()) {
                deleteblobs.add(blob);
                rv.delete();
                File f = new File(old.getFilename());
                if (f.exists()) {
                    f.delete();
                }
            }
        }
        for (String de : deleteblobs) {
            Blob delete = Utils.readObject(
                    new File(".gitlet/blobFolder/" + de), Blob.class);
            _map.remove(delete.getFilename());
        }
        for (String a : addblobs) {
            Blob ad = Utils.readObject(
                    new File(".gitlet/blobFolder/" + a), Blob.class);
            _map.put(ad.getFilename(), a);
        }
    }

    /**
     * private method.
     */
    private void saveCommit() throws IOException {
        _hashcode = Utils.sha1(Utils.serialize(this));
        File commit = new File(".gitlet/commitFolder/" + _hashcode);
        commit.createNewFile();
        Utils.writeObject(commit, this);
    }

    public String getHashcode() {
        return _hashcode;
    }

    public String getMessage() {
        return _message;
    }

    public String getTimestamp() {
        return _timestamp;
    }

    public String getParent() {
        return _parent;
    }

    public String getParent2() {
        return _parent2;
    }

    public HashMap<String, String> getMap() {
        return _map;
    }
}
