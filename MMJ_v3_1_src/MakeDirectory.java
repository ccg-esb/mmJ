/*START "MakeDirectory_.java" code
** note that "mkdirs" (vs. "mkdir") makes
** all intermediate parent directories
** even if they don't yet exist
*/
import ij.*;
import ij.plugin.*;
import java.io.*;

public class MakeDirectory implements PlugIn {
String DirToMake = Macro.getOptions();
public void run(String arg) {
File newDir = new File(DirToMake);
newDir.mkdirs();
}
}
/** END "MakeDirectory_.java" code */