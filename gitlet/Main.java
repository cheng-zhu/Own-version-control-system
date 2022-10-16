package gitlet;

import java.io.File;
import java.io.IOException;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Cheng Zhu
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) throws IOException {
        bloop = new Repository();
        if (args.length == 0 || args[0].equals("")) {
            System.out.println("Please enter a command.");
            System.exit(0);
        } else if (args[0].equals("init")) {
            bloop.init();
        } else if (args[0].equals("add") && exist()) {
            if (args.length == 1) {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            for (int i = 1; i < args.length; i++) {
                bloop.add(args[i]);
            }
        } else if (args[0].equals("commit") && exist()) {
            if (args.length == 1 || args[1].equals("")) {
                System.out.println("Please enter a commit message.");
                System.exit(0);
            } else if (args.length > 2) {
                System.out.println("Incorrect operands.");
                System.exit(0);
            } else {
                bloop.commit(args[1], null);
            }
        } else if (args[0].equals("checkout") && exist()) {
            if (args.length == 1 || args[1].equals("")) {
                System.out.println("Please enter a checkout message.");
                System.exit(0);
            } else if (args[1].equals("--")) {
                if (args.length != 3) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                bloop.checkout(args[2], true, null);
            } else if (args.length == 4) {
                if (!args[2].equals("--")) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                bloop.checkout(args[3], false, args[1]);
            } else if (args.length == 2) {
                bloop.checkoutbranch(args[1]);
            } else {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
        } else {
            helper(args);
        }
    }

    /** check if there is a .gitlet folder.
     * @return exist
     */
    private static boolean exist() {
        File cwd = new File(System.getProperty("user.dir"));

        File git = new File(".gitlet");
        if (git.exists()) {
            return true;
        }
        System.out.println("Not in an initialized Gitlet directory.");
        System.exit(0);
        return false;
    }

    /**
     * private helper method.
     * @param args
     */
    private static void helper(String... args) throws IOException {
        if (args[0].equals("log") && exist()) {
            bloop.log();
        } else if (args[0].equals("global-log") && exist()) {
            bloop.globallog();
        } else if (args[0].equals("find") && exist()) {
            if (args.length != 2) {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            bloop.find(args[1]);
        } else if (args[0].equals("status") && exist()) {
            if (args.length != 1) {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            bloop.status();
        } else if (args[0].equals("rm") && exist()) {
            if (args.length == 1) {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            for (int i = 1; i < args.length; i++) {
                bloop.remove(args[i]);
            }
        } else if (args[0].equals("branch") && exist()) {
            if (args.length != 2) {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            bloop.branch(args[1]);
        } else if (args[0].equals("rm-branch") && exist()) {
            if (args.length != 2) {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            bloop.rmbranch(args[1]);
        } else if (args[0].equals("reset") && exist()) {
            if (args.length != 2) {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            bloop.reset(args[1]);
        } else if (args[0].equals("merge") && exist()) {
            if (args.length != 2) {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            bloop.merge(args[1]);
        } else {
            System.out.println("No command with that name exists.");
            System.exit(0);
        }
    }

    /**
     * private variable.
     */
    private static Repository bloop;
}
