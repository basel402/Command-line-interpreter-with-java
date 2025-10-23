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

        int appendindex = input.lastIndexOf(">>");
        int redirectindex = input.lastIndexOf(">");

        if (appendindex != -1) {
            String commandpart = input.substring(0, appendindex).trim();
            redirectoutputfile = input.substring(appendindex + 2).trim();
            appendredirect = true;
            input = commandpart;
        } else if (redirectindex != -1) {
            String commandpart = input.substring(0, redirectindex).trim();
            redirectoutputfile = input.substring(redirectindex + 1).trim();
            appendredirect = false;
            input = commandpart;
        }

        if (redirectoutputfile != null && redirectoutputfile.isEmpty()) {
            System.out.println("Error: Missing redirection file name.");
            return false;
        }

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
        File[] filesList = currentDirectory.listFiles();

        if (filesList == null) {
            System.out.println("Error: Cannot list contents of " + currentDirectory.getAbsolutePath());
            return;
        }

        List<String> names = new ArrayList<>();
        for (File f : filesList) {
            names.add(f.getName());
        }

        Collections.sort(names);

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
    // abdelrahman
    public void touch(String[] args) {
        if(args.length == 0 || args.length > 1) return;
        File file = new File(args[0]);

        try {
            System.out.println(file.createNewFile() ? "new file created !" : "file already exists");
        } catch (IOException e) {
            System.err.println("file cannot be created !" + e.getMessage());
        }

    }

    public void rm(String[] args) {
        if(args.length == 0 || args.length > 1) return;
        File file = new File(args[0]);
        System.out.println(file.delete() ? "deleted successfully" : "file does not exist !");
    }

    public void cat(String[] args) {
        if (args.length == 0 || args.length > 2) return;

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
        if(args.length == 0 || args.length > 1) return;

        int lineCount = 0;
        int wordCount = 0;
        int charCount = 0;

        try {
            Scanner counter = new Scanner(new File(args[0]));

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
            System.out.println("Error Occurred " + e.getMessage());
        }
        System.out.println(lineCount + " " + wordCount + " " + charCount + " " + args[0]);
    }

    public void zip(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: zip <archive-name.zip> <file1> [file2] ...");
            return;
        }

        String zipfilename = args[0];
        File zipfile = new File(currentDirectory, zipfilename);

        try (FileOutputStream fos = new FileOutputStream(zipfile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            byte[] buffer = new byte[1024];
            for (int i = 1; i < args.length; i++) {
                File filetozip = new File(currentDirectory, args[i]);

                if (!filetozip.exists() || !filetozip.isFile()) {
                    System.out.println("Warning: File not found or is a directory, skipping: " + args[i]);
                    continue;
                }

                try (FileInputStream fis = new FileInputStream(filetozip);
                     BufferedInputStream bis = new BufferedInputStream(fis)) {

                    ZipEntry zipentry = new ZipEntry(filetozip.getName());
                    zos.putNextEntry(zipentry);

                    int len;
                    while ((len = bis.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                    zos.closeEntry();
                    System.out.println("Added: " + args[i]);
                }
            }
            System.out.println("Successfully created zip file: " + zipfilename);
        } catch (IOException e) {
            System.out.println("Error creating zip file: " + e.getMessage());
        }
    }

    public void unzip(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: unzip <archive-name.zip>");
            return;
        }

        String zipfilename = args[0];
        File zipfile = new File(currentDirectory, zipfilename);

        if (!zipfile.exists() || !zipfile.isFile()) {
            System.out.println("Error: Zip file not found: " + zipfilename);
            return;
        }

        byte[] buffer = new byte[1024];
        try (FileInputStream fis = new FileInputStream(zipfile);
             ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis))) {

            ZipEntry zipentry;
            while ((zipentry = zis.getNextEntry()) != null) {
                File newfile = new File(currentDirectory, zipentry.getName());

                String canonicalpath = newfile.getCanonicalPath();
                String canonicalcurrentdir = currentDirectory.getCanonicalPath();
                if (!canonicalpath.startsWith(canonicalcurrentdir + File.separator)) {
                    throw new IOException("Zip slip vulnerability detected! Entry: " + zipentry.getName());
                }

                if (zipentry.isDirectory()) {
                    if (!newfile.isDirectory() && !newfile.mkdirs()) {
                        throw new IOException("Failed to create directory: " + newfile);
                    }
                } else {
                    File parent = newfile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create parent directory: " + parent);
                    }

                    try (FileOutputStream fos = new FileOutputStream(newfile);
                         BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            bos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
                System.out.println("Unzipped: " + newfile.getPath());
            }
            System.out.println("Successfully unzipped file: " + zipfilename);
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
                    File file = new File("temp.txt");
                    file.delete();
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

