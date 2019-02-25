/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package assimagilator;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import net.coobird.thumbnailator.Thumbnails;

/**
 *
 * @author steve
 */
public class Assimagilator implements Runnable {
    public static final String VERSION = "1.0";
    private String[] args;
    
    private Assimagilator(String[] args) {
        this.args = args;
    }
    
    public void printHelp() {
        System.out.println("Assimagilator Version "+VERSION);
        System.out.println("Created by Steve Hannah.  https://sjhannah.com");
        System.out.println();
        System.out.println("Synopsis: Fits a source image into one or more destination images.");
        System.out.println();
        System.out.println("Usage Instructions:\n");
        System.out.println("fitimg [flags] [BGColor] srcImage destImages...");
        System.out.println();
        System.out.println("Flags:");
        System.out.println(" -r Allow directory inputs and recurse them for images.");
        System.out.println(" -v Verbose error output");
        System.out.println(" -h Prints this help");
        System.out.println();
        System.out.println(" BGCOLOR : The background color to use.  Optional.  Should be in hex rgb format. #RRGGBB");
        System.out.println(" srcImage : The path to the source image that will be 'fit' into the destination images");
        System.out.println(" destImages : One or more destination images that will be overwritten.");
        System.out.println();
        System.out.println("Examples:");
        System.out.println(" fitimg #ffffff myicon.png destIcon-1024.png destIcon-256.png destIcon-128.png");
        System.out.println("  This fits the image myicon.png onto the 3 destination images, overwriting them.  Uses white background.");
        System.out.println();
        System.out.println(" fitimg myicon.png -d Assets");
        System.out.println("  This fits myicon.png onto all images in the Assets directory.");
        
    }
    
    public void run() {
        if (args.length == 0) {
            printHelp();
            System.exit(1);
        }
        String srcFileParam = null;
        ArrayList<String> destFiles = new ArrayList<String>();
        boolean allowDirectories = false;
        boolean verbose = false;
        String backgroundColorParam = null;
        for (String arg : args) {
            if (arg.startsWith("#")) {
                backgroundColorParam = arg;
                continue;
            }
            if ("-h".equals(arg)) {
                printHelp();
                System.exit(0);
            }
            if ("-v".equals(arg)) {
                verbose = true;
                continue;
            }
            if ("-r".equals(arg)) {
                allowDirectories = true;
                continue;
            } else if (!arg.startsWith("-")) {
                if (srcFileParam == null) {
                    if (new File(arg).exists()) {
                        srcFileParam = arg;
                        continue;
                    } else {
                        System.err.println("Source file not found:" +arg);
                        System.exit(1);
                    }
                } else  {
                    if (new File(arg).exists()) {
                        destFiles.add(arg);
                        continue;
                    } else {
                        System.err.println("File not found: "+arg+". Skipping");
                    }
                }
            } else {
                System.err.println("Unrecognized option "+arg);
                System.exit(1);
            }
        }
        
        if (destFiles.isEmpty()) {
            System.err.println("No destination files specified.");
            System.exit(1);
        }
        
        Color backgroundColor = null;
        if (backgroundColorParam != null) {
            try {
                backgroundColor = hex2Rgb(backgroundColorParam);
            } catch (Throwable t) {
                System.err.println("Failed to parse background color.  Background color should be in hex format: #RRGGBB");
            }
        }
        
        
        for (String destFileParam : destFiles) {
            try {
                fit(System.out, new File(srcFileParam), new File(destFileParam), allowDirectories, backgroundColor);
            } catch (IOException ex) {
                System.err.println("Failed to assimilate "+destFileParam+". Reason: "+ex.getMessage());
                System.err.println("Use the -v flag to see a more verbose error message.");
                if (verbose) {
                    ex.printStackTrace();
                }
            }
        }
        
        
    }
    
    public static Color hex2Rgb(String colorStr) {
        return new Color(
                Integer.valueOf(colorStr.substring(1, 3), 16),
                Integer.valueOf(colorStr.substring(3, 5), 16),
                Integer.valueOf(colorStr.substring(5, 7), 16));
    }
    
    private void fit(PrintStream out, File srcImageFile, File destImageFile, boolean allowDirectories, Color backgroundColor) throws IOException {
        
        if (destImageFile.isDirectory()) {
            if (!allowDirectories) {
                throw new IOException("Failed to assimilate "+destImageFile+" because it is a directory.  Use the -r flag to allow directory recursion");
            }
            for (File child : destImageFile.listFiles()) {
                fit(out, srcImageFile, child, allowDirectories, backgroundColor);
            }
            return;
        }
        
        BufferedImage srcImage = ImageIO.read(srcImageFile);
        
        BufferedImage destImage;
        try {
            destImage = ImageIO.read(destImageFile);
        } catch (Throwable t) {
            if (out != null) {
                out.println("Failed to load "+destImageFile+".  Skipping.");
            }
            return;
        }
        File tmpImg = File.createTempFile(destImageFile.getName(), ".png");
        tmpImg.deleteOnExit();
        Thumbnails.of(srcImageFile)
                .size(destImage.getWidth(), destImage.getHeight())
                .toFile(tmpImg);
        
        srcImage = ImageIO.read(tmpImg);
        tmpImg.delete();
        fit(srcImage, destImage, backgroundColor);
        String format = getFormatName(destImageFile);
        if (format == null) {
            throw new IOException("Could not find format of "+destImageFile);
        }
        ImageIO.write(destImage, format, destImageFile);
        if (out != null) {
            out.println(srcImageFile+" fitted successfully to "+destImageFile);
        }
        
    }
    
    private String getFormatName(File file) {
        String name= file.getName();
        String ext = name.lastIndexOf(".") != -1 ? name.substring(name.lastIndexOf(".")+1) : null;
        return ext;
        
    }
    
    private void fit(BufferedImage srcImage, BufferedImage destImage, Color backgroundColor) throws IOException {
        int srcW = srcImage.getWidth();
        int srcH = srcImage.getHeight();
        
        int destW = destImage.getWidth();
        int destH = destImage.getHeight();
        
        Graphics2D g = destImage.createGraphics();
        
        g.clearRect(0, 0, destW, destH);
        if (backgroundColor != null) {
            g.setColor(backgroundColor);
            g.fillRect(0, 0, destW, destH);
        }
        
        if (srcW > destW) {
            srcH = (int)Math.round(srcH * destW / (double)srcW);
            srcW = destW;
        }
        if (srcH > destH) {
            srcW = (int)Math.round(srcW * destH / (double)srcH);
            srcH = destH;
        }
        
        int x = (destW - srcW)/2;
        int y = (destH - srcH)/2;
        int w = srcW;
        int h = srcH;
        g.setComposite(AlphaComposite.Src);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
        //g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(srcImage, x, y, w, h, null);
        g.dispose();
        
    }
    
    public static void main(String[] args) {
        new Assimagilator(args).run();
    }
    
}
