import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.oocourse.library3.LibraryBookId;
import com.oocourse.library3.LibraryBookIsbn;
import com.oocourse.library3.LibraryBookState;
import com.oocourse.library3.LibraryOpenCmd;
import com.oocourse.library3.LibraryCloseCmd;
import com.oocourse.library3.LibraryReqCmd;
import com.oocourse.library3.LibraryIO;
import com.oocourse.library3.LibraryMoveInfo;
import com.oocourse.library3.LibraryTrace;
import com.oocourse.library3.LibraryQcsCmd;

public class Library {
    private Map<LibraryBookIsbn, Book> books;
    private Map<LibraryBookId, BookCopy> bookCopies;
    private Map<String, User> users;
    private LocalDate currentDate;
    private List<Appointment> activeAppointments;
    private Map<LibraryBookIsbn, List<User>> pendingReservationsByIsbn;
    private Map<LibraryBookIsbn, Book> hotBooks;

    public Library(Map<LibraryBookIsbn, Integer> initialInventory) {
        this.books = new HashMap<>();
        this.bookCopies = new HashMap<>();
        this.users = new HashMap<>();
        this.activeAppointments = new ArrayList<>();
        this.pendingReservationsByIsbn = new HashMap<>();
        this.hotBooks = new HashMap<>();

        for (Map.Entry<LibraryBookIsbn, Integer> entry : initialInventory.entrySet()) {
            LibraryBookIsbn isbn = entry.getKey();
            int count = entry.getValue();
            Book book = new Book(isbn, count);
            books.put(isbn, book);
            for (int i = 1; i <= count; i++) {
                LibraryBookId bookId = turn(isbn, i);
                BookCopy copy = new BookCopy(bookId);
                bookCopies.put(bookId, copy);
            }
        }
    }

    private LibraryBookId turn(LibraryBookIsbn isbn, int num) {
        if (num != 10) {
            return new LibraryBookId(isbn.getType(), isbn.getUid(), "0" + num);
        }
        else {
            return new LibraryBookId(isbn.getType(), isbn.getUid(), "10");
        }
    }

    private User findOrCreateUser(String studentId) {
        return users.computeIfAbsent(studentId, User::new);
    }

    private Book findBookType(LibraryBookIsbn isbn) {
        return books.get(isbn);
    }

    private BookCopy findBookCopy(LibraryBookId bookId) {
        return bookCopies.get(bookId);
    }

    public void open(LibraryOpenCmd cmd) {
        currentDate = cmd.getDate();
        openOrganize();
    }

    public void close(LibraryCloseCmd cmd) {
        currentDate = cmd.getDate();
        closeOrganize();
    }

    public void queryCreditsScore(LibraryQcsCmd cmd) {
        currentDate = cmd.getDate();
        String studentId = cmd.getStudentId();
        User user = findOrCreateUser(studentId);
        LibraryIO.PRINTER.info(cmd,user.getCredits());
    }

    public void processRequest(LibraryReqCmd cmd) {
        currentDate = cmd.getDate();
        String studentId = cmd.getStudentId();
        LibraryReqCmd.Type type = cmd.getType();
        User user = findOrCreateUser(studentId);

        switch (type) {
            case QUERIED:
                LibraryBookId queryBookId = cmd.getBookId();
                handleQuery(cmd, user, queryBookId);
                break;
            case BORROWED:
                LibraryBookIsbn borrowIsbn = cmd.getBookIsbn();
                handleBorrow(cmd, user, borrowIsbn);
                break;
            case ORDERED:
                LibraryBookIsbn orderIsbn = cmd.getBookIsbn();
                handleOrder(cmd, user, orderIsbn);
                break;
            case RETURNED:
                LibraryBookId returnBookId = cmd.getBookId();
                handleReturn(cmd, user, returnBookId);
                break;
            case PICKED:
                LibraryBookIsbn pickBookId = cmd.getBookIsbn();
                handlePick(cmd, user, pickBookId);
                break;
            case READ:
                LibraryBookIsbn readBookId = cmd.getBookIsbn();
                handleRead(cmd, user, readBookId);
                break;
            case RESTORED:
                LibraryBookId restoredBookId = cmd.getBookId();
                handleRestored(cmd, user, restoredBookId);
                break;
            // TODO: 添加 CREDIT 和 RETURN (归还阅读室书) 类型，如果作业需要
            default:
                System.err.println("Unknown command type: " + type);
                break;
        }
    }

