import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
public class Carver {

    //TODO: remove testing main statement
    public static void main(String[] args) throws IOException { // For testing
        Carver carver = new Carver();
        carver.carve("src/main/resources/images/download.png", 300, 300);
    }

    public int carve(String filePath, int cutSize, int cutSizeY) throws IOException {
        File file = new File(filePath);
        BufferedImage image;
        try {
            image = ImageIO.read(file);
        } catch (IOException e) {
            return -2;
        }

        int height = image.getHeight();
        int width = image.getWidth();
        System.out.println("Image size is " + width + " by " + height);
        System.out.println("Requested cut is " + cutSize + " and " + cutSizeY);

        if (cutSize >= image.getWidth() || cutSizeY >= image.getHeight()) {
            System.out.println("Cut size too big!");
            return -1;
        }

        // compression
        int maxSize = 1000; //Integer.MAX_VALUE;
        if (height > maxSize || width > maxSize) {
            long start = System.currentTimeMillis();
            double scale = (double) maxSize / Math.max(height, width);

            int newW = (int) (width*scale);
            int newH = (int) (height*scale);
            Image scaled = image.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
            BufferedImage scaledImage = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
            scaledImage.createGraphics().drawImage(scaled, 0, 0, null);
            scaledImage.createGraphics().dispose(); //TODO: CHECK IF THIS IS ACTUALLY DISPOSED!
            image = scaledImage;
            System.out.println("Image too large! Scaling down to " + newW + " by " + newH);

            double cutFactorX = (double) cutSize / width;
            double cutFactorY = (double) cutSizeY / height;

            cutSize = (int) (cutFactorX * newW);
            cutSizeY = (int) (cutFactorY * newH);
            System.out.println("Adjusted cut size to " + cutSize + " and " + cutSizeY);
            height = newH;
            width = newW;

            long end = System.currentTimeMillis();
            System.out.println("Compression took " + (end - start) + "ms!");
        }

        // Conversion to ARGB
        BufferedImage imageARGB = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        imageARGB.createGraphics().drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);


        imageARGB.createGraphics().dispose();
        image = imageARGB;


        // Determining cut is possible (size)
        if (cutSize > image.getWidth()) {
            System.out.println("Cut size too big!");
            return -1;
        }

        // Timing
        int convertToRGBTime = 0;
        int energyMapTime = 0;
        int shortestSeamTime = 0;
        int pathRemovalTime = 0;
        long iterationStartTime = System.currentTimeMillis();

        int iWidth = image.getWidth(); // Initial width used for navigating the image data buffer

        for (int i = 0; i < cutSize; i++) { // Main carving

            width = image.getWidth(); // Need to update width and height
            height = image.getHeight();

            long startTime = System.currentTimeMillis();
            int[] imageRGB = convertToRGB(image, iWidth); // BufferedImage to RGB values conversion
            long endTime = System.currentTimeMillis();
            convertToRGBTime += (endTime - startTime);

            startTime = System.currentTimeMillis();
            int[] energyMap = calculateEnergy(imageRGB, width, height); // RGB Values to Energy Map conversion
            endTime = System.currentTimeMillis();
            energyMapTime += (endTime - startTime);

            startTime = System.currentTimeMillis();
            int[] path = shortestPath(energyMap, width, height); // Lowest energy path determination
            endTime = System.currentTimeMillis();
            shortestSeamTime += (endTime - startTime);

            startTime = System.currentTimeMillis();
            image = removePath(path, image, iWidth); // Path removal
            endTime = System.currentTimeMillis();
            pathRemovalTime += (endTime - startTime);

            //Timing for iterations
            if (i%50 == 0) {
                long iterationEndTime = System.currentTimeMillis();
                System.out.println("Iteration " + i + " took " + (iterationEndTime - iterationStartTime) + " milliseconds");
                iterationStartTime = System.currentTimeMillis();
            }
        }


