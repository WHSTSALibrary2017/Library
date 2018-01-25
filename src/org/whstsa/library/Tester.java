package org.whstsa.library;

import org.json.JSONArray;
import org.json.JSONObject;
import org.whstsa.library.api.BookType;
import org.whstsa.library.api.IPerson;
import org.whstsa.library.api.Serializable;
import org.whstsa.library.api.books.IBook;
import org.whstsa.library.api.exceptions.OutOfStockException;
import org.whstsa.library.api.impl.Book;
import org.whstsa.library.api.impl.Person;
import org.whstsa.library.api.impl.library.Checkout;
import org.whstsa.library.api.impl.library.Library;
import org.whstsa.library.api.impl.library.Member;
import org.whstsa.library.api.library.ICheckout;
import org.whstsa.library.api.library.ILibrary;
import org.whstsa.library.api.library.IMember;
import org.whstsa.library.db.Loader;
import org.whstsa.library.db.ObjectDelegate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by eric on 11/18/17.
 */
public class Tester {

    public static final boolean VERBOSE = false;

    private static final Random RANDOM = new Random();
    private static final String SEPARATOR = "----=====================----";
    private static final String SEPARATOR_MINI = "--==========--";

    private final ILibrary library;

    private static Tester tester;

    public Tester() {
        this.library = new Library("POPTROPICA");
        Loader.getLoader().loadLibrary(library);
        this.populatePeople();
        this.populateBooks();
        this.addPeopleToAllLibraries();
        this.addBooksToAllLibraries();
        this.checkoutRandomAllLibraries();
        this.advanceTime(20);
        this.checkoutRandomAllLibraries();
        this.testUnregisteredBookChecks();
        this.testAllReturns();
        this.advanceTime(40);
        this.addToAllUsers(100);
        this.printAllBalances();
        this.testAllReturns();
        this.printAllBalances();
        Loader.getLoader().load(this.computeJSON());
        this.testDeregistrationWhileHavingBooks();
    }

    public static void print(JSONObject... objects) {
        if (!VERBOSE) {
            return;
        }
        for (JSONObject object : objects) {
            System.out.println(object.toString(4));
        }
    }

    public static void print(Serializable... objects) {
        if (!VERBOSE) {
            return;
        }
        for (Serializable object : objects) {
            Tester.print(object.toJSON());
        }
    }

    public static void print(String... messages) {
        if (!VERBOSE) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        for (String message : messages) {
            builder.append(message);
            builder.append('\n');
        }
        System.out.println(builder.toString());
    }

    private void populatePeople() {
        System.out.println("Populating people");
        Map<String, String> names = new HashMap<>();
        names.put("Eric", "Rabil");
        names.put("Cameron", "Newborn");
        names.put("Andre", "Roberts");
        names.put("Bradley", "Woods");
        for (String firstName : names.keySet()) {
            Loader.getLoader().loadPerson(new Person(firstName, names.get(firstName), RANDOM.nextBoolean()));
        }
    }

    private void populateBooks() {
        System.out.println("Populating books");
        IBook book1 = new Book("Gone With The Wind", "Super thicc 42", BookType.FICTION);
        IBook book2 = new Book("A Wrinkle in Time", "Super thiccc 43", BookType.HORROR);
        IBook book3 = new Book("Diary of a Wimpy Kid", "Super thiccccccc 44", BookType.NONFICTION);
        Loader.getLoader().loadBook(book1);
        Loader.getLoader().loadBook(book2);
        Loader.getLoader().loadBook(book3);
    }

    private void testDeregistrationWhileHavingBooks() {
        IMember member = this.library.getMembers().get(0);
        IBook book = this.library.getBooks().get(0);
        try {
            ICheckout checkout = this.library.reserveBook(member, book);
        } catch (OutOfStockException ex) {
            System.out.format("Failed: %s", ex.getMessage());
        }
        System.out.println("Removing member while they have a book checked-out");
        try {
            this.library.removeMember(member);
        } catch (Exception ex) {
            System.out.format("Failed: %s", ex.getMessage());
        }
    }

    private void addPeopleToLibrary(ILibrary library) {
        System.out.format("Adding all users to library %s", library.getName());
        for (IPerson person : ObjectDelegate.getPeople()) {
            library.addMember(person);
        }
    }

    private void addPeopleToAllLibraries() {
        System.out.println("Adding all users to all libraries");
        for (ILibrary library : ObjectDelegate.getLibraries()) {
            this.addPeopleToLibrary(library);
        }
    }