    private void handleBorrow(LibraryReqCmd cmd, User user, LibraryBookIsbn isbn) {
        Book book = findBookType(isbn);
        if (book == null) {
            LibraryIO.PRINTER.reject(cmd);
            return;
        }
        if (!user.canBorrow(book)) {
            LibraryIO.PRINTER.reject(cmd);
            return;
        }
        BookCopy availableCopy = null;
        for (BookCopy copy : bookCopies.values()) {
            if (copy.getIsbn().equals(isbn) &&
                (copy.getCurrentLocation() == LibraryBookState.BOOKSHELF
                || copy.getCurrentLocation() == LibraryBookState.HOT_BOOKSHELF)) {
                availableCopy = copy;
                break;
            }
        }
        if (availableCopy == null) {
            LibraryIO.PRINTER.reject(cmd);
            return;
        }
        user.addHeldCopy(availableCopy);
        availableCopy.moveTo(LibraryBookState.USER, currentDate, user);
        availableCopy.beBorrowed(currentDate,user);
        hotBooks.put(isbn, book);
        LibraryIO.PRINTER.accept(cmd, availableCopy.getBookNumber());
    }

    private void handleReturn(LibraryReqCmd cmd, User user, LibraryBookId bookId) {
        BookCopy copy = findBookCopy(bookId);
        user.removeHeldCopy(copy);
        copy.moveTo(LibraryBookState.BORROW_RETURN_OFFICE, currentDate, null);
        int days;
        if (copy.getIsbn().isTypeB()) { days = 30; }
        else { days = 60; }
        int borrowDays = (int)ChronoUnit.DAYS.between(copy.getBorrowedDate(), currentDate);
        copy.beReturned();
        if (borrowDays <= days) {
            user.update(10);
            LibraryIO.PRINTER.accept(cmd,"not overdue");
        }
        else {
            LibraryIO.PRINTER.accept(cmd,"overdue");
        }
    }

    private void handleQuery(LibraryReqCmd cmd, User user, LibraryBookId bookId) {
        BookCopy copy = findBookCopy(bookId);
        if (copy == null) {
            System.err.println("Error: Query for non-existent book: " + bookId);
            return;
        }
        List<LibraryTrace> history = copy.getMovementHistory();
        LibraryIO.PRINTER.info(currentDate, bookId, history);
    }

    private void handleOrder(LibraryReqCmd cmd, User user, LibraryBookIsbn isbn) {
        Book book = findBookType(isbn);
        if (book == null) {
            LibraryIO.PRINTER.reject(cmd);
            return;
        }
        if (!user.canReserve(book)) {
            LibraryIO.PRINTER.reject(cmd);
            return;
        }
        boolean alreadyReserved = false;
        for (Appointment app : activeAppointments) {
            if (app.getUser().equals(user)) {
                alreadyReserved = true;
                break;
            }
        }
        if (!user.getReservedCopies().isEmpty()) {
            alreadyReserved = true;
        }
        if (alreadyReserved) {
            LibraryIO.PRINTER.reject(cmd);
            return;
        }
        List<User> list;
        if (pendingReservationsByIsbn.containsKey(isbn)) {
            list = pendingReservationsByIsbn.get(isbn);
        }
        else {
            list = new ArrayList<>();
        }
        list.add(user);
        pendingReservationsByIsbn.put(isbn, list);
        user.addReservedCopy(findBookType(isbn));
        LibraryIO.PRINTER.accept(cmd);
    }

