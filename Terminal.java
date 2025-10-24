package CLD;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

class Parser {
    private String commandName;
    private String[] args;
    private String redirectoutputfile = null;
    private boolean appendredirect = false;

    public boolean parse(String input) {
        input = input.trim();
        if (input.isEmpty()) return false;

        redirectoutputfile = null;
        appendredirect = false;
        String commandpart = input;

        int appendindex = input.lastIndexOf(">>");
        int redirectindex = input.lastIndexOf(">");

        if (appendindex != -1) {
            commandpart = input.substring(0, appendindex).trim();
            redirectoutputfile = input.substring(appendindex + 2).trim();
            appendredirect = true;
        } else if (redirectindex != -1) {
            commandpart = input.substring(0, redirectindex).trim();
            redirectoutputfile = input.substring(redirectindex + 1).trim();
            appendredirect = false;
        }

        if (commandpart.isEmpty()) {
            System.out.println("Error: Invalid command syntax.");
            return false;
        }
        if (redirectoutputfile != null && redirectoutputfile.isEmpty()) {
            System.out.println("Error: Missing redirection file name.");
            return false;
        }

        List<String> tokens = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();
        boolean inQuote = false;

        for (int i = 0; i < commandpart.length(); i++) {
            char c = commandpart.charAt(i);

            if (c == '\"') {
                inQuote = !inQuote;
            } else if (c == ' ' && !inQuote) {
                if (currentToken.length() > 0) {
                    tokens.add(currentToken.toString());
                    currentToken.setLength(0);
                }
            } else {
                currentToken.append(c);
            }
        }

        if (currentToken.length() > 0) {
            tokens.add(currentToken.toString());
        }

        if (inQuote) {
            System.out.println("Error: Unmatched quotes in command.");
            return false;
        }

        if (tokens.isEmpty()) {
            return false;
        }

        commandName = tokens.get(0);
        args = tokens.subList(1, tokens.size()).toArray(new String[0]);

        return true;
    }

    public String getCommandName() {
        return commandName;
    }

    public String[] getArgs() {
        return args;
    }

    public String getredirectfile() {
        return redirectoutputfile;
    }

    public boolean isappendredirect() {
        return appendredirect;
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
        if (args.length == 0) {
            currentDirectory = new File(System.getProperty("user.home"));
        }

        else if (args.length == 1 && args[0].equals("..")) {
            File parent = currentDirectory.getParentFile();
            if (parent != null) {
                currentDirectory = parent;
            } else {
                System.out.println("Error: Already at root directory.");
            }
        }

        else if (args.length == 1) {
            String path = args[0];
            File newDir = new File(path);

            if (!newDir.isAbsolute()) {
                newDir = new File(currentDirectory, path);
            }

            if (newDir.exists() && newDir.isDirectory()) {
                try{
                    currentDirectory = newDir.getCanonicalFile();
                }
                catch (IOException e) {
                    System.out.println("Error resolving path: " + e.getMessage());
                }
            }
            else{
                System.out.println("Error: No such directory: " + path);
            }

        }
        else{
            System.out.println("Error: cd takes 0 or 1 argument.");
        }
    }

    public void ls() {
        // This is the original behavior: list current directory
        ls(new String[0]);
    }

