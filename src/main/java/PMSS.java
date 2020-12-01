import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * PMSS algorithm sorts input text file of strings using polyphase merge procedure with a help of two or more auxilary files.
 * In the first part it uses these files to distribute input data and then merges them in the second part.
 * <p>
 * Distribution of the input data is based on Fibonacci numbers sequence which is depending
 * on number of auxilary files that algorithm is working with. The algorithm uses internal
 * data structure <i>PriorityQueue</i> to store run elements. Priority queue is also used
 * to find a minimum element that will be first written to the output file.
 * The algorithm uses a class named <i>StringRecord</i> which represent a single line in
 * input file defined by two attributes: value itself and file index where value is stored in.
 * All elements of the priority queue are of type StringRecord.
 * <p>
 * Distribute procedure repeats until entire input data is distributed following Fibonacchi sequence numbers.
 * Merge procedure repeats until all the data is sorted in ascending order. The algorithm
 * produces a brand new output file which contains sorted data and thus retains input file unchanged.
 * <p>
 *
 * @throws IOException       if an input or output exception occurred during file operations.
 *
 */
public class PMSS
{
    /** Amount of input data read by input file reader in distribute phase of the algorithm. */
    static long data_read = 0;

    /** Variable used to store the first element of next run. Used only in writeNextStringRun() method. */
    static String next_run_element;

    /** Index of current active output file where runs are being merged. */
    static int output_file_index;

    /** Index of previous active output file (previous distribution level). */
    static int old_output_file_index;

    /** Amount of runs on current distribution level. */
    static int runs_per_level;

    /** Array used to store missing (dummy) runs for each input file after the distribute phase. */
    static int missing_runs[];

    /** Array used to determine distribution of runs in input files. Each input file should
     * contain exactly the same amount of runs as specified in this arrays indexes.
     **/
    static int distribution_array[];

    /** Array used as a semaphore for input file readers. If value on a certain index equals 1,
     * input file reader is allowed to read from attached file. If value is 0, reading is not allowed.
     **/
    static int allow_read[];

    /** Array used to store all last elements of the current runs from each input file. */
    static String last_elements[];

    /** Array used to store all last elements of the current runs from each input file. Used only in
     * distribute phase of the algorithm.
     **/
    static String run_last_elements[];

    /** Array used to store all first elements of the next runs from each input file. */
    static StringRecord next_run_first_elements[];

    /**
     * OldClasses.Main data structure of the algorithm. It is used to extract next minimum string
     * that needs to be written to output file. When q is empty on a certain distribution
     * level, all runs on this level have merged to output file.
     */
    static PriorityQueue<StringRecord> q = new PriorityQueue<StringRecord>();


    private static int getFileSizeMegaBytes(File file) {
        return (int) file.length() / (1024 * 1024);
    }

