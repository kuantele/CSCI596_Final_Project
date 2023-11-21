import java.util.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.nio.Buffer;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.geom.AffineTransform;

public class ImageFilter {
    private String imgPath;
    private BufferedImage img;

    public ImageFilter(String path){
        this.imgPath = path;
        this.img = null;
    }

    public void readImage() throws Exception{
        File file = new File(imgPath);

        //Read image
        try{
            this.img = ImageIO.read(file);
        }
        catch(IOException e){
            e.printStackTrace(System.out);
        }

        if(img != null){
            //Display the original image in a JPanel popup
            display(img);

            //Display the gray scale image
            //img = toGrayScale2(img);
            img = toGrayScale(img);
            display(img);

            //Display the pixelated image
            //img = pixelate(img);
            //img = pixelateN(img,10);
            //display(img);

            //Display the resized image
            //resize(img,10);
            //display(img);

            //Gaussian Blur (Weighted blurring)
            img = blur(img);
            display(img);
        }
    }

    /*
     * Convert image into Gray scale by using Graphics library
     */
    public BufferedImage toGrayScale(BufferedImage img){
        System.out.println(" Converting to GrayScale");

        //Create new buffered image for gray image stroing
        BufferedImage grayImage = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);

        Graphics g = grayImage.getGraphics();
        g.drawImage(img,0,0,null);
        g.dispose();
        return grayImage;
    }

    /*
     * Convert image into Gray scale manually
     */
    public BufferedImage toGrayScale2(BufferedImage img){
        System.out.println(" Converting to GrayScale2");

        //Create new buffered image for gray image stroing
        BufferedImage grayImage = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        
        //Initiate RGB value
        int rgb=0, r=0, g=0, b=0;

        //This part could be done in multi-tasks
        for(int y=0; y<img.getHeight(); y++){
            for(int x=0; x<img.getWidth(); x++){
                rgb = img.getRGB(x,y);
                r = ((rgb >> 16) & 0xFF);
                g = ((rgb >> 8) & 0xFF);
                b = (rgb & 0xFF);
                rgb = (int)(0.299*r + 0.587*g + 0.114*b);
                rgb = (255<<24) | (rgb<<16) | (rgb<<8) | rgb;
                grayImage.setRGB(x,y,rgb);
            }
        }
        return grayImage;
    }

    /*
     * 2x2 Pixelate the gray scale image
     */
    public BufferedImage pixelate(BufferedImage img){
        System.out.println(" Pixelating the image (2x2)");

        //Create new buffered image for pixelated image storing
        BufferedImage pixImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        
        //Initiate pixel RGB value
        int pix=0, p=0;

        //This part could be done in multi-tasks
        for(int y=0; y<img.getHeight()-2; y+=2){
            for(int x=0; x<img.getWidth()-2; x+=2){
                //Calculate the average RGB value
                pix = (int)((img.getRGB(x, y)&0xFF)
                + (img.getRGB(x+1, y)&0xFF)
                + (img.getRGB(x, y+1)&0xFF)
                + (img.getRGB(x+1, y+1)&0xFF))/4;
                p = (255<<24) | (pix<<16) | (pix<<8) | pix;

                //Set current four pixels to the calculated RGB value
                pixImg.setRGB(x, y, p);
                pixImg.setRGB(x+1, y, p);
                pixImg.setRGB(x, y+1, p);
                pixImg.setRGB(x+1, y+1, p);
            }
        }

        return pixImg;
    }

    /*
     * nxn Pixelate the gray scale image
     */
    public BufferedImage pixelateN(BufferedImage img, int n){
        System.out.println(" Pixelating the image (nxn)");

        //Create new buffered image for pixelated image storing
        BufferedImage pixImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);

        //Initiate pix RGB value
        int pix=0, p=0;
        
        //This part could be done in multi-tasks
        for(int y=0; y<img.getHeight()-n; y+=n){
            for(int x=0; x<img.getWidth()-n; x+=n){
                for(int a=0; a<n; a++){
                    for(int b=0; b<n; b++){
                        pix += (img.getRGB(x+a, y+b)&0xFF);
                    }
                }
                pix = (int)(pix/n/n);
                for(int a=0; a<n; a++){
                    for(int b=0; b<n; b++){
                        p = (255<<24) | (pix<<16) | (pix<<8) | pix;
                        pixImg.setRGB(x+a, y+b, p);
                    }
                }
            }
        }
        return pixImg;
    }

    /*
     * Resize the gray scale image
     */
    public BufferedImage resize(BufferedImage img, int newHieght) {
        System.out.println(" Scaling image");

        //Create new buffered image for resized image storing
        double ratio = (double) newHieght/img.getHeight();
        BufferedImage scaledImg = new BufferedImage((int)(img.getWidth()*ratio), (int)newHieght, BufferedImage.TYPE_BYTE_GRAY);

        //Resize the image using AffineTransform
        AffineTransform at = new AffineTransform();
        at.scale(ratio, ratio);
        AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);

        return scaleOp.filter(img, scaledImg);
    }

    /*
     * Apply Guassian blur to a grayscale image
     */
    public BufferedImage blur(BufferedImage img){
        System.out.println(" Gussian blur");

        //Create new buffered image for blurred image storing
        BufferedImage blurImg = new BufferedImage(img.getWidth()-2, img.getHeight()-2, BufferedImage.TYPE_BYTE_GRAY);
        
        //Initiate pixel RGB value
        int pix = 0;

        //This part can be done parallely
        for(int y=0; y<blurImg.getHeight(); y++){
            for(int x=0; x<blurImg.getWidth(); x++){
                //weighted average
                pix = (int)(4*(img.getRGB(x+1, y+1)& 0xFF)
                + 2*(img.getRGB(x+1, y)& 0xFF)
                + 2*(img.getRGB(x+1, y+2)& 0xFF)
                + 2*(img.getRGB(x, y+1)& 0xFF)
                + 2*(img.getRGB(x+2, y+1)& 0xFF)
                + (img.getRGB(x, y)& 0xFF)
                + (img.getRGB(x, y+2)& 0xFF)
                + (img.getRGB(x+2, y)& 0xFF)
                + (img.getRGB(x+2, y+2)& 0xFF))/16;
                int p = (255<<24) | (pix<<16) | (pix<<8) | pix;
                blurImg.setRGB(x, y, p);
            }
        }
        return blurImg;
    }

    private void display(BufferedImage img) {
        System.out.println(" Displaying image");
        JFrame frame = new JFrame();
        JLabel label = new JLabel();

        //Set the fram size to be the same as image
        frame.setSize(img.getWidth(), img.getHeight());

        //Attach the image to Label
        label.setIcon(new ImageIcon(img));

        //Add the label to the frame
        frame.getContentPane().add(label, BorderLayout.CENTER);

        //Window exit on window close
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        frame.pack();
        frame.setVisible(true);
    }
}
