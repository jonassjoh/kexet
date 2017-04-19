import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class Database {

	private final static String DB_URL = "jdbc:mysql://127.0.0.1:3306/kexet";
	private final static String DB_USER = "";
	private final static String DB_PASS = "";
	private Connection conn;

	public Database() {

	}

	public boolean init() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
			conn.setAutoCommit(false);
			return true;
		} catch (SQLException | ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	public ArrayList<User> getUsers() throws SQLException {
		ArrayList<User> users = new ArrayList<>();

		Statement stmt = conn.createStatement();
		String qry = "SELECT * FROM `users`";
		ResultSet rs = stmt.executeQuery(qry);
		
		while(rs.next()) {
			User user = new User(rs.getString("name"), rs.getString("path"));
			user.setDropped(rs.getString("dropped"));
			user.id = rs.getInt("id");
			user.nrOfTitles = rs.getInt("nrOfTitles");
			users.add(user);
		}
		
		rs.close();
		stmt.close();
		
		return users;
	}

	public void getReviewsForAll(ArrayList<User> users) throws SQLException {
		Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		String qry = "SELECT A.imdb, title, year, user, rating, reviewUrl, review FROM `movies` A INNER JOIN `reviews` B ON A.imdb=B.imdb";
		ResultSet rs = stmt.executeQuery(qry);

		rs.last();
		int rows = rs.getRow();
		rs.beforeFirst();
		
		System.out.println("Found " + rows + " reviews.");
		
		int r = 0;
		int r2 = 0;
		// STEP 5: Extract data from result set
		while (rs.next()) {
			String imdb = rs.getString("imdb");
			String title = rs.getString("title");
			String year = rs.getString("year");
			int userId = rs.getInt("user");
			String rating = rs.getString("rating");
			String reviewUrl = rs.getString("reviewUrl");
			String review = rs.getString("review");
			
			Movie movie = new Movie();
			movie.id = imdb;
			movie.title = title;
			movie.year = year;
			movie.rating = rating;
			movie.reviewUrl = reviewUrl;
			movie.review = review;
			
			for(User u : users) if(u.id == userId) {
				u.add(movie);
			}
		}
		// STEP 6: Clean-up environment
		rs.close();
		stmt.close();
	}
}