    public static void main(String args[]) throws Exception
    {
        int temp_files = 24;
        String file_extension = ".splitPart";
        String working_dir = "src/main/resources/";
        File main_file = new File(working_dir + "test.txt"); //TODO: CHANGE FILENAME AND DIRECTORY
        File sorted_file = new File(working_dir + "/sorted.txt");
        BufferedReader main_file_reader = new BufferedReader(new FileReader(main_file));
        long main_file_length = main_file.length();

        if (getFileSizeMegaBytes(main_file)>100)
            temp_files = 50;
        else if(getFileSizeMegaBytes(main_file)<10)
            temp_files = 2;



        File working_files[] = new File[temp_files + 1];

        sorted_file.delete();

        allow_read = new int[temp_files + 1];
        missing_runs = new int[temp_files + 1];
        last_elements = new String[temp_files + 1];
        distribution_array = new int[temp_files + 1];
        run_last_elements = new String[temp_files + 1];
        next_run_first_elements = new StringRecord[temp_files + 1];

        String working_file_name = "working_file_";
        BufferedReader run_file_readers[] = new BufferedReader[temp_files + 1];

        for(int i=0; i<working_files.length; i++)
        {
            working_files[i] = new File(working_dir + working_file_name + (i+1) + file_extension);
        }

        /* START - initial run distribution */

        distribute(temp_files, working_files, main_file_length, main_file_reader);

        /* END - initial run distribution */

        /* START - polyphase merge */

        long start = System.currentTimeMillis();

        int min_dummy_values = getMinDummyValue();
        initMergeProcedure(min_dummy_values);
        BufferedWriter writer = new BufferedWriter(new FileWriter(working_files[output_file_index]));

        for(int i=0; i<run_file_readers.length-1; i++)
        {
            run_file_readers[i] = new BufferedReader(new FileReader(working_files[i]));
        }

        while(runs_per_level > 1)
        {
            last_elements[output_file_index] = null;
            merge(distribution_array[getMinFileIndex()] - min_dummy_values, run_file_readers, writer);

            setPreviousRunDistributionLevel();
            updateOutputFileIndex();
            resetAllowReadArray();

            min_dummy_values = getMinDummyValue();
            writer = new BufferedWriter(new FileWriter(working_files[output_file_index]));
            run_file_readers[old_output_file_index] = new BufferedReader(new FileReader(working_files[old_output_file_index]));
        }

        writer.close();
        main_file_reader.close();
        closeReaders(run_file_readers);
        clearTempFiles(working_dir, main_file, working_files);

        long end = System.currentTimeMillis();
        /* END - polyphase merge */

        System.out.println("Merge phase done in " + (end-start) + " ms");
    }

    /**
     * Distributes contents of main input file to @temp_files temporary output files.
     * Input data is distributed according to distribution_array which contains for
     * every temporary file a predifined amount of runs that should reside in certain file
     * on a certain distribution level. Calculation of array values is based on Fibonacci
     * sequence numbers. When input data on a certain level is distributed, next level is calculated
     * if and only if the input file is not consumed yet.
     *
     * @param temp_files       number of temporary files to work with.
     * @param working_files    array of all working files. The size of this array equals @temp_files.
     * @param main_file_length length of the main input file.
     * @param main_file_reader reader used to read main input file.
     */
    private static void distribute(int temp_files, File working_files[], long main_file_length, BufferedReader main_file_reader)
    {
        try
        {
            long start = System.currentTimeMillis();

            runs_per_level = 1;
            distribution_array[0] = 1;
            output_file_index = working_files.length - 1;

            int write_sentinel[] = new int[temp_files];
            BufferedWriter run_file_writers[] = new BufferedWriter[temp_files];

            for(int i=0; i<temp_files; i++)
            {
                run_file_writers[i] = new BufferedWriter(new FileWriter(working_files[i],false));
            }

            /* START - distribute runs */
            while(data_read < main_file_length)
            {
                for(int i=0; i<temp_files; i++)
                {
                    while(write_sentinel[i] != distribution_array[i])
                    {
                        while(runsMerged(main_file_length, i, next_run_element))
                        {
                            writeNextStringRun(main_file_length, main_file_reader, run_file_writers[i], i);
                        }

                        writeNextStringRun(main_file_length, main_file_reader, run_file_writers[i], i);
                        missing_runs[i]++;
                        write_sentinel[i]++;
                    }
                }
                setNextDistributionLevel();
            }

            closeWriters(run_file_writers);

            //Begin of Shell Sorting Files

            File dir = new File("src/main/resources");
            File[] files = dir.listFiles((dir1, name) -> name.toLowerCase().startsWith("working"));
            Arrays.sort(files);
            for(File file: files) {
                //int sortedFileIndex = 1;
                Scanner scanner = new Scanner(file);
                ArrayList<String> strings = new ArrayList<>();
                while(scanner.hasNext()) {
                    strings.add(scanner.next());
                }
                String[] tempsArray = strings.toArray(new String[0]);
                ShellSort.sort(tempsArray);

                FileWriter fileWriter = new FileWriter("src/main/resources/tmp/"+file.getName(),false);
                for (String aString: tempsArray) {
                    fileWriter.write(aString+'\n');
                }
                //sortedFileIndex++;
                fileWriter.close();
                strings.clear();
            }


            long end = System.currentTimeMillis();
            System.out.println("Distribute phase done in " + (end-start) + " ms");

            setPreviousRunDistributionLevel();
            setMissingRunsArray();



            /* END - distribute runs */
        }

        catch(Exception e)
        {
            System.out.println("Exception thrown in distribute(): " + e.getMessage());
        }
    }

