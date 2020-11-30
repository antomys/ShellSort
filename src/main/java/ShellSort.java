import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

public class ShellSort {

    private static int getFileSizeMegaBytes(File file) {
        return (int) file.length() / (1024 * 1024);
    }

    public void shellFilesLexical(String path, String fileName) throws IOException {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        long start;
        File inputFile = new File(path+"/"+fileName);
        int mBperSplit = 1;
        int filesize = getFileSizeMegaBytes(inputFile);
        if(filesize >100) {
            mBperSplit = 100;
        } else if (filesize > 10) {
            mBperSplit = 10;
        }
        Split_30 split_30 = new Split_30(path+"/"+fileName);
        split_30.splitFile(mBperSplit);
        FileWriter fileWriter;
        File outfile = new File(path+"/sorted");
        outfile.mkdir();
        File[] infile = new File(path+"/tmp").listFiles();
        Arrays.sort(infile);
        int fileCount = 0;
        start = System.nanoTime();
        for (File files: infile) {
            Scanner scanner = new Scanner(files);
            //ArrayList<Integer> input = new ArrayList<Integer>();
            ArrayList<String> input = new ArrayList<>();
            while (scanner.hasNext()) {
                //input.add(scanner.nextInt());
                input.add(scanner.next());
            }
            String[] tempsArray = input.toArray(new String[0]);
            //shellSort(input);
            Shell.sort(tempsArray);
            fileWriter = new FileWriter(path+"/sorted/"+ fileCount +".splitPart",false);
            /*for (Integer integer: input) {
                fileWriter.write(integer.toString()+'\n');
            }*/
            for (String aString: input) {
                fileWriter.write(aString+'\n');
            }
            fileCount++;
            fileWriter.close();
            input.clear();
            scanner.close();
        }
        long elapsedTime = (System.nanoTime() - start)/1000000;
        split_30.clearTemp(path+ "/tmp");
        FileWriter file = new FileWriter("src/main/resources/logging", true);
        file.write("Lexical ShellFles "+"Current time: " + dtf.format(now) +
                " Method: ShellSort " + "Time: "+ elapsedTime + " ms" + " File size:" +filesize+"\n");
        file.close();
    }

    public void shellFiles(String path, String fileName) throws IOException {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        long start;
        File inputFile = new File(path+"/"+fileName);
        int mBperSplit = 1;
        int filesize = getFileSizeMegaBytes(inputFile);
        if(filesize >100) {
            mBperSplit = 100;
        } else if (filesize > 10) {
            mBperSplit = 10;
        }
        Split_30 split_30 = new Split_30(path+"/"+fileName);
        split_30.splitFile(mBperSplit);
        FileWriter fileWriter;
        File outfile = new File(path+"/sorted");
        outfile.mkdir();
        File[] infile = new File(path+"/tmp").listFiles();
        Arrays.sort(infile);
        int fileCount = 0;
        start = System.nanoTime();
        for (File files: infile) {
            Scanner scanner = new Scanner(files);
            ArrayList<Integer> input = new ArrayList<Integer>();
            while (scanner.hasNext()) {
                input.add(scanner.nextInt());
            }
            shellSort(input);
            fileWriter = new FileWriter(path+"/sorted/"+ fileCount +".splitPart",false);
            for (Integer integer: input) {
                fileWriter.write(integer.toString()+'\n');
            }
            fileCount++;
            fileWriter.close();
            input.clear();
            scanner.close();
        }
        long elapsedTime = (System.nanoTime() - start)/1000000;
        split_30.clearTemp(path+ "/tmp");
        FileWriter file = new FileWriter("src/main/resources/logging", true);
        file.write("Common ShellFles "+"Current time: " + dtf.format(now) +
                " Method: ShellSort " + "Time: "+ elapsedTime + " ms" + " File size:" +filesize+"\n");
        file.close();
    }

    private static int[] shellSort(int[] arr){
        int interval = 1;
        int temp;
        // interval calculation using Knuth's interval sequence
        while(interval <= arr.length/3){
            interval = (interval * 3) + 1;
        }
        while(interval > 0){
            for(int i = interval; i < arr.length; i++){
                temp = arr[i];
                int j;
                for(j = i; j > interval - 1 && arr[j-interval] >= temp; j=j-interval){
                    arr[j] = arr[j - interval];
                }
                arr[j] = temp;
            }
            // reduce interval
            interval = (interval - 1)/3;
        }
        return arr;
    }
    private static ArrayList<Integer> shellSort(ArrayList<Integer> arr){
        int interval = 1;
        int temp;
        // interval calculation using Knuth's interval sequence
        while(interval <= arr.size()/3){
            interval = (interval * 3) + 1;
        }
        while(interval > 0){
            for(int i = interval; i < arr.size(); i++){
                temp = arr.get(i);
                int j;
                for(j = i; j > interval - 1 && arr.get(j-interval) >= temp; j=j-interval){
                    Collections.swap(arr,j,j-interval);
                }
                arr.set(j,temp);
            }
            // reduce interval
            interval = (interval - 1)/3;
        }
        return arr;
    }
}