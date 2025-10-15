package CLD;

import java.io.*;
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
    // mohamed
    public String pwd() {
        // to be implemented by mohamed
        return ""; // i wrote this line to not give an error , you have to change it according to ur code
    }

    public void cd(String[] args) {
        // to be implemented by mohamed
    }

    public void ls() {
        // to be implemented by mohamed
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
        // to be implemented by abdelrahman
    }

    public void rm(String[] args) {
        // to be implemented by abdelrahman
    }

    public void cat(String[] args) {
        // to be implemented by abdelrahman
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
