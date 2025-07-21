import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.oocourse.library3.LibraryBookIsbn;
import com.oocourse.library3.LibraryBookIsbn.Type;

// 表示用户
public class User {
    private String studentId;
    private List<BookCopy> heldCopies;
    private List<Book> reservedCopies;
    private int borrowedBCount;
    private Set<LibraryBookIsbn> borrowedCIsbns;
    private BookCopy copy;
    private int credits;
    // TODO: 可以增加信用积分属性，如果题目后续需要

    public User(String studentId) {
        this.studentId = studentId;
        this.heldCopies = new ArrayList<>();
        this.reservedCopies = new ArrayList<>();
        this.borrowedBCount = 0;
        this.borrowedCIsbns = new HashSet<>();
        this.copy = null;
        this.credits = 100;
    }

    public String getStudentId() {
        return studentId;
    }

    public void orderNewBook() {}

    public void getOrderedBook() {}

    public List<Book> getReservedCopies() {
        return reservedCopies;
    }

    public void update(int x) {
        int updateCredits = credits + x;
        if (updateCredits > 180) {
            updateCredits = 180;
        }
        else if (updateCredits < 0) {
            updateCredits = 0;
        }
        credits = updateCredits;
    }

    public int getCredits() {
        return credits;
    }

    public boolean canBorrow(Book book) {
        Type category = book.getCategory();
        LibraryBookIsbn isbn = book.getIsbn();
        if (credits < 60) {
            return false;
        }
        if (category == Type.A) {
            return false;
        } else if (category == Type.B) {
            return borrowedBCount < 1;
        } else if (category == Type.C) {
            return !borrowedCIsbns.contains(isbn);
        }
        return true;
    }

    public boolean canRead(Book book) {
        if (credits <= 0) {
            return false;
        }
        if (credits < 40 && book.getCategory() == Type.A) {
            return false;
        }
        return copy == null;
    }

    public void read(BookCopy copy) {
        this.copy = copy;
    }

    public void restored() {
        this.copy = null;
    }

    public void addHeldCopy(BookCopy copy) {
        heldCopies.add(copy);
        Type category = copy.getIsbn().getType();
        if (category == Type.B) {
            borrowedBCount++;
        } else if (category == Type.C) {
            borrowedCIsbns.add(copy.getIsbn());
        }
    }

    public void removeHeldCopy(BookCopy copy) {
        heldCopies.remove(copy);
        Type category = copy.getIsbn().getType();
        if (category == Type.B) {
            borrowedBCount--;
        } else if (category == Type.C) {
            borrowedCIsbns.remove(copy.getIsbn());
        }
    }

    public boolean canReserve(Book book) {
        Type category = book.getCategory();
        LibraryBookIsbn isbn = book.getIsbn();
        if (credits < 100) {
            return false;
        }
        if (category == Type.A) {
            return false;
        } else if (category == Type.B) {
            return borrowedBCount < 1;
        } else if (category == Type.C) {
            return !borrowedCIsbns.contains(isbn);
        }
        return true;
    }

    public void addReservedCopy(Book book) {
        reservedCopies.add(book);
    }

    public void removeReservedCopy(Book book) {
        reservedCopies.remove(book);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof User) {
            User other = (User) obj;
            return studentId.equals(other.studentId);
        }
        return false;
    }
}
