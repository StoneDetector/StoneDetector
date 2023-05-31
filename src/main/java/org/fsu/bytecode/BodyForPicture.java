package org.fsu.bytecode;

import soot.Body;
import soot.Unit;
import soot.UnitPatchingChain;
import soot.util.Chain;

public class BodyForPicture extends Body {
    public BodyForPicture(UnitPatchingChain unitPatchingChain)
    {
        this.unitChain=unitPatchingChain;
    }
    @Override
    public Object clone() {
        return null;
    }
}
