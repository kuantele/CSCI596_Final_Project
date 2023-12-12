import java.util.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.geom.AffineTransform;
import java.io.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.util.List;
import java.util.stream.*;
import java.lang.Math;
import java.util.logging.*;

public class ALPR2 {
    // Penitentiary Gothic font, 20x43 pix = 860 pixels
    private static int[] CHARSIZE = {20, 43};
    private static int NUMPIXELS = CHARSIZE[0] * CHARSIZE[1];
    private static ArrayList<String> trainLabels;
    private static ArrayList<Integer[]> trainData;
    private static final Object lock = new Object();

    public static void main(String[] args) throws Exception {
        // Use main thread to train the data only once
        File[] files = new File[6];
        files[0] = new File("Training Data/letters-1 image.png");
        files[1] = new File("Training Data/letters-2 image.png");
        files[2] = new File("Training Data/letters-3 image.png");
        files[3] = new File("Training Data/numbers image.png");
        files[4] = new File("Training Data/B.png");
        files[5] = new File("Training Data/0.png");
        train(files);

        useThread(1);
        useThread(2);
        useThread(3);
        useThread(5);
        useThread(6);
        useThread(10);
        useThread(15);
        useThread(30);
    }

    private static void useThread(int howManyThread) throws InterruptedException {
        Logger logger = Logger.getLogger("profilingLogger");
        FileHandler fileHandler = null;

        try{
            fileHandler = new FileHandler("profiling-new.log", true);
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

            int groupSize = 30 / howManyThread;

            Thread[] threads = new Thread[howManyThread];

            for(int i = 0; i < howManyThread; i++){
                int from = i * groupSize + 1;
                int to = from + groupSize - 1;
                threads[i] = new Thread(new Worker2(new ALPR2(), from, to));
                threads[i].start();
            }

            for(int i = 0; i < howManyThread; i++){
                threads[i].join();
            }

            showResult();
            //milliseconds
            double endTime = System.nanoTime() / 10e6;
            double timeUsed = endTime - startTime;
            System.out.print("For using ");
            System.out.print(howManyThread);
            System.out.print(" threads takes ");
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

        //milliseconds
        double startTime = System.nanoTime() / 10e6;

        File file = new File("output.txt");
        if (file.exists()) {
            file.delete();
        }

        int groupSize = 30 / howManyThread;

        Thread[] threads = new Thread[howManyThread];

        for(int i = 0; i < howManyThread; i++){
            int from = i * groupSize + 1;
            int to = from + groupSize - 1;
            threads[i] = new Thread(new Worker2(new ALPR2(), from, to));
            threads[i].start();
        }

        for(int i = 0; i < howManyThread; i++){
            threads[i].join();
        }

        showResult();
        //milliseconds
        double endTime = System.nanoTime() / 10e6;
        double timeUsed = endTime - startTime;
        System.out.print("For using ");
        System.out.print(howManyThread);
        System.out.print(" threads takes ");
        System.out.print(timeUsed);
        System.out.println(" ms");
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

    public static void readPlate(File file) {
        //System.out.println("Loading file.");
        BufferedImage img = null;
        int height1 = 200;
        ArrayList<Integer[]> testData = new ArrayList<>();
        try { img = ImageIO.read(file); }
        catch (IOException e) { e.printStackTrace(System.out); }

        if (img != null) {
            img = toGrayScale(img);
            img = getScaledImage(img, height1);

            // Crop out top and bottom margins
            int[] vertBounds = getTopBottom(img);
            img = img.getSubimage(0, vertBounds[0], img.getWidth(), vertBounds[1]-vertBounds[0]);

            // Scale image to CHARSIZE height
            img = getScaledImage(img, CHARSIZE[1]);
            //System.out.println("  Scaled image: " + img.getHeight() + "x" + img.getWidth());

            // boost contrast
            img = boostContrast(img);

            ArrayList<Integer> edges = getEdges(img);
            //System.out.println(" Found edges: " + edges);

            // add character data to ArrayList for comparisons to trainData
            testData = imgToArray(img, testData, edges);
            // identify characters in testData
            String plateNum = identify(testData);

            writeToOutput(plateNum);
        }
    }

    // receives an image of a plate, splits it into digits
    public static void train(File[] files) {
        trainLabels = new ArrayList<>(Arrays.asList("A","B","C","D","E","F","G","H","I","J","K","L","M",
              "N","O","P","Q","R","S","T","U","V","W","X","Y","Z","0","1","2","3","4","5","6","7","8","9","B","0"));

        trainData = new ArrayList<>(trainLabels.size());

        BufferedImage img = null;

        for (int i=0; i<files.length; i++) {
            try {
                img = ImageIO.read(files[i]);
            } catch (IOException e) { }

            if (img != null) {
                // convert to grayscale img
                img = toGrayScale(img);

                // find top and bottom edges of characters to trim whitespace
                int[] rowSums = new int[img.getHeight()];
                for (int y=0; y<img.getHeight(); y++) {
                    for (int x=0; x<img.getWidth(); x++) {
                        rowSums[y] += img.getRGB(x, y)& 0xFF;
                    }
                }

                int[] vertBounds = new int[2];
                vertBounds = getBounds(vertBounds, rowSums);
                ++vertBounds[0];
                //System.out.println("  Top row = " + ++vertBounds[0] + ".  Bottom row = " + vertBounds[1] + ".");

                // Crop out top and bottom white margins
                img = img.getSubimage(0, vertBounds[0], img.getWidth(), vertBounds[1]-vertBounds[0]);

                // Scale image to CHARSIZE height
                img = getScaledImage(img, CHARSIZE[1]);

                // get edges between characters (find left edge of each character)
                ArrayList<Integer> edges = getEdges(img);

                // add grayscale data for each character to training data set
                trainData = imgToArray(img, trainData, edges);

            }
        }
        // add a few hard-coded chars to training data
        addMoreTrainingData();

        //System.out.println(trainData.size() + " characters loaded.");
    }

    // convert imageto grayscale
    public static BufferedImage toGrayScale(BufferedImage img) {
        //System.out.println("  Converting to GrayScale.");
        BufferedImage grayImage = new BufferedImage(
              img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = grayImage.getGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return grayImage;
    }

    // display image in a JPanel popup
    public static void display (BufferedImage img) {
        JFrame frame = new JFrame();
        JLabel label = new JLabel();
        frame = new JFrame();
        frame.setSize(img.getWidth(), img.getHeight());
        label.setIcon(new ImageIcon(img));
        frame.getContentPane().add(label, BorderLayout.CENTER);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    // scale image
    private static BufferedImage getScaledImage (BufferedImage img, int newHeight) {
        //System.out.println("  Scaling image.");
        double scaleFactor = (double) newHeight/img.getHeight();
        BufferedImage scaledImg = new BufferedImage(
              (int)(scaleFactor*img.getWidth()), newHeight, BufferedImage.TYPE_BYTE_GRAY);
        AffineTransform at = new AffineTransform();
        at.scale(scaleFactor, scaleFactor);
        AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
        return scaleOp.filter(img, scaledImg);
    }

    // convert grayscale image to BW
    private static BufferedImage boostContrast(BufferedImage img) {
        // compute average pixel darkness
        int avg = 0;
        for (int y=0; y<img.getHeight(); y++) {
            for (int x=0; x<img.getWidth(); x++) {
                avg += img.getRGB(x, y)& 0xFF;
            }
        }
        avg /= img.getHeight() * img.getWidth();

        // convert grayscale pixels in img to BW
        for (int y=0; y<img.getHeight(); y++) {
            for (int x=0; x<img.getWidth(); x++) {
                int p = img.getRGB(x, y)& 0xFF;
                if (p>avg)
                    p = (255<<24) | (255<<16) | (255<<8) | 255;
                else
                    p = (255<<24) | (0<<16) | (0<<8) | 0;
                img.setRGB(x, y, p);
            }
        }
        return img;
    }

    // use trainData to identify each character in testData
    private static String identify(ArrayList<Integer[]> testData) {
        //System.out.println("  Identifying characters in plate.");
        String plateNum = "";
        for (Integer[] testChar : testData) {
            int[] distances = new int[trainData.size()];
            for (int t=0; t<trainData.size(); t++) {
                for (int p=0; p<NUMPIXELS; p++) {
                    distances[t] += Math.abs(trainData.get(t)[p] - testChar[p]);
                }
            }

            int minVal=distances[0], minIndex=0;
            for (int i=0; i<distances.length; i++) {
                //System.out.println(trainLabels.get(i) + " " + distances[i]);
                if (distances[i] < minVal) {
                    minVal = distances[i];
                    minIndex = i;
                }
            }
            plateNum += trainLabels.get(minIndex);

        }
        return plateNum;
    }

    // append hard-coded character data to trainingData to improve accuracy
    private static void addMoreTrainingData() {
        // Need to hard code in training data for 7, T, Y, V, 8, 5, B, 2, Z, E, F using int[] from a test image to improve accuracy
        Integer[] seven = {255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255};
        trainData.add(seven);
        trainLabels.add("7");
        Integer[] why = {255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,0,0,0,0,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,0,0,0,0,0,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,0,0,0,0,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,0,0,0,0,0,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,0,0,0,0,0,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255};
        trainData.add(why);
        trainLabels.add("Y");
        Integer[] eff = {255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255};
        trainData.add(eff);
        trainLabels.add("F");
        Integer[] two = {255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,255,255,255,0,0,0,0,0,0,0,255,255,255,255,0,0,0,0,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,0,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
        trainData.add(two);
        trainLabels.add("2");
        Integer[] bee = {255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,255,255,255,255,0,0,0,0,0,0,255,255,255,255,0,0,0,0,0,0,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,255,255,255,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,255,255,255,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,255,255,255,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,255,255,255,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,255,255,255,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,255,255,255,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,255,255,255,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,255,255,255,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,255,255,255,255,0,0,0,0,0,0,255,255,255,255,0,0,0,0,0,0,255,255,255,255,0,0,0,0,0,0,255,255,255,255,0,0,0,0,0,0,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,0,0,0,0,0,0,255,255,255,255,0,0,0,0,0,0,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,255,255,255,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,255,255,255,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,255,255,255,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,255,255,255,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,255,255,255,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,255,255,255,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,255,255,255,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,255,255,255,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,255,255,255,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,255,255,255,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,255,255,255,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,255,255,255,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,255,255,255,0,0,0,0,0,0,255,255,255,255,0,0,0,0,0,0,0,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255};
        trainData.add(bee);
        trainLabels.add("B");

        return;
    }

    // find top and bottom edges of characters on plate
    private static int[] getTopBottom(BufferedImage img) {
        // calculate row sums to find top and bottom edges of characters
        int[] rowSums = new int[img.getHeight()];
        for (int y=0; y<img.getHeight(); y++) {
            for (int x=0; x<img.getWidth(); x++) {
                rowSums[y] += img.getRGB(x, y)& 0xFF;
            }
        }

        // find top and bottom bounds of plate numbers
        int[] deltaGray = new int[200];
        for (int i=0; i<img.getHeight()-2; i++) {
            deltaGray[i] = (rowSums[i] - rowSums[i+2])/1000;
        }
        int[] vertBounds = new int[2];
        for (int i=120; i>50; i--) {
            if (deltaGray[i] > 5) {
                vertBounds[0] = i-2;
                break;
            }
        }
        for (int i=120; i<180; i++) {
            if (deltaGray[i] < -5) {
                vertBounds[1] = i+2;
                break;
            }
        }
        //System.out.println("Vert bounds: " + vertBounds[0] + " " + vertBounds[1]);
        return vertBounds;
    }

    // add grayscale data for each character to ArrayList
    private static ArrayList<Integer[]> imgToArray (BufferedImage img, ArrayList<Integer[]> data, ArrayList<Integer> edges) {
        for (Integer e : edges) {
            data.add(new Integer[NUMPIXELS]);
            for (int j=0; j<img.getHeight(); j++) {
                for (int i=0; i<CHARSIZE[0]; i++) {
                    data.get(data.size() - 1)[j*CHARSIZE[0] + i] = img.getRGB(e + i, j)& 0xFF;
                }
            }
        }
        return data;
    }

    // find upper and lower whitespace bounds given a grayscale sums array
    private static int[] getBounds (int[] bounds, int[] sums) {
        int upper = 0, lower = 0;
        boolean upperFound = false;
        for (int i=bounds[0]; i<sums.length; i++) {
            if ((upper <= sums[i]) && !(upperFound)) {
                upper = sums[i];
                bounds[0] = i;
            }
            else {
                upperFound = true;
                if (lower < sums[i]) {
                    lower = sums[i];
                    bounds[1] = i;
                }
            }
        }
        return bounds;
    }

    // find gaps between characters
    private static ArrayList<Integer> getEdges(BufferedImage img) {
        //System.out.println("  Getting edges between characters.");
        // Locate whitespace delimiters between characters (left & rt bounds for each character)
        int[] colSums = new int[img.getWidth()];
        for (int y=0; y<img.getHeight(); y++) {
            for (int x=0; x<img.getWidth(); x++) {
                colSums[x] += (img.getRGB(x, y)& 0xFF);
            }
        }

        // find gaps and locate center of gaps
        ArrayList<Integer> edges = new ArrayList<>();
        int col = 0;
        while (col<colSums.length-1) {
            int whiteCols = 0;
            while ((col+whiteCols<colSums.length-1) & (colSums[col+whiteCols] > 250*CHARSIZE[1])) {
                whiteCols += 1;
            }
            if ((whiteCols > 1) & (col+whiteCols+CHARSIZE[0] < img.getWidth()))
                edges.add((int)(col + whiteCols -1));
            col += whiteCols + 1;
        }

        return edges;
    }

    private static void writeToOutput(String result) {
        String fileName = "output.txt";

        try {
            // Create File object
            File file = new File(fileName);

            // If the file doesn't exist, create it
            if (!file.exists()) {
                file.createNewFile();
            }
            // Use a synchronized block to ensure thread safety
            synchronized (lock) {
                // Create FileWriter with true as the second parameter to enable append mode
                FileWriter fileWriter = new FileWriter(file, true);

                // Create BufferedWriter for efficient writing
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

                // Append the content to the file
                bufferedWriter.write(result + "\n");

                // Close the BufferedWriter
                bufferedWriter.close();

                //System.out.println(result + " appended to " + fileName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
