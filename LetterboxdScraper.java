import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class LetterboxdScraper {
	
	private static class URL {
		public static String POPULAR_USERS = "https://letterboxd.com/reviewers/popular/";
		
		public static class PROFILE {
			/* BASE_BEFORE + userName + BASE_AFTER */
			public static String BASE_BEFORE = "https://letterboxd.com";
			public static String BASE_AFTER = "films/reviews/";
			public static String PAGE = "page/";
			public static String PAGE_AFTER = "/";
		}
		public static String REVIEW_FULL_BASE = "https://letterboxd.com";
	}
	
	private static class CSS {
		public static String PAGES = ".pagination .paginate-pages .paginate-page:last-child a";
		public static String MOVIES = ".film-detail .film-detail-content";
		public static String POPULAR_USER_PATH = ".person-table .table-person .person-summary .title-3 a";
		public static class MOVIE {
			public static String TITLE = ".headline-2 a:first-child";
			public static String YEAR = ".headline-2 a:last-child";
			public static String RATING = ".attribution-block .attribution .rating";
			public static String WATCHED = ".attribution-block .attribution .content-metadata ._nobr";
			public static String REVIEW = ".body-text";
			public static String REVIEW_URL_CLASS = "data-full-text-url";
		}
	}
	
	public final static int TIMEOUT = 30*1000;
	
	public static void main(String[] args) {
		// new LetterboxdScraper();
		// new FileScraper();
		new SentimentAnalysis();
	}
	
	public LetterboxdScraper() {
		
		try {
			// TODO: Save progress while scraping?
			scrapeAll( getAllUsers() );
			
			// TODO: Add to database
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private ArrayList<User> getAllUsers() throws IOException {

		ArrayList<User> allUsers = new ArrayList<>();
	
		Document doc = Jsoup.connect(URL.POPULAR_USERS).timeout(TIMEOUT).get();
		Elements usersHtml = doc.select(CSS.POPULAR_USER_PATH);
		
		for(Element e : usersHtml) {
			User u = new User(e.html(), e.attr("href"));
			allUsers.add(u);
		}
		
		return allUsers;
	}
	
	private void scrapeAll(ArrayList<User> user) throws IOException {
		System.out.println("--Scraping all ("+user.size()+") users(!)--");
		int current = 1;
		for(User u : user) {
			System.out.print("("+current+++"/"+user.size()+") ");
			
			if(current == 2) {
				System.out.println("Skipped "+u.getName());
				continue;
			}
			
			scrapeUser(u, true);
		}
		System.out.println();
		System.out.println("Finsihed scraping all users.");
	}
	
	private void scrapeUser(User user, boolean log) throws IOException {
		String url = URL.PROFILE.BASE_BEFORE + user + URL.PROFILE.BASE_AFTER;

		Document doc = Jsoup.connect(url).timeout(TIMEOUT).get();
		
		Element pages = doc.select(CSS.PAGES).first();
		
		int totPages = Integer.parseInt(pages.html());
		
		System.out.println(user + " has " + totPages + " pages of reviews.");

		for(int current = 1; current <= totPages; current++) {

			System.out.println("Scraping page " + current + "/" + totPages + "  " + url);
			
			scrapeMovies(doc, user);
			
			if(current < totPages) {
				url = URL.PROFILE.BASE_BEFORE + user + URL.PROFILE.BASE_AFTER + URL.PROFILE.PAGE + (current + 1) + URL.PROFILE.PAGE_AFTER;
				doc = Jsoup.connect(url).timeout(TIMEOUT).timeout(TIMEOUT).get();
			}
		}
		
		if(log) {
			System.out.print("Logging...");
			try {
				PrintWriter writer = new PrintWriter("data/"+user.getPath().substring(1, user.getPath().length()-1)+".data","UTF-8");
				
				writer.write("<div name=\""+user.getName()+"\" path=\""+user.getPath()+"\" titles=\""+user.getReviews().size()+"\" dropped=\""+user.getDropped()+"\">\n");
				for(Movie movie : user.getReviews()) {
					writer.write("<div>\n"
									+ "<div class=\"title\">"+movie.title+"</div>\n"
									+ "<div class=\"year\">"+movie.year+"</div>\n"
									+ "<div class=\"rating\">"+movie.rating+"</div>\n"
									+ "<div class=\"watched\">"+movie.watched+"</div>\n"
									+ "<div class=\"reviewUrl\">"+movie.reviewUrl+"</div>\n"
									+ "<div class=\"review\">"+movie.review+"</div>\n"
								+ "</div>\n");
				}
				writer.write("</div>\n");
				
				writer.close();
				
				System.out.println("Done.");
			} catch (IOException e) {
				System.out.println("Failed to log user. Aborting.");
				e.printStackTrace();
				System.exit(0);
			}
		}
		
		System.out.println("Finished scraping user " + user);
	}
	
	private void scrapeMovies(final Document doc, final User user) throws IOException {
		final Elements allMovies = doc.select(CSS.MOVIES);
		
		allMovies.forEach((movieHtml) -> {
			final Movie movie = new Movie();
			movie.title = movieHtml.select(CSS.MOVIE.TITLE).first().html();
			movie.year = movieHtml.select(CSS.MOVIE.YEAR).first().html();
			try {
				movie.rating = movieHtml.select(CSS.MOVIE.RATING).first().className();
			} catch (Exception e) {
				System.out.println("Dropped movie -> NO RATING");
				user.addDropped();
				return;
			}
			movie.trimRating();
			movie.watched = movieHtml.select(CSS.MOVIE.WATCHED).first().html();
			movie.reviewUrl = movieHtml.select(CSS.MOVIE.REVIEW).first().attr(CSS.MOVIE.REVIEW_URL_CLASS);
			
			user.add(movie);
			
			try {
				getMovieReview(movie);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}
	
	int i = 0;
	private void getMovieReview(Movie movie) throws IOException {
		final String url = URL.REVIEW_FULL_BASE + movie.reviewUrl;
		Document doc = Jsoup.connect(url).timeout(TIMEOUT).get();
		movie.review = doc.body().select("p").html();
	}
}