    // Overloaded method to handle 'ls' and 'ls [path]'
    public void ls(String[] args) {
        File targetDirectory;

        // If no args, use the current directory
        if (args.length == 0) {
            targetDirectory = currentDirectory;

            // If 1 arg, use that as the directory path
        } else if (args.length == 1) {
            String path = args[0];
            targetDirectory = new File(path);

            // Handle relative paths
            if (!targetDirectory.isAbsolute()) {
                targetDirectory = new File(currentDirectory, path);
            }

        } else {
            System.out.println("Error: ls takes 0 or 1 argument.");
            return;
        }

        // Check if the target directory is valid
        if (!targetDirectory.exists() || !targetDirectory.isDirectory()) {
            System.out.println("Error: No such directory: " + targetDirectory.getPath());
            return;
        }

        // Get all files and directories in the target directory
        File[] filesList = targetDirectory.listFiles();

        if (filesList == null) {
            System.out.println("Error: Cannot list contents of " + targetDirectory.getAbsolutePath());
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
        if (args.length == 0) {
            System.out.println("mkdir: missing operand");
            return;
        }
        String fullCommand = String.join(" ", args).trim();
        boolean isQuotedPath = fullCommand.startsWith("\"") && fullCommand.endsWith("\"");

        if (isQuotedPath) {
            String path = fullCommand.substring(1, fullCommand.length() - 1);
            File newDir = new File(path);

            if (newDir.exists()) {
                System.out.println("The directory already exists: " + newDir.getName());
            } else {
                boolean created = newDir.mkdirs();
                if (!created) {
                    if (!newDir.getParentFile().exists()) {
                        System.out.println("Error: parent path does not exist -> " + newDir.getParent());
                    } else {
                        System.out.println("Error creating directory: " + newDir.getPath());
                    }
                }
            }
            return;
        }

        for (String folderName : args) {
            folderName = folderName.trim();
            if (folderName.isEmpty()) continue;

            File newDir = new File(currentDirectory, folderName);

            if (!newDir.exists()) {
                boolean created = newDir.mkdirs();
                if (!created) {
                    System.out.println("Error creating directory: " + newDir.getPath());
                }
            }
        }
    }


    public void rmdir(String[] args) {
        if (args.length == 0) {
            System.out.println("rmdir: missing operand");
            return;
        }

        if (args.length == 1 && args[0].equals("*")) {
            File dir = currentDirectory;
            File[] files = dir.listFiles();
            boolean found = false;

            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory() && (f.list() == null || f.list().length == 0)) {
                        if (f.delete()) {
                            found = true;
                        }
                    }
                }
            }

