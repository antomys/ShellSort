import MinHeap.MinHeap;
import MinHeap.MinHeapLexical;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) throws Exception {
        String path = "/home/antomys/IdeaProjects/ShellSort/src/main/resources";

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();

        ShellSort shellSort = new ShellSort();
        //shellSort.shellFiles(path, "testtest.txt");
        shellSort.shellFilesLexical(path, "testtest.txt");

        File[] file = new File("src/main/resources/sorted").listFiles();
        Arrays.sort(file);
        long start = System.nanoTime();
        //ArrayList<ArrayList<Integer>> inputArray = new ArrayList<>();
        ArrayList<ArrayList<String>> inputArray = new ArrayList<>();
        //ArrayList<Integer> temp = new ArrayList<>();
        ArrayList<String> temp = new ArrayList<>();

        for (File file1 : file) {
            Scanner scanner = new Scanner(file1);
            while (scanner.hasNext())
                //temp.add(scanner.nextInt());
                temp.add(scanner.next());
            inputArray.add(new ArrayList<>(temp));
            temp.clear();
        }
        //MinHeap.merge(inputArray);
        MinHeapLexical.merge(inputArray);

        long elapsedTime = (System.nanoTime() - start)/1000000;
        clearTemporary(path+"/sorted");
        long afterUsedMem=(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/(1024 * 1024);
        FileWriter tempfile = new FileWriter("src/main/resources/logging", true);
        tempfile.write("Current time: " + dtf.format(now) +
                " Method: MergeFiles " + "Time: "+ elapsedTime + " ms" + " Memory Used: "+ afterUsedMem+ " mB "+"\n\n");
        tempfile.close();
    }

    public static void clearTemporary(String path) {
        File inputFile = new File(path);
        for(File file: inputFile.listFiles()){
            file.delete();
        }
    }
}
