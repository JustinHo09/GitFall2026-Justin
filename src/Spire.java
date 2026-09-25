package src;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;

import java.util.ArrayList;
import java.util.Scanner;
import java.util.Random;

// This is where I found the .jar file for OpenPDF: https://repo1.maven.org/maven2/com/github/librepdf/openpdf/3.0.5/
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.PdfWriter;
import org.openpdf.text.Image;
import org.openpdf.text.pdf.PdfPTable;

// This is where I found the .jar file for JFreeChart:https://mvnrepository.com/artifact/org.jfree/jfreechart/1.5.6
import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.chart.plot.PlotOrientation;

/**
 *  @author Justin Ho
 *
 *  @since 09-18-2026
 *
 * @version 1.10
 *
 *  This method takes in a txt file that is a deck for Slay the Spire and
 *  generates a PDF report using it that details the random deck id, invalid cards, total
 *  deck cost, a histogram of the deck costs, table of all valid cards sorted by energy lowest
 *  to highest, and some statistics about the energy.
 *
 *  The third extra credit is the table and stats in the writePDF.
 *
 */
public class Spire {

    /**
     * This is the main method used to run the program to take in a deck file and
     * print either a standard report or a void report if it is invalid.
     * @param args The user input which should be the file name for the deck.
     */
    public static void main(String[] args) {
        // Takes in User Input
        System.out.println("Please enter in the deck's .txt file name or path for the program to use");
        Scanner scanner = new Scanner(System.in);
        String fileName = scanner.nextLine();

        //Creates a file using the input
        File file = new File(fileName);
        scanner.close();

        //Generate the random id to ensure it is 9-digits long
        Random gen = new Random();
        // having it be at minimum the smallest 9-digit number and having it bound to the
        // highest 9-digit number ensures that it will be 9-digits long.
        // This will alwasy generate a random numebr which means if the same file
        // is used as input it will always generate another random number
        // This is an intentional design choice since the deck may have
        // had then and now and the pdf may not update the same one properly.
        int id = gen.nextInt(0,900000000) + 100000000;

        // Checks to see if the deck is valid or not to see if it should read the file and continue or not.
        if(!isValid(file)){
            System.out.println("Invalid File");
        }else{
            // If valid call readfile then checks if the PDF file exists before saying
            // the print was successful, tho there will be a delay from the print and the
            // file appearing.
            readFile(file,id);
            if(new File("SpireDeck_"+id+".pdf").exists()) {
                System.out.println("\nSpireDeck_" + id + ".pdf was successfully created.");
            }
        }
    }

    /**
     * This checks to see if the inputed file is a valid file being that is even exists.
     * @param file The deck to check if it is a valid file or not.
     * @return True if the file does exist and False if the file does not exist.
     */
    private static boolean isValid(File file){
        // Check if the file even exists.
        return file.exists();
    }

