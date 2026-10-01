/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.gui.util.classfile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ClassReaderUtil {
    /**
     * Latest class file major version supported by the bundled ASM library.
     */
    public static final int LATEST_MAJOR_VERSION = Opcodes.V27;

    public static ClassReader newClassReader(InputStream is) throws IOException {
        return newClassReader(readAllBytes(is));
    }

    /**
     * ASM refuses class files whose major version is newer than the latest version it knows. JD-GUI only reads
     * declarations and the constant pool, which keep the same structure across versions, so class files compiled
     * by a newer JDK are read as if they had been compiled for the latest supported version.
     */
    public static ClassReader newClassReader(byte[] classFile) {
        if ((classFile.length >= 8) && (((classFile[6] & 0xFF) << 8) | (classFile[7] & 0xFF)) > LATEST_MAJOR_VERSION) {
            classFile = classFile.clone();
            classFile[6] = (byte)(LATEST_MAJOR_VERSION >>> 8);
            classFile[7] = (byte)LATEST_MAJOR_VERSION;
        }

        return new ClassReader(classFile);
    }

    protected static byte[] readAllBytes(InputStream is) throws IOException {
        if (is == null) {
            throw new IOException("Class not found");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int len;

        while ((len = is.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }

        return baos.toByteArray();
    }
}
