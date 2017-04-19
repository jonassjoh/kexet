
public class Movie {
	public String title;
	public String year;
	public String rating;
	public String watched;
	public String reviewUrl;
	public String review;
	public String id;

	public void trimRating() {
		rating = rating.split(" ")[1].split("-")[1];
	}

	public String toString() {
		return title + " (" + year + ")";
	}
}