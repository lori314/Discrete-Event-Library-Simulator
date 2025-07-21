import com.oocourse.library3.LibraryBookIsbn;
import com.oocourse.library3.LibraryBookIsbn.Type;

public class Book {
    private LibraryBookIsbn isbn;
    private int initialCopyCount;
    private Type category;

    public Book(LibraryBookIsbn isbn, int initialCopyCount) {
        this.isbn = isbn;
        this.initialCopyCount = initialCopyCount;
        this.category = isbn.getType();
    }

    public LibraryBookIsbn getIsbn() {
        return isbn;
    }

    public Type getCategory() {
        return category;
    }

    @Override
    public String toString() {
        return "Book{" +
                "isbn=" + isbn +
                ", initialCopyCount=" + initialCopyCount +
                ", category=" + category +
                '}';
    }
}
