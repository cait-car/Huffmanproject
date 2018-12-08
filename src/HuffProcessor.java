import java.util.PriorityQueue;

/**
 * Although this class has a history of several years,
 * it is starting from a blank-slate, new and clean implementation
 * as of Fall 2018.
 * <P>
 * Changes include relying solely on a tree for header information
 * and including debug and bits read/written information
 * 
 * @author Owen Astrachan
 */

public class HuffProcessor {

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD); 
	public static final int PSEUDO_EOF = ALPH_SIZE;
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;

	private final int myDebugLevel;
	
	public static final int DEBUG_HIGH = 4;
	public static final int DEBUG_LOW = 1;
	
	public HuffProcessor() {
		this(0);
	}
	
	public HuffProcessor(int debug) {
		myDebugLevel = debug;
	}

	/**
	 * Compresses a file. Process must be reversible and loss-less.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void compress(BitInputStream in, BitOutputStream out){

		int[] counts = readForCounts(in);
		HuffNode root = makeTreeFromCounts(counts);
		String [] codings = makeCodingsFromTree(root);
		
		out.writeBits(BITS_PER_INT, HUFF_TREE);
		writeHeader(root,out);
		
		in.reset();
		writeCompressedBits(codings,in,out);
		out.close();
	}
	
	

	private int[] readForCounts(BitInputStream in) {
		int [] counts = new int [ALPH_SIZE+1];
		
		
		
		while(true) {
			int input = in.readBits(BITS_PER_WORD);
			if(input== -1) {
				break;
			}
			counts[input]++;	

		}
		counts[PSEUDO_EOF] = 1;
		return counts;
	}
	
	

	private HuffNode makeTreeFromCounts(int [] counts) {
		PriorityQueue<HuffNode> pq = new PriorityQueue<>();

		for(int co : counts) {
			if(counts[co] > 0) {
				pq.add(new HuffNode(co,counts[co], null, null));
			}
		}
		
		while(pq.size() > 1) {
			HuffNode left = pq.remove();
			HuffNode right = pq.remove();
			HuffNode newNode = new HuffNode(0 , left.myWeight+right.myWeight, left, right);
			pq.add(newNode);
		}
		HuffNode root = pq.remove();
		return root;
	}

	 
	
	private String [] makeCodingsFromTree(HuffNode root) {
		String [] encodings = new String[ALPH_SIZE+1];
		codingHelper(root,"",encodings);
		return encodings;
	}
		
		private void codingHelper(HuffNode t, String path, String [] encodings) {
			if (t == null) return;
			
			if (t.myLeft == null && t.myRight == null) {
				encodings[t.myValue] = path;
				return;
			}
			codingHelper(t.myLeft,path+"0",encodings);
			codingHelper(t.myRight,path+"1",encodings);
		}
	
	
	
	private void writeHeader(HuffNode root, BitOutputStream out) {
	
		if(root.myLeft==null&root.myRight == null) {
		out.writeBits(1,1);
		out.writeBits(BITS_PER_WORD, root.myValue);
		
		}
		else {
			out.writeBits(1,0);
			writeHeader(root.myRight, out);
			writeHeader(root.myLeft, out);
		}
		
	}
	
	
	private void writeCompressedBits(String [] codings, BitInputStream in, BitOutputStream out) {
		
		for(int k=0;k<codings.length;k++) {
			String code = codings[k];
			out.writeBits(code.length(), Integer.parseInt(code,2));
	}
		String code1 = codings[PSEUDO_EOF];
		out.writeBits(code1.length(), Integer.parseInt(code1,2));
	}

	


	/**
	 * Decompresses a file. Output file must be identical bit-by-bit to the
	 * original.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be decompressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 *           
	 */

	 
	public void decompress(BitInputStream in, BitOutputStream out){

		int bits = in.readBits(BITS_PER_INT);
		if(bits !=HUFF_TREE) {
			throw new HuffException("illegal header starts with "+bits);
			
		}
		HuffNode root = readTreeHeader(in);
		readCompressedBits(root,in,out);
		out.close();
	
}
	private HuffNode readTreeHeader(BitInputStream in) {

		int bit = in.readBits(1);

		if (bit == -1) throw new HuffException("illegal bit");
		
		if (bit == 0) {
			HuffNode rootL = readTreeHeader(in);
			HuffNode rootR = readTreeHeader(in);
					return new HuffNode(0,0,rootL,rootR);
		}
		else {
			int value = in.readBits(BITS_PER_WORD+1);
			return new HuffNode(value,0,null,null);
		
		}
	}
	
		private void readCompressedBits(HuffNode root, BitInputStream in, BitOutputStream out) {
			
			HuffNode current = root; 
			while (true) {
				int bits = in.readBits(1);
				if (bits == -1) 
					throw new HuffException("bad input, no PSEUDO_EOF");
				
				else { 
					if (bits == 0) current = current.myLeft;
					else current = current.myRight;

					//if (bits == 1) {
					if(current.myRight==null && current.myLeft==null) {
						if (current.myValue == PSEUDO_EOF) 
							break;   // out of loop
							
						else {
							out.writeBits(BITS_PER_WORD, current.myValue);
									current = root; // start back after leaf
						}
					}
				}
				
				
			}

		}

}