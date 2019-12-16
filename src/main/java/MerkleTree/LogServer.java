package MerkleTree;

import java.util.List;
import java.util.ArrayList;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class LogServer extends UnicastRemoteObject implements LogServerInterface {

	private final Object msgLock = new Object();
	MerkleTree merkleTree;

	protected LogServer(String log) throws RemoteException {
		super();
		List<byte[]> byteLines = new ArrayList<>();
		String[] lines = log.split("\n");
		for (String line : lines) {
			byteLines.add(line.getBytes());
		}
		this.merkleTree = new MerkleTree(byteLines);
	}

	public byte[] getRootHash() throws RemoteException {
		byte[] hash = null;
		synchronized (msgLock) {
			hash = this.merkleTree.root.getHash();
		}
		return hash;
	}

	public void appendLog(String log) throws RemoteException {
		byte[] bytelog = log.getBytes();

		synchronized (msgLock) {
			this.merkleTree.append(bytelog);
		}
	}

	public void appendLogs(List<String> logs) throws RemoteException {
		synchronized (msgLock) {
			for (String log : logs) {
				this.appendLog(log);
			}
		}
	}

	public List<byte[]> genPath(int index) throws RemoteException {
		List<byte[]> path = null;
		synchronized (msgLock) {
			path = this.merkleTree.getPath(index);
		}
		return path;
	}

	public List<byte[]> genProof(int size) throws RemoteException {
		List<byte[]> proof = null;
		synchronized (msgLock) {
			proof = this.merkleTree.getProof(size);
		}
		return proof;
	}

	public int getSize() throws RemoteException {
		int size = 0;
		synchronized (msgLock) {
			size = this.merkleTree.size;
		}
		return size;
	}
}
