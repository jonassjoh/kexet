import java.util.ArrayList;

public class ReviewTokenizer {

	private static boolean dev = !true;
	
	public static void tokenizeAllTitles(ArrayList<User> users) {
		for(User u : users) {
			tokenizeMoviesTitles(u.getReviews());
			System.out.println("Finished tokenizing all movies for " + u.getName());
			if(dev) break;
		}
	}
	
	public static void tokenizeMoviesTitles(ArrayList<Movie> movies) {
		int i = 0;
		for(Movie m : movies) {
			tokenizeTitle(m);
			if(dev && i>2) break;
			i++;
		}
	}
	
	public static void tokenizeTitle(Movie movie) {
		movie.review = movie.review.replaceAll("\"?"+movie.title+"(.?\")?\"?", "_MOVIE");
	}
}