    /**
     * Merges predefined amount of dummy runs from all input files to output file.
     * Amount of dummy runs that can be merged is defined by @param min_dummy.
     * Since there should be no special markers in input files, all the merging results
     * as adequate substraction/addition in missing_runs array.
     * Additionaly this method resets allow_read array once dummy run merge is over.
     *
     * @param min_dummy minimum amount of runs which is the same for all input files.
     */
    private static void initMergeProcedure(int min_dummy)
    {
        try
        {
            for(int i=0; i<missing_runs.length - 1; i++)
            {
                missing_runs[i] -= min_dummy;
            }

            missing_runs[output_file_index] += min_dummy;

            resetAllowReadArray();
        }

        catch(Exception e)
        {
            System.out.println("Exception thrown in initMergeProcedure(): " + e.getMessage());
        }
    }

    /**
     * Merges @param min_file_values runs into a single run and writes it to output file bounded by @param writer.
     * @param min_file_values is a minimum number of runs per current distribution level. Procedure merges @param min_file_values
     * from all of the input files and terminates afterwards.
     *
     * @param min_file_values  number of runs that will be merged in a single execution of merge procedure.
     * @param run_file_readers array of all input file readers.
     * @param writer           writer for output file.
     */
    private static void merge(int min_file_values, BufferedReader run_file_readers[], BufferedWriter writer)
    {
        String line;
        int min_file;
        int heap_empty = 0;
        StringRecord record;

        /* Initial heap population */
        populateHeap(run_file_readers);

        try
        {
            while(heap_empty != min_file_values)
            {
                record = q.poll();
                writer.write(record.getValue() + "\n");
                min_file = record.getFileIndex();

                if(allow_read[min_file] == 1 && (line = readFileLine(run_file_readers[min_file],min_file)) != null)
                {
                    q.add(new StringRecord(line,min_file));
                }

                try
                {
                    /* Once heap is empty all n-th runs have merged */
                    if(q.size() == 0)
                    {
                        heap_empty++;

                        for(int i=0; i<next_run_first_elements.length; i++)
                        {
                            try
                            {
                                q.add(new StringRecord(next_run_first_elements[i].getValue(),i));
                                last_elements[i] = next_run_first_elements[i].getValue();
                            }
                            catch(Exception e){}
                        }

                        populateHeap(run_file_readers);
                        resetAllowReadArray();

                        if(heap_empty == min_file_values)
                        {
                            writer.close();
                            return;
                        }
                    }
                }
                catch(Exception e){}
            }
        }
        catch(Exception e)
        {
            System.out.println("Exception thrown in merge(): " + e.getMessage());
        }
    }

    /**
     * Updates current output_file_index which points to output file where runs are being merged to.
     */
    private static void updateOutputFileIndex()
    {
        if(output_file_index > 0)
        {
            output_file_index--;
        }
        else
        {
            output_file_index = distribution_array.length - 1;
        }
    }

