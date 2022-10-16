package gitlet;

import java.io.Serializable;

/** Content of a file.
 * @author Cheng Zhu
 */
public class Blob implements Serializable {

    /**
     * private variable.
     */
    private byte[] _text;

    /**
     * private variable.
     */
    private String _hashcode;

    /**
     * private variable.
     */
    private String _filename;

    public Blob(String filename, byte[] text) {
        _filename = filename;
        _text = text;
        _hashcode = Utils.sha1(Utils.serialize(this));
    }

    public byte[] getText() {
        return _text;
    }

    public String getFilename() {
        return _filename;
    }

    public String getHashcode() {
        return _hashcode;
    }
}
