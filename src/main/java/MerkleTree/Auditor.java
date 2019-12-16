package MerkleTree;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;

public class Auditor {

	LogServerInterface server;
	byte[] root;
	int size;

	public Auditor(LogServerInterface server) {
		try {
			this.server = server;
			this.root = server.getRootHash();
			this.size = server.getSize();
		} catch (RemoteException e) {
			System.err.println("Error fetching from remote server.");
			e.printStackTrace();
		}
	}

	public boolean isMember(String log) throws RemoteException {
		byte[] hash = Utils.hash(Utils.concat(new byte[] { 0x00 }, log.getBytes()));

		// update tree size and get last root
		this.size = this.server.getSize();
		this.root = this.server.getRootHash();

		for (int i = 0; i < this.size; i++) {
			List<byte[]> path = this.server.genPath(i);
			if (this.verifyPath(path, hash, i))
				return true;
		}
		return false;
	}

	private boolean verifyPath(List<byte[]> path, byte[] ref, int index) {
		if (path.size() > 0) {
			byte[] nh = null;
			if (index % 2 == 0)
				nh = Utils.hash(Utils.concat(Utils.concat(new byte[] { 0x01 }, ref), path.get(0)));
			else
				nh = Utils.hash(Utils.concat(Utils.concat(new byte[] { 0x01 }, path.get(0)), ref));
			if (path.size() == 1)
				return Arrays.equals(this.root, nh);
			path.remove(0);
			// No tail call optimization? too bad :'(
			return verifyPath(path, nh, index / 2);
		} else
			return false;
	}
}
