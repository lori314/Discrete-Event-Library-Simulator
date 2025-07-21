import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import com.oocourse.library3.LibraryBookId;
import com.oocourse.library3.LibraryBookIsbn;
import com.oocourse.library3.LibraryBookState;
import com.oocourse.library3.LibraryTrace;

public class BookCopy {
    private LibraryBookId bookNumber;
    private LibraryBookState currentLocation;
    private User holder;
    private LocalDate borrowedDate;
    private LocalDate lastPunishedDate;
    private List<LibraryTrace> movementHistory;

    public BookCopy(LibraryBookId bookNumber) {
        this.bookNumber = bookNumber;
        this.currentLocation = LibraryBookState.BOOKSHELF; // 初始位置是书架
        this.holder = null;
        this.borrowedDate = null;
        this.lastPunishedDate = null;
        this.movementHistory = new ArrayList<>();
    }

    public void beBorrowed(LocalDate borrowedDate, User holder) {
        this.borrowedDate = borrowedDate;
        this.holder = holder;
        if (bookNumber.isTypeB()) {
            this.lastPunishedDate = borrowedDate.plusDays(30);
        }
        else if (bookNumber.isTypeC()) {
            this.lastPunishedDate = borrowedDate.plusDays(60);
        }
    }

    public void beReturned() {
        this.holder = null;
        this.borrowedDate = null;
        this.lastPunishedDate = null;
    }

    public void bePunished(LocalDate lastPunishedDate) {
        this.lastPunishedDate = lastPunishedDate;
    }

    public LocalDate getBorrowedDate() {
        return borrowedDate;
    }

    public LocalDate getLastPunishedDate() {
        return lastPunishedDate;
    }

    public User getHolder() {
        return holder;
    }

    public LibraryBookId getBookNumber() {
        return bookNumber;
    }

    public LibraryBookIsbn getIsbn() {
        return bookNumber.getBookIsbn();
    }

    public LibraryBookState getCurrentLocation() {
        return currentLocation;
    }

    public List<LibraryTrace> getMovementHistory() {
        return movementHistory;
    }

    public void moveTo(LibraryBookState newLocation, LocalDate date, User user) {
        LibraryBookState oldLocation = this.currentLocation;
        LibraryTrace record = new LibraryTrace(date, oldLocation, newLocation);
        addTrace(record);
        this.currentLocation = newLocation;
        if (newLocation == LibraryBookState.USER) {
            this.holder = user;
        } else {
            this.holder = null;
        }
    }

    public void addTrace(LibraryTrace record) {
        this.movementHistory.add(record);
    }

    @Override
    public String toString() {
        return "BookCopy{" +
                "bookNumber=" + bookNumber +
                ", currentLocation=" + currentLocation +
                '}';
    }
}
