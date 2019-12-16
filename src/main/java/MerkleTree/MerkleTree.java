package MerkleTree;

import java.util.Collections;
import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.io.OutputStream;

public class MerkleTree {

	TreeElement root;
	List<TreeElement> leaves;
	int size;

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
			this.left = left;
			this.right = right;
			this.start = Math.min(right.getStart(), left.getStart());
			this.end = Math.max(right.getEnd(), left.getEnd());
			updateHash();
		}

		public void updateHash() {
			this.hash = Utils.hash(Utils.concat(Utils.concat(new byte[] { 0x01 }, left.getHash()),
					right.getHash()));
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

		public Leaf(byte[] val, int index) {
			this.hash = Utils.hash(Utils.concat(new byte[] { 0x00 }, val));
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

		public void setValue(byte[] val) {
			this.hash = Utils.hash(Utils.concat(new byte[] { 0x00 }, val));
		}

		@Override
		public String toString() {
			String hash = Utils.encodeHexString(this.getHash(), 5);
			return hash;
		}
	}

	public MerkleTree(List<byte[]> items) {
		List<TreeElement> leaves = new ArrayList<>();
		for (int i = 0; i < items.size(); i++) {
			leaves.add(new Leaf(items.get(i), i));
		}
		// pad the tree with empty nodes
		int next_pow = (int) Math.pow(2, (int) Math.ceil(Math.log((double) items.size()) / Math.log(2.0)));

		for (int i = items.size(); i < next_pow; i++) {
			leaves.add(new Leaf(new byte[] { 0x00 }, i));
		}
		this.size = items.size();
		this.leaves = leaves;
		this.root = build(leaves);

	}

	public void append(byte[] content) {
		// check if enough capacity
		this.size++;
		if (this.size > this.leaves.size()) {
			int next_pow = (int) Math.pow(2, (int) Math.ceil(Math.log((double) this.size) / Math.log(2.0)));
			for (int i = this.size; i < next_pow; i++) {
				this.leaves.add(new Leaf(new byte[] { 0x00 }, i));
			}
			this.root = build(this.leaves);
		}
		TreeElement nl = this.leaves.get(this.size - 1);
		((Leaf) nl).setValue(content);
		// update path to new appended node
		Stack<TreeElement> path = new Stack<>();
		path.push(this.root);
		while (true) {
			Node current = (Node) path.peek();
			if (current.left.equals(nl) || current.right.equals(nl))
				break;
			if (nl.getStart() >= current.right.getStart())
				path.push(current.right);
			else
				path.push(current.left);
		}

		while (!path.isEmpty())
			((Node) path.pop()).updateHash();
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
		while (current.getType() != TreeElementType.LEAF) {

			Node currentNode = ((Node) current);

			if (index >= currentNode.right.getStart()) {
				path.add(currentNode.left.getHash());
				current = currentNode.right;
			} else {
				path.add(currentNode.right.getHash());
				current = currentNode.left;
			}
		}
		// apparently we want it in reverse order...
		Collections.reverse(path);
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

	private TreeElement build(List<TreeElement> leaves) {
		int half = leaves.size() % 2 == 0 ? leaves.size() / 2 : leaves.size() / 2 + 1;
		List<TreeElement> left = leaves.subList(0, half);
		List<TreeElement> right = leaves.subList(half, leaves.size());
		if (left.size() == 0 && right.size() == 0)
			return null;
		else if (left.size() == 0 && right.size() == 1)
			return right.get(0);
		else if (left.size() == 1 && right.size() == 0)
			return left.get(0);
		else
			return new Node(build(left), build(right));

	}

	@Override
	public String toString() {
		// reccursive here too. bad thing.
		return this.root.toString();
	}
}
