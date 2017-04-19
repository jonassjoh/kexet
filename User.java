import java.util.ArrayList;

public class User {
	private ArrayList<Movie> review;
	private String name;
	private String path;
	private int dropped = 0;
	public int nrOfTitles = 0;
	public int id = -1;

	public User(String name, String path) {
		this.name = name;
		this.path = path;
		review = new ArrayList<>();
	}

	public void add(Movie movie) {
		review.add(movie);
	}

	public ArrayList<Movie> getReviews() {
		return review;
	}

	public String getName() {
		return name;
	}

	public String getPath() {
		return path;
	}

	public String toString() {
		return getPath();
	}

	public void addDropped() {
		dropped++;
	}
	
	public void setDropped(String s) {
		dropped = Integer.parseInt(s);
	}

	public int getDropped() {
		return dropped;
	}
}