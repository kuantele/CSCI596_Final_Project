import java.io.File;

public class Worker implements Runnable{
    private ALPR alpr;
    private int indexFrom;
    private int indexTo;

    public Worker(ALPR alpr, int from, int to){
        this.alpr = alpr;
        this.indexFrom = from;
        this.indexTo = to;
    }

    @Override
    public void run() {
        String directoryPath = "License Plate Photos";

        // Create a File object for the directory
        File directory = new File(directoryPath);

        // Check if the directory exists
        if (directory.exists() && directory.isDirectory()) {
            // Get all files in the directory
            File[] files = directory.listFiles();

            System.out.println(files.length);

            // Check if files were found
            if (files != null) {
                for(int i = indexFrom - 1; i < indexTo; i++){
                    alpr.readPlate(files[i]);
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
}
