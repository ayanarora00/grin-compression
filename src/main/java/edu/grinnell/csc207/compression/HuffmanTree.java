package edu.grinnell.csc207.compression;

import java.util.*;


/**
 * A HuffmanTree derives a space-efficient coding of a collection of byte
 * values.
 *
 * The huffman tree encodes values in the range 0--255 which would normally
 * take 8 bits.  However, we also need to encode a special EOF character to
 * denote the end of a .grin file.  Thus, we need 9 bits to store each
 * byte value.  This is fine for file writing (modulo the need to write in
 * byte chunks to the file), but Java does not have a 9-bit data type.
 * Instead, we use the next larger primitive integral type, short, to store
 * our byte values.
 */
public class HuffmanTree {

    // Declaring the eof variable of type short
    private short eof = 256;

    // Implementing the Node Class for the Huffman Tree nodes
    private static class Node implements Comparable<Node>{

        // Declaring the variables
        int freq;
        Node left;
        Node right;
        short value;

        // The Node Constructor for a leaf
        Node(int freq, short value) {
            this.freq = freq;
            this.value = value;
            this.left = null;
            this.right = null;
        }  

        // The Node constructor for internal nodes
        Node(Node left, Node right) {
            this.freq = left.freq + right.freq;
            this.left = left;
            this.right = right;
            this.value = 0;
        }   

        // Returns true if the given node is a leaf
        public boolean isleaf(){
            return this.left == null && this.right == null;
        }

        // Compares the given node's frequency to the other node's (the one given as argument) frequency
        @Override
        public int compareTo(Node o) {
            return Integer.compare(this.freq, o.freq);
        }

    }


    // Declaring the first node of the Huffman Tree
    private Node first;


    /**
     * Constructs a new HuffmanTree from a frequency map.
     * @param freqs a map from 9-bit values to frequencies.
     */
    public HuffmanTree (Map<Short, Integer> freqs) {
        
        // Putting the eof into the frequency map
        freqs.put(eof, 1);

        // Creating the PriorityQueue to implement the HuffmanTree
        PriorityQueue queue = new PriorityQueue<>();

        // The following instructions for creating the Huffman Tree come from the wikipedia page I referenced to
        // understand the tree and it's structure

        // 1 . Create a leaf node for each symbol and add it to the priority queue.
        for (short key : freqs.keySet()){

            // Getting the frequency that maps to each key 
            int freq = freqs.get(key);

            // Creating a new leaf node with that frequency and it's key
            Node leaf = new Node(freq, key);

            // Adding it to our priority queue
            queue.add(leaf);

        }

        // 2 . While there is more than one node in the queue:
        //     Remove the two nodes of highest priority (lowest probability) from the queue
        //     Create a new internal node with these two nodes as children and with probability 
        //     equal to the sum of the two nodes' probabilities.
        //     Add the new node to the queue.
        while (queue.size() > 1){

            // Referenced java documentation to see what poll() function associated with Priority Queues does

            // Getting the two nodes of highest priority (given by poll which returns the node of lowest probability,
            // consistent with the structure of priority queues)

            // First node (highets priority)
            Node smallest = (Node) queue.poll();

            // Now that the first node is taken out, the second highest priority node is returned this time
            Node smallest2 = (Node) queue.poll();

            // Creating an internal node with the 2 nodes taken above as it's subtrees
            Node internal = new Node(smallest, smallest2);

            // Adding the internal node to the priority queue
            queue.add(internal);

        }

        // 3. The remaining node is the root node and the tree is complete.
        this.first = (Node) queue.poll();

    }

    /**
     * Constructs a new HuffmanTree from the given file.
     * @param in the input file (as a BitInputStream)
     */
    public HuffmanTree (BitInputStream in) {

        // We assign the root of our Huffman tree to what we construct through reading in the file 
        // from inputhelper
        this.first = inputhelper(in);

    }

    /**
     * Helper to construct a new HuffmanTree from the given file. Constructs the tree and then the actual function assigns
     * our declared root of the tree to this tree we create with the helper
     * 
     * @param in the input file (as a BitInputStream)
     * @return nothing
     */
    public Node inputhelper(BitInputStream in){

        // We read a bit
        int bit = in.readBit();

        // If the bit is 0, it is a leaf and 
        // we get the value from the next 9 bits and create a leaf node with frequency 0 and that value
        if (bit == 0){
            int value = in.readBits(9);
            return new Node(0, (short) value);

        // If it is 1, it is an internal node and we construct it by 
        // calling inputhelper recursively to get the left and right nodes and then creating the internal node using them
        } else {
            Node left = inputhelper(in);
            Node right = inputhelper(in);
            return new Node(left, right);
        }
        
    }