    private void addBooksToLibrary(ILibrary library) {
        System.out.format("Adding all books to library %s", library.getName());
        for (IBook book : ObjectDelegate.getBooks()) {
            library.addBook(book);
        }
    }

    private void printAllBalances() {
        System.out.println(SEPARATOR);
        for (IPerson person : ObjectDelegate.getPeople()) {
            System.out.format("%s has $%.2f", person.getName(), person.getWallet());
        }
        System.out.println(SEPARATOR);
    }

    private void addBooksToAllLibraries() {
        System.out.println("Adding all books to all libraries");
        for (ILibrary library : ObjectDelegate.getLibraries()) {
            this.addBooksToLibrary(library);
        }
    }

    private void checkin(IMember member, ICheckout checkout) {
        System.out.format("Checking in %s", checkout.getBook().getTitle());
        try {
            member.checkInAndPayFines(checkout);
            System.out.println("Successfully checked-in the book");
            System.out.println("Testing re-checkin returns");
            member.checkInAndPayFines(checkout);
        } catch (Exception ex) {
            System.out.format("Failed to check-in the book: %s", ex.getMessage());
        }
    }

    private void testReturns(ILibrary library) {
        System.out.println(SEPARATOR);
        System.out.println("Testing returns");
        for (IMember member : library.getMembers()) {
            System.out.println(SEPARATOR_MINI);
            System.out.format("Testing returns for %s", member.getPerson().getName());
            List<ICheckout> checkouts = member.getCheckouts().stream()
                    .filter(checkout -> !checkout.isReturned())
                    .collect(Collectors.toList());
            for (ICheckout checkout : checkouts) {
                this.checkin(member, checkout);
            }
            System.out.println("Testing mismatch returns");
            Member dummyMember = new Member(new Person("Bich", "bich", false), new Library("Bich"));
            Checkout checkout = new Checkout(dummyMember, ObjectDelegate.getBooks().get(0));
            this.checkin(member, checkout);
            System.out.println(SEPARATOR_MINI);
        }
        System.out.println(SEPARATOR);
    }

    private void testAllReturns() {
        System.out.println("Testing all returns");
        ObjectDelegate.getLibraries().forEach(this::testReturns);
    }

    private void testUnregisteredBookCheck(ILibrary library) {
        IMember member = library.getMembers().get(0);
        System.out.println("Testing checkout for unregistered book");
        ICheckout checkout = new Checkout(member, new Book("BITCH DIE", "THICCY THICC", BookType.HORROR));
        try {
            member.checkout(checkout);
        } catch (Exception ex) {
            System.out.format("Failed to check-out: %s", ex.getMessage());
        }
    }

    private void testUnregisteredBookChecks() {
        System.out.println(SEPARATOR);
        System.out.println("Testing unregistered book checks");
        for (ILibrary library : ObjectDelegate.getLibraries()) {
            this.testUnregisteredBookCheck(library);
        }
        System.out.println(SEPARATOR);
    }

    private void addToAllUsers(double money) {
        System.out.format("Adding $%.2f to all users", money);
        for (IPerson person : ObjectDelegate.getPeople()) {
            System.out.format("Adding $%.2f to %s", money, person.getName());
            person.addMoney(money);
        }
    }

    private void checkoutRandomBookEachMember(ILibrary library) {
        System.out.format("Checking out a random book for each member of library %s", library.getName());
        for (IMember member : library.getMembers()) {
            IBook book = library.getBooks().get(RANDOM.nextInt(library.getBooks().size()));
            try {
                library.reserveBook(member, book);
            } catch (OutOfStockException ex) {
                System.out.format("Failed: %s", ex.getMessage());
            }
        }
    }

    private void advanceTime(int days) {
        System.out.format("Advancing time by %d days", days);
        Date currentDate = World.getDate();
        Calendar cal = Calendar.getInstance();
        cal.setTime(currentDate);
        cal.add(Calendar.DAY_OF_MONTH, days);
        World.setDate(cal.getTime());
    }

    private void checkoutRandomAllLibraries() {
        System.out.println("Checking out a random book for each member of all libraries");
        for (ILibrary library : ObjectDelegate.getLibraries()) {
            this.checkoutRandomBookEachMember(library);
        }
    }

    public JSONObject computeJSON() {
        System.out.println("Computing JSON");
        return Loader.getLoader().computeJSON();
    }
}
