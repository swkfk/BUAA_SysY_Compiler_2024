import top.swkfk.compiler.utils.Pair;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Archiver {
    private static final Map<String, String> colors = Map.ofEntries(
        Map.entry("red", "\u001B[31m"),
        Map.entry("green", "\u001B[32m"),
        Map.entry("yellow", "\u001B[33m"),
        Map.entry("blue", "\u001B[34m"),
        Map.entry("cyan", "\u001B[36m"),
        Map.entry("reset", "\u001B[0m")
    );

    private static final String config = """
{
    "programming language": "java",
    "object code": "mips"
}
""";

    private static final String target = "submit";
    private static final String source = "src";

    private static String colorize(String color, String text) {
        return colors.get(color) + text + colors.get("reset");
    }

    @SafeVarargs
    private static void println(Pair<String, String>... items) {
        System.out.println(convert(items));
    }

    @SafeVarargs
    private static String convert(Pair<String, String>... item) {
        StringBuilder sb = new StringBuilder();
        for (var i : item) {
            sb.append(colorize(i.first(), i.second()));
        }
        return sb.toString();
    }

    private static Pair<String, String> p(String color, String text) {
        return new Pair<>(color, text);
    }

    private static String getTime() {
        return new java.text.SimpleDateFormat("yyMMdd_HHmmss").format(new java.util.Date());
    }

    private static List<String> searchJavaFiles() {
        List<String> javaFiles = new LinkedList<>();
        try (var walker = Files.walk(Paths.get(Archiver.source))) {
            walker
                .filter(Files::isRegularFile)
                .filter(f -> f.toString().endsWith(".java"))
                .forEach(f -> javaFiles.add(f.toString()));
        } catch (Exception e) {
            println(p("red", "Fatal: failed to search Java source files!"));
        }
        return javaFiles;
    }

    public static void main(String[] args) {
        println(p("cyan", "// Java Source Code Archiver //"));

        // 0, check the target directory
        try {
            println(
                p("cyan", "-> "),
                p("blue", "try creating directory: "),
                p("green", target)
            );
            Files.createDirectories(Paths.get(target));
        } catch (FileAlreadyExistsException e) {
            // Skip
        } catch (Exception e) {
            println(p("red", "Fatal: failed to create the target directory!"));
            return;
        }

        // 1, generate the target file name
        final String zipFilePath = "homework_" + getTime() + ".zip";
        println(
            p("cyan", "=> "),
            p("blue", "use "),
            p("yellow", "target"),
            p("blue", " as "),
            p("green", zipFilePath)
        );

        // 2, search all the Java source files
        println(
            p("cyan", "=> "),
            p("blue", "use "),
            p("yellow", "source"),
            p("blue", " as "),
            p("green", source)
        );
        println(p("cyan", "-> "), p("blue", "searching Java source files..."));

        final List<String> javaFiles = new LinkedList<>(searchJavaFiles());
        println(
            p("cyan", "=> "),
            p("blue", "found "),
            p("green", String.valueOf(javaFiles.size())),
            p("blue", " files to archive")
        );

        // 3, archive the Java source files
        println(p("cyan", "-> "), p("blue", "archiving Java source files..."));
        try (var zipFile = new ZipOutputStream(Files.newOutputStream(Paths.get(target, zipFilePath)))) {
            zipFile.setLevel(0);
            int lastLineLength = 1;
            for (var javaFile : javaFiles) {
                Path path = Paths.get(javaFile);
                Path entry = Paths.get(source).relativize(path);  // skip the outermost directory

                System.out.print("\r" + " ".repeat(lastLineLength) + "\r");  // clear the last line
                final String message = convert(p("cyan", "=> "), p("blue", "archived "), p("green", entry.toString()));
                System.out.print(message);
                System.out.flush();
                lastLineLength = message.length();

                zipFile.putNextEntry(new ZipEntry(entry.toString()));
                Files.copy(path, zipFile);
                zipFile.closeEntry();
            }

            System.out.print("\r" + " ".repeat(lastLineLength) + "\r");

            println(p("cyan", "-> "), p("blue", "adding config json file..."));
            zipFile.putNextEntry(new ZipEntry("config.json"));
            zipFile.write(config.getBytes());
            zipFile.closeEntry();

        } catch (Exception e) {
            println(p("red", "Fatal: failed to archive Java source files: " + e));
        }

        println(p("cyan", "// Done //"));
    }
}
