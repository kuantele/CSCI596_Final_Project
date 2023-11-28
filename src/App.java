import java.io.File;

public class App {
    public static void main(String[] args) throws Exception {
        //ImageFilter imgFilter = new ImageFilter("photo.png");
        //imgFilter.readImage();

        ALPR alpr = new ALPR();
		File[] files = new File[4];
		files[0] = new File("Training Data/letters-1 image.png");
		files[1] = new File("Training Data/letters-2 image.png");
		files[2] = new File("Training Data/letters-3 image.png");
		files[3] = new File("Training Data/numbers image.png");
		alpr.train(files);

		File file = new File("License Plate Photos/ca_aug2012.png");
		//File file = new File("License Plate Photos/ca_12.jpeg");
		//File file = new File("License Plate Photos/ca_10.jpeg");
		//File file = new File("License Plate Photos/ca2004.jpg");
		//File file = new File("License Plate Photos/ca2005.jpg");
		//File file = new File("License Plate Photos/ca2006.jpg");
		//File file = new File("License Plate Photos/ca_blue_1.jpeg");
		//File file = new File("License Plate Photos/tx_4.jpeg");
		//File file = new File("License Plate Photos/ny_1.jpeg");
		//File file = new File("License Plate Photos/fl_1.jpeg");
		alpr.readPlate(file);
    }
}