    private void handlePick(LibraryReqCmd cmd, User user, LibraryBookIsbn bookId) {
        Appointment appointment = null;
        for (Appointment app : activeAppointments) {
            if (app.getBookCopy() != null &&
                app.getBookCopy().getIsbn().equals(bookId) && app.getUser().equals(user)) {
                appointment = app;
                if (appointment.isExpired(currentDate)) {
                    appointment = null;
                }
                break;
            }
        }
        if (appointment == null) {
            LibraryIO.PRINTER.reject(cmd);
            return;
        }
        BookCopy copy = appointment.getBookCopy();
        boolean passesLimit = user.canBorrow(findBookType(copy.getIsbn()));
        if (!passesLimit) {
            LibraryIO.PRINTER.reject(cmd);
            return;
        }
        user.removeReservedCopy(findBookType(bookId));
        activeAppointments.remove(appointment);
        user.addHeldCopy(copy);
        copy.moveTo(LibraryBookState.USER, currentDate, user);
        copy.beBorrowed(currentDate,user);
        LibraryIO.PRINTER.accept(cmd, copy.getBookNumber());
    }

    private void handleRead(LibraryReqCmd cmd, User user, LibraryBookIsbn isbn) {
        Book book = findBookType(isbn);
        if (book == null) {
            LibraryIO.PRINTER.reject(cmd);
            return;
        }
        if (!user.canRead(book)) {
            LibraryIO.PRINTER.reject(cmd);
            return;
        }
        BookCopy availableCopy = null;
        for (BookCopy copy : bookCopies.values()) {
            if (copy.getIsbn().equals(isbn) &&
                (copy.getCurrentLocation() == LibraryBookState.BOOKSHELF
                || copy.getCurrentLocation() == LibraryBookState.HOT_BOOKSHELF)) {
                availableCopy = copy;
                break;
            }
        }
        if (availableCopy == null) {
            LibraryIO.PRINTER.reject(cmd);
            return;
        }
        user.read(availableCopy);
        availableCopy.moveTo(LibraryBookState.READING_ROOM, currentDate, user);
        hotBooks.put(isbn, book);
        availableCopy.beBorrowed(currentDate,user);
        LibraryIO.PRINTER.accept(cmd, availableCopy.getBookNumber());
    }

    private void handleRestored(LibraryReqCmd cmd, User user, LibraryBookId bookId) {
        BookCopy copy = findBookCopy(bookId);
        copy.moveTo(LibraryBookState.BORROW_RETURN_OFFICE, currentDate, user);
        user.restored();
        user.update(10);
        copy.beReturned();
        LibraryIO.PRINTER.accept(cmd);
    }

    private void openOrganize() {
        List<LibraryMoveInfo> moves = new ArrayList<>();
        openHelp(moves);
        int count = bookCopies.size() / 10 + 1;
        for (Map.Entry<LibraryBookIsbn,List<User>> entry : pendingReservationsByIsbn.entrySet()) {
            LibraryBookIsbn isbn = entry.getKey();
            List<User> users = entry.getValue();
            List<User> temp = new ArrayList<>();
            for (User user : users) {
                boolean admitted = (count != 0);
                if (admitted) {
                    BookCopy availableCopy = null;
                    for (BookCopy copy : bookCopies.values()) {
                        if (copy.getIsbn().equals(isbn) &&
                            copy.getCurrentLocation() == LibraryBookState.BOOKSHELF) {
                            availableCopy = copy;
                            break;
                        }
                    }
                    if (availableCopy != null) {
                        activeAppointments.add(new Appointment(user,
                            availableCopy,currentDate,currentDate.plusDays(4)));
                        temp.add(user);
                        count--;
                        availableCopy.moveTo(LibraryBookState.APPOINTMENT_OFFICE,currentDate, user);
                        moves.add(new LibraryMoveInfo(availableCopy.getBookNumber(),
                            LibraryBookState.BOOKSHELF, LibraryBookState.APPOINTMENT_OFFICE,
                            user.getStudentId()));
                    }
                }
            }
            users.removeAll(temp);
        }
        for (User user : users.values()) {
            user.restored();
        }
        checkUserHeld();
        LibraryIO.PRINTER.move(currentDate, moves);
    }