    /**
     * Reads next value of each run into a priority queue. Reading is allowed if certain input file
     * contains no dummy runs and reading from input file is allowed by allow_read array.
     *
     * @param run_file_readers array of all input file readers.
     */
    private static void populateHeap(BufferedReader run_file_readers[])
    {
        try
        {
            String line;

            for(int i=0;i<run_file_readers.length;i++)
            {
                if(missing_runs[i] == 0)
                {
                    if((allow_read[i] == 1) && (line = readFileLine(run_file_readers[i],i)) != null)
                    {
                        q.add(new StringRecord(line,i));
                    }
                }

                else
                {
                    missing_runs[i]--;
                }
            }
        }
        catch(Exception e)
        {
            System.out.println("Exception thrown while initial heap population: " + e.getMessage());
        }
    }

    /**
     * Reads next line of text from file bounded by @param file_reader.
     *
     * @param file_reader reader used to read from input file.
     * @param file_index  index of a file from which @param file_reader reads the data.
     * @return next line of text from file, null instead.
     */
    private static String readFileLine(BufferedReader file_reader, int file_index)
    {
        try
        {
            String current_line = file_reader.readLine();

            /* End of run */
            if(last_elements[file_index] != null && current_line.compareTo(last_elements[file_index]) < 0)
            {
                next_run_first_elements[file_index] = new StringRecord(current_line,file_index);
                allow_read[file_index] = 0;

                return null;
            }

            else
            {
                last_elements[file_index] = current_line;
                return current_line;
            }
        }
        catch(Exception e)
        {
            allow_read[file_index] = 0;
            next_run_first_elements[file_index] = null;
        }

        return null;
    }

    /**
     * Resets allow_read array to its initial state. It additionally sets
     * index of output_file_index to 0 and thus prevents read operations
     * from output file.
     */
    private static void resetAllowReadArray()
    {
        for(int i=0; i<allow_read.length; i++)
        {
            allow_read[i] = 1;
        }

        allow_read[output_file_index] = 0;
    }

    /**
     * Returns the minimum amount of dummy runs present amongst input files.
     *
     * @return the minimum amount of present runs.
     */
    private static int getMinDummyValue()
    {
        int min = Integer.MAX_VALUE;

        for(int i=0; i<missing_runs.length; i++)
        {
            if(i != output_file_index && missing_runs[i] < min)
            {
                min = missing_runs[i];
            }
        }

        return min;
    }

    /**
     * Returns index of a file which according to distribution_array contains the minimum amount of runs.
     *
     * @return index of a file with minimum amount of runs.
     */
    private static int getMinFileIndex()
    {
        int min_file_index = -1;
        int min = Integer.MAX_VALUE;

        for(int i=0; i<distribution_array.length; i++)
        {
            if(distribution_array[i] != 0 && distribution_array[i] < min)
            {
                min_file_index = i;
            }
        }

        return min_file_index;
    }

    /**
     * Writes next string run to output file pointed by @param run_file_writer.
     *
     * @param main_file_length length of the main input file.
     * @param main_file_reader reader used to read main input file.
     * @param run_file_writer  writer used to write to a specific output file
     * @param file_index       used to update missing_runs and run_last_elements arrays.
     *                         In case when main input file is read to the end amount of
     *                         dummy runs on this index in missing_runs array needs to be
     *                         decreased. When run is ended run_last_elements array on
     *                         this index is populated with the last element of the run.
     *
     */
    private static void writeNextStringRun(long main_file_length, BufferedReader main_file_reader, BufferedWriter run_file_writer, int file_index)
    {
        if(data_read >= main_file_length)
        {
            missing_runs[file_index]--;
            return;
        }

        try
        {
            if(next_run_element != null)
            {
                run_file_writer.write(next_run_element + "\n");
                data_read += next_run_element.length() + 1;
            }

            String min_value = "";
            String current_line = main_file_reader.readLine();

            /* Case if run is a single element: acordingly update variables and return */
            if(next_run_element != null)
            {
                if(next_run_element.compareTo(current_line) > 0)
                {
                    run_last_elements[file_index] = next_run_element;
                    next_run_element = current_line;

                    return;
                }
            }

            while(current_line !=  null)
            {
                if(current_line.compareTo(min_value) >= 0)
                {
                    run_file_writer.write(current_line + "\n");
                    data_read += current_line.length() + 1;

                    min_value = current_line;
                    current_line = main_file_reader.readLine();
                }

                else
                {
                    next_run_element = current_line;
                    run_last_elements[file_index] = min_value;

                    return;
                }
            }
        }
        catch(Exception e){}
    }

