package MerkleTree;

import java.util.Arrays;
import java.util.HashMap;
import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.io.OutputStream;

public class MerkleTree {

	TreeElement root;
	HashMap<String, TreeElement> memmap;
	List<TreeElement> leaves;

	interface TreeElement {
		public int getStart();

		public int getEnd();

		public byte[] getHash();

		public TreeElementType getType();

		public String getKey();
	}

	enum TreeElementType {
		LEAF, NODE
	}

	class Node implements TreeElement {
		byte[] hash;
		TreeElement right, left;
		int start, end;

		public Node(TreeElement left, TreeElement right) {
			this.hash = hash(concat(concat(new byte[] { 0x01 }, left.getHash()), right.getHash()));
			this.left = left;
			this.right = right;
			this.start = Math.min(right.getStart(), left.getStart());
			this.end = Math.max(right.getEnd(), left.getEnd());
		}

		public byte[] getHash() {
			return this.hash;
		}

		public int getStart() {
			return this.start;
		}

		public int getEnd() {
			return this.end;
		}

		public TreeElementType getType() {
			return TreeElementType.NODE;
		}

		public String getKey() {
			return String.format("%d%d", this.start, this.end);
		}

		@Override
		public String toString() {
			String hash = Utils.encodeHexString(this.getHash(), 5);
			return String.format("(%s %s %s)", hash, this.left, this.right);
		}
	}

	class Leaf implements TreeElement {
		byte[] hash;
		int index;

		public Leaf(byte[] hash, int index) {
			this.hash = hash;
			this.index = index;
		}

		public byte[] getHash() {
			return this.hash;
		}

		public int getStart() {
			return this.index;
		}

		public int getEnd() {
			return this.index;
		}

		public TreeElementType getType() {
			return TreeElementType.LEAF;
		}

		public String getKey() {
			return String.format("%d", this.index);
		}

		@Override
		public String toString() {
			String hash = Utils.encodeHexString(this.getHash(), 5);
			return hash;
		}
	}

	public MerkleTree(List<byte[]> items) {
		this.memmap = new HashMap<>();
		List<TreeElement> leaves = new ArrayList<>();
		for (int i = 0; i < items.size(); i++) {
			leaves.add(new Leaf(hash(items.get(i)), i));
		}
		this.leaves = leaves;
		this.root = build(leaves);

	}

	public void append(byte[] content) {
		Leaf newLeaf = new Leaf(content, this.leaves.size());
		this.leaves.add((TreeElement) newLeaf);
		this.root = build(this.leaves);
	}

	public void exportTreeViz(String path) {
		Stack<TreeElement> stack = new Stack<>();
		String output = new String();
		output = output.concat("digraph G {\n");
		TreeElement currentNode = this.root;
		do {
			switch (currentNode.getType()) {
			case NODE:
				Node node = (Node) currentNode;
				stack.push(node.right);
				stack.push(node.left);
				String currentHash = Utils.encodeHexString(node.getHash(), 5);
				String leftHash = Utils.encodeHexString(node.left.getHash(), 5);
				String rightHash = Utils.encodeHexString(node.right.getHash(), 5);
				output = output.concat(String.format("\"%s\"->\"%s\";\n", currentHash, leftHash));
				output = output.concat(String.format("\"%s\"->\"%s\";\n", currentHash, rightHash));
				break;
			case LEAF:

			}
		} while (!stack.empty() && (currentNode = stack.pop()) != null);
		output = output.concat("}\n");
		OutputStream os = null;
		try {
			os = new FileOutputStream(new File(path));
			os.write(output.getBytes(), 0, output.length());
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				os.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public List<byte[]> getPath(int index) {
		ArrayList<byte[]> path = new ArrayList<>();

		if (index > this.root.getEnd())
			return null;

		TreeElement current = this.root;
		path.add(current.getHash());
		while (current.getType() != TreeElementType.LEAF) {

			Node currentNode = ((Node) current);

			if (index >= currentNode.right.getStart()) {
				current = currentNode.right;
			} else {
				current = currentNode.left;
			}
			path.add(current.getHash());
		}
		return path;
	}

	public List<byte[]> getProof(int size) {
		ArrayList<byte[]> proof = new ArrayList<>();

		// Maybe someday Java will support unsigned int...
		if (size > this.leaves.size() || size < 0)
			return null;
		int start = size;
		int end = this.leaves.size() - 1;

		System.out.printf("proof for : %d %d", start, end);

		TreeElement current = this.root;
		while (current.getType() != TreeElementType.LEAF) {
			Node currentNode = (Node) current;
			if (currentNode.getStart() == start && currentNode.getEnd() == end) {
				proof.add(currentNode.getHash());
				break;
			} else if (start >= currentNode.right.getStart() && end <= currentNode.getEnd()) {
				proof.add(currentNode.left.getHash());
				current = currentNode.right;
			} else {
				proof.add(currentNode.right.getHash());
				current = currentNode.left;
			}
		}
		return proof;
	}

	private byte[] concat(byte[] first, byte[] second) {
		byte[] concat = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, concat, first.length, second.length);
		return concat;
	}

	private static byte[] hash(byte[] val) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return digest.digest(val);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private TreeElement build(List<TreeElement> leaves) {
		List<TreeElement> left = leaves.subList(0, leaves.size() / 2);
		List<TreeElement> right = leaves.subList(leaves.size() / 2, leaves.size());
		if (left.size() == 0 && right.size() == 0) {
			return null;
		} else if (left.size() == 0 && right.size() == 1) {
			return right.get(0);
		} else if (left.size() == 1 && right.size() == 0) {
			return left.get(0);
		} else {
			TreeElement rightNode = null;
			TreeElement leftNode = null;

			String rightKey = String.format("%d%d", right.get(0).getStart(),
					right.get(right.size() - 1).getEnd());
			String leftKey = String.format("%d%d", left.get(0).getStart(),
					left.get(left.size() - 1).getEnd());

			if (this.memmap.containsKey(rightKey)) {
				rightNode = this.memmap.get(rightKey);
			} else {
				// bad practice in java, but meh...
				rightNode = build(right);
				// we should clean unused values in the map at some point, but this is for sake
				// of demonstration.
				this.memmap.put(rightKey, rightNode);
			}

			if (this.memmap.containsKey(leftKey)) {
				leftNode = this.memmap.get(leftKey);
			} else {
				leftNode = build(left);
				this.memmap.put(leftKey, leftNode);
			}
			return new Node(leftNode, rightNode);
		}
	}

	@Override
	public String toString() {
		// reccursive here too. bad thing.
		return this.root.toString();
	}
}
