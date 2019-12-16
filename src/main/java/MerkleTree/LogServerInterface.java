package MerkleTree;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface LogServerInterface extends Remote {

	public byte[] getRootHash() throws RemoteException;

	public void appendLog(String log) throws RemoteException;

	public void appendLogs(List<String> logs) throws RemoteException;

	public List<byte[]> genPath(int index) throws RemoteException;

	public List<byte[]> genProof(int size) throws RemoteException;

	public int getSize() throws RemoteException;
}
