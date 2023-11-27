public class App {
    public static void main(String[] args) throws Exception {
        ImageFilter imgFilter = new ImageFilter("src/photo.png");
        imgFilter.readImage();
    }
}