    private void closeOrganize() {
        List<LibraryMoveInfo> moves = new ArrayList<>();
        List<Appointment> expiredAppointments = new ArrayList<>();
        List<BookCopy> expiredCopiesAtAo = new ArrayList<>();
        comeBackCopies(moves);
        for (Appointment app : activeAppointments) {
            if (app.isExpired(currentDate)) {
                expiredAppointments.add(app);
                if (app.getBookCopy() != null &&
                    app.getBookCopy().getCurrentLocation() == LibraryBookState.APPOINTMENT_OFFICE) {
                    expiredCopiesAtAo.add(app.getBookCopy());
                }
                User user = app.getUser();
                user.removeReservedCopy(findBookType(app.getBookCopy().getIsbn()));
            }
        }
        activeAppointments.removeAll(expiredAppointments);
        for (BookCopy copy : expiredCopiesAtAo) {
            copy.moveTo(LibraryBookState.BOOKSHELF, currentDate, null);
            moves.add(new LibraryMoveInfo(copy.getBookNumber(),
                LibraryBookState.APPOINTMENT_OFFICE, LibraryBookState.BOOKSHELF));
        }
        checkAndHandleExpiredAppointments();
        int count = bookCopies.size() / 10 + 1;
        for (Map.Entry<LibraryBookIsbn,List<User>> entry : pendingReservationsByIsbn.entrySet()) {
            LibraryBookIsbn isbn = entry.getKey();
            List<User> users = entry.getValue();
            List<User> temp = new ArrayList<>();
            for (User user : users) {
                boolean admitted = (count != 0);
                if (admitted) {
                    BookCopy availableCopy = null;
                    for (BookCopy copy : bookCopies.values()) {
                        if (copy.getIsbn().equals(isbn) &&
                            copy.getCurrentLocation() == LibraryBookState.BOOKSHELF) {
                            availableCopy = copy;
                            break;
                        }
                    }
                    if (availableCopy != null) {
                        activeAppointments.add(new Appointment(user,availableCopy,
                            currentDate,currentDate.plusDays(5)));
                        temp.add(user);
                        count--;
                        availableCopy.moveTo(LibraryBookState.APPOINTMENT_OFFICE,currentDate, user);
                        moves.add(new LibraryMoveInfo(availableCopy.getBookNumber(),
                            LibraryBookState.BOOKSHELF, LibraryBookState.APPOINTMENT_OFFICE,
                            user.getStudentId()));
                    }
                }
            }
            users.removeAll(temp);
        }
        LibraryIO.PRINTER.move(currentDate, moves);
    }

