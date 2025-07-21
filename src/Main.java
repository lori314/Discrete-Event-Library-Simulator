import java.util.Map;
import com.oocourse.library3.LibraryIO;
import com.oocourse.library3.LibraryCommand;
import com.oocourse.library3.LibraryOpenCmd;
import com.oocourse.library3.LibraryCloseCmd;
import com.oocourse.library3.LibraryReqCmd;
import com.oocourse.library3.LibraryBookIsbn;
import com.oocourse.library3.LibraryQcsCmd;

public class Main {
    public static void main(String[] args) {
        Map<LibraryBookIsbn, Integer> initialInventory = LibraryIO.SCANNER.getInventory();
        Library library = new Library(initialInventory);
        while (true) {
            LibraryCommand command = LibraryIO.SCANNER.nextCommand();
            if (command == null) {
                break;
            }
            if (command instanceof LibraryOpenCmd) {
                library.open((LibraryOpenCmd) command);
            } else if (command instanceof LibraryCloseCmd) {
                library.close((LibraryCloseCmd) command);
            } else if (command instanceof LibraryQcsCmd) {
                library.queryCreditsScore((LibraryQcsCmd) command);
            } else if (command instanceof LibraryReqCmd) {
                library.processRequest((LibraryReqCmd) command);
            } else {
                System.err.println("Unknown command type: " + command);
            }
        }
    }
}