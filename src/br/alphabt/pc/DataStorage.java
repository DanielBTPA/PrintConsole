package br.alphabt.pc;

import java.io.*;

@Deprecated
public class DataStorage<M extends Serializable> {

    public void saveState(String name, M obj) {
        File file = new File(name + ".data");

        try {
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(obj);
            oos.close();
        } catch (Exception io) {
            throw new RuntimeException(io);
        }
    }

    public M getState(String name) {
        M obj;

        try {
            FileInputStream fis = new FileInputStream(name + ".data");
            ObjectInputStream ois = new ObjectInputStream(fis);
            obj = (M) ois.readObject();
            ois.close();
        } catch (Exception io) {
            throw new RuntimeException(io);
        }

        return obj;
    }

    public boolean isFileExist(String name) {
        File file = new File(name + ".data");
        return file.exists();
    }

}
