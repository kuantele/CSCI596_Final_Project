import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class App {
    public static void main(String[] args) throws Exception {
        //ImageFilter imgFilter = new ImageFilter("photo.png");
        //imgFilter.readImage();

		 //milliseconds
		 double startTime = System.nanoTime() / 10e6;

		 File file = new File("output.txt");
		 if (file.exists()) {
			 file.delete();
		 }

		 ALPR alpr = new ALPR();
		 File[] files = new File[5];
		 files[0] = new File("Training Data/letters-1 image.png");
		 files[1] = new File("Training Data/letters-2 image.png");
		 files[2] = new File("Training Data/letters-3 image.png");
		 files[3] = new File("Training Data/numbers image.png");
		 files[4] = new File("Training Data/B.png");
		 alpr.train(files);

		 testAll(alpr);
		 showResult();
		 //milliseconds
		 double endTime = System.nanoTime() / 10e6;
		 double timeUsed = endTime - startTime;
		 System.out.print("Use ");
		 System.out.print(timeUsed);
		 System.out.println(" ms");
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

			int totalLines = 0;
			int correctLines = 0;

			String inputLine, outputLine;

			// Read lines from both files and compare
			while ((inputLine = inputFileReader.readLine()) != null && (outputLine = outputFileReader.readLine()) != null) {
				totalLines++;

				// Compare the lines
				if (inputLine.equals(outputLine)) {
					correctLines++;
				}
				else{
					System.out.println("Input: " + inputLine + " Output: " + outputLine);
				}
			}

			// Close the readers
			inputFileReader.close();
			outputFileReader.close();

			// Calculate correct rate
			double correctRate = (double) correctLines / totalLines * 100;

			System.out.println("Total lines: " + totalLines);
			System.out.println("Correct lines: " + correctLines);
			System.out.println("Correct rate: " + correctRate + "%");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
