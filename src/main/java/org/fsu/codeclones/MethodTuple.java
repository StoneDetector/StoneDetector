package org.fsu.codeclones;

import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtExecutable;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class MethodTuple {
    public List<List<Encoder>> encodePathSet;
    public MethodInfo info;

    public MethodTuple(CtExecutable m, List<List<Encoder>> encodePathSet, Path currentFile) {
        this.encodePathSet = encodePathSet;
        this.info = new MethodInfo(m,currentFile);
    }

    public MethodTuple(List<List<Encoder>> encodePathSet, String subFolder,String name, int startLine,int endLine) {
        this.encodePathSet = encodePathSet;
        this.info = new MethodInfo(subFolder,name,startLine,endLine);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return this.info.equals(((MethodTuple) o).info);
    }
}
