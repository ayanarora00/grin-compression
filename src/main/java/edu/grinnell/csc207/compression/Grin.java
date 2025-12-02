package edu.grinnell.csc207.compression;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The driver for the Grin compression program.
 */
public class Grin {
    /**
     * Decodes the .grin file denoted by infile and writes the output to the
     * .grin file denoted by outfile.
     * @param infile the file to decode
     * @param outfile the file to ouptut to
     * @throws IOException 
     */
    public static void decode (String infile, String outfile) throws IOException {
        
        // Creating input stream and output stream objects reading in the files
        BitInputStream in = new BitInputStream(infile);
        BitOutputStream out = new BitOutputStream(outfile);

        // Declaring the magic variable - by reading in the first 32 bits
        int magic = in.readBits(32); 

        // Checking if it corresponds to what it is supposed to be
        if (magic != 0x736){
            in.close();
            out.close();
        }

        // Creating a new huffman tree object from the input stream
        HuffmanTree huffmantree = new HuffmanTree(in);

        // Decoding that serialized huffman tree and writing it to the output stream
        huffmantree.decode(in, out);

        // Closing both the infile and outfile
        in.close();
        out.close();


    }

    /**
     * Creates a mapping from 8-bit sequences to number-of-occurrences of
     * those sequences in the given file. To do this, read the file using a
     * BitInputStream, consuming 8 bits at a time.
     * @param file the file to read
     * @return a freqency map for the given file
     * @throws IOException 
     */
    public static Map<Short, Integer> createFrequencyMap (String file) throws IOException {

        // Creating a map object
        Map<Short, Integer> freqs = new HashMap<>();

        // Declaring an int object to read bits from the input file/stream of bits
        int bits;

        // Creating input stream object
        BitInputStream in = new BitInputStream(file);

        // The loop which runs while it isn't -1 which is what comes when the input stream becomes empty
        while ((bits = in.readBits(8)) != -1){

            // We assign those bits to a short object
            short value = (short) bits;

            // We get count of that value from the map
            Integer count = freqs.get(value);

            // If the count value is null, we then put an entry into the map with value as key and count as 1
            if (count == null){
                freqs.put(value, 1);

            // Otherwise, we put with the existing count incremented by 1
            } else {
                freqs.put(value, count + 1);
            }

            
        }

        // Closing the infile and returning the frequency map created
        in.close();
        return freqs;


    }

    /**
     * Encodes the given file denoted by infile and writes the output to the
     * .grin file denoted by outfile.
     * @param infile the file to encode.
     * @param outfile the file to write the output to.
     * @throws IOException 
     */
    public static void encode(String infile, String outfile) throws IOException {
        
        // Creating a frequency map using the input stream/file
        Map<Short, Integer> freqs = createFrequencyMap(infile);

        // Creating input stream and output stream objects reading in the files
        BitInputStream in = new BitInputStream(infile);
        BitOutputStream out = new BitOutputStream(outfile);

        // Creating a huffman tree object  from the frequency map
        HuffmanTree huffmantree = new HuffmanTree(freqs);

        // Writing out the magic number at the start
        out.writeBits(0x736, 32);

        // Serializing the huffman tree to write it to out
        huffmantree.serialize(out);

        // Using the huffman tree to encode the in file and write the compressed version to the out file
        huffmantree.encode(in, out);

        // Closing both the infile and outfile
        in.close();
        out.close();
        
    }

    /**
     * The entry point to the program.
     * @param args the command-line arguments.
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
        
        // Checking for irregularities in command line arguments passed
        if (args.length != 3){
            System.out.println("Usage: java Grin <encode|decode> <infile> <outfile>");
        }

        // Creating objects from the command line arguments
        String function = args[0];
        String inputfile = args[1];
        String outputfile = args[2];

        // If the function to carry out is encode, we encode the in file to the out file
        if (function.equals("encode")){
            encode(inputfile, outputfile);

        // If it is decode, we decode the in file to the out file
        } else if (function.equals("decode")){
            decode(inputfile, outputfile);

        // If not either, we print out a guiding message as to how to use the program and what the function argument
        // requires
        } else {
            System.out.println("Usage: java Grin <encode|decode> <infile> <outfile>");
        }

    }

}