    private void openHelp(List<LibraryMoveInfo> moves) {
        List<Appointment> expiredAppointments = new ArrayList<>();
        List<BookCopy> expiredCopiesAtAo = new ArrayList<>();
        comeBackCopies(moves);
        for (Appointment app : activeAppointments) {
            if (app.isExpired(currentDate)) {
                expiredAppointments.add(app);
                if (app.getBookCopy() != null &&
                    app.getBookCopy().getCurrentLocation()
                    == LibraryBookState.APPOINTMENT_OFFICE) {
                    expiredCopiesAtAo.add(app.getBookCopy());
                }
                User user = app.getUser();
                user.update(-15);
                user.removeReservedCopy(findBookType(app.getBookCopy().getIsbn()));
            }
        }
        activeAppointments.removeAll(expiredAppointments);
        for (BookCopy copy : expiredCopiesAtAo) {
            copy.moveTo(LibraryBookState.BOOKSHELF, currentDate, null);
            moves.add(new LibraryMoveInfo(copy.getBookNumber(),
                LibraryBookState.APPOINTMENT_OFFICE, LibraryBookState.BOOKSHELF));
        }
        checkAndHandleExpiredAppointments();
        ArrayList<BookCopy> hotCopies = new ArrayList<>();
        for (Book book : hotBooks.values()) {
            for (BookCopy copy : bookCopies.values()) {
                if (copy.getIsbn().equals(book.getIsbn()) &&
                    copy.getCurrentLocation() == LibraryBookState.BOOKSHELF) {
                    hotCopies.add(copy);
                }
            }
        }
        for (BookCopy copy : hotCopies) {
            copy.moveTo(LibraryBookState.HOT_BOOKSHELF, currentDate, null);
            moves.add(new LibraryMoveInfo(copy.getBookNumber(),
                LibraryBookState.BOOKSHELF, LibraryBookState.HOT_BOOKSHELF));
        }
        hotBooks.clear();
    }

    private void comeBackCopies(List<LibraryMoveInfo> moves) {
        List<BookCopy> broCopies = new ArrayList<>();
        for (BookCopy copy : bookCopies.values()) {
            if (copy.getCurrentLocation() == LibraryBookState.BORROW_RETURN_OFFICE) {
                broCopies.add(copy);
            }
        }
        for (BookCopy copy : broCopies) {
            copy.moveTo(LibraryBookState.BOOKSHELF, currentDate, null);
            moves.add(new LibraryMoveInfo(copy.getBookNumber(),
                LibraryBookState.BORROW_RETURN_OFFICE, LibraryBookState.BOOKSHELF));
        }
        broCopies.clear();
        for (BookCopy copy : bookCopies.values()) {
            if (copy.getCurrentLocation() == LibraryBookState.READING_ROOM) {
                broCopies.add(copy);
            }
        }
        for (BookCopy copy : broCopies) {
            User user = copy.getHolder();
            user.update(-10);
            copy.beReturned();
            copy.moveTo(LibraryBookState.BOOKSHELF, currentDate, null);
            moves.add(new LibraryMoveInfo(copy.getBookNumber(),
                LibraryBookState.READING_ROOM, LibraryBookState.BOOKSHELF));
        }
        broCopies.clear();
        for (BookCopy copy : bookCopies.values()) {
            if (copy.getCurrentLocation() == LibraryBookState.HOT_BOOKSHELF) {
                broCopies.add(copy);
            }
        }
        for (BookCopy copy : broCopies) {
            copy.moveTo(LibraryBookState.BOOKSHELF, currentDate, null);
            moves.add(new LibraryMoveInfo(copy.getBookNumber(),
                LibraryBookState.HOT_BOOKSHELF, LibraryBookState.BOOKSHELF));
        }
    }

    private void checkAndHandleExpiredAppointments() {
        List<Appointment> expiredAppointments = new ArrayList<>();
        for (Appointment app : activeAppointments) {
            if (app.isExpired(currentDate)) {
                expiredAppointments.add(app);
            }
        }
        activeAppointments.removeAll(expiredAppointments);
    }

    private void checkUserHeld() {
        int days;
        int borrowDays;
        for (BookCopy copy:bookCopies.values()) {
            if (copy.getIsbn().isTypeB()) { days = 30; }
            else { days = 60; }
            if (copy.getBorrowedDate() == null) { continue; }
            borrowDays = (int)ChronoUnit.DAYS.between(copy.getBorrowedDate(), currentDate);
            if (borrowDays > days) {
                copy.getHolder().update(-5 *
                    (int)ChronoUnit.DAYS.between(copy.getLastPunishedDate(), currentDate));
                copy.bePunished(currentDate);
            }
        }
    }
}
