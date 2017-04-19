import java.io.File;

import java.io.IOException;
import java.util.ArrayList;

import java.sql.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class FileScraper {

	private ArrayList<Movie> ids = new ArrayList<>();
	private ArrayList<Movie> unsure = new ArrayList<>();

	private final static String API_OMDB = "http://www.omdbapi.com/?s=";

	private ArrayList<User> users;
	private final static String DB_URL = "jdbc:mysql://127.0.0.1:3306/kexet";
	private final static String DB_USER = "";
	private final static String DB_PASS = "";
	private Connection con;

	public FileScraper() {
		try {
			users = new ArrayList<>();
			File file = new File("data");

			if (con == null) {
				try {
					Class.forName("com.mysql.jdbc.Driver");
					con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
					con.setAutoCommit(false);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			createUsers(file.listFiles());
			makeAll(users);
			System.out.println("\nDone.\nAdded " + ids.size() + " titles to ids");
			System.out.println("\nWas unsure about " + unsure.size() + " titles");
			System.out.println();
			for (Movie m : unsure)
				System.out.println(m.title + " (" + m.year + ")");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	boolean dev = false;

	private void makeMovie(Movie movie, ArrayList<Movie> failed, User user) {
		// INSERT INTO `movies` (`imdb`, `title`, `year`) VALUES ('tt0112462',
		// 'Batman Forever', '1995');

		if (dev)
			return;
		// dev = true;

		try {
			if (getOmdb(movie)) {
				sqlAdd(movie, user);
				sqlAddReview(movie, user);
			}
		} catch (SQLException e) {
			failed.add(movie);

			e.printStackTrace();
		} catch (IOException e) {
			failed.add(movie);
			e.printStackTrace();
		} catch (ParseException e) {
			failed.add(movie);
			e.printStackTrace();
		}
	}

	private boolean getOmdb(Movie movie) throws IOException, ParseException {
		for (Movie m : ids)
			if (m.title.equals(movie.title) && m.year.equals(movie.year)) {
				movie.id = m.id;
				return true;
			}

		System.out.print("Calling OMDB for \"" + movie.title + " (" + movie.year + ")\"...");
		String doc = Jsoup.connect(API_OMDB + movie.title + "&y=" + movie.year).timeout(LetterboxdScraper.TIMEOUT)
				.ignoreContentType(true).execute().body();
		System.out.println("Done.");

		JSONObject search = (JSONObject) new JSONParser().parse(doc);

		if (((String) search.get("Response")).equals("False")) {
			unsure.add(movie);
			System.out.println("Could not find title " + movie.title + " (" + movie.year + ")");
			return false;
		}

		int results = Integer.parseInt((String) search.get("totalResults"));

		JSONArray searchRes = (JSONArray) search.get("Search");

		if (results > 1) {
			for (int i = 0; i < searchRes.size(); i++) {
				search = (JSONObject) searchRes.get(i);
				if (search.get("Title").equals(movie.title)) {
					movie.id = (String) search.get("imdbID");
					ids.add(movie);
					return true;
				}
			}
			unsure.add(movie);
			System.out.println("I was unsure");
			return false;
		}
		search = (JSONObject) searchRes.get(0);
		movie.id = (String) search.get("imdbID");

		ids.add(movie);
		return true;
	}

	private void sqlAdd(Movie movie, User user) {

		try {
			con.setAutoCommit(false);

			String qry = "INSERT INTO `movies` (`imdb`, `title`, `year`) VALUES (?,?,?);";
			PreparedStatement sqlMovie = null;

			try {
				sqlMovie = con.prepareStatement(qry);
				sqlMovie.setString(1, movie.id);
				sqlMovie.setString(2, movie.title);
				sqlMovie.setString(3, movie.year);
				sqlMovie.executeUpdate();
				con.commit();
			} catch (SQLException e) {
				if (sqlMovie != null)
					sqlMovie.close();
				//e.printStackTrace();
			} finally {
				if (sqlMovie != null)
					sqlMovie.close();
			}

		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			//e1.printStackTrace();
		}
	}

	private void sqlAddReview(Movie movie, User user) throws SQLException {
		// INSERT INTO `reviews` (`imdb`, `user`, `rating`, `watched`,
		// `reviewUrl`, `review`) VALUES ('tt0372784', '1', '3', 'datehere',
		// 'urlehere', 'reviewhere');
		String qry = "INSERT INTO `reviews` (`imdb`, `user`, `rating`, `watched`, `reviewUrl`, `review`) VALUES (?,?,?,?,?,?);";
		PreparedStatement sqlMovie = null;

		try {
			sqlMovie = con.prepareStatement(qry);
			sqlMovie.setString(1, movie.id);
			sqlMovie.setInt(2, user.id);
			sqlMovie.setInt(3, Integer.parseInt(movie.rating));
			sqlMovie.setString(4, movie.watched);
			sqlMovie.setString(5, movie.reviewUrl);
			sqlMovie.setString(6, movie.review);
			sqlMovie.executeUpdate();
			con.commit();
		} catch (SQLException e) {
			if (sqlMovie != null)
				sqlMovie.close();
			// e.printStackTrace();
			throw e;
		} finally {
			if (sqlMovie != null)
				sqlMovie.close();
		}
	}

	private void makeAll(ArrayList<User> users) {
		for (User user : users) {
			ArrayList<Movie> failed = new ArrayList<>();
			System.out.println("Adding titles from user " + user.getName());
			makeUser(user, failed);
			if (failed.size() > 0)
				System.out.println("Failed to add " + failed.size() + " titles for user " + user.getPath());
			else
				System.out.println("Added all titles for user " + user.getPath());
		}
	}

	private void makeUser(User user, ArrayList<Movie> failed) {
		for (Movie movie : user.getReviews())
			makeMovie(movie, failed, user);
	}

	private void createUsers(File[] f) throws IOException {
		for (File file : f) {
			Document doc = Jsoup.parse(file, "UTF-8");
			Element userDiv = doc.select("div").first();
			User user = new User(userDiv.attr("name"), userDiv.attr("path"));
			user.setDropped(userDiv.attr("dropped"));

			Elements userMovies = userDiv.children();
			user.nrOfTitles = userMovies.size();

			int i = 0;
			for (Element movieHtml : userMovies) {
				if (i >= 4)
					break;

				Movie movie = new Movie();
				movie.title = movieHtml.select(".title").html();
				movie.year = movieHtml.select(".year").html();
				movie.rating = movieHtml.select(".rating").html();
				movie.watched = movieHtml.select(".watched").html();
				movie.reviewUrl = movieHtml.select(".reviewUrl").html();
				movie.review = movieHtml.select(".review").html();

				user.add(movie);
			}
			try {
				getUserId(user);
				users.add(user);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("Error user " + user.getPath());
			}
		}
	}

	private void getUserId(User user) throws SQLException {
		String qry = "SELECT id FROM users WHERE path='" + user.getPath() + "'";
		Statement statement = con.createStatement();
		ResultSet res = statement.executeQuery(qry);
		res.next();
		user.id = res.getInt("id");
		System.out.println(user.getName() + " " + user.id);
	}

	private void addUsers() throws SQLException {
		// INSERT INTO `users` (`id`, `name`, `path`, `dropped`) VALUES ('1',
		// 'Jonas', '/aosdasd/', '3');
		if (con == null) {
			try {
				Class.forName("com.mysql.jdbc.Driver");
				con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}

		con.setAutoCommit(false);

		int id = 1;
		for (User u : users) {
			String qry = "INSERT INTO `users` (`id`, `name`, `path`, `nrOfTitles`, `dropped`) VALUES (?,?,?,?,?);";
			PreparedStatement sqlMovie = null;

			try {
				sqlMovie = con.prepareStatement(qry);
				sqlMovie.setInt(1, id);
				sqlMovie.setString(2, u.getName());
				sqlMovie.setString(3, u.getPath());
				sqlMovie.setInt(4, u.nrOfTitles);
				sqlMovie.setInt(5, u.getDropped());
				sqlMovie.executeUpdate();
				con.commit();
				id++;
			} catch (SQLException e) {
				if (sqlMovie != null)
					sqlMovie.close();
				e.printStackTrace();
				throw e;
			} finally {
				if (sqlMovie != null)
					sqlMovie.close();
			}
		}
	}

}