        if (cutSizeY > 0) {
            long start = System.currentTimeMillis();

            BufferedImage transpose = new BufferedImage(image.getHeight(), image.getWidth(),
                    BufferedImage.TYPE_INT_ARGB);

            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    transpose.setRGB(y, x, image.getRGB(x, y));
                }
            }
            image = transpose;

            long end = System.currentTimeMillis();
            System.out.println("Tranposing took  " + (end - start) + "ms!");
            iWidth = image.getWidth();
            for (int i = 0; i < cutSizeY; i++) {

                width = image.getWidth();
                height = image.getHeight();

                long startTime = System.currentTimeMillis();
                int[] imageRGB = convertToRGB(image, iWidth);
                long endTime = System.currentTimeMillis();
                convertToRGBTime += (endTime - startTime);

                startTime = System.currentTimeMillis();
                int[] energyMap = calculateEnergy(imageRGB, width, height);
                endTime = System.currentTimeMillis();
                energyMapTime += (endTime - startTime);

                startTime = System.currentTimeMillis();
                int[] path = shortestPath(energyMap, width, height);
                endTime = System.currentTimeMillis();
                shortestSeamTime += (endTime - startTime);

                startTime = System.currentTimeMillis();
                image = removePath(path, image, iWidth);
                endTime = System.currentTimeMillis();
                pathRemovalTime += (endTime - startTime);

                width = image.getWidth();
                height = image.getHeight();

                //Timing for iterations
                if (i%50 == 0) {
                    long iterationEndTime = System.currentTimeMillis();
                    System.out.println("Iteration " + i + " took " + (iterationEndTime - iterationStartTime) + " milliseconds");
                    iterationStartTime = System.currentTimeMillis();
                }
            }

            transpose = new BufferedImage(image.getHeight(), image.getWidth(),
                    BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    transpose.setRGB(y, x, image.getRGB(x, y));
                }
            }
            image = transpose;
        }


        System.out.println("For a new image:");
        System.out.println("Convert to RGB Time: " + convertToRGBTime + " ms");
        System.out.println("Energy mapping Time: " + energyMapTime + " ms");
        System.out.println("Path identification Time: " + shortestSeamTime + " ms");
        System.out.println("Path removal Time: " + pathRemovalTime + " ms");
        int totalTime = convertToRGBTime + energyMapTime + shortestSeamTime + pathRemovalTime;
        System.out.println("TOTAL TIME:" + totalTime + " ms");


        File outputFile = new File("src/main/resources/images/carved.PNG");
        ImageIO.write(image, "PNG", outputFile);
        return totalTime;
    }

    // I guess when subimages are taken in remove path, the underlying databuffer dimensions aren't changed.
    // we account for that by storing the intiial width as iWidth and using it to map 1d array to 2d
    private int[] convertToRGB(BufferedImage image, int iWidth) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] result = new int[width*height];
        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++)  {
                result[y*width + x] = pixels[y*iWidth + x];
            }
        }
        return result;
    }

    private int[] calculateEnergy(int[] colorArray, int width, int height) {
        int[] energyArray = new int[width*height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++)  {
                // Typed out for readability
                // Energy mapping algorithm is Δx^2(x, y) + Δy^2(x, y)
                int prev;
                int next;

                // Edges are trated as adjacent to the opposite side
                if (x == 0) {
                    prev = colorArray[y*width + (width-1)];
                    next = colorArray[y*width + x+1];
                } else if (x == width - 1) {
                    prev = colorArray[y*width + x-1];
                    next = colorArray[y*width];
                } else {
                    prev = colorArray[y*width + x-1];
                    next = colorArray[y*width + x+1];
                }

                int pB = prev & 0xff;
                int pG = (prev & 0xff00) >> 8;
                int pR = (prev & 0xff0000) >> 16;

                int nB = next & 0xFF;
                int nG = (next & 0xff00) >> 8;
                int nR = (next & 0xff0000) >> 16;

                int deltaR = (int) Math.pow(pR - nR, 2);
                int deltaG = (int) Math.pow(pG - nG, 2);
                int deltaB = (int) Math.pow(pB - nB, 2);

                int xDeltaSquare = deltaR + deltaG + deltaB;

                if (y == 0) {
                    prev = colorArray[(height-1)*width + x];
                    next = colorArray[width*(y+1) + x];
                } else if (y == height - 1) {
                    prev = colorArray[(y-1)*width + x];
                    next = colorArray[x];
                } else {
                    prev = colorArray[(y-1)*width + x];
                    next = colorArray[(y+1)*width + x];
                }

                pB = prev & 0xff;
                pG = (prev & 0xff00) >> 8;
                pR = (prev & 0xff0000) >> 16;

                nB = next & 0xff;
                nG = (next & 0xff00) >> 8;
                nR = (next & 0xff0000) >> 16;

                deltaR = (int) Math.pow(pR - nR, 2);
                deltaG = (int) Math.pow(pG - nG, 2);
                deltaB = (int) Math.pow(pB - nB, 2);

                int yDeltaSquare = deltaR + deltaG + deltaB;
                energyArray[y*width + x] = xDeltaSquare + yDeltaSquare;
            }
        }

        return energyArray;
    }

    private int[] shortestPath(int[] energyArray, int width, int height) {
        // Each [x][y] pair is modelled as a node
        int[] parent = new int[width*height]; // The x value of the node's parent; the y is the child y - 1
        int[] distTo = new int[width*height]; // The shortest distance to the node
        Arrays.fill(distTo, Integer.MAX_VALUE); // Initialize all distances to infinity
        for (int i = 0; i < width; i++) { // Reinitialize the first row elements as their respective energy
            distTo[i] = energyArray[i];
        }

        int newDist = 0;
        // Traverse through every node in order for a weighted path tree
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
                    newDist = distTo[y*width + x] + energyArray[(y+1)*width + x+i];
                    if (newDist < distTo[(y+1)*width+x+i]) { //if the path through the current node is shorter
                        distTo[(y+1)*width+x+i] = newDist; // update the node with the newest shortest path
                        parent[(y+1)*width+x+i] = x; // store the newest shortest path in the parent
                    }
                }
            }
        }

        // Backtracking from the minimum of the last row:
        // Finding minimum of the last row:
        int minEnergy = distTo[(height-1)*width];
        int minX = 0;

        for (int x = 1; x < width; x++) { // Find the lowest energy path by looking at the bottom row of nodes
            if (distTo[(height-1)*width+x] < minEnergy) {
                minX = x;
                minEnergy = distTo[(height-1)*width+x];
            }
        }

        // Start the minPath array to store the x values of the minimum path, with the indicies indicating the y coord
        int[] minPath = new int[height];
        minPath[height-1] = minX;

        int childX = minX;

        int parentX;
        for (int y = height - 1; y > 0; y--) { // use the parent array to traverse backwards to find shortest path
            parentX = parent[y*width+childX];
            minPath[y-1] = parentX;
            childX = parentX;
        }
        return minPath; //min path stores x values of the shortest path. y value is inferred from the index
    }

    private BufferedImage removePath(int[] path, BufferedImage image, int iWidth) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = path[y]; x < image.getWidth() - 1; x++) {
                int position = y*iWidth + x;
                pixels[position] = pixels[position+1];
            }
        }
        return image.getSubimage(0, 0, image.getWidth() - 1, image.getHeight());
    }

    private int colorDifference(int a, int b) {
        int aB = a & 0xff;
        int aG = (a & 0xff00) << 8;
        int aR = (a & 0xff0000) << 16;

        int bB = b & 0xff;
        int bG = (b & 0xff00) >> 8;
        int bR = (b & 0xff0000) >> 16;

        int deltaR = (int) Math.pow(aR - bR, 2);
        int deltaG = (int) Math.pow(aG - bG, 2);
        int deltaB = (int) Math.pow(aB - bB, 2);
        return deltaR + deltaG + deltaB;
    }
}