            if (!found) {
                System.out.println("No empty directories found.");
            }
            return;
        }

        String path = String.join(" ", args).trim();
        if (path.startsWith("\"") && path.endsWith("\"")) {
            path = path.substring(1, path.length() - 1);
        }

        File targetDir;
        if (path.contains("\\") || path.contains("/")) {
            targetDir = new File(path);
        } else {
            targetDir = new File(currentDirectory, path);
        }

        if (!targetDir.exists()) {
            System.out.println("Error: directory does not exist -> " + targetDir.getPath());
        } else if (!targetDir.isDirectory()) {
            System.out.println("Error: not a directory -> " + targetDir.getPath());
        } else if (targetDir.list() != null && targetDir.list().length > 0) {
            System.out.println("Error: directory not empty -> " + targetDir.getPath());
        } else {
            boolean deleted = targetDir.delete();
            if (!deleted) {

                System.out.println("Error removing directory -> " + targetDir.getPath());
            }
        }
    }
    public void cp(String[] args) {
        // to be implemented by nariman
        if (args.length != 2) {
            System.out.println("cp: requires 2 arguments (source and destination)");
            return;
        }
        File sourceFile = new File(currentDirectory, args[0]);
        File destinationFile = new File(currentDirectory, args[1]);
        if (!sourceFile.exists ()){
            System.out.println ("error source doesn't exist ");
            return;
        }
        try (InputStream in = new FileInputStream (sourceFile );
             OutputStream out = new FileOutputStream (destinationFile)){
            byte [] buffer = new byte[1024];
            int length ;
            while ((length = in.read (buffer))>0){
                out.write (buffer , 0 , length);
            }

        }catch (IOException e ){
            System.out.println ("error copy "+e.getMessage());
        }
    }

    public void cp_r(String[] args) {
        // to be implemented by nariman
        if (args.length != 2 ){
            System.out.println ("cp require two arg ");
            return ;
        }
        File sourceDir = new File (currentDirectory , args[0]);
        File destDir = new File (currentDirectory , args [1]);
        if (!sourceDir.exists() || !sourceDir.isDirectory ()){
            System.out.println ("source dir dosn;t exist ");
            return ;
        }
        if (!destDir.exists()){
            destDir.mkdirs();
        }try {
            copydirRec (sourceDir , new File (destDir , sourceDir.getName ()));

        }catch (IOException e ){
            System.out.println ("error copy the file ");
        }
    }
    private void copydirRec(File source, File destination) throws IOException {
        if (source.isDirectory()) {
            if (!destination.exists()) {
                destination.mkdirs();
            }

            String[] children = source.list();
            if (children != null) {
                for (String child : children) {
                    copydirRec(new File(source, child), new File(destination, child));
                }
            }
        } else {
            try (FileInputStream in = new FileInputStream(source);
                 FileOutputStream out = new FileOutputStream(destination)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            }
        }
    }

    /**
     * Helper method for 'zip -r'. Recursively adds files/dirs to the zip stream.
     */
    private void addDirectoryToZip(File fileToZip, String parentPath, ZipOutputStream zos) throws IOException {
        String entryName = parentPath + fileToZip.getName();
        byte[] buffer = new byte[1024];

        if (fileToZip.isDirectory()) {
            // Add the directory entry itself
            zos.putNextEntry(new ZipEntry(entryName + "/"));
            zos.closeEntry();
            System.out.println("Adding directory: " + entryName);

            // Recursively add all contents
            File[] children = fileToZip.listFiles();
            if (children != null) {
                for (File child : children) {
                    addDirectoryToZip(child, entryName + "/", zos);
                }
            }
        } else {
            // Add a file
            try (FileInputStream fis = new FileInputStream(fileToZip);
                 BufferedInputStream bis = new BufferedInputStream(fis)) {

                ZipEntry zipEntry = new ZipEntry(entryName);
                zos.putNextEntry(zipEntry);

                int len;
                while ((len = bis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
                zos.closeEntry();
                System.out.println("Added file: " + entryName);
            }
        }
    }

    // abdelrahman
    public void touch(String[] args) {
        if (args.length == 0 || args.length > 1) {
            System.out.println("Error: touch requires one argument.");
            return;
        }

        String path = args[0];
        File file = new File(path);

        if (!file.isAbsolute()) {
            file = new File(currentDirectory, path);
        }

        try {
            System.out.println(file.createNewFile() ? "new file created !" : "file already exists");
        } catch (IOException e) {
            System.err.println("file cannot be created !" + e.getMessage());
        }
    }

    public void rm(String[] args) {
        if (args.length == 0 || args.length > 1) {
            System.out.println("Error: rm requires one argument.");
            return;
        }

        String path = args[0];
        File file = new File(path);

        if (!file.isAbsolute()) {
            file = new File(currentDirectory, path);
        }

        System.out.println(file.delete() ? "deleted successfully" : "file does not exist !");
    }

    public void cat(String[] args) {
        if (args.length == 0) {
            System.out.println("Error: cat requires at least one file argument.");
            return;
        }

        for (String path : args) {
            File file = new File(path);

            // This 'if' block is the fix for the pathing bug.
            if (!file.isAbsolute()) {
                file = new File(currentDirectory, path);
            }

            // This 'try' block is the fix for the logic.
            // It reads from the file and prints DIRECTLY to System.out.
            // It does NOT use "temp.txt".
            try (Scanner scanner = new Scanner(file)) {
                while (scanner.hasNextLine()) {
                    System.out.println(scanner.nextLine());
                }
            } catch (FileNotFoundException e) {
                System.err.println("Error: File not found: " + path);
                // If one file is not found, print an error but continue to the next one.
            }
        }
    }


    public void wc(String[] args) {
        if (args.length == 0 || args.length > 1) {
            System.out.println("Error: wc requires one argument.");
            return;
        }

        String path = args[0];
        File file = new File(path);

        if (!file.isAbsolute()) {
            file = new File(currentDirectory, path);
        }

        int lineCount = 0;
        int wordCount = 0;
        int charCount = 0;

        try {
            Scanner counter = new Scanner(file); // <-- Use the corrected 'file' variable

            while (counter.hasNextLine()) {
                lineCount++;
                String data = counter.nextLine();
                String[] words = data.split(" ");
                int spaceCount = words.length - 1;
                charCount += spaceCount;
                for(String word : words) {
                    wordCount++;
                    charCount += word.length();
                }
            }

            counter.close();
        }
        catch (FileNotFoundException e) {
            System.out.println("Error: No such file: " + path);
            return;
        }
        System.out.println(lineCount + " " + wordCount + " " + charCount + " " + args[0]);
    }

    public void zip(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: zip [-r] <archive-name.zip> <file_or_dir_1> [file_or_dir_2] ...");
            return;
        }

        boolean recursive = false;
        int argOffset = 0;

        // Check for the -r flag
        if (args[0].equals("-r")) {
            recursive = true;
            argOffset = 1; // Shift all other arguments
            if (args.length < 3) { // Need at least 3 args for -r
                System.out.println("Usage: zip -r <archive-name.zip> <directory_to_compress>");
                return;
            }
        }

        String zipFilename = args[argOffset];
        File zipFile = new File(currentDirectory, zipFilename);

        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            for (int i = argOffset + 1; i < args.length; i++) {
                File fileToZip = new File(args[i]);
                if (!fileToZip.isAbsolute()) {
                    fileToZip = new File(currentDirectory, args[i]);
                }

                if (!fileToZip.exists()) {
                    System.out.println("Warning: File or directory not found, skipping: " + args[i]);
                    continue;
                }

                // If it's a directory and -r is set, use the recursive helper
                if (fileToZip.isDirectory() && recursive) {
                    addDirectoryToZip(fileToZip, "", zos);

                    // If it's a file, use the original logic
                } else if (fileToZip.isFile()) {
                    byte[] buffer = new byte[1024];
                    try (FileInputStream fis = new FileInputStream(fileToZip);
                         BufferedInputStream bis = new BufferedInputStream(fis)) {

                        ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
                        zos.putNextEntry(zipEntry);

                        int len;
                        while ((len = bis.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                        }
                        zos.closeEntry();
                        System.out.println("Added: " + args[i]);
                    }
                } else if (fileToZip.isDirectory() && !recursive) {
                    System.out.println("Warning: Skipping directory (use -r to include): " + args[i]);
                }
            }
            System.out.println("Successfully created zip file: " + zipFilename);
        } catch (IOException e) {
            System.out.println("Error creating zip file: " + e.getMessage());
        }
    }

    public void unzip(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: unzip <archive-name.zip> [-d /path/to/destination/]");
            return;
        }

        String zipFilename = "";
        File destinationDir = currentDirectory;

        if (args.length == 1) {
            zipFilename = args[0];

        } else if (args.length == 3 && args[1].equals("-d")) {
            zipFilename = args[0];
            destinationDir = new File(args[2]);
            if (!destinationDir.isAbsolute()) {
                destinationDir = new File(currentDirectory, args[2]);
            }

        } else if (args.length == 3 && args[0].equals("-d")) {
            zipFilename = args[2];
            destinationDir = new File(args[1]);
            if (!destinationDir.isAbsolute()) {
                destinationDir = new File(currentDirectory, args[1]);
            }

        } else {
            System.out.println("Usage: unzip <archive-name.zip> [-d /path/to/destination/]");
            return;
        }

        File zipFile = new File(currentDirectory, zipFilename);
        if (!zipFile.exists() || !zipFile.isFile()) {
            System.out.println("Error: Zip file not found: " + zipFilename);
            return;
        }

        if (!destinationDir.exists()) {
            destinationDir.mkdirs();
        }

        byte[] buffer = new byte[1024];
        try (FileInputStream fis = new FileInputStream(zipFile);
             ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis))) {

            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                File newFile = new File(destinationDir, zipEntry.getName());

                String canonicalDestDirPath = destinationDir.getCanonicalPath();
                String canonicalNewFilePath = newFile.getCanonicalPath();
                if (!canonicalNewFilePath.startsWith(canonicalDestDirPath + File.separator)) {
                    throw new IOException("Zip slip vulnerability detected! Entry: " + zipEntry.getName());
                }

                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory: " + newFile);
                    }
                } else {
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create parent directory: " + parent);
                    }

                    try (FileOutputStream fos = new FileOutputStream(newFile);
                         BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            bos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
                System.out.println("Unzipped: " + newFile.getPath());
            }
            System.out.println("Successfully unzipped file: " + zipFilename);
        } catch (IOException e) {
            System.out.println("Error unzipping file: " + e.getMessage());
        }
    }

    public void redirectoutput(String commandoutput, String filename, boolean append) {
        File outputfile = new File(currentDirectory, filename);

        try (FileWriter writer = new FileWriter(outputfile, append)) {
            writer.write(commandoutput);
        } catch (IOException e) {
            System.err.println("Error redirecting output: " + e.getMessage());
        }
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
                    ls(args);
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
                    File file = new File("temp.txt");
                    file.delete(); // This is no longer needed since 'cat' is fixed
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
            System.out.print(terminal.pwd() + " >> ");
            String input = scanner.nextLine();

            if (!terminal.parser.parse(input)) continue;

            String command = terminal.parser.getCommandName();
            String[] cmdArgs = terminal.parser.getArgs();
            String redirectfile = terminal.parser.getredirectfile();

            if (redirectfile != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(baos);

                PrintStream oldout = System.out;
                PrintStream olderr = System.err;

                System.setOut(ps);
                System.setErr(ps);

                try {
                    terminal.chooseCommandAction(command, cmdArgs);
                } finally {
                    System.out.flush();
                    System.err.flush();

                    System.setOut(oldout);
                    System.setErr(olderr);
                }

                String output = baos.toString();

                terminal.redirectoutput(output, redirectfile, terminal.parser.isappendredirect());

            } else {
                terminal.chooseCommandAction(command, cmdArgs);
            }
        }
    }
}