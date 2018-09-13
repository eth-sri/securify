package ch.securify;

import org.junit.Test;
import org.omg.CosNaming.NamingContextPackage.NotFound;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

public class CompilationHelpersTest {

    @Test
    public void bytecodeOffsetToSourceOffset() throws NotFound {
        testBytecodeOffsetToSourceOffset("0:1:1;1:1:1;2:1:1;;;3:1:1", 1,0);
        testBytecodeOffsetToSourceOffset("0:1:1;1:1:1;2:1:1;;;3:1:1", 2,1);
        testBytecodeOffsetToSourceOffset("0:1:1;1:1:1;2:1:1;;;3:1:1", 6,3);
        testBytecodeOffsetToSourceOffset("0:1:1;1:1:1;2:1:1;;;3:1:1", 5,2);
    }

    public void testBytecodeOffsetToSourceOffset(String map, int index, int res) throws NotFound {
        List<String[]> explodedMap = CompilationHelpers.explodeMappingString(map);
        assertEquals(CompilationHelpers.bytecodeOffsetToSourceOffset(index, explodedMap), res );
    }
}