/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package movie.genre.pmm;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 * @author Olayinka
 */
public class DirFileWriter extends FileWriter {

    public DirFileWriter(String fileName) throws IOException {
        super(fileName, new File(fileName).getParentFile().mkdirs() & false);
    }

    public DirFileWriter(String fileName, boolean append) throws IOException {
        super(fileName, new File(fileName).getParentFile().mkdirs() ? append : append);
    }

}