    /**
     * This reads the deck file to get all the information needed for the report or the void report
     * if the deck meets certain conditions. This will calculate the total cost of the deck, get the
     * frequency of each cost for the histogram, call methods to generate either one of the normal
     * report of the void report.
     * @param file The deck to write the repot for.
     * @param id The 9-digit id for the deck.
     */
    private static void readFile(File file,int id) {
        // Try to read the file to see if it is readable or will cause an error.
        try (Scanner fileScanner = new Scanner(file)){

            // Keep track of the invalid cards
            ArrayList <String> invalid = new ArrayList<>();

            // Total cost of the deck
            int total = 0;

            ArrayList <String> valid = new ArrayList<>();
            ArrayList <Double> energy = new ArrayList<>();

            // The cost of a card after it is deciphered
            int cost;

            // Count of how many cards there are in the deck
            int rowCount=0;

            // The ArrayList of all valid card names which are real Slay the src.Spire | card names
            ArrayList<String> validCardNames = validNames();

            // Iterate over the entire deck
            while(fileScanner.hasNextLine()){
                String line = fileScanner.nextLine();

                // Only if the line contains a colon which indicates the row is a data row that is valid
                // Blank lines do not count as cards thus they do not count as invalid for me either
                // because it makes no sense to have an empty line in the report for invalid.
                if(line.contains(":")) {

                    // Increment card count and if it is over 1000 return with the void file.
                    rowCount++;
                    if (rowCount > 1000) {
                        voidFile(id);
                        return;
                    }

                    // Split the row into the card name and the cost which are both strings
                    // since those two are divided by a colon.
                    String[] split = line.split(":");

                    // See if both the card name and cost are valid values to either add to the total cost
                    // and card name and cost to the appropriate arrays or invalid to add to the invalid counter.
                    if (split.length == 2) {
                        if (validCost(split[1].strip()) && validCard(split[0].strip(), validCardNames)) {
                            cost = (int) split[1].strip().charAt(0) - 48;
                            total += cost;
                            valid.add(split[0].strip());
                            energy.add((double) (cost));
                        } else {
                            // If it is invalid it will append energy to the end and then if there
                            // Are more than 10 invalid cards then it will return and print the void file.
                            invalid.add(line + " energy");
                            if (invalid.size() > 10) {
                                voidFile(id);
                                return;
                            }
                        }
                    }else{
                        // If it is invalid it will append energy to the end and then if there
                        // Are more than 10 invalid cards then it will return and print the void file.
                        invalid.add(line + " energy");
                        if (invalid.size() > 10) {
                            voidFile(id);
                            return;
                        }
                    }
                }
            }

            //Write PDF REPORT
            writePDF(id,total, valid, energy,invalid);

        }catch (FileNotFoundException e){
            // If the file is not found print so just in case
            // this is extra insurance.
            System.out.println("File not found");
        }
    }

    /**
     * This sees if the cost of a card is a valid cost, being between and including 0 and 6.
     * @param cost The cost of the card as a string.
     * @return True if the cost is valid and False if the cost is invalid.
     */
    private static boolean validCost(String cost){
        // Found .matches from Javadoc on String class
        return cost.matches("[0-6]");
    }

    /**
     * This checks if a card's name is in the ArrayList of valid names, which are real
     * names of Slay the src.Spire | cards, or not to check if a card is valid from its name.
     * @param cardName The name of the card to check.
     * @param validCardNames The ArrayList of valid card names.
     * @return True if the card name is valid and False if the card name is invalid.
     */
    private static boolean validCard(String cardName, ArrayList<String> validCardNames){
        // if the name is not just an empy string and if the name is a real card name
        // then it returns true.
        return !cardName.isEmpty() && validCardNames.contains(cardName.toLowerCase());
    }

    /**
     * This reads a file on the same directory as the program called cards.txt, which contains
     * all the valid card names from Slay the src.Spire where each row is one card name, and
     * puts them in an ArrayList.
     * @return ArrayList of with all the card names inside of it.
     * @throws FileNotFoundException The file card.txt where the real Slay the Spire card name could not
     * be found it is either missing or the wrong path is below.
     */
    private static ArrayList<String> validNames() throws FileNotFoundException {
        // Find cards.txt which is where the file of real names is.
        Scanner scanner = new Scanner(new File("src/cards.txt"));

        ArrayList<String> validNames = new ArrayList<>();

        // Add all the names to the valid name arrau
        while (scanner.hasNextLine()) {
            validNames.add(scanner.nextLine().toLowerCase().strip());
        }
        return validNames;
    }

    /**
     * This method takes in the ID of the deck and produces the VOID PDF.
     * @param id The 9-digit id of the deck.
     */
    private static void voidFile(int id) {
        //Make the void PDF
        // I used the Javadoc for OpenPDF as well as the Tutorial to help me understand
        // how to use this library:
        // https://javadoc.io/doc/com.github.librepdf/openpdf/latest/com.github.librepdf.openpdf/org/openpdf/text/pdf/PdfDocument.html
        Document document = new Document();
        try {
            // Creates a PDF and opens it so things can be written.
            PdfWriter pdfWriter = PdfWriter.getInstance(document, new FileOutputStream("SpireDeck_" + id + "(VOID).pdf"));
            document.open();
            // Add this line it/
            document.add(new Paragraph("VOID"));
            document.close();
        }catch (IOException | DocumentException e){
            // If there is any error with the file or PDF writing
            // then print saying there was an issue here.
            System.out.println("Error with Void Report creation.");
        }
    }

