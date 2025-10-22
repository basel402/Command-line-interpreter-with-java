package CLD;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

class Parser {
    private String commandName;
    private String[] args;

    public boolean parse(String input) {
        input = input.trim();
        if (input.isEmpty()) return false;

        String[] tokens = input.split("\\s+");
        commandName = tokens[0];

        if (tokens.length > 1) {
            args = Arrays.copyOfRange(tokens, 1, tokens.length);
        } else {
            args = new String[0];
        }

        return true;
    }

    public String getCommandName() {
        return commandName;
    }

    public String[] getArgs() {
        return args;
    }
}

public class Terminal {
    private Parser parser;
    private File currentDirectory;

    public Terminal() {
        parser = new Parser();
        currentDirectory = new File(System.getProperty("user.dir"));
    }

    public String pwd() {
        return currentDirectory.getAbsolutePath();
    }

    public void cd(String[] args) {
        // Case 1: "cd" with no arguments - change to home directory
        if (args.length == 0) {
            currentDirectory = new File(System.getProperty("user.home"));

            // Case 2: "cd .." - change to parent directory
        } else if (args.length == 1 && args[0].equals("..")) {
            File parent = currentDirectory.getParentFile();
            if (parent != null) {
                currentDirectory = parent;
            } else {
                System.out.println("Error: Already at root directory.");
            }

            // Case 3: "cd [path]" - change to specified directory
        } else if (args.length == 1) {
            String path = args[0];
            File newDir = new File(path);

            // Check if the path is absolute. If not, resolve it relative to the current directory.
            if (!newDir.isAbsolute()) {
                newDir = new File(currentDirectory, path);
            }

            // Check if the new path exists and is a directory
            if (newDir.exists() && newDir.isDirectory()) {
                // Use getCanonicalFile() to resolve ".." and "." in the path
                try {
                    currentDirectory = newDir.getCanonicalFile();
                } catch (IOException e) {
                    System.out.println("Error resolving path: " + e.getMessage());
                }
            } else {
                System.out.println("Error: No such directory: " + path);
            }

            // Error: "cd" with too many arguments
        } else {
            System.out.println("Error: cd takes 0 or 1 argument.");
        }
    }

    public void ls() {
        // Get all files and directories in the current directory
        File[] filesList = currentDirectory.listFiles();

        if (filesList == null) {
            System.out.println("Error: Cannot list contents of " + currentDirectory.getAbsolutePath());
            return;
        }

        // Create a list to hold the names for sorting
        List<String> names = new ArrayList<>();
        for (File f : filesList) {
            names.add(f.getName());
        }

        // Sort the list alphabetically (as required by the assignment)
        Collections.sort(names);

        // Print each name
        for (String name : names) {
            System.out.println(name);
        }
    }

    // nariman
    public void mkdir(String[] args) {
        // to be implemented by nariman
    }

    public void rmdir(String[] args) {
        // to be implemented by nariman
    }

    public void cp(String[] args) {
        // to be implemented by nariman
    }

    public void cp_r(String[] args) {
        // to be implemented by nariman
    }

    // abdelrahman
    public void touch(String[] args) {
        if(args.length == 0) return;
        File file = new File(args[0]);

        try {
            System.out.println(file.createNewFile() ? "new file created !" : "file already exists");
        } catch (IOException e) {
            System.err.println("file cannot be created !" + e.getMessage());
        }

    }

    public void rm(String[] args) {
        if(args.length == 0) return;
        File file = new File(args[0]);
        System.out.println(file.delete() ? "deleted successfully" : "file does not exist !");
    }

    public void cat(String[] args) {
        if (args.length == 0) return;

        try (FileWriter write = new FileWriter("temp.txt")) {
            for (int i = 0; i < args.length; i++) {
                File file = new File(args[i]);

                try (Scanner scanner = new Scanner(file)) {
                    while (scanner.hasNextLine()) {
                        write.write(scanner.nextLine() + System.lineSeparator());
                    }

                } catch (FileNotFoundException e) {
                    System.err.println("File not found" +e.getMessage());
                }
            }

        } catch (IOException e) {
            System.err.println("Error Occurred " + e.getMessage());
        }

        try (Scanner scanner = new Scanner(new File("temp.txt"))) {

            while (scanner.hasNextLine()) {
                System.out.println(scanner.nextLine());
            }

        } catch (FileNotFoundException e) {
            System.err.println("Error Occurred " + e.getMessage());
        }

    }


    public void wc(String[] args) {
        // to be implemented by abdelrahman
    }

    public void zip(String[] args) {
        // to be implemented by abdelrahman
    }

    public void unzip(String[] args) {
        // to be implemented by abdelrahman
    }

    // Redirection (abdelrahman) , this is the >, >> commands
    public void redirectOutput(String commandOutput, String fileName, boolean append) {
        // to be implemented by abdelrahman
    }

    public void chooseCommandAction(String command, String[] args) {
        try {
            switch (command) {
                case "pwd":
                    System.out.println(pwd());
                    break;
                case "cd":
                    cd(args);
                    break;
                case "ls":
                    ls();
                    break;
                case "mkdir":
                    mkdir(args);
                    break;
                case "rmdir":
                    rmdir(args);
                    break;
                case "touch":
                    touch(args);
                    break;
                case "rm":
                    rm(args);
                    break;
                case "cat":
                    cat(args);
                    break;
                case "wc":
                    wc(args);
                    break;
                case "cp":
                    if (args.length > 0 && args[0].equals("-r"))
                        cp_r(Arrays.copyOfRange(args, 1, args.length));
                    else
                        cp(args);
                    break;
                case "zip":
                    zip(args);
                    break;
                case "unzip":
                    unzip(args);
                    break;
                case "exit":
                    System.out.println("Exiting CLI...");
                    System.exit(0);
                    break;
                default:
                    System.out.println(" Unknown command: " + command);
                    break;
            }
        } catch (Exception e) {
            System.out.println(" Error: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        Terminal terminal = new Terminal();
        Scanner scanner = new Scanner(System.in);

        System.out.println(" Command Line Interpreter (Type 'exit' to quit)");

        while (true) {
            System.out.print(">> ");
            String input = scanner.nextLine();

            if (!terminal.parser.parse(input)) continue;

            String command = terminal.parser.getCommandName();
            String[] cmdArgs = terminal.parser.getArgs();

            terminal.chooseCommandAction(command, cmdArgs);
        }
    }
}
