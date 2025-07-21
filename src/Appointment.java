import java.time.LocalDate;

public class Appointment {
    private User user;
    private BookCopy bookCopy;
    private LocalDate reservedDate;
    private LocalDate expiryDate; // 预约失效日期(可以取书的最后一天)

    public Appointment(User user, BookCopy bookCopy, LocalDate reservedDate, LocalDate expiryDate) {
        this.user = user;
        this.bookCopy = bookCopy;
        this.reservedDate = reservedDate;
        this.expiryDate = expiryDate;
    }

    public User getUser() {
        return user;
    }

    public BookCopy getBookCopy() {
        return bookCopy;
    }

    public boolean isExpired(LocalDate currentDate) {
        return currentDate.isAfter(expiryDate);
    }

    @Override
    public String toString() {
        return "Appointment{" +
                "user=" + (user != null ? user.getStudentId() : "null") +
                ", bookCopy=" + (bookCopy != null ? bookCopy.getBookNumber() : "null") +
                ", reservedDate=" + reservedDate +
                ", expiryDate=" + expiryDate +
                '}';
    }
}
