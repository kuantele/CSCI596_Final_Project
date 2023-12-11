import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.*;

public class App {
    public static void main(String[] args) throws Exception {
        //ImageFilter imgFilter = new ImageFilter("photo.png");
        //imgFilter.readImage();
		 useThread(1);
		 useThread(2);
		 useThread(3);
		 useThread(5);
		 useThread(6);
		 useThread(10);
		 useThread(15);
		 useThread(30);
    }

	 private static void testAll(ALPR alpr){
		 String directoryPath = "License Plate Photos";

		 // Create a File object for the directory
		 File directory = new File(directoryPath);

		 // Check if the directory exists
		 if (directory.exists() && directory.isDirectory()) {
			 // Get all files in the directory
			 File[] files = directory.listFiles();

			 // Check if files were found
			 if (files != null) {
				 for (File file : files) {
					 alpr.readPlate(file);
				 }
			 }
			 else {
				 System.out.println("No files found in the directory.");
			 }
		 }
		 else {
			 System.out.println("The specified directory does not exist.");
		 }
	 }

	private static void showResult(){
		String inputFileName = "input.txt";
		String outputFileName = "output.txt";

		try {
			// Open input file for reading
			BufferedReader inputFileReader = new BufferedReader(new FileReader(inputFileName));

			// Open output file for reading
			BufferedReader outputFileReader = new BufferedReader(new FileReader(outputFileName));



			String inputLine, outputLine;
			List<String> inputList = new ArrayList<>();
			List<String> outputList = new ArrayList<>();

			// Read lines from both files and compare
			while ((inputLine = inputFileReader.readLine()) != null && (outputLine = outputFileReader.readLine()) != null) {
				inputList.add(inputLine);
				outputList.add(outputLine);
			}

			// Close the readers
			inputFileReader.close();
			outputFileReader.close();

			Collections.sort(inputList);
			Collections.sort(outputList);

			int correctLines = 0;
			int totalLines = inputList.size();
			for(int i = 0; i < totalLines; i++){
				 String input = inputList.get(i);
				 String output = outputList.get(i);

				 if(input.equals(output)) correctLines++;
				 else System.out.println("Input: " + input + " Output: " + output);
			}

			// Calculate correct rate
			double correctRate = (double) correctLines / totalLines * 100;

			System.out.println("Total lines: " + totalLines);
			System.out.println("Correct lines: " + correctLines);
			System.out.println("Correct rate: " + correctRate + "%");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void useThread(int howManyThread) throws InterruptedException {
		Logger logger = Logger.getLogger("profilingLogger");
		FileHandler fileHandler = null;

		try {
            fileHandler = new FileHandler("profiling.log", true);
            fileHandler.setFormatter(new SimpleFormatter() {
                @Override
                public String format(LogRecord record) {
                    return record.getMessage() + "\n";
                }
            });
            logger.addHandler(fileHandler);
            logger.setLevel(Level.INFO);

			//milliseconds
			double startTime = System.nanoTime() / 10e6;

			File file = new File("output.txt");
			if (file.exists()) {
				file.delete();
			}

			File[] files = new File[6];
			files[0] = new File("Training Data/letters-1 image.png");
			files[1] = new File("Training Data/letters-2 image.png");
			files[2] = new File("Training Data/letters-3 image.png");
			files[3] = new File("Training Data/numbers image.png");
			files[4] = new File("Training Data/B.png");
			files[5] = new File("Training Data/0.png");

			int groupSize = 30 / howManyThread;

			Thread[] threads = new Thread[howManyThread];

			for(int i = 0; i < howManyThread; i++){
				int from = i * groupSize + 1;
				int to = from + groupSize - 1;
				threads[i] = new Thread(new Worker(new ALPR(files), from, to));
				threads[i].start();
			}

			for(int i = 0; i < howManyThread; i++){
				threads[i].join();
			}

			showResult();
			//milliseconds
			double endTime = System.nanoTime() / 10e6;
			double timeUsed = endTime - startTime;
			System.out.print("Use ");
			System.out.print(timeUsed);
			System.out.println(" ms");

            logger.log(Level.INFO, howManyThread + "," + timeUsed);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fileHandler != null) {
                fileHandler.close();
            }
        }
	}

}
