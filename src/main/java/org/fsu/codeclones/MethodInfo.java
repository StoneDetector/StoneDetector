package org.fsu.codeclones;

import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtExecutable;

import java.nio.file.Path;
import java.util.Objects;

public class MethodInfo {
    public String subDir;
    public String fileName;
    public int startLine;
    public int endLine;

    public MethodInfo(CtExecutable m) {
        this.startLine = m.getPosition().getLine();
        this.endLine = m.getPosition().getEndLine();
        for (CtAnnotation annotation : m.getAnnotations())
        {
            if (annotation.getPosition().getLine()<this.startLine)
                this.startLine=annotation.getPosition().getLine();
        }
    }


    public MethodInfo(CtExecutable m, Path currentFile) {
        this.subDir = currentFile.getParent().getFileName().toString();
        this.fileName = currentFile.getFileName().toString();
        this.startLine = m.getPosition().getLine();
        this.endLine = m.getPosition().getEndLine();
        for (CtAnnotation annotation : m.getAnnotations())
        {
            if (annotation.getPosition().getLine()<this.startLine)
                this.startLine=annotation.getPosition().getLine();
        }
    }

    public MethodInfo(String subFolder,String name, int startLine,int endLine) {
        this.subDir = subFolder;
        this.fileName = name;
        this.startLine = startLine;
        this.endLine = endLine;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodInfo that = (MethodInfo) o;
        return startLine == that.startLine &&
                endLine == that.endLine &&
                Objects.equals(subDir, that.subDir) &&
                Objects.equals(fileName, that.fileName);
    }
}