    /**
     * Writes this HuffmanTree to the given file as a stream of bits in a
     * serialized format.
     * @param out the output file as a BitOutputStream
     */
    public void serialize (BitOutputStream out) {

        // We pass in out and first (the root node we start the tree traversal with)
        serial_helper(first, out);
    }

    /**
     * Helps to write this HuffmanTree to the given file as a stream of bits in a
     * serialized format.
     * @param out the output file as a BitOutputStream
     * @param node the Current Node we start the tree traversal with
     */
    public void serial_helper(Node node, BitOutputStream out){

        // If the node is a leaf, we write a 0 bit and then 9 bits corresponding to the byte value stored at the leaf.
        if (node.isleaf()){
            out.writeBit(0);
            out.writeBits(node.value, 9);

        // If the node is an interior node, we write a 1 bit and then recursively write the left and right children of this node
        } else {
            out.writeBit(1);
            serial_helper(node.left, out);
            serial_helper(node.right, out);
        }
    
    }
   
    /**
     * Encodes the file given as a stream of bits into a compressed format
     * using this Huffman tree. The encoded values are written, bit-by-bit
     * to the given BitOuputStream.
     * @param in the file to compress.
     * @param out the file to write the compressed output to.
     */
    public void encode (BitInputStream in, BitOutputStream out) throws IllegalStateException{

        // Declaring an int object to read the 8 bits
        int bits;

        // The loop which runs while it isn't -1 which is what comes when the input stream becomes empty
        while ((bits = in.readBits(8)) != -1){

            // We get the bits as a short object
            short value = (short) bits;

            // We call the find helper to get the huffman code for that value
            String huffman_code = find(first, value, "");

            if (huffman_code == null){
                throw new IllegalStateException();
            }

            // We translate that code string into a character array
            char[] code_arr = huffman_code.toCharArray();

            // We then traverse it and write the bits into the out file according to what is in the char array
            // corresponding to the huffman code
            for (int i = 0; i < code_arr.length; i++){
                if (code_arr[i] == '0'){
                    out.writeBit(0);
                } else {
                    out.writeBit(1);
                }
            }


        }

        // After writing everything, we try to find the code for the EOF character and repeat the same process
        // to write it to the out file:

        // We call the find helper to get the huffman code for the EOF
        String EOF = find(first, eof, "");

        // We translate that code string into a character array
        char[] EOF_arr = EOF.toCharArray();

        // We then traverse it and write the bits into the out file according to what is in the char array
        // corresponding to the huffman code
        for (int i = 0; i < EOF_arr.length; i++){
            if (EOF_arr[i] == '0'){
                out.writeBit(0);
            } else {
                out.writeBit(1);
            }
        }

    }

    /**
     * Helper function used to find the Huffman code as a String for a bit stream/value
     * @param node the node we traverse the Huffman tree with to find the value
     * @param value the value whose equivalent Huffman Code needs to be found
     * @param binary the string in which we return the huffmancode
     * 
     */
    public String find(Node node, short value, String binary){

        // If the node is null, we simply return null
        if (node == null){
            return null;
        }

        // If it is a leaf and it's value is equal to the value whose corresponding huffman code we want to find,
        // we return the string
        if (node.isleaf()){
            if (node.value == value){
                return binary;
            }
        }

        // Otherwise, we recursively repeat the process over the left and right nodes of the internal node
        // (given it isn't null and it isn't a leaf)

        // For the left string, we add a "0" to binary whereas for the right string we add a "1"
        String left = find(node.left, value, binary + "0");
        if (left != null){
            return left;
        }

        String right = find(node.right, value, binary + "1");
        if (right != null){
            return right;
        }

        return null;

    }

    /**
     * Decodes a stream of huffman codes from a file given as a stream of
     * bits into their uncompressed form, saving the results to the given
     * output stream. Note that the EOF character is not written to out
     * because it is not a valid 8-bit chunk (it is 9 bits).
     * @param in the file to decompress.
     * @param out the file to write the decompressed output to.
     */
    public void decode (BitInputStream in, BitOutputStream out) {
        
        // We set current (the node we use to traverse the tree) to the root of our tree
        // Effectively assigning the entire tree to current
        Node current = first;

        // We declare an int object for reading in the bit
        int bit;

        // While we don't read in -1 (which is what comes when the input stream becomes empty)
        while ((bit = in.readBit()) != -1){

            // If the bit read is 0
            if (bit == 0){
                // We go to the left
                current = current.left;

            // If 1, we go to the right
            } else {
                current = current.right;
            }

            // If the node we eventually reach is a leaf:
            if (current.isleaf()){

                // We break if it is an eof character
                if (current.value == eof){
                    break;

                // If not, we write out the value of the current node as 8 bits
                } else {
                    out.writeBits(current.value, 8);

                    // Current gets reassigned to first for traversal (but the bits have moved forward)
                    // Now, we basically repeat the process for the next element
                    current = first;
                }

            }


        }
        

    }

}