    /**
     * This creates the PDF report of the deck featuring its id, histogram, and a list of
     * invalid cards in the deck.
     * @param id The 9-digit integer id of the deck.
     * @param total The total cost of all the cards in the deck.
     * @param valid The array of all the valid card names in the deck.
     * @param cost The array of all the energy cost of all the cards in the deck, in the same order as valid.
     * @param invalid An ArrayList of all invalid cards in the deck.
     */
    private static void writePDF(int id, int total,  ArrayList<String> valid, ArrayList<Double> cost, ArrayList<String> invalid){

        // Create a new document
        Document document = new Document();
        try {
            // Create the PDF according to the name and open it so it can be written in
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream("SpireDeck_" + id + ".pdf"));
            document.open();
            // Write Deck id
            document.add(new Paragraph("Deck id: "+ id));

            // Write Total cost of all cards in the deck
            document.add(new Paragraph("Total cost: "+ total + " energy"));

            // Histogram of all the cards in the deck
            // I used the JFreeChart Documentation and the official demo on GitHub
            // to help me understand how to make the histogram.
            HistogramDataset frequencies = new HistogramDataset();
            double [] data = new double[cost.size()];

            // Sort the name and energy for later
            sort(valid,cost);

            // Add all the costs into data so it can be used as histogram input
            for (int i = 0; i < cost.size(); i++) {
                data[i] = cost.get(i);
            }

            // Checks if empty in case there were 0 cards.
            if(!cost.isEmpty()) {
                // Add data as teh input for the histogram and make sure there are 7 bars
                // and it is -.5 to 6.5 because there bars do clip out a bit.
                frequencies.addSeries("Data", data, 7, -0.5, 6.5);
            }

            // Create the histogram ensuring it is vertical
            // and that unnecessary details like legend are there and then save is as a PGN
            JFreeChart histogram = ChartFactory.createHistogram("Card Cost Distribution",
                    "Energy", "Frequency",frequencies,PlotOrientation.VERTICAL,false,false,false);

            ChartUtils.saveChartAsPNG(new File("histogram_"+id+".png"), histogram, 320, 240);

            // I used the official guide on GitHub to help me understand how to do this:
            // https://github.com/jfree/jfree-demos/blob/master/src/main/java/org/jfree/chart/demo2/PieChartDemo1.java.
            // This makes the image and Image object so it can be added to the PDF.
            Image histogramImage = Image.getInstance("histogram_"+id+".png");
            document.add(histogramImage);

            // List of invalid cards
            // Checks if there are any to begin with and write accordingly
            if(invalid.isEmpty()){
                document.add(new Paragraph("No invalid cards"));
            } else {
                // Write all the invalid cards
                document.add(new Paragraph("Invalid Cards:"));
                for(String s: invalid){
                    document.add(new Paragraph(s));
                }
            }

            document.add(new Paragraph("\n"));

            // *** THIS IS MY EXTRA CREDIT NUMBER 3 : The table and stats***
            // Table of all cards in the deck in order from smallest to largest
            // and the summary statistics

            // Creates a new table with 2 columns one for the Name other for cost.
            PdfPTable table = new PdfPTable(2);
            for(int i = 0; i < valid.size(); i++){
                // Writes name then cost and it writes left to right top down
                // and so it has to be done one pair at a time.
                table.addCell(valid.get(i));
                table.addCell((int)((double)cost.get(i)) + " energy");
            }
            // add the completed table
            document.add(table);

            // Write some statistics like mean median and mode but
            // only if there are cards otherwise it will be N/A.
            document.add(new Paragraph("Statistics:"));
            double mean = 0.0;
            if(!cost.isEmpty()) {
                // Call some of the appropriate method or do the calculations for them.
                mean = total / (double)cost.size();
                document.add(new Paragraph("Mean Energy: "+ mean));
                double median = median(cost);
                document.add(new Paragraph("Median Energy: " + median));
                String mode = mode(cost).toString();
                document.add(new Paragraph("Mode Energy(ies): " + mode));
            }else{
                document.add(new Paragraph("Mean Energy: N/A"));
                document.add(new Paragraph("Median Energy: N/A"));
                document.add(new Paragraph("Mode Energy(ies): N/A"));
            }

            document.close();

        } catch ( IOException | DocumentException e){
            // Catch any arror and print saying there was one so that
            // it it known that this is where the error was.
            System.out.println("Error with Report creation.");
        }
    }

    /**
     * This is an Insertion sort algorithm that will sort the name and energy in place
     * together, but names and energy must be paired in their current order.
     * @param name Names of valid cards.
     * @param cost Energy cost of the cards.
     */
    private static void sort( ArrayList<String> name, ArrayList<Double> cost){
        // Name and Cost must be the same size becuase their pairs if not something is wrong.
        if(name.size() != cost.size()){
            System.out.println("Error name and cost mismatch");
            return;
        }
        int n = name.size();

        // Sorts based off insertion sort starts from one form the start until the end
        for (int i = 1; i < n; i++) {
            // Go backwards to swap so that everything will be moved down as needed.
            for(int j = i; j > 0; j--){
                if(cost.get(j) < cost.get(j-1)){

                    // Swap Costs positions
                    Double dTemp = cost.get(j-1);
                    cost.set(j-1, cost.get(j));
                    cost.set(j, dTemp);

                    // Swap Names too for it to be accurate
                    String sTemp = name.get(j-1);
                    name.set(j-1, name.get(j));
                    name.set(j, sTemp);
                } else {
                    // Leave early for if it is not less than the previous thus being in the
                    // right place for now.
                    break;
                }
            }
        }
    }

    /**
     * This method finds the median of a sorted Double ArrayList.
     * @param cost The sorted Double ArrayList to find the median for.
     * @return The median of the input ArrayList.
     */
    private static double median(ArrayList<Double> cost){
        double median;
        // Check to see if it is even or odd
        if(cost.size()%2==0){
            // Even so the median is the average of the two in the middle
            median = (cost.get(cost.size()/2) + cost.get((cost.size()/2 )-1))/2.0;
        }else{
            // Odd so the median is the middle
            median = cost.get(cost.size()/2);
        }

        return median;
    }

    /**
     * This finds the mode(s) of an ArrayList of Doubles only if the input is sorted.
     * @param cost The sorted Double ArrayList to find the mode(s) for.
     * @return An ArrayList of the modes in the ArrayList.
     */
    private static ArrayList<Integer> mode(ArrayList<Double> cost){
        // Tracks the fist and last occurence of a specific energy.
        int first;
        int last;
        // Highest count
        int highest = -1;
        //Array of the mode(s) if multiple
        ArrayList<Integer> mode = new ArrayList<>();

        // Checks for each energy cost
        for(int i = 0; i < 7; i++){
            // Check the first and last occurnce of each energy as a double since that
            // is what is is stored as.
            first = cost.indexOf((double) i);
            last = cost.lastIndexOf((double) i);
            // If the first and last do exist continue
            if(first != -1 && last != -1) {
                // If this cost has a higher frequency
                // then clear the array and have this be the new mode.
                // last-first +1 because indexes start at 0.
                if (last - first + 1 > highest) {
                    mode.clear();
                    highest = last - first + 1;
                    mode.add(i);
                } else if (last - first + 1 == highest) {
                   // If this frequency si the same as the highest then
                    // it is also a mode so add it to the array of modes.
                    mode.add(i);
                }
            }
        }
        return mode;
    }
}
