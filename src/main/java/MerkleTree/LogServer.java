package MerkleTree;

import java.util.List;
import java.util.ArrayList;

public class LogServer {

	MerkleTree merkleTree;

	public LogServer(String log) {
		List<byte[]> byteLines = new ArrayList<>();
		String[] lines = log.split("\n");
		for (String line : lines) {
			byteLines.add(line.getBytes());
		}
		this.merkleTree = new MerkleTree(byteLines);
	}

	public byte[] getRootHash() {
		return this.merkleTree.root.getHash();
	}

	public void appendLog(String log) {
		byte[] bytelog = log.getBytes();

		this.merkleTree.append(bytelog);
	}

	public void appendLogs(List<String> logs) {
		for (String log : logs) {
			this.appendLog(log);
		}
	}

	public List<byte[]> genPath(int index) {
		return this.merkleTree.getPath(index);
	}

	public List<byte[]> genProof(int size) {
		return this.merkleTree.getProof(size);
	}
}
