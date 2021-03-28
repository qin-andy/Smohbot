import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class Carver {

    public static void main(String[] args) throws IOException { // For testing
        Carver carver = new Carver();
        carver.createEnergyArray("src/main/resources/images/testpath_small.PNG");
    }


    // Creates an energy map from a filepath to an image
    public int[][] createEnergyArray(String filePath) throws IOException {
        File file = new File(filePath);
        BufferedImage image = ImageIO.read(file);
        System.out.println(image);
        Color[][] imageRGB = convertToRGB(image);
        shortestSeamPath(createEnergyMap(imageRGB));

        return null;
    }

    // Takes a buffered image and converts into a 2d array with Color object for each pixel
    // TODO: Handle alpha channel
    private Color[][] convertToRGB(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        Color[][] result = new Color[width][height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++)  {
                result[x][y] = new Color(image.getRGB(x, y));
            }
        }
        return result;
    }

    private int[][] createEnergyMap(Color[][] colorArray) {
        int width = colorArray.length;
        int height = colorArray[0].length;
        int[][] energyArray = new int[width][height];
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++)  {
                // Typed out for readability
                // Energy mapping algorithm is Δx^2(x, y) + Δy^2(x, y)
                Color prev = colorArray[x-1][y];
                Color next = colorArray[x+1][y];
                int deltaR = (int) Math.pow(prev.getRed() - next.getRed(), 2);
                int deltaG = (int) Math.pow(prev.getGreen() - next.getGreen(), 2);
                int deltaB = (int) Math.pow(prev.getBlue() - next.getBlue(), 2);

                int xDeltaSquare = deltaR + deltaG + deltaB;

                prev = colorArray[x][y-1];
                next = colorArray[x][y+1];
                deltaR = (int) Math.pow(prev.getRed() - next.getRed(), 2);
                deltaG = (int) Math.pow(prev.getGreen() - next.getGreen(), 2);
                deltaB = (int) Math.pow(prev.getBlue() - next.getBlue(), 2);

                int yDeltaSquare = deltaR + deltaG + deltaB;
                energyArray[x][y] = xDeltaSquare + yDeltaSquare;

                // Debug to test
                System.out.print("[" + x +"," + y + ":" + energyArray[x][y] + "] ");
            }
            System.out.println();
        }
        return energyArray;
    }

    private int[][] shortestSeamPath(int[][] energyArray) {
        int width = energyArray.length;
        int height = energyArray[0].length;

        // Each [x][y] pair is modelled as a node
        int[][] parent = new int[width][height]; // The x value of the node's parent; the y is the child y - 1
        int[][] distTo = new int[width][height]; // The shortest distance to the node
        for (int i = 0; i < width; i++) {
            Arrays.fill(distTo[i], Integer.MAX_VALUE);
            distTo[i][0] = energyArray[i][0];
        }

        int newDist = 0;
        // Traverse through every node in order
        for (int y = 0; y < height - 1; y++) {
            for (int x = 0; x < width; x++) {

                int lower = -1;
                int upper = 1;
                if (x == 0) {
                    lower = 0;
                } else if (x == width - 1) {
                    upper = 0;
                }

                // For each of the current node's children, check if the path through the current node is shorter
                for (int i = lower; i < upper + 1; i++) {
                    newDist = distTo[x][y] + energyArray[x+i][y+1];
                    if (newDist < distTo[x+i][y+1]) {
                        distTo[x+i][y+1] = newDist;
                        parent[x+i][y+1] = x;
                    }
                }
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                System.out.print("[" + distTo[x][y] + "]");
            }
            System.out.println();
        }
        return distTo;
    }
}