    /**
     * Checks if two adjacent runs have merged into a single run. It does this by comparing the first element
     * of the second run with the last element of the first run. If this two elements are in sorted order, the runs
     * have merged.
     *
     * @param main_file_length length of the main input file.
     * @param file_index       index in array from which last element of the first run is taken for comparison.
     * @param first_element    first element of the second run to be taken into comparison.
     * @return true if the runs have merged, false instead.
     */
    private static boolean runsMerged(long main_file_length, int file_index, String first_element)
    {
        if(data_read < main_file_length && run_last_elements[file_index] != null && first_element != null)
        {
            return run_last_elements[file_index].compareTo(first_element) <= 0 ? true:false;
        }
        return false;
    }

    /**
     * Calculates next run distribution level. The new level is calculated following
     * Finonacchi sequence rule.
     */
    private static void setNextDistributionLevel()
    {
        runs_per_level = 0;
        int current_distribution_array[] = distribution_array.clone();

        for(int i=0; i<current_distribution_array.length - 1; i++)
        {
            distribution_array[i] = current_distribution_array[0] + current_distribution_array[i+1];
            runs_per_level +=  distribution_array[i];
        }
    }

    /**
     * Calculates previous run distribution level. The new level is calculated following.
     * Finonacchi sequence rule.
     */
    private static void setPreviousRunDistributionLevel()
    {
        int diff;
        int current_distribution_array[] = distribution_array.clone();
        int last = current_distribution_array[current_distribution_array.length - 2];

        old_output_file_index = output_file_index;

        runs_per_level = 0;
        runs_per_level += last;
        distribution_array[0] = last;

        for(int i=current_distribution_array.length - 3; i>=0; i--)
        {
            diff = current_distribution_array[i] - last;
            distribution_array[i+1] = diff;
            runs_per_level += diff;
        }
    }

    /**
     * Calculates the amount of dummy runs for every input file after distribute phase of the algorithm.
     */
    private static void setMissingRunsArray()
    {
        for(int i=0; i<distribution_array.length - 1; i++)
        {
            missing_runs[i] = (distribution_array[i] - missing_runs[i]);
        }
    }

    /**
     * Closes all file readers used to read from run files in distribute phase of the algorithm.
     *
     * @param run_file_readers array of readers to read from run files.
     */
    private static void closeReaders(BufferedReader run_file_readers[])
    {
        try
        {
            for(int i=0; i<run_file_readers.length; i++)
            {
                run_file_readers[i].close();
            }
        }

        catch(Exception e){}
    }

    /**
     * Closes all file writers used to write to run files in distribute phase of the algorithm.
     *
     * @param run_file_writers array of writers to write to run files.
     */
    private static void closeWriters(BufferedWriter run_file_writers[])
    {
        try
        {
            for(int i=0; i<run_file_writers.length; i++)
            {
                run_file_writers[i].close();
            }
        }

        catch(Exception e){}
    }

    /**
     * Clears all unnecessary temporary files and renames the sorted one to its final name.
     *
     * @param working_dir path to working directory where entire sorting process is taking place.
     * @param main_file main input file.
     * @param temp_files number of temporary files to work with.
     */
    private static void clearTempFiles(String working_dir, File main_file, File temp_files[])
    {
        File sorted_file = new File(working_dir+"sorted.txt");

        for(int i=0; i<temp_files.length; i++)
        {
            if(temp_files[i].length() == main_file.length())
            {
                temp_files[i].renameTo(sorted_file);
                temp_files[i].delete();
            }

            temp_files[i].delete();
        }
    }
}
